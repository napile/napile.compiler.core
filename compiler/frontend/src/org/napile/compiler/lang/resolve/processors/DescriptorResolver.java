/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.napile.compiler.lang.resolve.processors;

import static org.napile.compiler.lang.diagnostics.Errors.CONSTRUCTORS_EXPECTED;
import static org.napile.compiler.lang.diagnostics.Errors.NULLABLE_SUPERTYPE;
import static org.napile.compiler.lang.diagnostics.Errors.UPPER_BOUND_VIOLATED;
import static org.napile.compiler.lang.diagnostics.Errors.VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.AsmConstants;
import org.napile.asm.lib.NapileLangPackage;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.descriptors.*;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.diagnostics.Errors;
import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingContextUtils;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.DescriptorUtils;
import org.napile.compiler.lang.resolve.TraceBasedRedeclarationHandler;
import org.napile.compiler.lang.resolve.calls.autocasts.DataFlowInfo;
import org.napile.compiler.lang.resolve.processors.members.AnnotationResolver;
import org.napile.compiler.lang.resolve.processors.members.TypeParameterResolver;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.resolve.scopes.WritableScope;
import org.napile.compiler.lang.resolve.scopes.WritableScopeImpl;
import org.napile.compiler.lang.types.DeferredType;
import org.napile.compiler.lang.types.ErrorUtils;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.TypeSubstitutor;
import org.napile.compiler.lang.types.TypeUtils;
import org.napile.compiler.lang.types.checker.JetTypeChecker;
import org.napile.compiler.lang.types.expressions.ExpressionTypingServices;
import org.napile.compiler.lang.types.expressions.VariableAccessorResolver;
import org.napile.compiler.util.lazy.LazyValue;
import org.napile.compiler.util.lazy.LazyValueWithDefault;
import com.google.common.collect.Lists;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * @author abreslav
 */
public class DescriptorResolver
{
	@NotNull
	private TypeResolver typeResolver;
	@NotNull
	private AnnotationResolver annotationResolver;
	@NotNull
	private ExpressionTypingServices expressionTypingServices;
	@NotNull
	private TypeParameterResolver typeParameterResolver;

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
	public void setExpressionTypingServices(@NotNull ExpressionTypingServices expressionTypingServices)
	{
		this.expressionTypingServices = expressionTypingServices;
	}

	@Inject
	public void setTypeParameterResolver(@NotNull TypeParameterResolver typeParameterResolver)
	{
		this.typeParameterResolver = typeParameterResolver;
	}

	public void resolveMutableClassDescriptor(@NotNull NapileClass classElement, @NotNull MutableClassDescriptor descriptor, BindingTrace trace)
	{
		descriptor.setTypeParameterDescriptors(typeParameterResolver.resolveTypeParameters(descriptor, (WritableScope) descriptor.getScopeForSupertypeResolution(), classElement.getTypeParameters(), trace));

		descriptor.setModality(Modality.resolve(classElement));

		descriptor.setVisibility(Visibility.resolve(classElement));

		trace.record(BindingContext.CLASS, classElement, descriptor);
	}

	public void resolveSupertypesForMutableClassDescriptor(@NotNull NapileSuperListOwner superListOwner, @NotNull MutableClassDescriptor descriptor, BindingTrace trace)
	{
		for(JetType supertype : resolveSupertypes(descriptor.getScopeForSupertypeResolution(), superListOwner, trace))
			descriptor.addSupertype(supertype);
	}

	public List<JetType> resolveSupertypes(@NotNull JetScope scope, @NotNull NapileSuperListOwner superListOwner, BindingTrace trace)
	{
		if(NapileLangPackage.ANY.equals(NapilePsiUtil.getFQName(superListOwner)))  // master object dont have super classes
			return Collections.emptyList();

		List<JetType> result = Lists.newArrayList();
		List<? extends NapileTypeReference> delegationSpecifiers = superListOwner.getSuperTypes();
		if(delegationSpecifiers.isEmpty())
			result.add(TypeUtils.getTypeOfClassOrErrorType(scope, NapileLangPackage.ANY, false));
		else
		{
			Collection<JetType> supertypes = resolveDelegationSpecifiers(scope, delegationSpecifiers, typeResolver, trace, false);
			result.addAll(supertypes);
		}
		return result;
	}

