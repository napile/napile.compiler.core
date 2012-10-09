/*
 * Copyright 2010-2012 napile.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.napile.compiler.lang.resolve.processors.members;

import static org.napile.compiler.lang.diagnostics.Errors.FINAL_CLASS_OBJECT_UPPER_BOUND;
import static org.napile.compiler.lang.diagnostics.Errors.FINAL_UPPER_BOUND;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.ParameterDescriptor;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptorImpl;
import org.napile.compiler.lang.descriptors.Visibility;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.psi.NapileParameterList;
import org.napile.compiler.lang.psi.NapilePsiUtil;
import org.napile.compiler.lang.psi.NapileTypeParameter;
import org.napile.compiler.lang.psi.NapileTypeParameterListOwner;
import org.napile.compiler.lang.psi.NapileTypeReference;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.TraceBasedRedeclarationHandler;
import org.napile.compiler.lang.resolve.processors.DescriptorResolver;
import org.napile.compiler.lang.resolve.processors.TypeResolver;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.resolve.scopes.WritableScope;
import org.napile.compiler.lang.resolve.scopes.WritableScopeImpl;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.TypeUtils;
import org.napile.compiler.lang.types.checker.JetTypeChecker;
import org.napile.compiler.lexer.NapileTokens;
import com.google.common.collect.Lists;

/**
 * @author VISTALL
 * @date 18:42/08.10.12
 */
public class TypeParameterResolver
{
	static final class UpperBoundCheckerTask
	{
		NapileTypeReference upperBound;
		JetType upperBoundType;
		boolean isClassObjectConstraint;

		private UpperBoundCheckerTask(NapileTypeReference upperBound, JetType upperBoundType, boolean classObjectConstraint)
		{
			this.upperBound = upperBound;
			this.upperBoundType = upperBoundType;
			isClassObjectConstraint = classObjectConstraint;
		}
	}

	@NotNull
	private AnnotationResolver annotationResolver;

	@NotNull
	private TypeResolver typeResolver;

	@NotNull
	private DescriptorResolver descriptorResolver;

	@Inject
	public void setTypeResolver(@NotNull TypeResolver typeResolver)
	{
		this.typeResolver = typeResolver;
	}

	@Inject
	public void setAnnotationResolver(@NotNull AnnotationResolver annotationResolver)
	{
		this.annotationResolver = annotationResolver;
	}

	@Inject
	public void setDescriptorResolver(@NotNull DescriptorResolver descriptorResolver)
	{
		this.descriptorResolver = descriptorResolver;
	}

	public List<TypeParameterDescriptor> resolveTypeParameters(DeclarationDescriptor containingDescriptor, WritableScope extensibleScope, List<NapileTypeParameter> typeParameters, BindingTrace trace)
	{
		List<TypeParameterDescriptor> result = new ArrayList<TypeParameterDescriptor>();
		for(int i = 0, typeParametersSize = typeParameters.size(); i < typeParametersSize; i++)
		{
			NapileTypeParameter typeParameter = typeParameters.get(i);
			result.add(resolveTypeParameter(containingDescriptor, extensibleScope, typeParameter, i, trace));
		}
		return result;
	}

	private TypeParameterDescriptorImpl resolveTypeParameter(DeclarationDescriptor containingDescriptor, WritableScope extensibleScope, NapileTypeParameter typeParameter, int index, BindingTrace trace)
	{
		TypeParameterDescriptorImpl typeParameterDescriptor = new TypeParameterDescriptorImpl(containingDescriptor, annotationResolver.createAnnotationStubs(typeParameter.getModifierList(), trace), typeParameter.hasModifier(NapileTokens.REIFIED_KEYWORD), NapilePsiUtil.safeName(typeParameter.getName()), index);

		extensibleScope.addTypeParameterDescriptor(typeParameterDescriptor);
		trace.record(BindingContext.TYPE_PARAMETER, typeParameter, typeParameterDescriptor);
		return typeParameterDescriptor;
	}

	public void postResolving(@NotNull NapileTypeParameterListOwner declaration, @NotNull JetScope scope, @NotNull List<TypeParameterDescriptor> parameters, @NotNull BindingTrace trace)
	{
		resolveGenericBounds(declaration, scope, parameters, trace);

		resolveConstructors(declaration, scope, parameters, trace);
	}

