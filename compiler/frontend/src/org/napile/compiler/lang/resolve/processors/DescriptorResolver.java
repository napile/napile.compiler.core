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
import org.napile.asm.lib.NapileLangPackage;
import org.napile.compiler.lang.descriptors.*;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.lexer.NapileNodes;
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
import org.napile.compiler.util.lazy.LazyValue;
import org.napile.compiler.util.lazy.LazyValueWithDefault;
import com.google.common.collect.Lists;

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

	public void resolveSupertypesForMutableClassDescriptor(@NotNull NapileClassLike jetClass, @NotNull MutableClassDescriptor descriptor, BindingTrace trace)
	{
		for(JetType supertype : resolveSupertypes(descriptor.getScopeForSupertypeResolution(), jetClass, trace))
		{
			descriptor.addSupertype(supertype);
		}
	}

	public List<JetType> resolveSupertypes(@NotNull JetScope scope, @NotNull NapileClassLike jetClass, BindingTrace trace)
	{
		if(NapileLangPackage.ANY.equals(jetClass.getFqName()))  // master object dont have super classes
			return Collections.emptyList();

		List<JetType> result = Lists.newArrayList();
		List<NapileTypeReference> delegationSpecifiers = jetClass.getExtendTypeList();
		if(delegationSpecifiers.isEmpty())
			result.add(TypeUtils.getTypeOfClassOrErrorType(scope, NapileLangPackage.ANY, false));
		else
		{
			Collection<JetType> supertypes = resolveDelegationSpecifiers(scope, delegationSpecifiers, typeResolver, trace, false);
			result.addAll(supertypes);
		}
		return result;
	}

	public Collection<JetType> resolveDelegationSpecifiers(JetScope extensibleScope, List<NapileTypeReference> delegationSpecifiers, @NotNull TypeResolver resolver, BindingTrace trace, boolean checkBounds)
	{
		if(delegationSpecifiers.isEmpty())
		{
			return Collections.emptyList();
		}
		Collection<JetType> result = Lists.newArrayList();
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
	public SimpleMethodDescriptor resolveMethodDescriptor(DeclarationDescriptor containingDescriptor, final JetScope scope, final NapileNamedMethodOrMacro method, final BindingTrace trace)
	{
		NapileSimpleNameExpression referenceExpression = method.getVariableRef();

		final SimpleMethodDescriptorImpl methodDescriptor = new SimpleMethodDescriptorImpl(containingDescriptor, annotationResolver.resolveAnnotations(scope, method.getModifierList(), trace), NapilePsiUtil.safeName(method.getName()), CallableMemberDescriptor.Kind.DECLARATION, method.hasModifier(NapileTokens.STATIC_KEYWORD), method.hasModifier(NapileTokens.NATIVE_KEYWORD), method.getNode().getElementType() == NapileNodes.MACRO);
		WritableScope innerScope = new WritableScopeImpl(scope, methodDescriptor, new TraceBasedRedeclarationHandler(trace), "Function descriptor header scope");

		List<TypeParameterDescriptor> typeParameterDescriptors = typeParameterResolver.resolveTypeParameters(methodDescriptor, innerScope, method.getTypeParameters(), trace);
		innerScope.changeLockLevel(WritableScope.LockLevel.BOTH);
		typeParameterResolver.postResolving(method, innerScope, typeParameterDescriptors, trace);

		List<ParameterDescriptor> parameterDescriptors = resolveValueParameters(methodDescriptor, innerScope, method.getValueParameters(), trace);

		innerScope.changeLockLevel(WritableScope.LockLevel.READING);

		final NapileExpression bodyExpression = method.getBodyExpression();
		NapileTypeReference returnTypeRef = method.getReturnTypeRef();
		JetType returnType;
		if(returnTypeRef != null)
			returnType = typeResolver.resolveType(innerScope, returnTypeRef, trace, true);
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

		if(bodyExpression != null && methodDescriptor.isMacro())
			trace.record(BindingContext.MACRO_BODY, methodDescriptor, bodyExpression);

		if(referenceExpression != null)
			expressionTypingServices.safeGetType(scope, referenceExpression, TypeUtils.NO_EXPECTED_TYPE, DataFlowInfo.EMPTY, trace);

		Modality modality = Modality.resolve(method);
		Visibility visibility = Visibility.resolve(method);

		methodDescriptor.initialize(DescriptorUtils.getExpectedThisObjectIfNeeded(containingDescriptor), typeParameterDescriptors, parameterDescriptors, returnType, modality, visibility);

		BindingContextUtils.recordMethodDeclarationToDescriptor(trace, method, methodDescriptor);

		return methodDescriptor;
	}

	@NotNull
	public List<ParameterDescriptor> resolveValueParameters(MethodDescriptor methodDescriptor, WritableScope parameterScope, NapileElement[] valueParameters, BindingTrace trace)
	{
		List<ParameterDescriptor> result = new ArrayList<ParameterDescriptor>();
		for(int i = 0, valueParametersSize = valueParameters.length; i < valueParametersSize; i++)
		{
			NapileElement parameter = valueParameters[i];
			if(parameter instanceof NapilePropertyParameter)
			{
				NapileTypeReference typeReference = ((NapilePropertyParameter) parameter).getTypeReference();
	
				JetType type;
				if(typeReference == null)
				{
					trace.report(VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION.on(((NapilePropertyParameter) parameter)));
					type = ErrorUtils.createErrorType("Type annotation was missing");
				}
				else
				{
					type = typeResolver.resolveType(parameterScope, typeReference, trace, true);
				}
	
				ParameterDescriptor parameterDescriptor = resolveValueParameterDescriptor(parameterScope, methodDescriptor, ((NapilePropertyParameter) parameter), i, type, trace);
				parameterScope.addVariableDescriptor(parameterDescriptor);
				result.add(parameterDescriptor);
			}
			else if(parameter instanceof NapileReferenceParameter)
			{
				NapileSimpleNameExpression ref = ((NapileReferenceParameter) parameter).getReferenceExpression();

				ReferenceParameterDescriptor parameterDescriptor = new ReferenceParameterDescriptor(i, methodDescriptor);
				JetType jetType = null;
				if(ref == null)
					jetType = ErrorUtils.createErrorType("Reference expected");
				else
					jetType = expressionTypingServices.safeGetType(parameterScope, ref, TypeUtils.NO_EXPECTED_TYPE, DataFlowInfo.EMPTY, trace);

				DeclarationDescriptor refDesc = trace.get(BindingContext.REFERENCE_TARGET, ref);
				parameterDescriptor.initialize(jetType, ref.getReferencedNameAsName(), refDesc instanceof PropertyDescriptor ? (PropertyDescriptor) refDesc : null);

				result.add(parameterDescriptor);
			}
		}
		return result;
	}

	@NotNull
	public MutableParameterDescriptor resolveValueParameterDescriptor(JetScope scope, DeclarationDescriptor declarationDescriptor, NapilePropertyParameter valueParameter, int index, JetType type, BindingTrace trace)
	{
		JetType varargElementType = null;
		JetType variableType = type;

		MutableParameterDescriptor valueParameterDescriptor = new PropertyParameterDescriptorImpl(declarationDescriptor, index, annotationResolver.resolveAnnotations(scope, valueParameter.getModifierList(), trace), NapilePsiUtil.safeName(valueParameter.getName()), variableType, valueParameter.getDefaultValue() != null, varargElementType, Modality.resolve(valueParameter));

		trace.record(BindingContext.VALUE_PARAMETER, valueParameter, valueParameterDescriptor);
		return valueParameterDescriptor;
	}

	@NotNull
	public VariableDescriptor resolveLocalVariableDescriptor(@NotNull DeclarationDescriptor containingDeclaration, @NotNull JetScope scope, @NotNull NapilePropertyParameter parameter, BindingTrace trace)
	{
		JetType type = resolveParameterType(scope, parameter, trace);
		return resolveLocalVariableDescriptor(containingDeclaration, parameter, type, trace, scope);
	}

	private JetType resolveParameterType(JetScope scope, NapilePropertyParameter parameter, BindingTrace trace)
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

	public VariableDescriptor resolveLocalVariableDescriptor(@NotNull DeclarationDescriptor containingDeclaration, @NotNull NapilePropertyParameter parameter, @NotNull JetType type, BindingTrace trace, JetScope scope)
	{
		VariableDescriptor variableDescriptor = new LocalVariableDescriptor(containingDeclaration, annotationResolver.resolveAnnotations(scope, parameter.getModifierList(), trace), NapilePsiUtil.safeName(parameter.getName()), type, Modality.resolve(parameter));
		trace.record(BindingContext.VALUE_PARAMETER, parameter, variableDescriptor);
		return variableDescriptor;
	}

	@NotNull
	public VariableDescriptor resolveLocalVariableDescriptor(DeclarationDescriptor containingDeclaration, JetScope scope, NapileVariable property, DataFlowInfo dataFlowInfo, BindingTrace trace)
	{
		VariableDescriptorImpl variableDescriptor = resolveLocalVariableDescriptorWithType(containingDeclaration, property, null, trace, scope);

		JetType type = getVariableType(scope, property, dataFlowInfo, false, trace); // For a local variable the type must not be deferred
		variableDescriptor.setOutType(type);
		return variableDescriptor;
	}

	@NotNull
	public VariableDescriptorImpl resolveLocalVariableDescriptorWithType(DeclarationDescriptor containingDeclaration, NapileVariable property, JetType type, BindingTrace trace, @NotNull JetScope scope)
	{
		VariableDescriptorImpl variableDescriptor = new LocalVariableDescriptor(containingDeclaration, annotationResolver.resolveAnnotations(scope, property.getModifierList(), trace), NapilePsiUtil.safeName(property.getName()), type, Modality.resolve(property));
		trace.record(BindingContext.VARIABLE, property, variableDescriptor);
		return variableDescriptor;
	}

	@NotNull
	public VariableDescriptor resolveAnonymDeclaration(@NotNull DeclarationDescriptor containingDeclaration, @NotNull NapileClassLike objectDeclaration, @NotNull ClassDescriptor classDescriptor, BindingTrace trace, @NotNull JetScope scope)
	{
		VariableDescriptorImpl variableDescriptor = new LocalVariableDescriptor(containingDeclaration, annotationResolver.resolveAnnotations(scope, objectDeclaration.getModifierList(), trace), NapilePsiUtil.safeName(objectDeclaration.getName()), classDescriptor.getDefaultType(), Modality.FINAL);

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
	public PropertyDescriptor resolvePropertyDescriptor(@NotNull DeclarationDescriptor containingDeclaration, @NotNull JetScope scope, NapileVariable property, BindingTrace trace)
	{
		NapileModifierList modifierList = property.getModifierList();

		PropertyDescriptor propertyDescriptor = new PropertyDescriptor(containingDeclaration, annotationResolver.resolveAnnotations(scope, modifierList, trace), Modality.resolve(property), Visibility.resolve(property), NapilePsiUtil.safeName(property.getName()), CallableMemberDescriptor.Kind.DECLARATION, property.hasModifier(NapileTokens.STATIC_KEYWORD));

		List<TypeParameterDescriptor> typeParameterDescriptors;

		{
			NapileTypeParameter[] typeParameters = property.getTypeParameters();
			if(typeParameters.length == 0)
			{
				typeParameterDescriptors = Collections.emptyList();
			}
			else
			{
				WritableScope writableScope = new WritableScopeImpl(scope, containingDeclaration, new TraceBasedRedeclarationHandler(trace), "Scope with type parameters of a property");
				typeParameterDescriptors = typeParameterResolver.resolveTypeParameters(containingDeclaration, writableScope, typeParameters, trace);
				writableScope.changeLockLevel(WritableScope.LockLevel.READING);
				typeParameterResolver.postResolving(property, writableScope, typeParameterDescriptors, trace);
			}
		}

		JetScope propertyScope = getPropertyDeclarationInnerScope(scope, typeParameterDescriptors, trace);

		JetType type = getVariableType(propertyScope, property, DataFlowInfo.EMPTY, true, trace);

		propertyDescriptor.setType(type, typeParameterDescriptors, DescriptorUtils.getExpectedThisObjectIfNeeded(containingDeclaration));

		trace.record(BindingContext.FQNAME_TO_VARIABLE_DESCRIPTOR, DescriptorUtils.getFQName(propertyDescriptor).toSafe(), propertyDescriptor);
		trace.record(BindingContext.VARIABLE, property, propertyDescriptor);
		return propertyDescriptor;
	}

	@NotNull
	private JetType getVariableType(@NotNull final JetScope scope, @NotNull final NapileVariable property, @NotNull final DataFlowInfo dataFlowInfo, boolean allowDeferred, final BindingTrace trace)
	{
		// TODO : receiver?
		NapileTypeReference propertyTypeRef = property.getPropertyTypeRef();

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
			return typeResolver.resolveType(scope, propertyTypeRef, trace, true);
		}
	}

	@NotNull
	public ConstructorDescriptor resolveConstructorDescriptor(@NotNull JetScope scope, @NotNull ClassDescriptor classDescriptor, @NotNull NapileConstructor constructor, BindingTrace trace)
	{
		NapileModifierList modifierList = constructor.getModifierList();
		ConstructorDescriptor constructorDescriptor = new ConstructorDescriptor(classDescriptor, annotationResolver.resolveAnnotations(scope, modifierList, trace), false);
		constructorDescriptor.setReturnType(classDescriptor.getDefaultType());
		trace.record(BindingContext.CONSTRUCTOR, constructor, constructorDescriptor);
		WritableScopeImpl parameterScope = new WritableScopeImpl(scope, constructorDescriptor, new TraceBasedRedeclarationHandler(trace), "Scope with value parameters of a constructor");
		parameterScope.changeLockLevel(WritableScope.LockLevel.BOTH);
		constructorDescriptor.setParametersScope(parameterScope);

		resolveDelegationSpecifiers(scope, constructor.getSuperCallTypeList(), typeResolver, trace, true);

		return constructorDescriptor.initialize(classDescriptor.getTypeConstructor().getParameters(), resolveValueParameters(constructorDescriptor, parameterScope, constructor.getValueParameters(), trace), Visibility.resolve(constructor));
	}

	@NotNull
	public ConstructorDescriptor resolveStaticConstructorDescriptor(@NotNull JetScope scope, @NotNull ClassDescriptor classDescriptor, @NotNull NapileStaticConstructor constructor, BindingTrace trace)
	{
		ConstructorDescriptor constructorDescriptor = new ConstructorDescriptor(classDescriptor, Collections.<AnnotationDescriptor>emptyList(), true);
		trace.record(BindingContext.CONSTRUCTOR, constructor, constructorDescriptor);
		constructorDescriptor.setReturnType(TypeUtils.getTypeOfClassOrErrorType(scope, NapileLangPackage.NULL));

		return constructorDescriptor.initialize(Collections.<TypeParameterDescriptor>emptyList(), Collections.<ParameterDescriptor>emptyList(), Visibility.PUBLIC);
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

		List<NapileTypeReference> jetTypeArguments = typeElement.getTypeArguments();
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
				Set<ConstructorDescriptor> targetTypeConstructors = typeArgument.getConstructor().getDeclarationDescriptor().getConstructors();

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
								List<ParameterDescriptor> l1 = targetToSearch.getValueParameters();
								List<ParameterDescriptor> l2 = temp.getValueParameters();

								if(l1.size() != l2.size())
									continue;

								for(int i = 0; i < l1.size(); i++)
								{
									ParameterDescriptor p1 = l1.get(i);
									ParameterDescriptor p2 = l2.get(i);

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