	public Collection<JetType> resolveDelegationSpecifiers(JetScope extensibleScope, List<? extends NapileTypeReference> delegationSpecifiers, @NotNull TypeResolver resolver, BindingTrace trace, boolean checkBounds)
	{
		if(delegationSpecifiers.isEmpty())
			return Collections.emptyList();

		Collection<JetType> result = new ArrayList<JetType>(delegationSpecifiers.size());
		for(NapileTypeReference typeReference : delegationSpecifiers)
		{
			result.add(resolver.resolveType(extensibleScope, typeReference, trace, checkBounds));
			NapileTypeElement typeElement = typeReference.getTypeElement();
			while(typeElement instanceof NapileNullableType)
			{
				NapileNullableType nullableType = (NapileNullableType) typeElement;
				trace.report(NULLABLE_SUPERTYPE.on(nullableType));
				typeElement = nullableType.getInnerType();
			}
		}
		return result;
	}

	@NotNull
	public SimpleMethodDescriptor resolveMethodDescriptor(DeclarationDescriptor containingDescriptor, final JetScope scope, final NapileNamedMethod method, final BindingTrace trace)
	{
		final SimpleMethodDescriptorImpl methodDescriptor = new SimpleMethodDescriptorImpl(containingDescriptor, annotationResolver.bindAnnotations(scope, method, trace), NapilePsiUtil.safeName(method.getName()), CallableMemberDescriptor.Kind.DECLARATION, resolveStatic(method), method.hasModifier(NapileTokens.NATIVE_KEYWORD), false);
		WritableScope innerScope = new WritableScopeImpl(scope, methodDescriptor, new TraceBasedRedeclarationHandler(trace), "Function descriptor header scope");

		List<TypeParameterDescriptor> typeParameterDescriptors = typeParameterResolver.resolveTypeParameters(methodDescriptor, innerScope, method.getTypeParameters(), trace);
		innerScope.changeLockLevel(WritableScope.LockLevel.BOTH);
		typeParameterResolver.postResolving(method, innerScope, typeParameterDescriptors, trace);

		List<CallParameterDescriptor> parameterDescriptors = resolveCallParameters(methodDescriptor, innerScope, method.getCallParameters(), trace);

		innerScope.changeLockLevel(WritableScope.LockLevel.READING);

		final NapileExpression bodyExpression = method.getBodyExpression();
		NapileTypeReference returnTypeRef = method.getReturnTypeRef();
		JetType returnType;
		if(returnTypeRef != null)
			returnType = typeResolver.resolveType(innerScope, returnTypeRef, trace, false);
		else if(method.hasBlockBody())
			returnType = TypeUtils.getTypeOfClassOrErrorType(scope, NapileLangPackage.NULL, false);
		else
		{
			if(bodyExpression != null)
			{
				returnType = DeferredType.create(trace, new LazyValueWithDefault<JetType>(ErrorUtils.createErrorType("Recursive dependency"))
				{
					@Override
					protected JetType compute()
					{
						return expressionTypingServices.inferFunctionReturnType(scope, method, methodDescriptor, trace);
					}
				});
			}
			else
				returnType = ErrorUtils.createErrorType("No type, no body");
		}

		Modality modality = Modality.resolve(method);
		Visibility visibility = Visibility.resolve(method);

		methodDescriptor.initialize(DescriptorUtils.getExpectedThisObjectIfNeeded(containingDescriptor), typeParameterDescriptors, parameterDescriptors, returnType, modality, visibility);

		BindingContextUtils.recordMethodDeclarationToDescriptor(trace, method, methodDescriptor);

		return methodDescriptor;
	}