	private void resolveConstructors(@NotNull NapileTypeParameterListOwner declaration, JetScope scope, List<TypeParameterDescriptor> parameters, BindingTrace trace)
	{
		List<NapileTypeParameter> typeParameters = declaration.getTypeParameters();
		for(int i = 0; i < parameters.size(); i++)
		{
			NapileTypeParameter typeParameter = typeParameters.get(i);
			TypeParameterDescriptor typeParameterDescriptor = parameters.get(i);

			for(NapileParameterList parameterList : typeParameter.getConstructorParameterLists())
			{
				ConstructorDescriptor constructorDescriptor = new ConstructorDescriptor(typeParameterDescriptor, Collections.<AnnotationDescriptor>emptyList(), false);

				WritableScope innerScope = new WritableScopeImpl(scope, constructorDescriptor, new TraceBasedRedeclarationHandler(trace), "TPConstructor descriptor header scope");
				innerScope.changeLockLevel(WritableScope.LockLevel.BOTH);
				List<ParameterDescriptor> parameterDescriptors = descriptorResolver.resolveValueParameters(constructorDescriptor, innerScope, parameterList.getParameters(), trace);

				//constructorDescriptor.setParametersScope(innerScope);
				constructorDescriptor.initialize(Collections.<TypeParameterDescriptor>emptyList(), parameterDescriptors, Visibility.PUBLIC);
				innerScope.changeLockLevel(WritableScope.LockLevel.READING);

				((TypeParameterDescriptorImpl) typeParameterDescriptor).addConstructor(constructorDescriptor);
				((WritableScope)scope).addConstructorDescriptor(constructorDescriptor);
			}
		}
	}

	private void resolveGenericBounds(@NotNull NapileTypeParameterListOwner declaration, JetScope scope, List<TypeParameterDescriptor> parameters, BindingTrace trace)
	{
		List<UpperBoundCheckerTask> deferredUpperBoundCheckerTasks = Lists.newArrayList();

		List<NapileTypeParameter> typeParameters = declaration.getTypeParameters();
		for(int i = 0; i < typeParameters.size(); i++)
		{
			NapileTypeParameter jetTypeParameter = typeParameters.get(i);
			TypeParameterDescriptor typeParameterDescriptor = parameters.get(i);

			for(NapileTypeReference extendsBound : jetTypeParameter.getExtendsBound())
			{
				JetType type = typeResolver.resolveType(scope, extendsBound, trace, false);
				((TypeParameterDescriptorImpl) typeParameterDescriptor).addUpperBound(type);
				deferredUpperBoundCheckerTasks.add(new UpperBoundCheckerTask(extendsBound, type, false));
			}
		}

		for(TypeParameterDescriptor typeParameterDescriptor : parameters)
		{
			((TypeParameterDescriptorImpl) typeParameterDescriptor).addDefaultUpperBound(scope);

			((TypeParameterDescriptorImpl) typeParameterDescriptor).setInitialized();

			/*if(false)
			{
				PsiElement nameIdentifier = typeParameters.get(typeParameterDescriptor.getIndex()).getNameIdentifier();
				if(nameIdentifier != null)
					trace.report(CONFLICTING_UPPER_BOUNDS.on(nameIdentifier, typeParameterDescriptor));
			}  */
		}

		for(UpperBoundCheckerTask checkerTask : deferredUpperBoundCheckerTasks)
			checkUpperBoundType(checkerTask.upperBound, checkerTask.upperBoundType, checkerTask.isClassObjectConstraint, trace);
	}

	private static void checkUpperBoundType(NapileTypeReference upperBound, JetType upperBoundType, boolean isClassObjectConstraint, BindingTrace trace)
	{
		if(!TypeUtils.canHaveSubtypes(JetTypeChecker.INSTANCE, upperBoundType))
		{
			if(isClassObjectConstraint)
			{
				trace.report(FINAL_CLASS_OBJECT_UPPER_BOUND.on(upperBound, upperBoundType));
			}
			else
			{
				trace.report(FINAL_UPPER_BOUND.on(upperBound, upperBoundType));
			}
		}
	}
}
