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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.TraceBasedRedeclarationHandler;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.resolve.scopes.WritableScope;
import org.napile.compiler.lang.resolve.scopes.WritableScopeImpl;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.TypeConstructor;
import org.napile.compiler.lang.types.TypeSubstitution;
import org.napile.compiler.lang.types.TypeSubstitutor;
import org.napile.compiler.lang.types.lang.JetStandardClasses;

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

	public static Map<TypeConstructor, JetType> createSubstitutionContext(@NotNull FunctionDescriptor functionDescriptor, List<JetType> typeArguments)
	{
		if(functionDescriptor.getTypeParameters().isEmpty())
			return Collections.emptyMap();

		Map<TypeConstructor, JetType> result = new HashMap<TypeConstructor, JetType>();

		int typeArgumentsSize = typeArguments.size();
		List<TypeParameterDescriptor> typeParameters = functionDescriptor.getTypeParameters();
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
	public static List<ValueParameterDescriptor> getSubstitutedValueParameters(FunctionDescriptor substitutedDescriptor, @NotNull FunctionDescriptor functionDescriptor, @NotNull TypeSubstitutor substitutor)
	{
		List<ValueParameterDescriptor> result = new ArrayList<ValueParameterDescriptor>();
		List<ValueParameterDescriptor> unsubstitutedValueParameters = functionDescriptor.getValueParameters();
		for(int i = 0, unsubstitutedValueParametersSize = unsubstitutedValueParameters.size(); i < unsubstitutedValueParametersSize; i++)
		{
			ValueParameterDescriptor unsubstitutedValueParameter = unsubstitutedValueParameters.get(i);
			// TODO : Lazy?
			JetType substitutedType = substitutor.substitute(unsubstitutedValueParameter.getType());
			JetType varargElementType = unsubstitutedValueParameter.getVarargElementType();
			JetType substituteVarargElementType = varargElementType == null ? null : substitutor.substitute(varargElementType);
			if(substitutedType == null)
				return null;
			result.add(new ValueParameterDescriptorImpl(substitutedDescriptor, unsubstitutedValueParameter, unsubstitutedValueParameter.getAnnotations(), unsubstitutedValueParameter.isVar(), substitutedType, substituteVarargElementType));
		}
		return result;
	}

	@Nullable
	public static JetType getSubstitutedReturnType(@NotNull FunctionDescriptor functionDescriptor, TypeSubstitutor substitutor)
	{
		return substitutor.substitute(functionDescriptor.getReturnType());
	}

	@Nullable
	public static FunctionDescriptor substituteFunctionDescriptor(@NotNull List<JetType> typeArguments, @NotNull FunctionDescriptor functionDescriptor)
	{
		Map<TypeConstructor, JetType> substitutionContext = createSubstitutionContext(functionDescriptor, typeArguments);
		return functionDescriptor.substitute(TypeSubstitutor.create(substitutionContext));
	}

	@NotNull
	public static JetScope getFunctionInnerScope(@NotNull JetScope outerScope, @NotNull FunctionDescriptor descriptor, @NotNull BindingTrace trace)
	{
		WritableScope parameterScope = new WritableScopeImpl(outerScope, descriptor, new TraceBasedRedeclarationHandler(trace), "Function inner scope");
		ReceiverDescriptor receiver = descriptor.getReceiverParameter();
		if(receiver.exists())
		{
			parameterScope.setImplicitReceiver(receiver);
		}
		for(TypeParameterDescriptor typeParameter : descriptor.getTypeParameters())
		{
			parameterScope.addTypeParameterDescriptor(typeParameter);
		}
		for(ValueParameterDescriptor valueParameterDescriptor : descriptor.getValueParameters())
		{
			parameterScope.addVariableDescriptor(valueParameterDescriptor);
		}
		parameterScope.addLabeledDeclaration(descriptor);
		parameterScope.changeLockLevel(WritableScope.LockLevel.READING);
		return parameterScope;
	}

	public static void initializeFromFunctionType(@NotNull FunctionDescriptorImpl functionDescriptor, @NotNull JetType functionType, @NotNull ReceiverDescriptor expectedThisObject, @NotNull Modality modality, @NotNull Visibility visibility)
	{

		assert JetStandardClasses.isFunctionType(functionType);
		functionDescriptor.initialize(JetStandardClasses.getReceiverType(functionType), expectedThisObject, Collections.<TypeParameterDescriptorImpl>emptyList(), JetStandardClasses.getValueParameters(functionDescriptor, functionType), JetStandardClasses.getReturnTypeFromFunctionType(functionType), modality, visibility);
	}

	public static <D extends CallableDescriptor> D alphaConvertTypeParameters(D candidate)
	{
		return (D) candidate.substitute(MAKE_TYPE_PARAMETERS_FRESH);
	}

	public static FunctionDescriptor getInvokeFunction(@NotNull JetType functionType)
	{
		assert JetStandardClasses.isFunctionType(functionType);

		ClassifierDescriptor classDescriptorForFunction = functionType.getConstructor().getDeclarationDescriptor();
		assert classDescriptorForFunction instanceof ClassDescriptor;
		Collection<FunctionDescriptor> invokeFunctions = ((ClassDescriptor) classDescriptorForFunction).getMemberScope(functionType.getArguments()).getFunctions(Name.identifier("invoke"));
		assert invokeFunctions.size() == 1;
		return invokeFunctions.iterator().next();
	}
}