	@NotNull
	public SimpleMethodDescriptor resolveMacroDescriptor(DeclarationDescriptor containingDescriptor, final JetScope scope, final NapileNamedMacro macro, final BindingTrace trace)
	{
		final SimpleMethodDescriptorImpl methodDescriptor = new SimpleMethodDescriptorImpl(containingDescriptor, annotationResolver.bindAnnotations(scope, macro, trace), NapilePsiUtil.safeName(macro.getName()), CallableMemberDescriptor.Kind.DECLARATION, resolveStatic(macro), false, true);
		WritableScope innerScope = new WritableScopeImpl(scope, methodDescriptor, new TraceBasedRedeclarationHandler(trace), "Function descriptor header scope");

		List<TypeParameterDescriptor> typeParameterDescriptors = typeParameterResolver.resolveTypeParameters(methodDescriptor, innerScope, macro.getTypeParameters(), trace);
		innerScope.changeLockLevel(WritableScope.LockLevel.BOTH);
		typeParameterResolver.postResolving(macro, innerScope, typeParameterDescriptors, trace);

		List<CallParameterDescriptor> parameterDescriptors = resolveCallParameters(methodDescriptor, innerScope, macro.getCallParameters(), trace);

		innerScope.changeLockLevel(WritableScope.LockLevel.READING);

		final NapileExpression bodyExpression = macro.getBodyExpression();
		NapileTypeReference returnTypeRef = macro.getReturnTypeRef();
		JetType returnType;
		if(returnTypeRef != null)
			returnType = typeResolver.resolveType(innerScope, returnTypeRef, trace, false);
		else if(macro.hasBlockBody())
			returnType = TypeUtils.getTypeOfClassOrErrorType(scope, NapileLangPackage.NULL, false);
		else
		{
			if(bodyExpression != null)
			{
				returnType = DeferredType.create(trace, new LazyValueWithDefault<JetType>(ErrorUtils.createErrorType("Recursive dependency"))
				{
					@Override
					protected JetType compute()
					{
						return expressionTypingServices.inferFunctionReturnType(scope, macro, methodDescriptor, trace);
					}
				});
			}
			else
				returnType = ErrorUtils.createErrorType("No type, no body");
		}

		if(bodyExpression != null)
			trace.record(BindingContext.MACRO_BODY, methodDescriptor, bodyExpression);

		Modality modality = Modality.resolve(macro);
		Visibility visibility = Visibility.resolve(macro);

		methodDescriptor.initialize(DescriptorUtils.getExpectedThisObjectIfNeeded(containingDescriptor), typeParameterDescriptors, parameterDescriptors, returnType, modality, visibility);

		BindingContextUtils.recordMethodDeclarationToDescriptor(trace, macro, methodDescriptor);

		return methodDescriptor;
	}

	@NotNull
	public List<CallParameterDescriptor> resolveCallParameters(MethodDescriptor methodDescriptor, WritableScope parameterScope, NapileCallParameter[] valueParameters, BindingTrace trace)
	{
		List<CallParameterDescriptor> result = new ArrayList<CallParameterDescriptor>();
		for(int i = 0, valueParametersSize = valueParameters.length; i < valueParametersSize; i++)
		{
			NapileCallParameter parameter = valueParameters[i];

			CallParameterDescriptor callParameterDescriptor = null;
			if(parameter instanceof NapileCallParameterAsVariable)
			{
				NapileTypeReference typeReference = ((NapileCallParameterAsVariable) parameter).getTypeReference();
	
				JetType type;
				if(typeReference == null)
				{
					trace.report(VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION.on(((NapileCallParameterAsVariable) parameter)));
					type = ErrorUtils.createErrorType("Type annotation was missing");
				}
				else
					type = typeResolver.resolveType(parameterScope, typeReference, trace, false);

				callParameterDescriptor = resolveCallParameterDescriptor(parameterScope, methodDescriptor, ((NapileCallParameterAsVariable) parameter), i, type, trace);
				parameterScope.addVariableDescriptor(callParameterDescriptor);
			}
			else if(parameter instanceof NapileCallParameterAsReference)
				callParameterDescriptor = resolveCallParameterAsReferenceDescriptor(parameterScope, methodDescriptor, (NapileCallParameterAsReference) parameter, i, trace);

			result.add(callParameterDescriptor);
		}
		return result;
	}

