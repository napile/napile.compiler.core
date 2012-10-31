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

import static org.napile.compiler.lang.diagnostics.Errors.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.ParameterDescriptor;
import org.napile.compiler.lang.diagnostics.Errors;
import org.napile.compiler.lang.psi.Call;
import org.napile.compiler.lang.psi.NapileBinaryExpression;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileReferenceExpression;
import org.napile.compiler.lang.psi.NapileSimpleNameExpression;
import org.napile.compiler.lang.psi.NapileTypeArgumentList;
import org.napile.compiler.lang.psi.NapileValueArgumentList;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.calls.autocasts.DataFlowInfo;
import org.napile.compiler.lang.resolve.calls.inference.ConstraintSystem;
import org.napile.compiler.lang.resolve.calls.inference.InferenceErrorData;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.lang.types.ErrorUtils;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.expressions.OperatorConventions;
import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.compiler.lang.lexer.NapileToken;
import com.google.common.collect.Sets;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;

/**
 * Stores candidates for call resolution.
 *
 * @author abreslav
 */
public class ResolutionTask<D extends CallableDescriptor, F extends D> extends ResolutionContext
{
	private final Collection<ResolutionCandidate<D>> candidates;
	private final Set<ResolvedCallWithTrace<F>> resolvedCalls = Sets.newLinkedHashSet();
	/*package*/ final NapileReferenceExpression reference;
	private DescriptorCheckStrategy checkingStrategy;

	public ResolutionTask(@NotNull Collection<ResolutionCandidate<D>> candidates, @NotNull NapileReferenceExpression reference, BindingTrace trace, JetScope scope, Call call, JetType expectedType, DataFlowInfo dataFlowInfo)
	{
		super(trace, scope, call, expectedType, dataFlowInfo);
		this.candidates = candidates;
		this.reference = reference;
	}

	public ResolutionTask(@NotNull Collection<ResolutionCandidate<D>> candidates, @NotNull NapileReferenceExpression reference, @NotNull BasicResolutionContext context)
	{
		this(candidates, reference, context.trace, context.scope, context.call, context.expectedType, context.dataFlowInfo);
	}

	@NotNull
	public Collection<ResolutionCandidate<D>> getCandidates()
	{
		return candidates;
	}

	@NotNull
	public Set<ResolvedCallWithTrace<F>> getResolvedCalls()
	{
		return resolvedCalls;
	}

	public void setCheckingStrategy(DescriptorCheckStrategy strategy)
	{
		checkingStrategy = strategy;
	}

	public boolean performAdvancedChecks(D descriptor, BindingTrace trace, TracingStrategy tracing)
	{
		if(checkingStrategy != null && !checkingStrategy.performAdvancedChecks(descriptor, trace, tracing))
		{
			return false;
		}
		return true;
	}

	public ResolutionTask<D, F> withTrace(BindingTrace newTrace)
	{
		ResolutionTask<D, F> newTask = new ResolutionTask<D, F>(candidates, reference, newTrace, scope, call, expectedType, dataFlowInfo);
		newTask.setCheckingStrategy(checkingStrategy);
		return newTask;
	}

	public interface DescriptorCheckStrategy
	{
		<D extends CallableDescriptor> boolean performAdvancedChecks(D descriptor, BindingTrace trace, TracingStrategy tracing);
	}

