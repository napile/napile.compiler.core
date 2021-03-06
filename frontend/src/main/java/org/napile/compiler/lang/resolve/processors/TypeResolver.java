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

import static org.napile.compiler.lang.diagnostics.Errors.WRONG_NUMBER_OF_TYPE_ARGUMENTS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.lib.NapileLangPackage;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.ClassifierDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.BindingTraceKeys;
import org.napile.compiler.lang.resolve.processors.members.AnnotationResolver;
import org.napile.compiler.lang.resolve.scopes.NapileScope;
import org.napile.compiler.lang.resolve.scopes.LazyScopeAdapter;
import org.napile.compiler.lang.types.ErrorUtils;
import org.napile.compiler.lang.types.NapileType;
import org.napile.compiler.lang.types.MethodTypeConstructor;
import org.napile.compiler.lang.types.MultiTypeEntry;
import org.napile.compiler.lang.types.TypeConstructor;
import org.napile.compiler.lang.types.TypeSubstitutor;
import org.napile.compiler.lang.types.TypeUtils;
import org.napile.compiler.lang.types.impl.NapileTypeImpl;
import org.napile.compiler.lang.types.impl.MethodTypeConstructorImpl;
import org.napile.compiler.lang.types.impl.MultiTypeConstructorImpl;
import org.napile.compiler.lang.types.impl.SelfTypeConstructorImpl;
import org.napile.compiler.util.lazy.LazyValue;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * @author abreslav
 */
public class TypeResolver
{
	private AnnotationResolver annotationResolver;
	private DescriptorResolver descriptorResolver;
	private QualifiedExpressionResolver qualifiedExpressionResolver;

	@Inject
	public void setDescriptorResolver(DescriptorResolver descriptorResolver)
	{
		this.descriptorResolver = descriptorResolver;
	}

	@Inject
	public void setAnnotationResolver(AnnotationResolver annotationResolver)
	{
		this.annotationResolver = annotationResolver;
	}

	@Inject
	public void setQualifiedExpressionResolver(QualifiedExpressionResolver qualifiedExpressionResolver)
	{
		this.qualifiedExpressionResolver = qualifiedExpressionResolver;
	}

	@NotNull
	public NapileType resolveType(@NotNull final NapileScope scope, @NotNull final NapileTypeReference typeReference, BindingTrace trace, boolean checkBounds)
	{
		NapileType cachedType = trace.get(BindingTraceKeys.TYPE, typeReference);
		if(cachedType != null)
			return cachedType;

		final List<AnnotationDescriptor> annotations = annotationResolver.bindAnnotations(scope, typeReference, trace);

		NapileTypeElement typeElement = typeReference.getTypeElement();
		NapileType type = resolveTypeElement(scope, annotations, typeElement, false, trace, checkBounds);
		trace.record(BindingTraceKeys.TYPE, typeReference, type);
		trace.record(BindingTraceKeys.TYPE_RESOLUTION_SCOPE, typeReference, scope);

		return type;
	}