	@NotNull
	public CallParameterDescriptor resolveCallParameterDescriptor(JetScope scope, DeclarationDescriptor declarationDescriptor, NapileCallParameterAsVariable parameter, int index, JetType type, BindingTrace trace)
	{
		AbstractCallParameterDescriptorImpl descriptor = new CallParameterAsVariableDescriptorImpl(declarationDescriptor, index, annotationResolver.bindAnnotations(scope, parameter, trace), NapilePsiUtil.safeName(parameter.getName()), type, Modality.resolve(parameter), parameter.isMutable(), parameter.isRef());

		trace.record(BindingContext.VALUE_PARAMETER, parameter, descriptor);

		resolveCallParameterDefaultValue(scope, parameter, trace, descriptor);

		return descriptor;
	}

	private CallParameterDescriptor resolveCallParameterAsReferenceDescriptor(JetScope scope, DeclarationDescriptor declarationDescriptor, NapileCallParameterAsReference parameter, int index, BindingTrace trace)
	{
		NapileSimpleNameExpression ref = parameter.getReferenceExpression();

		VariableDescriptor variableDescriptor = null;
		JetType type = null;
		if(ref == null)
			type = ErrorUtils.createErrorType("Reference expected");
		else
		{
			variableDescriptor = VariableAccessorResolver.resolveSetterForReferenceParameter(ref, expressionTypingServices, trace, scope);

			if(variableDescriptor == null)
			{
				trace.report(Errors.UNRESOLVED_REFERENCE.on(ref));
				type = ErrorUtils.createErrorType("Reference expected");
			}
			else
			{
				type = variableDescriptor.getType();
				trace.record(BindingContext.REFERENCE_TARGET, ref, variableDescriptor);
			}
		}

		AbstractCallParameterDescriptorImpl descriptor = null;
		if(variableDescriptor == null)
			descriptor = new CallParameterAsVariableDescriptorImpl(declarationDescriptor, index, annotationResolver.bindAnnotations(scope, parameter, trace), NapilePsiUtil.safeName(parameter.getName()), type, Modality.resolve(parameter), false, false);
		else
			descriptor = new CallParameterAsReferenceDescriptorImpl(declarationDescriptor, index, Collections.<AnnotationDescriptor>emptyList(), ref.getReferencedNameAsName(), type, variableDescriptor);

		resolveCallParameterDefaultValue(scope, parameter, trace, descriptor);

		return descriptor;
	}

	private void resolveCallParameterDefaultValue(JetScope scope, NapileCallParameter parameter, BindingTrace trace, AbstractCallParameterDescriptorImpl descriptor)
	{
		NapileExpression defaultValue = parameter.getDefaultValue();
		if(defaultValue != null)
		{
			descriptor.setHasDefaultValue(true);

			expressionTypingServices.getType(scope, defaultValue, descriptor.getType(), DataFlowInfo.EMPTY, trace);

			trace.record(BindingContext.DEFAULT_VALUE_OF_PARAMETER, descriptor, defaultValue);
		}
	}

	@NotNull
	public VariableDescriptor resolveLocalVariableDescriptor(@NotNull DeclarationDescriptor containingDeclaration, @NotNull JetScope scope, @NotNull NapileCallParameterAsVariable parameter, BindingTrace trace)
	{
		JetType type = resolveParameterType(scope, parameter, trace);
		return resolveLocalVariableDescriptor(containingDeclaration, parameter, type, trace, scope);
	}

	private JetType resolveParameterType(JetScope scope, NapileCallParameterAsVariable parameter, BindingTrace trace)
	{
		NapileTypeReference typeReference = parameter.getTypeReference();
		JetType type;
		if(typeReference != null)
		{
			type = typeResolver.resolveType(scope, typeReference, trace, true);
		}
		else
		{
			// Error is reported by the parser
			type = ErrorUtils.createErrorType("Annotation is absent");
		}

		return type;
	}

	public VariableDescriptor resolveLocalVariableDescriptor(@NotNull DeclarationDescriptor containingDeclaration, @NotNull NapileCallParameterAsVariable parameter, @NotNull JetType type, BindingTrace trace, JetScope scope)
	{
		VariableDescriptor variableDescriptor = new LocalVariableDescriptor(containingDeclaration, annotationResolver.bindAnnotations(scope, parameter, trace), NapilePsiUtil.safeName(parameter.getName()), type, Modality.resolve(parameter), parameter.isMutable());
		trace.record(BindingContext.VALUE_PARAMETER, parameter, variableDescriptor);
		return variableDescriptor;
	}