	public final TracingStrategy tracing = new TracingStrategy()
	{
		@Override
		public <D extends CallableDescriptor> void bindReference(@NotNull BindingTrace trace, @NotNull ResolvedCallWithTrace<D> resolvedCall)
		{
			CallableDescriptor descriptor = resolvedCall.getResultingDescriptor();
			if(resolvedCall instanceof VariableAsFunctionResolvedCall)
			{
				descriptor = ((VariableAsFunctionResolvedCall) resolvedCall).getVariableCall().getResultingDescriptor();
			}
			DeclarationDescriptor storedReference = trace.get(BindingContext.REFERENCE_TARGET, reference);
			if(storedReference == null || !ErrorUtils.isError(descriptor))
			{
				trace.record(BindingContext.REFERENCE_TARGET, reference, descriptor);
			}
		}

		@Override
		public <D extends CallableDescriptor> void bindResolvedCall(@NotNull BindingTrace trace, @NotNull ResolvedCallWithTrace<D> resolvedCall)
		{
			trace.record(BindingContext.RESOLVED_CALL, call.getCalleeExpression(), resolvedCall);
			trace.record(BindingContext.CALL, call.getCalleeExpression(), call);
		}

		@Override
		public <D extends CallableDescriptor> void recordAmbiguity(BindingTrace trace, Collection<ResolvedCallWithTrace<D>> candidates)
		{
			Collection<D> descriptors = new HashSet<D>(candidates.size());
			for(ResolvedCallWithTrace<D> candidate : candidates)
				descriptors.add(candidate.getCandidateDescriptor());

			trace.record(BindingContext.AMBIGUOUS_REFERENCE_TARGET, reference, descriptors);
		}

		@Override
		public void unresolvedReference(@NotNull BindingTrace trace)
		{
			trace.report(UNRESOLVED_REFERENCE.on(reference));
		}

		@Override
		public void noValueForParameter(@NotNull BindingTrace trace, @NotNull ParameterDescriptor valueParameter)
		{
			PsiElement reportOn;
			NapileValueArgumentList valueArgumentList = call.getValueArgumentList();
			if(valueArgumentList != null)
			{
				reportOn = valueArgumentList;
			}
			else
			{
				reportOn = reference;
			}
			trace.report(NO_VALUE_FOR_PARAMETER.on(reportOn, valueParameter));
		}

		@Override
		public void wrongReceiverType(@NotNull BindingTrace trace, @NotNull ReceiverDescriptor receiverParameter, @NotNull ReceiverDescriptor receiverArgument)
		{
			if(receiverArgument instanceof ExpressionReceiver)
			{
				ExpressionReceiver expressionReceiver = (ExpressionReceiver) receiverArgument;
				trace.report(TYPE_MISMATCH.on(expressionReceiver.getExpression(), receiverParameter.getType(), receiverArgument.getType()));
			}
			else
			{
				trace.report(TYPE_MISMATCH.on(reference, receiverParameter.getType(), receiverArgument.getType()));
			}
		}

		@Override
		public void noReceiverAllowed(@NotNull BindingTrace trace)
		{
			trace.report(NO_RECEIVER_ADMITTED.on(reference));
		}

		@Override
		public void wrongNumberOfTypeArguments(@NotNull BindingTrace trace, int expectedTypeArgumentCount)
		{
			NapileTypeArgumentList typeArgumentList = call.getTypeArgumentList();
			if(typeArgumentList != null)
			{
				trace.report(WRONG_NUMBER_OF_TYPE_ARGUMENTS.on(typeArgumentList, expectedTypeArgumentCount));
			}
			else
			{
				trace.report(WRONG_NUMBER_OF_TYPE_ARGUMENTS.on(reference, expectedTypeArgumentCount));
			}
		}

		@Override
		public <D extends CallableDescriptor> void ambiguity(@NotNull BindingTrace trace, @NotNull Collection<ResolvedCallWithTrace<D>> descriptors)
		{
			trace.report(OVERLOAD_RESOLUTION_AMBIGUITY.on(call.getCallElement(), descriptors));
		}

		@Override
		public <D extends CallableDescriptor> void noneApplicable(@NotNull BindingTrace trace, @NotNull Collection<ResolvedCallWithTrace<D>> descriptors)
		{
			trace.report(NONE_APPLICABLE.on(reference, descriptors));
		}

		@Override
		public void instantiationOfAbstractClass(@NotNull BindingTrace trace)
		{
			trace.report(CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS.on(call.getCallElement()));
		}

		@Override
		public void unsafeCall(@NotNull BindingTrace trace, @NotNull JetType type, boolean isCallForImplicitInvoke)
		{
			ASTNode callOperationNode = call.getCallOperationNode();
			if(callOperationNode != null && !isCallForImplicitInvoke)
			{
				trace.report(UNSAFE_CALL.on(callOperationNode.getPsi(), type));
			}
			else
			{
				PsiElement callElement = call.getCallElement();
				if(callElement instanceof NapileBinaryExpression)
				{
					NapileBinaryExpression binaryExpression = (NapileBinaryExpression) callElement;
					NapileSimpleNameExpression operationReference = binaryExpression.getOperationReference();

					Name operationString = operationReference.getReferencedNameElementType() == NapileTokens.IDENTIFIER ? Name.identifier(operationReference.getText()) : OperatorConventions.getNameForOperationSymbol((NapileToken) operationReference.getReferencedNameElementType());

					NapileExpression right = binaryExpression.getRight();
					if(right != null)
					{
						trace.report(UNSAFE_INFIX_CALL.on(reference, binaryExpression.getLeft().getText(), operationString.getName(), right.getText()));
					}
				}
				else
				{
					trace.report(UNSAFE_CALL.on(reference, type));
				}
			}
		}

		@Override
		public void unnecessarySafeCall(@NotNull BindingTrace trace, @NotNull JetType type)
		{
			ASTNode callOperationNode = call.getCallOperationNode();
			assert callOperationNode != null;
			trace.report(UNNECESSARY_SAFE_CALL.on(callOperationNode.getPsi(), type));
		}

		@Override
		public void danglingFunctionLiteralArgumentSuspected(@NotNull BindingTrace trace, @NotNull List<NapileExpression> functionLiteralArguments)
		{
			for(NapileExpression functionLiteralArgument : functionLiteralArguments)
			{
				trace.report(DANGLING_FUNCTION_LITERAL_ARGUMENT_SUSPECTED.on(functionLiteralArgument));
			}
		}

		@Override
		public void invisibleMember(@NotNull BindingTrace trace, @NotNull DeclarationDescriptor descriptor)
		{
			trace.report(INVISIBLE_MEMBER.on(call.getCallElement(), descriptor, descriptor.getContainingDeclaration()));
		}

		@Override
		public void instanceCallFromStatic(@NotNull BindingTrace trace, @NotNull DeclarationDescriptor descriptor)
		{
			trace.report(INSTANCE_CALL_FROM_STATIC_CONTEXT.on(call.getCallElement()));
		}

		@Override
		public void typeInferenceFailed(@NotNull BindingTrace trace, @NotNull InferenceErrorData data, @NotNull ConstraintSystem systemWithoutExpectedTypeConstraint)
		{
			ConstraintSystem constraintSystem = data.constraintSystem;
			assert !constraintSystem.isSuccessful();
			if(constraintSystem.hasErrorInConstrainingTypes())
			{
				return;
			}
			boolean successfulWithoutExpectedTypeConstraint = systemWithoutExpectedTypeConstraint.isSuccessful();
			if(constraintSystem.hasExpectedTypeMismatch() || successfulWithoutExpectedTypeConstraint)
			{
				JetType returnType = data.descriptor.getReturnType();
				assert returnType != null;
				if(successfulWithoutExpectedTypeConstraint)
				{
					returnType = systemWithoutExpectedTypeConstraint.getResultingSubstitutor().substitute(returnType);
					assert returnType != null;
				}
				trace.report(TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH.on(reference, returnType, data.expectedType));
			}
			else if(constraintSystem.hasTypeConstructorMismatch())
			{
				trace.report(TYPE_INFERENCE_TYPE_CONSTRUCTOR_MISMATCH.on(reference, data));
			}
			else if(constraintSystem.hasConflictingConstraints())
			{
				trace.report(TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS.on(reference, data));
			}
			else
			{
				assert constraintSystem.hasUnknownParameters();
				trace.report(TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER.on(reference, data));
			}
		}

		@Override
		public void upperBoundViolated(@NotNull BindingTrace trace, @NotNull InferenceErrorData inferenceErrorData)
		{
			trace.report(Errors.TYPE_INFERENCE_UPPER_BOUND_VIOLATED.on(reference, inferenceErrorData));
		}
	};
}
