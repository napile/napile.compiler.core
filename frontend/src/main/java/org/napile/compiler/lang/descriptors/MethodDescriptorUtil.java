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
import org.napile.compiler.lang.NapileConstants;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.resolve.BindingTraceKeys;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.TraceBasedRedeclarationHandler;
import org.napile.compiler.lang.resolve.scopes.NapileScope;
import org.napile.compiler.lang.resolve.scopes.WritableScope;
import org.napile.compiler.lang.resolve.scopes.WritableScopeImpl;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.lang.types.NapileType;
import org.napile.compiler.lang.types.MethodTypeConstructor;
import org.napile.compiler.lang.types.TypeConstructor;
import org.napile.compiler.lang.types.TypeSubstitution;
import org.napile.compiler.lang.types.TypeSubstitutor;

/**
 * @author abreslav
 */
public class MethodDescriptorUtil
{
	private static final TypeSubstitutor MAKE_TYPE_PARAMETERS_FRESH = TypeSubstitutor.create(new TypeSubstitution()
	{

		@Override
		public NapileType get(TypeConstructor key)
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

	public static Map<TypeConstructor, NapileType> createSubstitutionContext(@NotNull MethodDescriptor methodDescriptor, List<NapileType> typeArguments)
	{
		if(methodDescriptor.getTypeParameters().isEmpty())
			return Collections.emptyMap();

		Map<TypeConstructor, NapileType> result = new HashMap<TypeConstructor, NapileType>();

		int typeArgumentsSize = typeArguments.size();
		List<TypeParameterDescriptor> typeParameters = methodDescriptor.getTypeParameters();
		assert typeArgumentsSize == typeParameters.size();
		for(int i = 0; i < typeArgumentsSize; i++)
		{
			TypeParameterDescriptor typeParameterDescriptor = typeParameters.get(i);
			NapileType typeArgument = typeArguments.get(i);
			result.put(typeParameterDescriptor.getTypeConstructor(), typeArgument);
		}
		return result;
	}

	@Nullable
	public static List<CallParameterDescriptor> getSubstitutedValueParameters(DeclarationDescriptor newOwner, MethodDescriptor substitutedDescriptor, @NotNull MethodDescriptor methodDescriptor, @NotNull TypeSubstitutor substitutor)
	{
		List<CallParameterDescriptor> result = new ArrayList<CallParameterDescriptor>();
		List<CallParameterDescriptor> unsubstitutedValueParameters = methodDescriptor.getValueParameters();
		for(int i = 0, unsubstitutedValueParametersSize = unsubstitutedValueParameters.size(); i < unsubstitutedValueParametersSize; i++)
		{
			CallParameterDescriptor unsubstitutedValueParameter = unsubstitutedValueParameters.get(i);
			// TODO : Lazy?
			NapileType substitutedType = substitutor.substitute(unsubstitutedValueParameter.getType(), newOwner);
			if(substitutedType == null)
				return null;
			result.add(new CallParameterAsVariableDescriptorImpl(substitutedDescriptor, unsubstitutedValueParameter, unsubstitutedValueParameter.getAnnotations(), unsubstitutedValueParameter.getName(), substitutedType, unsubstitutedValueParameter.getModality(), false, false));
		}
		return result;
	}

	@Nullable
	public static NapileType getSubstitutedReturnType(@NotNull MethodDescriptor methodDescriptor, @NotNull DeclarationDescriptor ownerDescriptor, TypeSubstitutor substitutor)
	{
		return substitutor.substitute(methodDescriptor.getReturnType(), ownerDescriptor);
	}

	@NotNull
	public static NapileScope getMethodInnerScope(@NotNull NapileScope outerScope, @NotNull MethodDescriptor descriptor, @NotNull BindingTrace trace, boolean variableAccessor)
	{
		WritableScope parameterScope = new WritableScopeImpl(outerScope, descriptor, new TraceBasedRedeclarationHandler(trace), "Function inner scope");
		for(TypeParameterDescriptor typeParameter : descriptor.getTypeParameters())
			parameterScope.addTypeParameterDescriptor(typeParameter);
		for(CallParameterDescriptor parameterDescriptor : descriptor.getValueParameters())
			parameterScope.addVariableDescriptor(parameterDescriptor);

		if(variableAccessor)
		{
			LocalVariableDescriptor temp = null;

			if(descriptor.getName().getIdentifier().endsWith("get"))
			{
				temp = new LocalVariableDescriptor(descriptor, Collections.<AnnotationDescriptor>emptyList(), NapileConstants.VARIABLE_FIELD_NAME, descriptor.getReturnType(), Modality.OPEN, false);
			}
			else
			{
				trace.record(BindingTraceKeys.AUTO_CREATED_IT, descriptor.getValueParameters().get(0), Boolean.TRUE);

				temp = new LocalVariableDescriptor(descriptor, Collections.<AnnotationDescriptor>emptyList(), NapileConstants.VARIABLE_FIELD_NAME, descriptor.getReturnType(), Modality.OPEN, true);
			}

			trace.record(BindingTraceKeys.AUTO_CREATED_IT, temp, Boolean.TRUE);
			parameterScope.addVariableDescriptor(temp);
		}

		parameterScope.changeLockLevel(WritableScope.LockLevel.READING);
		return parameterScope;
	}

	public static void initializeFromFunctionType(@NotNull AbstractMethodDescriptorImpl functionDescriptor, @NotNull NapileType functionType, @NotNull ReceiverDescriptor expectedThisObject, @NotNull Modality modality, @NotNull Visibility visibility)
	{
		assert functionType.getConstructor() instanceof MethodTypeConstructor;

		functionDescriptor.initialize(expectedThisObject, Collections.<TypeParameterDescriptorImpl>emptyList(), getValueParameters(functionDescriptor, functionType), ((MethodTypeConstructor) functionType.getConstructor()).getReturnType(), modality, visibility);
	}

	public static <D extends CallableDescriptor> D alphaConvertTypeParameters(D candidate)
	{
		return (D) candidate.substitute(MAKE_TYPE_PARAMETERS_FRESH);
	}

	@Nullable
	public static SimpleMethodDescriptor createDescriptorFromType(@NotNull Name name, @NotNull NapileType napileType, @NotNull DeclarationDescriptor owner)
	{
		if(!(napileType.getConstructor() instanceof MethodTypeConstructor))
			return null;

		SimpleMethodDescriptorImpl methodDescriptor = new SimpleMethodDescriptorImpl(owner, Collections.<AnnotationDescriptor>emptyList(), name, CallableMemberDescriptor.Kind.DECLARATION, false, false, false);
		methodDescriptor.initialize(ReceiverDescriptor.NO_RECEIVER, Collections.<TypeParameterDescriptor>emptyList(), getValueParameters(methodDescriptor, napileType), ((MethodTypeConstructor) napileType.getConstructor()).getReturnType(), Modality.FINAL, Visibility.PUBLIC);
		return methodDescriptor;
	}

	@NotNull
	public static List<CallParameterDescriptor> getValueParameters(@NotNull MethodDescriptor methodDescriptor, @NotNull NapileType type)
	{
		assert type.getConstructor() instanceof MethodTypeConstructor;

		Map<Name, NapileType> parameterTypes = ((MethodTypeConstructor) type.getConstructor()).getParameterTypes();
		List<CallParameterDescriptor> valueParameters = new ArrayList<CallParameterDescriptor>(parameterTypes.size());
		int i = 0;
		for(Map.Entry<Name, NapileType> entry : parameterTypes.entrySet())
		{
			CallParameterAsVariableDescriptorImpl valueParameterDescriptor = new CallParameterAsVariableDescriptorImpl(methodDescriptor, i, Collections.<AnnotationDescriptor>emptyList(), entry.getKey(), entry.getValue(), Modality.FINAL, false, false);
			valueParameters.add(valueParameterDescriptor);

			i++;
		}
		return valueParameters;
	}
}