	@NotNull
	public VariableDescriptor resolveLocalVariableDescriptor(DeclarationDescriptor containingDeclaration, JetScope scope, NapileVariable variable, DataFlowInfo dataFlowInfo, BindingTrace trace)
	{
		AbstractVariableDescriptorImpl variableDescriptor = resolveLocalVariableDescriptorWithType(containingDeclaration, variable, null, trace, scope);

		JetType type = getVariableType(scope, variable, dataFlowInfo, false, trace); // For a local variable the type must not be deferred
		variableDescriptor.setOutType(type);

		return variableDescriptor;
	}

	@NotNull
	public AbstractVariableDescriptorImpl resolveLocalVariableDescriptorWithType(DeclarationDescriptor containingDeclaration, NapileVariable variable, JetType type, BindingTrace trace, @NotNull JetScope scope)
	{
		AbstractVariableDescriptorImpl variableDescriptor = new LocalVariableDescriptor(containingDeclaration, annotationResolver.bindAnnotations(scope, variable, trace), NapilePsiUtil.safeName(variable.getName()), type, Modality.resolve(variable), variable.isMutable());
		trace.record(BindingContext.VARIABLE, variable, variableDescriptor);
		return variableDescriptor;
	}

	public JetScope getPropertyDeclarationInnerScope(@NotNull JetScope outerScope, @NotNull List<? extends TypeParameterDescriptor> typeParameters, BindingTrace trace)
	{
		WritableScopeImpl result = new WritableScopeImpl(outerScope, outerScope.getContainingDeclaration(), new TraceBasedRedeclarationHandler(trace), "Property declaration inner scope");
		for(TypeParameterDescriptor typeParameterDescriptor : typeParameters)
			result.addTypeParameterDescriptor(typeParameterDescriptor);
		result.changeLockLevel(WritableScope.LockLevel.READING);
		return result;
	}

	@NotNull
	public VariableDescriptor resolveVariableDescriptor(@NotNull MutableClassDescriptor containingDeclaration, @NotNull JetScope scope, NapileVariable variable, BindingTrace trace)
	{
		VariableDescriptorImpl variableDescriptor = new VariableDescriptorImpl(containingDeclaration, annotationResolver.bindAnnotations(scope, variable, trace), Modality.resolve(variable), Visibility.PUBLIC, NapilePsiUtil.safeName(variable.getName()), CallableMemberDescriptor.Kind.DECLARATION, resolveStatic(variable), variable.isMutable(), false);

		List<TypeParameterDescriptor> typeParameterDescriptors;

		NapileTypeParameter[] typeParameters = variable.getTypeParameters();
		if(typeParameters.length == 0)
			typeParameterDescriptors = Collections.emptyList();
		else
		{
			WritableScope writableScope = new WritableScopeImpl(scope, containingDeclaration, new TraceBasedRedeclarationHandler(trace), "Scope with type parameters of a variable");
			typeParameterDescriptors = typeParameterResolver.resolveTypeParameters(containingDeclaration, writableScope, typeParameters, trace);
			writableScope.changeLockLevel(WritableScope.LockLevel.READING);
			typeParameterResolver.postResolving(variable, writableScope, typeParameterDescriptors, trace);
		}

		JetScope propertyScope = getPropertyDeclarationInnerScope(scope, typeParameterDescriptors, trace);

		JetType type = getVariableType(propertyScope, variable, DataFlowInfo.EMPTY, true, trace);

		variableDescriptor.setType(type, typeParameterDescriptors, DescriptorUtils.getExpectedThisObjectIfNeeded(containingDeclaration));

		trace.record(BindingContext.VARIABLE, variable, variableDescriptor);

		resolveVariableAccessors(containingDeclaration, scope, variable, trace, variableDescriptor);

		return variableDescriptor;
	}

