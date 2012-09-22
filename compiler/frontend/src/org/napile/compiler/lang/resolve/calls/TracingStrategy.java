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

package org.napile.compiler.lang.resolve.calls;

import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.ParameterDescriptor;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.calls.inference.ConstraintSystem;
import org.napile.compiler.lang.resolve.calls.inference.InferenceErrorData;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.lang.types.JetType;

/**
 * @author abreslav
 */
/*package*/ interface TracingStrategy
{
	TracingStrategy EMPTY = new TracingStrategy()
	{

		@Override
		public <D extends CallableDescriptor> void bindReference(@NotNull BindingTrace trace, @NotNull ResolvedCallWithTrace<D> resolvedCall)
		{
		}

		@Override
		public <D extends CallableDescriptor> void bindResolvedCall(@NotNull BindingTrace trace, @NotNull ResolvedCallWithTrace<D> resolvedCall)
		{
		}

		@Override
		public void unresolvedReference(@NotNull BindingTrace trace)
		{
		}

		@Override
		public <D extends CallableDescriptor> void recordAmbiguity(BindingTrace trace, Collection<ResolvedCallWithTrace<D>> candidates)
		{
		}

		@Override
		public void wrongReceiverType(@NotNull BindingTrace trace, @NotNull ReceiverDescriptor receiverParameter, @NotNull ReceiverDescriptor receiverArgument)
		{
		}

		@Override
		public void noReceiverAllowed(@NotNull BindingTrace trace)
		{
		}

		@Override
		public void noValueForParameter(@NotNull BindingTrace trace, @NotNull ParameterDescriptor valueParameter)
		{
		}

		@Override
		public void wrongNumberOfTypeArguments(@NotNull BindingTrace trace, int expectedTypeArgumentCount)
		{
		}

		@Override
		public <D extends CallableDescriptor> void ambiguity(@NotNull BindingTrace trace, @NotNull Collection<ResolvedCallWithTrace<D>> descriptors)
		{
		}

		@Override
		public <D extends CallableDescriptor> void noneApplicable(@NotNull BindingTrace trace, @NotNull Collection<ResolvedCallWithTrace<D>> descriptors)
		{
		}

		@Override
		public void instantiationOfAbstractClass(@NotNull BindingTrace trace)
		{
		}

		@Override
		public void unsafeCall(@NotNull BindingTrace trace, @NotNull JetType type, boolean isCallForImplicitInvoke)
		{
		}

		@Override
		public void unnecessarySafeCall(@NotNull BindingTrace trace, @NotNull JetType type)
		{
		}

		@Override
		public void danglingFunctionLiteralArgumentSuspected(@NotNull BindingTrace trace, @NotNull List<NapileExpression> functionLiteralArguments)
		{
		}

		@Override
		public void invisibleMember(@NotNull BindingTrace trace, @NotNull DeclarationDescriptor descriptor)
		{
		}

		@Override
		public void instanceCallFromStatic(@NotNull BindingTrace trace, @NotNull DeclarationDescriptor descriptor)
		{
		}

		@Override
		public void typeInferenceFailed(@NotNull BindingTrace trace, @NotNull InferenceErrorData inferenceErrorData, @NotNull ConstraintSystem systemWithoutExpectedTypeConstraint)
		{
		}

		@Override
		public void upperBoundViolated(@NotNull BindingTrace trace, @NotNull InferenceErrorData inferenceErrorData)
		{
		}
	};

	<D extends CallableDescriptor> void bindReference(@NotNull BindingTrace trace, @NotNull ResolvedCallWithTrace<D> resolvedCall);

	<D extends CallableDescriptor> void bindResolvedCall(@NotNull BindingTrace trace, @NotNull ResolvedCallWithTrace<D> resolvedCall);

	void unresolvedReference(@NotNull BindingTrace trace);

	<D extends CallableDescriptor> void recordAmbiguity(BindingTrace trace, Collection<ResolvedCallWithTrace<D>> candidates);

	void wrongReceiverType(@NotNull BindingTrace trace, @NotNull ReceiverDescriptor receiverParameter, @NotNull ReceiverDescriptor receiverArgument);

	void noReceiverAllowed(@NotNull BindingTrace trace);

	void noValueForParameter(@NotNull BindingTrace trace, @NotNull ParameterDescriptor valueParameter);

	void wrongNumberOfTypeArguments(@NotNull BindingTrace trace, int expectedTypeArgumentCount);

	<D extends CallableDescriptor> void ambiguity(@NotNull BindingTrace trace, @NotNull Collection<ResolvedCallWithTrace<D>> descriptors);

	<D extends CallableDescriptor> void noneApplicable(@NotNull BindingTrace trace, @NotNull Collection<ResolvedCallWithTrace<D>> descriptors);

	void instantiationOfAbstractClass(@NotNull BindingTrace trace);

	void unsafeCall(@NotNull BindingTrace trace, @NotNull JetType type, boolean isCallForImplicitInvoke);

	void unnecessarySafeCall(@NotNull BindingTrace trace, @NotNull JetType type);

	void danglingFunctionLiteralArgumentSuspected(@NotNull BindingTrace trace, @NotNull List<NapileExpression> functionLiteralArguments);

	void invisibleMember(@NotNull BindingTrace trace, @NotNull DeclarationDescriptor descriptor);

	void instanceCallFromStatic(@NotNull BindingTrace trace, @NotNull DeclarationDescriptor descriptor);

	void typeInferenceFailed(@NotNull BindingTrace trace, @NotNull InferenceErrorData inferenceErrorData, @NotNull ConstraintSystem systemWithoutExpectedTypeConstraint);

	void upperBoundViolated(@NotNull BindingTrace trace, @NotNull InferenceErrorData inferenceErrorData);
}
