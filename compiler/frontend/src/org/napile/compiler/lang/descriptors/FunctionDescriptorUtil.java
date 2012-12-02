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

package org.napile.compiler.lang.descriptors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.compiler.lang.psi.NapileDeclarationWithBody;
import org.napile.compiler.lang.psi.NapileNamedMethod;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.TraceBasedRedeclarationHandler;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.resolve.scopes.WritableScope;
import org.napile.compiler.lang.resolve.scopes.WritableScopeImpl;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.MethodTypeConstructor;
import org.napile.compiler.lang.types.TypeConstructor;
import org.napile.compiler.lang.types.TypeSubstitution;
import org.napile.compiler.lang.types.TypeSubstitutor;
import com.intellij.psi.tree.IElementType;

/**
 * @author abreslav
 */
public class FunctionDescriptorUtil
{
	private static final TypeSubstitutor MAKE_TYPE_PARAMETERS_FRESH = TypeSubstitutor.create(new TypeSubstitution()
	{

		@Override
		public JetType get(TypeConstructor key)
		{
			return null;
		}

		@Override
		public boolean isEmpty()
		{
			return false;
		}

		@Override
		public String toString()
		{
			return "FunctionDescriptorUtil.MAKE_TYPE_PARAMETERS_FRESH";
		}
	});

	public static Map<TypeConstructor, JetType> createSubstitutionContext(@NotNull MethodDescriptor methodDescriptor, List<JetType> typeArguments)
	{
		if(methodDescriptor.getTypeParameters().isEmpty())
			return Collections.emptyMap();

		Map<TypeConstructor, JetType> result = new HashMap<TypeConstructor, JetType>();

		int typeArgumentsSize = typeArguments.size();
		List<TypeParameterDescriptor> typeParameters = methodDescriptor.getTypeParameters();
		assert typeArgumentsSize == typeParameters.size();
		for(int i = 0; i < typeArgumentsSize; i++)
		{
			TypeParameterDescriptor typeParameterDescriptor = typeParameters.get(i);
			JetType typeArgument = typeArguments.get(i);
			result.put(typeParameterDescriptor.getTypeConstructor(), typeArgument);
		}
		return result;
	}

	@Nullable
	public static List<CallParameterDescriptor> getSubstitutedValueParameters(MethodDescriptor substitutedDescriptor, @NotNull MethodDescriptor methodDescriptor, @NotNull TypeSubstitutor substitutor)
	{
		List<CallParameterDescriptor> result = new ArrayList<CallParameterDescriptor>();
		List<CallParameterDescriptor> unsubstitutedValueParameters = methodDescriptor.getValueParameters();
		for(int i = 0, unsubstitutedValueParametersSize = unsubstitutedValueParameters.size(); i < unsubstitutedValueParametersSize; i++)
		{
			CallParameterDescriptor unsubstitutedValueParameter = unsubstitutedValueParameters.get(i);
			// TODO : Lazy?
			JetType substitutedType = substitutor.substitute(unsubstitutedValueParameter.getType());
			if(substitutedType == null)
				return null;
			result.add(new CallParameterAsVariableDescriptorImpl(substitutedDescriptor, unsubstitutedValueParameter, unsubstitutedValueParameter.getAnnotations(), unsubstitutedValueParameter.getName(), substitutedType, unsubstitutedValueParameter.getModality()));
		}
		return result;
	}

	@Nullable
	public static JetType getSubstitutedReturnType(@NotNull MethodDescriptor methodDescriptor, TypeSubstitutor substitutor)
	{
		return substitutor.substitute(methodDescriptor.getReturnType());
	}

	@Nullable
	public static MethodDescriptor substituteFunctionDescriptor(@NotNull List<JetType> typeArguments, @NotNull MethodDescriptor methodDescriptor)
	{
		Map<TypeConstructor, JetType> substitutionContext = createSubstitutionContext(methodDescriptor, typeArguments);
		return methodDescriptor.substitute(TypeSubstitutor.create(substitutionContext));
	}