	@NotNull
	public VariableDescriptor resolveVariableDescriptor(@NotNull MutableClassDescriptor containingDeclaration, @NotNull JetScope scope, @NotNull NapileEnumValue enumValue, @NotNull MutableClassDescriptor m, BindingTrace trace)
	{
		VariableDescriptorImpl variableDescriptor = new VariableDescriptorImpl(containingDeclaration, annotationResolver.bindAnnotations(scope, enumValue, trace), Modality.resolve(enumValue), Visibility.resolve(enumValue), NapilePsiUtil.safeName(enumValue.getName()), CallableMemberDescriptor.Kind.DECLARATION, true, false, true);

		List<TypeParameterDescriptor> typeParameterDescriptors;

		NapileTypeParameter[] typeParameters = enumValue.getTypeParameters();
		if(typeParameters.length == 0)
			typeParameterDescriptors = Collections.emptyList();
		else
		{
			WritableScope writableScope = new WritableScopeImpl(scope, containingDeclaration, new TraceBasedRedeclarationHandler(trace), "Scope with type parameters of a enumValue");
			typeParameterDescriptors = typeParameterResolver.resolveTypeParameters(containingDeclaration, writableScope, typeParameters, trace);
			writableScope.changeLockLevel(WritableScope.LockLevel.READING);
			typeParameterResolver.postResolving(enumValue, writableScope, typeParameterDescriptors, trace);
		}

		variableDescriptor.setType(m.getDefaultType(), typeParameterDescriptors, DescriptorUtils.getExpectedThisObjectIfNeeded(containingDeclaration));

		resolveVariableAccessors(containingDeclaration, scope, enumValue, trace, variableDescriptor);
		trace.record(BindingContext.VARIABLE, enumValue, variableDescriptor);

		return variableDescriptor;
	}

	public void resolveVariableAccessors(@NotNull MutableClassDescriptor containingDeclaration, @NotNull JetScope scope, NapileVariable variable, BindingTrace trace, VariableDescriptorImpl variableDescriptor)
	{
		VariableAccessorDescriptor set = null;
		VariableAccessorDescriptor get = null;

		for(NapileVariableAccessor variableAccessor : variable.getAccessors())
		{
			IElementType accessorElementType = variableAccessor.getAccessorElementType();
			if(accessorElementType == null)
				continue;

			Name name = Name.identifier(variableDescriptor.getName().getName() + AsmConstants.ANONYM_SPLITTER + accessorElementType.toString());

			VariableAccessorDescriptorImpl variableAccessorDescriptor = new VariableAccessorDescriptorImpl(containingDeclaration, annotationResolver.bindAnnotations(scope, variableAccessor, trace), name, CallableMemberDescriptor.Kind.DECLARATION, variableDescriptor.isStatic(), variableAccessor.hasModifier(NapileTokens.NATIVE_KEYWORD), false, variableDescriptor);

			if(accessorElementType == NapileTokens.SET_KEYWORD)
			{
				List<CallParameterDescriptor> list = Collections.<CallParameterDescriptor>singletonList(new CallParameterAsReferenceDescriptorImpl(variableAccessorDescriptor, 0, Collections.<AnnotationDescriptor>emptyList(), Name.identifier("value"), variableDescriptor.getType(), variableDescriptor));

				variableAccessorDescriptor.initialize(DescriptorUtils.getExpectedThisObjectIfNeeded(containingDeclaration), Collections.<TypeParameterDescriptor>emptyList(), list, variableDescriptor.getType(), Modality.resolve(variableAccessor), Visibility.resolve(variableAccessor));

				trace.record(BindingContext.VARIABLE_SET_ACCESSOR, variableAccessor, variableAccessorDescriptor);
				set = variableAccessorDescriptor;
			}
			else
			{
				variableAccessorDescriptor.initialize(DescriptorUtils.getExpectedThisObjectIfNeeded(containingDeclaration), Collections.<TypeParameterDescriptor>emptyList(), Collections.<CallParameterDescriptor>emptyList(), variableDescriptor.getType(), Modality.resolve(variableAccessor), Visibility.resolve(variableAccessor));

				trace.record(BindingContext.VARIABLE_GET_ACCESSOR, variableAccessor, variableAccessorDescriptor);
				get = variableAccessorDescriptor;
			}
		}

		if(set == null)
		{
			VariableAccessorDescriptorImpl variableAccessorDescriptor = new VariableAccessorDescriptorImpl(containingDeclaration, Collections.<AnnotationDescriptor>emptyList(), Name.identifier(variableDescriptor.getName().getName() + AsmConstants.ANONYM_SPLITTER + "set"), CallableMemberDescriptor.Kind.DECLARATION, variableDescriptor.isStatic(), false, true, variableDescriptor);

			List<CallParameterDescriptor> list = Collections.<CallParameterDescriptor>singletonList(new CallParameterAsReferenceDescriptorImpl(variableAccessorDescriptor, 0, Collections.<AnnotationDescriptor>emptyList(), Name.identifier("value"), variableDescriptor.getType(), variableDescriptor));
			variableAccessorDescriptor.initialize(DescriptorUtils.getExpectedThisObjectIfNeeded(containingDeclaration), Collections.<TypeParameterDescriptor>emptyList(), list, variableDescriptor.getType(), Modality.resolve(variable), Visibility.resolve(variable));

			trace.record(BindingContext.VARIABLE_SET_ACCESSOR, variable, variableAccessorDescriptor);
			set = variableAccessorDescriptor;
		}

		if(get == null)
		{
			VariableAccessorDescriptorImpl variableAccessorDescriptor = new VariableAccessorDescriptorImpl(containingDeclaration, Collections.<AnnotationDescriptor>emptyList(), Name.identifier(variableDescriptor.getName().getName() + AsmConstants.ANONYM_SPLITTER + "get"), CallableMemberDescriptor.Kind.DECLARATION, variableDescriptor.isStatic(), false, true, variableDescriptor);
			variableAccessorDescriptor.initialize(DescriptorUtils.getExpectedThisObjectIfNeeded(containingDeclaration), Collections.<TypeParameterDescriptor>emptyList(), Collections.<CallParameterDescriptor>emptyList(), variableDescriptor.getType(), Modality.resolve(variable), Visibility.resolve(variable));

			trace.record(BindingContext.VARIABLE_GET_ACCESSOR, variable, variableAccessorDescriptor);
			get = variableAccessorDescriptor;
		}

		containingDeclaration.getBuilder().addMethodDescriptor(set);
		containingDeclaration.getBuilder().addMethodDescriptor(get);
	}