	@NotNull
	private NapileType resolveTypeElement(final NapileScope scope, final List<AnnotationDescriptor> annotations, NapileTypeElement typeElement, final boolean nullable, final BindingTrace trace, final boolean checkBounds)
	{

		final NapileType[] result = new NapileType[1];
		if(typeElement != null)
		{
			typeElement.accept(new NapileVisitorVoid()
			{
				@Override
				public void visitUserType(NapileUserType type)
				{
					NapileSimpleNameExpression referenceExpression = type.getReferenceExpression();
					String referencedName = type.getReferencedName();
					if(referenceExpression == null || referencedName == null)
					{
						return;
					}

					ClassifierDescriptor classifierDescriptor = resolveClass(scope, type, trace);
					if(classifierDescriptor == null)
					{
						resolveTypes(scope, type.getTypeArguments(), trace, checkBounds);
						return;
					}

					if(classifierDescriptor instanceof TypeParameterDescriptor)
					{
						TypeParameterDescriptor typeParameterDescriptor = (TypeParameterDescriptor) classifierDescriptor;

						trace.record(BindingTraceKeys.REFERENCE_TARGET, referenceExpression, typeParameterDescriptor);

						NapileScope scopeForTypeParameter = getScopeForTypeParameter(typeParameterDescriptor, checkBounds);
						if(scopeForTypeParameter instanceof ErrorUtils.ErrorScope)
						{
							result[0] = ErrorUtils.createErrorType("?");
						}
						else
						{
							result[0] = new NapileTypeImpl(annotations, typeParameterDescriptor.getTypeConstructor(), nullable, Collections.<NapileType>emptyList(), scopeForTypeParameter);
						}

						resolveTypes(scope, type.getTypeArguments(), trace, checkBounds);
					}
					else if(classifierDescriptor instanceof ClassDescriptor)
					{
						ClassDescriptor classDescriptor = (ClassDescriptor) classifierDescriptor;

						trace.record(BindingTraceKeys.REFERENCE_TARGET, referenceExpression, classifierDescriptor);
						TypeConstructor typeConstructor = classifierDescriptor.getTypeConstructor();
						List<NapileType> arguments = resolveTypes(scope, type.getTypeArguments(), trace, checkBounds);
						List<TypeParameterDescriptor> parameters = typeConstructor.getParameters();
						int expectedArgumentCount = parameters.size();
						int actualArgumentCount = arguments.size();
						if(ErrorUtils.isError(typeConstructor))
						{
							result[0] = ErrorUtils.createErrorType("[Error type: " + typeConstructor + "]");
						}
						else
						{
							if(actualArgumentCount != expectedArgumentCount)
							{
								if(actualArgumentCount == 0)
								{
									trace.report(WRONG_NUMBER_OF_TYPE_ARGUMENTS.on(type, expectedArgumentCount));
								}
								else
								{
									trace.report(WRONG_NUMBER_OF_TYPE_ARGUMENTS.on(type.getTypeArgumentList(), expectedArgumentCount));
								}
							}
							else
							{
								result[0] = new NapileTypeImpl(annotations, typeConstructor, nullable, arguments, classDescriptor.getMemberScope(arguments));
								if(checkBounds)
								{
									TypeSubstitutor substitutor = TypeSubstitutor.create(result[0]);
									for(int i = 0, parametersSize = parameters.size(); i < parametersSize; i++)
									{
										TypeParameterDescriptor parameter = parameters.get(i);
										NapileType argument = arguments.get(i);
										NapileTypeReference typeReference = type.getTypeArguments().get(i);

										if(typeReference != null)
										{
											descriptorResolver.checkBounds(typeReference, argument, parameter, substitutor, trace);
										}
									}
								}
							}
						}
					}
				}

				@Override
				public void visitMethodType(NapileMethodType type)
				{
					NapileCallParameter[] parameters = type.getParameters();
					Map<Name, NapileType> parameterTypes = new LinkedHashMap<Name, NapileType>(parameters.length);
					int i = 1;
					for(NapileElement parameter : parameters)
					{
						if(parameter instanceof NapileCallParameterAsVariable)
						{
							Name name = ((NapileCallParameterAsVariable) parameter).getNameAsName();
							NapileType napileType = resolveType(scope, ((NapileCallParameterAsVariable) parameter).getTypeReference(), trace, checkBounds);

							parameterTypes.put(name == null ? Name.identifier("p" + i) : name, napileType);
						}
						i++;
					}

					NapileTypeReference returnTypeRef = type.getReturnTypeRef();
					NapileType returnType;
					if(returnTypeRef != null)
						returnType = resolveType(scope, returnTypeRef, trace, checkBounds);
					else
						returnType = TypeUtils.getTypeOfClassOrErrorType(scope, NapileLangPackage.NULL, false);

					final String name = type.getName();
					Name expectedName = null;
					if(name == null)
					{
						final PsiElement parent = type.getParent().getParent(); // first parent is NapileTypeReference

						if(parent instanceof NapileVariable || parent instanceof NapileCallParameterAsVariable)
						{
							expectedName = ((NapileNamedDeclaration) parent).getNameAsSafeName();
						}
					}
					else
					{
						expectedName = Name.identifier(name);
					}

					MethodTypeConstructor methodTypeConstructor = new MethodTypeConstructorImpl(expectedName, returnType, parameterTypes, scope);
					result[0] = new NapileTypeImpl(annotations, methodTypeConstructor, false, Collections.<NapileType>emptyList(), scope);
				}

				@Override
				public void visitMultiType(NapileMultiType multiType)
				{
					NapileVariable[] variables = multiType.getVariables();
					List<MultiTypeEntry> variableDescriptors = new ArrayList<MultiTypeEntry>(variables.length);

					for(int i = 0; i < variables.length; i++)
					{
						NapileVariable variable = variables[i];
						NapileType type = null;
						NapileTypeReference typeReference = variable.getType();
						if(typeReference != null)
							type = resolveType(scope, typeReference, trace, true);
						else
							type = ErrorUtils.createErrorType("Type expected");

						MultiTypeEntry entry = new MultiTypeEntry(i, variable.isMutable(), variable.getNameAsSafeName(), type);

						trace.record(BindingTraceKeys.VARIABLE, variable, entry.descriptor);

						variableDescriptors.add(entry);
					}

					result[0] = new NapileTypeImpl(annotations, new MultiTypeConstructorImpl(variableDescriptors, scope), nullable, Collections.<NapileType>emptyList(), scope);
				}

				@Override
				public void visitNullableType(NapileNullableType nullableType)
				{
					result[0] = resolveTypeElement(scope, annotations, nullableType.getInnerType(), true, trace, checkBounds);
				}

				@Override
				public void visitSelfType(NapileSelfType type)
				{
					NapileClassLike classLike = PsiTreeUtil.getParentOfType(type, NapileClassLike.class);

					assert classLike != null;

					ClassDescriptor classDescriptor = trace.safeGet(BindingTraceKeys.CLASS, classLike);

					trace.record(BindingTraceKeys.REFERENCE_TARGET, type.getThisExpression(), classDescriptor);

					result[0] = new NapileTypeImpl(annotations, new SelfTypeConstructorImpl(classDescriptor), nullable, Collections.<NapileType>emptyList(), scope);
				}
			});
		}

		if(result[0] == null)
			return ErrorUtils.createErrorType(typeElement == null ? "No type element" : typeElement.getText());
		if(nullable)
			return TypeUtils.makeNullable(result[0]);
		return result[0];
	}