	@NotNull
	public static JetScope getMethodInnerScope(@NotNull JetScope outerScope, @NotNull MethodDescriptor descriptor, @NotNull NapileDeclarationWithBody declarationWithBody, @NotNull BindingTrace trace)
	{
		WritableScope parameterScope = new WritableScopeImpl(outerScope, descriptor, new TraceBasedRedeclarationHandler(trace), "Function inner scope");
		for(TypeParameterDescriptor typeParameter : descriptor.getTypeParameters())
			parameterScope.addTypeParameterDescriptor(typeParameter);
		for(CallParameterDescriptor parameterDescriptor : descriptor.getValueParameters())
			parameterScope.addVariableDescriptor(parameterDescriptor);

		if(declarationWithBody instanceof NapileNamedMethod)
		{
			DeclarationDescriptor varRef = trace.get(BindingContext.REFERENCE_TARGET, ((NapileNamedMethod) declarationWithBody).getVariableRef());
			if(varRef instanceof VariableDescriptor)
			{
				IElementType elementType = ((NapileNamedMethod)declarationWithBody).getPropertyAccessType();

				Modality modality = null;
				// modality option depends on property access type
				// if 'get' var is final
				// if 'set' var is open
				// if 'lazy' - no variable in scope
				if(elementType == NapileTokens.GET_KEYWORD)
					modality = Modality.FINAL;
				else if(elementType == NapileTokens.SET_KEYWORD)
					modality = Modality.OPEN;

				if(modality != null)
				{
					LocalVariableDescriptor variableDescriptor = new LocalVariableDescriptor(varRef.getContainingDeclaration(), Collections.<AnnotationDescriptor>emptyList(), Name.identifier("value"), ((VariableDescriptor) varRef).getType(), modality);
					parameterScope.addVariableDescriptor(variableDescriptor);

					trace.record(BindingContext.AUTO_CREATED_IT, variableDescriptor, true);
					trace.record(BindingContext.AUTO_CREATED_TO, variableDescriptor, (VariableDescriptor) varRef);
				}
			}
		}

		parameterScope.changeLockLevel(WritableScope.LockLevel.READING);
		return parameterScope;
	}

	public static void initializeFromFunctionType(@NotNull MethodDescriptorImpl functionDescriptor, @NotNull JetType functionType, @NotNull ReceiverDescriptor expectedThisObject, @NotNull Modality modality, @NotNull Visibility visibility)
	{
		assert functionType.getConstructor() instanceof MethodTypeConstructor;

		functionDescriptor.initialize(expectedThisObject, Collections.<TypeParameterDescriptorImpl>emptyList(), getValueParameters(functionDescriptor, functionType), ((MethodTypeConstructor) functionType.getConstructor()).getReturnType(), modality, visibility);
	}

	public static <D extends CallableDescriptor> D alphaConvertTypeParameters(D candidate)
	{
		return (D) candidate.substitute(MAKE_TYPE_PARAMETERS_FRESH);
	}

	@Nullable
	public static SimpleMethodDescriptor createDescriptorFromType(@NotNull Name name, @NotNull JetType jetType, @NotNull DeclarationDescriptor owner)
	{
		if(!(jetType.getConstructor() instanceof MethodTypeConstructor))
			return null;

		SimpleMethodDescriptorImpl methodDescriptor = new SimpleMethodDescriptorImpl(owner, Collections.<AnnotationDescriptor>emptyList(), name, CallableMemberDescriptor.Kind.DECLARATION, false, false, false);
		methodDescriptor.initialize(ReceiverDescriptor.NO_RECEIVER, Collections.<TypeParameterDescriptor>emptyList(), getValueParameters(methodDescriptor, jetType), ((MethodTypeConstructor) jetType.getConstructor()).getReturnType(), Modality.FINAL, Visibility.PUBLIC);
		return methodDescriptor;
	}

	@NotNull
	public static List<CallParameterDescriptor> getValueParameters(@NotNull MethodDescriptor methodDescriptor, @NotNull JetType type)
	{
		assert type.getConstructor() instanceof MethodTypeConstructor;

		Map<Name, JetType> parameterTypes = ((MethodTypeConstructor) type.getConstructor()).getParameterTypes();
		List<CallParameterDescriptor> valueParameters = new ArrayList<CallParameterDescriptor>(parameterTypes.size());
		int i = 0;
		for(Map.Entry<Name, JetType> entry : parameterTypes.entrySet())
		{
			CallParameterAsVariableDescriptorImpl valueParameterDescriptor = new CallParameterAsVariableDescriptorImpl(methodDescriptor, i, Collections.<AnnotationDescriptor>emptyList(), entry.getKey(), entry.getValue(), Modality.FINAL);
			valueParameters.add(valueParameterDescriptor);

			i++;
		}
		return valueParameters;
	}
}