	private static boolean resolveStatic(NapileModifierListOwner declaration)
	{
		if(declaration.hasModifier(NapileTokens.STATIC_KEYWORD))
			return true;
		NapileClass napileClass = PsiTreeUtil.getParentOfType(declaration, NapileClass.class);
		if(napileClass != null)
			return napileClass.hasModifier(NapileTokens.UTIL_KEYWORD);
		return false;
	}

	@NotNull
	private JetType getVariableType(@NotNull final JetScope scope, @NotNull final NapileVariable property, @NotNull final DataFlowInfo dataFlowInfo, boolean allowDeferred, final BindingTrace trace)
	{
		NapileTypeReference propertyTypeRef = property.getType();

		if(propertyTypeRef == null)
		{
			final NapileExpression initializer = property.getInitializer();
			if(initializer == null)
			{
				return ErrorUtils.createErrorType("No type, no body");
			}
			else
			{
				// TODO : a risk of a memory leak
				LazyValue<JetType> lazyValue = new LazyValueWithDefault<JetType>(ErrorUtils.createErrorType("Recursive dependency"))
				{
					@Override
					protected JetType compute()
					{
						return expressionTypingServices.safeGetType(scope, initializer, TypeUtils.NO_EXPECTED_TYPE, dataFlowInfo, trace);
					}
				};
				if(allowDeferred)
				{
					return DeferredType.create(trace, lazyValue);
				}
				else
				{
					return lazyValue.get();
				}
			}
		}
		else
		{
			return typeResolver.resolveType(scope, propertyTypeRef, trace, false);
		}
	}