	private NapileScope getScopeForTypeParameter(final TypeParameterDescriptor typeParameterDescriptor, boolean checkBounds)
	{
		if(checkBounds)
		{
			return typeParameterDescriptor.getUpperBoundsAsType().getMemberScope();
		}
		else
		{
			return new LazyScopeAdapter(new LazyValue<NapileScope>()
			{
				@Override
				protected NapileScope compute()
				{
					return typeParameterDescriptor.getUpperBoundsAsType().getMemberScope();
				}
			});
		}
	}

	public List<NapileType> resolveTypes(NapileScope scope, List<? extends NapileTypeReference> argumentElements, BindingTrace trace, boolean checkBounds)
	{
		final List<NapileType> arguments = new ArrayList<NapileType>(argumentElements.size());
		for(NapileTypeReference argumentElement : argumentElements)
			arguments.add(resolveType(scope, argumentElement, trace, checkBounds));

		return arguments;
	}

	@Nullable
	public ClassifierDescriptor resolveClass(NapileScope scope, NapileUserType userType, BindingTrace trace)
	{
		Collection<? extends DeclarationDescriptor> descriptors = qualifiedExpressionResolver.lookupDescriptorsForUserType(userType, scope, trace);
		for(DeclarationDescriptor descriptor : descriptors)
		{
			if(descriptor instanceof ClassifierDescriptor)
			{
				return (ClassifierDescriptor) descriptor;
			}
		}
		return null;
	}
}