	@NotNull
	public ConstructorDescriptor resolveConstructorDescriptor(@NotNull JetScope scope, @NotNull ClassDescriptor classDescriptor, @NotNull NapileConstructor constructor, BindingTrace trace)
	{
		ConstructorDescriptor constructorDescriptor = new ConstructorDescriptor(classDescriptor, annotationResolver.bindAnnotations(scope, constructor, trace), resolveStatic(constructor));
		constructorDescriptor.setReturnType(classDescriptor.getDefaultType());
		trace.record(BindingContext.CONSTRUCTOR, constructor, constructorDescriptor);
		WritableScopeImpl parameterScope = new WritableScopeImpl(scope, constructorDescriptor, new TraceBasedRedeclarationHandler(trace), "Scope with value parameters of a constructor");
		parameterScope.changeLockLevel(WritableScope.LockLevel.BOTH);
		constructorDescriptor.setParametersScope(parameterScope);

		resolveDelegationSpecifiers(scope, constructor.getSuperCallTypeList(), typeResolver, trace, true);

		return constructorDescriptor.initialize(classDescriptor.getTypeConstructor().getParameters(), resolveCallParameters(constructorDescriptor, parameterScope, constructor.getCallParameters(), trace), Visibility.resolve(constructor));
	}

	public void checkBounds(@NotNull NapileTypeReference typeReference, @NotNull JetType type, BindingTrace trace)
	{
		if(ErrorUtils.isErrorType(type))
			return;

		NapileTypeElement typeElement = typeReference.getTypeElement();
		if(typeElement == null)
			return;

		List<TypeParameterDescriptor> parameters = type.getConstructor().getParameters();
		List<JetType> arguments = type.getArguments();
		assert parameters.size() == arguments.size();

		List<? extends NapileTypeReference> jetTypeArguments = typeElement.getTypeArguments();
		assert jetTypeArguments.size() == arguments.size() : typeElement.getText();

		TypeSubstitutor substitutor = TypeSubstitutor.create(type);
		for(int i = 0; i < jetTypeArguments.size(); i++)
		{
			NapileTypeReference jetTypeArgument = jetTypeArguments.get(i);

			if(jetTypeArgument == null)
				continue;

			JetType typeArgument = arguments.get(i);
			checkBounds(jetTypeArgument, typeArgument, trace);

			TypeParameterDescriptor typeParameterDescriptor = parameters.get(i);
			checkBounds(jetTypeArgument, typeArgument, typeParameterDescriptor, substitutor, trace);
		}
	}

	public void checkBounds(@NotNull NapileTypeReference jetTypeArgument, @NotNull JetType typeArgument, @NotNull TypeParameterDescriptor typeParameterDescriptor, @NotNull TypeSubstitutor substitutor, BindingTrace trace)
	{
		for(JetType bound : typeParameterDescriptor.getUpperBounds())
		{
			JetType substitutedBound = substitutor.safeSubstitute(bound);
			if(!JetTypeChecker.INSTANCE.isSubtypeOf(typeArgument, substitutedBound))
				trace.report(UPPER_BOUND_VIOLATED.on(jetTypeArgument, substitutedBound, typeArgument));
			else
			{
				Set<ConstructorDescriptor> constructorDescriptors = typeParameterDescriptor.getConstructors();
				ClassifierDescriptor classifierDescriptor = typeArgument.getConstructor().getDeclarationDescriptor();
				if(classifierDescriptor == null)
				{
					if(!constructorDescriptors.isEmpty())
						trace.report(CONSTRUCTORS_EXPECTED.on(jetTypeArgument));
					return;
				}

				Set<ConstructorDescriptor> targetTypeConstructors = classifierDescriptor.getConstructors();

				if(!constructorDescriptors.isEmpty())
				{
					for(ConstructorDescriptor targetToSearch : constructorDescriptors)
					{
						boolean find = false;

						for(ConstructorDescriptor temp : targetTypeConstructors)
						{
							if(temp.getVisibility() != Visibility.PUBLIC)
								continue;
							loop:
							{
								List<CallParameterDescriptor> l1 = targetToSearch.getValueParameters();
								List<CallParameterDescriptor> l2 = temp.getValueParameters();

								if(l1.size() != l2.size())
									continue;

								for(int i = 0; i < l1.size(); i++)
								{
									CallParameterDescriptor p1 = l1.get(i);
									CallParameterDescriptor p2 = l2.get(i);

									if(!JetTypeChecker.INSTANCE.isSubtypeOf(p2.getType(), p1.getType()))
										break loop;
								}
								find = true;
							}
						}

						if(!find)
						{
							trace.report(CONSTRUCTORS_EXPECTED.on(jetTypeArgument));
							break;
						}
					}
				}
			}
		}
	}
}
