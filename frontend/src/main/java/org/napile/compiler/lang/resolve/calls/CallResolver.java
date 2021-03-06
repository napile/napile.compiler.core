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

import static org.napile.compiler.lang.diagnostics.Errors.AUTOCAST_IMPOSSIBLE;
import static org.napile.compiler.lang.diagnostics.Errors.CALLEE_NOT_A_FUNCTION;
import static org.napile.compiler.lang.diagnostics.Errors.NOT_A_CLASS;
import static org.napile.compiler.lang.diagnostics.Errors.NO_CONSTRUCTOR;
import static org.napile.compiler.lang.diagnostics.Errors.UNRESOLVED_REFERENCE;
import static org.napile.compiler.lang.resolve.BindingTraceKeys.AUTOCAST;
import static org.napile.compiler.lang.resolve.BindingTraceKeys.NON_DEFAULT_EXPRESSION_DATA_FLOW;
import static org.napile.compiler.lang.resolve.BindingTraceKeys.RESOLUTION_RESULTS_FOR_FUNCTION;
import static org.napile.compiler.lang.resolve.BindingTraceKeys.RESOLUTION_RESULTS_FOR_PROPERTY;
import static org.napile.compiler.lang.resolve.BindingTraceKeys.RESOLUTION_SCOPE;
import static org.napile.compiler.lang.resolve.BindingTraceKeys.TRACE_DELTAS_CACHE;
import static org.napile.compiler.lang.resolve.calls.ResolvedCallImpl.MAP_TO_CANDIDATE;
import static org.napile.compiler.lang.resolve.calls.ResolvedCallImpl.MAP_TO_RESULT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.lib.NapileAnnotationPackage;
import org.napile.asm.resolve.name.FqName;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.descriptors.*;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.lang.resolve.AnnotationUtils;
import org.napile.compiler.lang.resolve.BindingTraceKeys;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.BindingTraceImpl;
import org.napile.compiler.lang.resolve.DelegatingBindingTrace;
import org.napile.compiler.lang.resolve.DescriptorUtils;
import org.napile.compiler.lang.resolve.OverridingUtil;
import org.napile.compiler.lang.resolve.TemporaryBindingTrace;
import org.napile.compiler.lang.resolve.calls.autocasts.AutoCastServiceImpl;
import org.napile.compiler.lang.resolve.calls.autocasts.DataFlowInfo;
import org.napile.compiler.lang.resolve.calls.inference.ConstraintPosition;
import org.napile.compiler.lang.resolve.calls.inference.ConstraintSystem;
import org.napile.compiler.lang.resolve.calls.inference.ConstraintSystemImpl;
import org.napile.compiler.lang.resolve.calls.inference.ConstraintSystemWithPriorities;
import org.napile.compiler.lang.resolve.calls.inference.ConstraintsUtil;
import org.napile.compiler.lang.resolve.calls.inference.InferenceErrorData;
import org.napile.compiler.lang.resolve.processors.DescriptorResolver;
import org.napile.compiler.lang.resolve.processors.TypeResolver;
import org.napile.compiler.lang.resolve.scopes.NapileScope;
import org.napile.compiler.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.lang.types.ErrorUtils;
import org.napile.compiler.lang.types.NapileType;
import org.napile.compiler.lang.types.NapileTypeInfo;
import org.napile.compiler.lang.types.MethodTypeConstructor;
import org.napile.compiler.lang.types.TypeConstructor;
import org.napile.compiler.lang.types.TypeSubstitutor;
import org.napile.compiler.lang.types.TypeUtils;
import org.napile.compiler.lang.types.checker.NapileTypeChecker;
import org.napile.compiler.lang.types.expressions.ExpressionTypingServices;
import org.napile.compiler.util.slicedmap.WritableSlice;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.psi.PsiElement;

/**
 * @author abreslav
 */
public class CallResolver
{
	private final NapileTypeChecker typeChecker = NapileTypeChecker.INSTANCE;

	@NotNull
	private OverloadingConflictResolver overloadingConflictResolver;
	@NotNull
	private ExpressionTypingServices expressionTypingServices;
	@NotNull
	private TypeResolver typeResolver;
	@NotNull
	private DescriptorResolver descriptorResolver;


	@Inject
	public void setOverloadingConflictResolver(@NotNull OverloadingConflictResolver overloadingConflictResolver)
	{
		this.overloadingConflictResolver = overloadingConflictResolver;
	}

	@Inject
	public void setExpressionTypingServices(@NotNull ExpressionTypingServices expressionTypingServices)
	{
		this.expressionTypingServices = expressionTypingServices;
	}

	@Inject
	public void setTypeResolver(@NotNull TypeResolver typeResolver)
	{
		this.typeResolver = typeResolver;
	}

	@Inject
	public void setDescriptorResolver(@NotNull DescriptorResolver descriptorResolver)
	{
		this.descriptorResolver = descriptorResolver;
	}


	@NotNull
	public OverloadResolutionResults<VariableDescriptor> resolveSimpleProperty(@NotNull BasicResolutionContext context)
	{
		NapileExpression calleeExpression = context.call.getCalleeExpression();
		assert calleeExpression instanceof NapileSimpleNameExpression;
		NapileSimpleNameExpression nameExpression = (NapileSimpleNameExpression) calleeExpression;
		Name referencedName = nameExpression.getReferencedNameAsName();
		if(referencedName == null)
		{
			return OverloadResolutionResultsImpl.nameNotFound();
		}
		List<CallableDescriptorCollector<? extends VariableDescriptor>> callableDescriptorCollectors = Collections.<CallableDescriptorCollector<? extends VariableDescriptor>>singletonList(CallableDescriptorCollectors.VARIABLES);

		List<ResolutionTask<VariableDescriptor, VariableDescriptor>> prioritizedTasks = TaskPrioritizer.<VariableDescriptor, VariableDescriptor>computePrioritizedTasks(context, referencedName, nameExpression, callableDescriptorCollectors);
		return doResolveCallOrGetCachedResults(RESOLUTION_RESULTS_FOR_PROPERTY, context, prioritizedTasks, CallTransformer.VARIABLE_CALL_TRANSFORMER, nameExpression, true);
	}

	@NotNull
	public OverloadResolutionResults<MethodDescriptor> resolveCallWithGivenName(@NotNull BasicResolutionContext context, @NotNull final NapileReferenceExpression functionReference, @NotNull Name name, boolean bindReference)
	{
		List<ResolutionTask<CallableDescriptor, MethodDescriptor>> tasks = TaskPrioritizer.<CallableDescriptor, MethodDescriptor>computePrioritizedTasks(context, name, functionReference, CallableDescriptorCollectors.ALL);
		return doResolveCallOrGetCachedResults(RESOLUTION_RESULTS_FOR_FUNCTION, context, tasks, CallTransformer.ANONYM_METHOD_CALL_TRANSFORMER, functionReference, bindReference);
	}

	@NotNull
	public OverloadResolutionResults<MethodDescriptor> resolveFunctionCall(@NotNull BindingTrace trace, @NotNull NapileScope scope, @NotNull Call call, @NotNull NapileType expectedType, @NotNull DataFlowInfo dataFlowInfo)
	{
		return resolveFunctionCall(BasicResolutionContext.create(trace, scope, call, expectedType, dataFlowInfo));
	}

	@NotNull
	public OverloadResolutionResults<MethodDescriptor> resolveFunctionCall(@NotNull BasicResolutionContext context)
	{

		ProgressIndicatorProvider.checkCanceled();

		List<ResolutionTask<CallableDescriptor, MethodDescriptor>> prioritizedTasks;

		NapileExpression calleeExpression = context.call.getCalleeExpression();
		final NapileReferenceExpression functionReference;
		if(calleeExpression instanceof NapileSimpleNameExpression)
		{
			NapileSimpleNameExpression expression = (NapileSimpleNameExpression) calleeExpression;
			functionReference = expression;

			Name name = expression.getReferencedNameAsName();
			if(name == null)
				return checkArgumentTypesAndFail(context);

			prioritizedTasks = TaskPrioritizer.<CallableDescriptor, MethodDescriptor>computePrioritizedTasks(context, name, functionReference, CallableDescriptorCollectors.ALL);
			ResolutionTask.DescriptorCheckStrategy abstractConstructorCheck = new ResolutionTask.DescriptorCheckStrategy()
			{
				@Override
				public <D extends CallableDescriptor> boolean performAdvancedChecks(D descriptor, BindingTrace trace, TracingStrategy tracing)
				{
					if(descriptor instanceof ConstructorDescriptor && descriptor.getContainingDeclaration() instanceof ClassDescriptor)
					{
						Modality modality = ((ClassDescriptor) descriptor.getContainingDeclaration()).getModality();
						if(modality == Modality.ABSTRACT)
						{
							tracing.instantiationOfAbstractClass(trace);
							return false;
						}
					}
					return true;
				}
			};
			for(ResolutionTask task : prioritizedTasks)
			{
				task.setCheckingStrategy(abstractConstructorCheck);
			}
		}
		else
		{
			NapileValueArgumentList valueArgumentList = context.call.getValueArgumentList();
			PsiElement reportAbsenceOn = valueArgumentList == null ? context.call.getCallElement() : valueArgumentList;
			if(calleeExpression instanceof NapileConstructorCalleeExpression)
			{
				assert !context.call.getExplicitReceiver().exists();

				prioritizedTasks = Lists.newArrayList();

				NapileConstructorCalleeExpression expression = (NapileConstructorCalleeExpression) calleeExpression;

				NapileTypeReference typeReference = expression.getTypeReference();
				if(typeReference == null)
					return checkArgumentTypesAndFail(context); // No type there
				functionReference = new NapileFakeReferenceImpl(typeReference);

				NapileType constructedType = typeResolver.resolveType(context.scope, typeReference, context.trace, true);
				DeclarationDescriptor declarationDescriptor = constructedType.getConstructor().getDeclarationDescriptor();

				if(declarationDescriptor != null)
				{
					ClassifierDescriptor classDescriptor = (ClassifierDescriptor) declarationDescriptor;
					Set<ConstructorDescriptor> constructors = classDescriptor.getConstructors();
					if(constructors.isEmpty())
					{
						context.trace.report(NO_CONSTRUCTOR.on(reportAbsenceOn));
						return checkArgumentTypesAndFail(context);
					}
					Collection<ResolutionCandidate<CallableDescriptor>> candidates = TaskPrioritizer.<CallableDescriptor>convertWithImpliedThis(context.scope, Collections.<ReceiverDescriptor>singletonList(ReceiverDescriptor.NO_RECEIVER), constructors);
					for(ResolutionCandidate<CallableDescriptor> candidate : candidates)
					{
						candidate.setSafeCall(NapilePsiUtil.isSafeCall(context.call));
					}
					prioritizedTasks.add(new ResolutionTask<CallableDescriptor, MethodDescriptor>(candidates, functionReference, context));  // !! DataFlowInfo.EMPTY
				}
				else
				{
					context.trace.report(NOT_A_CLASS.on(calleeExpression));
					return checkArgumentTypesAndFail(context);
				}
			}
			else if(calleeExpression != null)
			{
				// Here we handle the case where the callee expression must be something of type function, e.g. (foo.bar())(1, 2)
				NapileType calleeType = expressionTypingServices.safeGetType(context.scope, calleeExpression, TypeUtils.NO_EXPECTED_TYPE, context.dataFlowInfo, context.trace); // We are actually expecting a function, but there seems to be no easy way of expressing this

				if(!(calleeType.getConstructor() instanceof MethodTypeConstructor))
				{
					//                    checkTypesWithNoCallee(trace, scope, call);
					if(!ErrorUtils.isErrorType(calleeType))
					{
						context.trace.report(CALLEE_NOT_A_FUNCTION.on(calleeExpression, calleeType));
					}
					return checkArgumentTypesAndFail(context);
				}

				AbstractMethodDescriptorImpl functionDescriptor = new ExpressionAsMethodDescriptor(context.scope.getContainingDeclaration(), Name.special("<for expression " +
						calleeExpression.getText() +
						">"));
				MethodDescriptorUtil.initializeFromFunctionType(functionDescriptor, calleeType, ReceiverDescriptor.NO_RECEIVER, Modality.FINAL, Visibility.LOCAL2);
				ResolutionCandidate<CallableDescriptor> resolutionCandidate = ResolutionCandidate.<CallableDescriptor>create(functionDescriptor, NapilePsiUtil.isSafeCall(context.call));
				resolutionCandidate.setExplicitReceiverKind(ExplicitReceiverKind.RECEIVER_ARGUMENT);

				// strictly speaking, this is a hack:
				// we need to pass a reference, but there's no reference in the PSI,
				// so we wrap what we have into a fake reference and pass it on (unwrap on the other end)
				functionReference = new NapileFakeReferenceImpl(calleeExpression);

				prioritizedTasks = Collections.singletonList(new ResolutionTask<CallableDescriptor, MethodDescriptor>(Collections.singleton(resolutionCandidate), functionReference, context));
			}
			else
			{
				//                checkTypesWithNoCallee(trace, scope, call);
				return checkArgumentTypesAndFail(context);
			}
		}

		return doResolveCallOrGetCachedResults(RESOLUTION_RESULTS_FOR_FUNCTION, context, prioritizedTasks, CallTransformer.ANONYM_METHOD_CALL_TRANSFORMER, functionReference, true);
	}

	private <D extends CallableDescriptor, F extends D> OverloadResolutionResults<F> doResolveCallOrGetCachedResults(@NotNull WritableSlice<CallKey, OverloadResolutionResults<F>> resolutionResultsSlice, @NotNull final BasicResolutionContext context, @NotNull final List<ResolutionTask<D, F>> prioritizedTasks, @NotNull CallTransformer<D, F> callTransformer, @NotNull final NapileReferenceExpression reference, boolean bindReference)
	{
		PsiElement element = context.call.getCallElement();
		if(element instanceof NapileExpression)
		{
			OverloadResolutionResults<F> cachedResults = context.trace.get(resolutionResultsSlice, CallKey.create(context.call.getCallType(), (NapileExpression) element));
			if(cachedResults != null)
			{
				DelegatingBindingTrace delegatingTrace = context.trace.safeGet(TRACE_DELTAS_CACHE, (NapileExpression) element);

				delegatingTrace.addAllMyDataTo(context.trace);
				return cachedResults;
			}
		}
		TemporaryBindingTrace delegatingBindingTrace = TemporaryBindingTrace.create(context.trace);
		BasicResolutionContext newContext = context.replaceTrace(delegatingBindingTrace);
		OverloadResolutionResults<F> results = doResolveCall(newContext, prioritizedTasks, callTransformer, reference, bindReference);
		DelegatingBindingTrace cloneDelta = new DelegatingBindingTrace(new BindingTraceImpl());
		delegatingBindingTrace.addAllMyDataTo(cloneDelta);
		cacheResults(resolutionResultsSlice, context, results, cloneDelta);

		if(prioritizedTasks.isEmpty())
		{
			delegatingBindingTrace.commit();
			return results;
		}

		TemporaryBindingTrace temporaryBindingTrace = null;
		if(results instanceof OverloadResolutionResultsImpl)
		{
			temporaryBindingTrace = ((OverloadResolutionResultsImpl) results).getTrace();
			if(temporaryBindingTrace != null)
			{
				newContext = newContext.replaceTrace(temporaryBindingTrace);
			}
		}
		TracingStrategy tracing = prioritizedTasks.iterator().next().tracing;
		OverloadResolutionResults<F> completeResults = completeTypeInferenceDependentOnExpectedType(newContext, results, tracing);
		if(temporaryBindingTrace != null)
		{
			temporaryBindingTrace.commit();
		}
		delegatingBindingTrace.commit();
		return completeResults;
	}

	private <D extends CallableDescriptor> OverloadResolutionResults<D> completeTypeInferenceDependentOnExpectedType(@NotNull BasicResolutionContext context, @NotNull OverloadResolutionResults<D> results, @NotNull TracingStrategy tracing)
	{
		if(results.getResultCode() != OverloadResolutionResults.Code.INCOMPLETE_TYPE_INFERENCE)
			return results;
		Set<ResolvedCallWithTrace<D>> successful = Sets.newLinkedHashSet();
		Set<ResolvedCallWithTrace<D>> failed = Sets.newLinkedHashSet();
		for(ResolvedCall<? extends D> call : results.getResultingCalls())
		{
			if(!(call instanceof ResolvedCallImpl))
				continue;
			ResolvedCallImpl<D> resolvedCall = (ResolvedCallImpl<D>) call;
			if(!resolvedCall.hasUnknownTypeParameters())
			{
				if(resolvedCall.getStatus().isSuccess())
				{
					successful.add(resolvedCall);
				}
				else
				{
					failed.add(resolvedCall);
				}
				continue;
			}
			completeTypeInferenceDependentOnExpectedTypeForCall(resolvedCall, context, tracing, successful, failed);
		}
		if(results.getResultingCalls().size() > 1)
		{
			for(ResolvedCallWithTrace<D> call : successful)
			{
				if(call instanceof ResolvedCallImpl)
				{
					((ResolvedCallImpl) call).addStatus(ResolutionStatus.OTHER_ERROR);
					failed.add(call);
				}
			}
			successful.clear();
		}
		return computeResultAndReportErrors(context.trace, tracing, successful, failed);
	}

	private <D extends CallableDescriptor> void completeTypeInferenceDependentOnExpectedTypeForCall(ResolvedCallImpl<D> resolvedCall, BasicResolutionContext context, TracingStrategy tracing, Set<ResolvedCallWithTrace<D>> successful, Set<ResolvedCallWithTrace<D>> failed)
	{
		assert resolvedCall.hasUnknownTypeParameters();
		D descriptor = resolvedCall.getCandidateDescriptor();
		ConstraintSystem constraintSystem = resolvedCall.getConstraintSystem();
		assert constraintSystem != null;

		TypeSubstitutor substituteDontCare = ConstraintSystemWithPriorities.makeConstantSubstitutor(resolvedCall.getCandidateDescriptor().getTypeParameters(), ConstraintSystemImpl.DONT_CARE);

		// constraints for function literals
		// Value parameters
		for(Map.Entry<CallParameterDescriptor, ResolvedValueArgument> entry : resolvedCall.getValueArguments().entrySet())
		{
			ResolvedValueArgument resolvedValueArgument = entry.getValue();
			CallParameterDescriptor parameterDescriptor = entry.getKey();

			for(ValueArgument valueArgument : resolvedValueArgument.getArguments())
			{
				if(!NapilePsiUtil.isFunctionLiteralWithoutDeclaredParameterTypes(valueArgument.getArgumentExpression()))
					continue;

				ConstraintSystem systemWithCurrentSubstitution = constraintSystem.copy();
				addConstraintForValueArgument(valueArgument, parameterDescriptor, constraintSystem.getCurrentSubstitutor(), systemWithCurrentSubstitution, context);
				if(systemWithCurrentSubstitution.hasContradiction() || systemWithCurrentSubstitution.hasErrorInConstrainingTypes())
				{
					addConstraintForValueArgument(valueArgument, parameterDescriptor, substituteDontCare, constraintSystem, context);
				}
				else
				{
					constraintSystem = systemWithCurrentSubstitution;
				}
			}
		}

		ConstraintSystem constraintSystemWithoutExpectedTypeConstraint = constraintSystem.copy();
		constraintSystem.addSubtypingConstraint(descriptor.getReturnType(), context.expectedType, ConstraintPosition.EXPECTED_TYPE_POSITION);


		if(!constraintSystem.isSuccessful())
		{
			if(constraintSystemWithoutExpectedTypeConstraint.isSuccessful())
			{
				resolvedCall.setResultingSubstitutor(constraintSystemWithoutExpectedTypeConstraint.getResultingSubstitutor());
			}
			List<NapileType> argumentTypes = checkValueArgumentTypes(context, resolvedCall, context.trace).argumentTypes;

			tracing.typeInferenceFailed(context.trace, InferenceErrorData.create(descriptor, constraintSystem, argumentTypes, context.expectedType), constraintSystemWithoutExpectedTypeConstraint);
			resolvedCall.addStatus(ResolutionStatus.TYPE_INFERENCE_ERROR);
			failed.add(resolvedCall);
			return;
		}

		resolvedCall.setResultingSubstitutor(constraintSystem.getResultingSubstitutor());
		// Here we type check the arguments with inferred types expected
		checkValueArgumentTypes(context, resolvedCall, context.trace);

		checkBounds(resolvedCall, constraintSystem, context.trace, tracing);
		resolvedCall.setHasUnknownTypeParameters(false);
		if(resolvedCall.getStatus().isSuccess() || resolvedCall.getStatus() == ResolutionStatus.UNKNOWN_STATUS)
		{
			resolvedCall.addStatus(ResolutionStatus.SUCCESS);
			successful.add(resolvedCall);
		}
		else
		{
			failed.add(resolvedCall);
		}
	}

	private <D extends CallableDescriptor> void checkBounds(@NotNull ResolvedCallImpl<D> call, @NotNull ConstraintSystem constraintSystem, @NotNull BindingTrace trace, @NotNull TracingStrategy tracing)
	{
		for(TypeParameterDescriptor typeParameter : call.getCandidateDescriptor().getTypeParameters())
		{
			if(!ConstraintsUtil.checkUpperBoundIsSatisfied(constraintSystem, typeParameter))
			{
				tracing.upperBoundViolated(trace, InferenceErrorData.create(call.getCandidateDescriptor(), constraintSystem));
			}
		}
	}

	private <F extends CallableDescriptor> void cacheResults(@NotNull WritableSlice<CallKey, OverloadResolutionResults<F>> resolutionResultsSlice, @NotNull BasicResolutionContext context, @NotNull OverloadResolutionResults<F> results, @NotNull DelegatingBindingTrace delegatingBindingTrace)
	{
		boolean canBeCached = true;
		for(ResolvedCall<? extends CallableDescriptor> call : results.getResultingCalls())
		{
			if(!call.getCandidateDescriptor().getTypeParameters().isEmpty())
			{
				canBeCached = false;
			}
		}
		if(!canBeCached)
			return;
		PsiElement callElement = context.call.getCallElement();
		if(!(callElement instanceof NapileExpression))
			return;

		context.trace.record(resolutionResultsSlice, CallKey.create(context.call.getCallType(), (NapileExpression) callElement), results);
		context.trace.record(TRACE_DELTAS_CACHE, (NapileExpression) callElement, delegatingBindingTrace);
	}

	private <D extends CallableDescriptor> OverloadResolutionResults<D> checkArgumentTypesAndFail(BasicResolutionContext context)
	{
		checkTypesWithNoCallee(context);
		return OverloadResolutionResultsImpl.nameNotFound();
	}

	@NotNull
	private <D extends CallableDescriptor, F extends D> OverloadResolutionResults<F> doResolveCall(@NotNull final BasicResolutionContext context, @NotNull final List<ResolutionTask<D, F>> prioritizedTasks, // high to low priority
			@NotNull CallTransformer<D, F> callTransformer, @NotNull final NapileReferenceExpression reference, boolean bindReference)
	{

		ResolutionDebugInfo.Data debugInfo = ResolutionDebugInfo.create();
		context.trace.record(ResolutionDebugInfo.RESOLUTION_DEBUG_INFO, context.call.getCallElement(), debugInfo);
		context.trace.record(RESOLUTION_SCOPE, context.call.getCalleeExpression(), context.scope);

		if(context.dataFlowInfo.hasTypeInfoConstraints())
		{
			context.trace.record(NON_DEFAULT_EXPRESSION_DATA_FLOW, context.call.getCalleeExpression(), context.dataFlowInfo);
		}

		debugInfo.set(ResolutionDebugInfo.TASKS, prioritizedTasks);

		TemporaryBindingTrace traceForFirstNonemptyCandidateSet = null;
		OverloadResolutionResultsImpl<F> resultsForFirstNonemptyCandidateSet = null;
		for(ResolutionTask<D, F> task : prioritizedTasks)
		{
			TemporaryBindingTrace taskTrace = TemporaryBindingTrace.create(context.trace);
			OverloadResolutionResultsImpl<F> results = performResolutionGuardedForExtraFunctionLiteralArguments(task.withTrace(taskTrace), callTransformer, context.trace, bindReference);
			if(results.isSuccess() || results.isAmbiguity())
			{
				taskTrace.commit();

				if(results.isSuccess())
				{
					debugInfo.set(ResolutionDebugInfo.RESULT, results.getResultingCall());
				}

				return results;
			}
			if(results.getResultCode() == OverloadResolutionResults.Code.INCOMPLETE_TYPE_INFERENCE)
			{
				results.setTrace(taskTrace);
				return results;
			}
			if(traceForFirstNonemptyCandidateSet == null && !task.getCandidates().isEmpty() && !results.isNothing())
			{
				traceForFirstNonemptyCandidateSet = taskTrace;
				resultsForFirstNonemptyCandidateSet = results;
			}
		}
		if(traceForFirstNonemptyCandidateSet != null)
		{
			traceForFirstNonemptyCandidateSet.commit();
			if(resultsForFirstNonemptyCandidateSet.isSingleResult())
			{

				debugInfo.set(ResolutionDebugInfo.RESULT, resultsForFirstNonemptyCandidateSet.getResultingCall());
			}
		}
		else
		{
			context.trace.report(UNRESOLVED_REFERENCE.on(reference, reference.getText()));
			checkTypesWithNoCallee(context);
		}
		return resultsForFirstNonemptyCandidateSet != null ? resultsForFirstNonemptyCandidateSet : OverloadResolutionResultsImpl.<F>nameNotFound();
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@NotNull
	private <D extends CallableDescriptor, F extends D> OverloadResolutionResultsImpl<F> performResolutionGuardedForExtraFunctionLiteralArguments(@NotNull ResolutionTask<D, F> task, @NotNull CallTransformer<D, F> callTransformer, @NotNull BindingTrace traceForResolutionCache, boolean bindReference)
	{
		OverloadResolutionResultsImpl<F> results = performResolution(task, callTransformer, traceForResolutionCache, bindReference);

		// If resolution fails, we should check for some of the following situations:
		//   class A {
		//     val foo = Bar() // The following is intended to be an anonymous initializer,
		//                     // but is treated as a function literal argument
		//     {
		//       ...
		//     }
		//  }
		//
		//  fun foo() {
		//    bar {
		//      buzz()
		//      {...} // intended to be a returned from the outer literal
		//    }
		//  }
		EnumSet<OverloadResolutionResults.Code> someFailed = EnumSet.of(OverloadResolutionResults.Code.MANY_FAILED_CANDIDATES, OverloadResolutionResults.Code.SINGLE_CANDIDATE_ARGUMENT_MISMATCH);
		if(someFailed.contains(results.getResultCode()) && !task.call.getFunctionLiteralArguments().isEmpty())
		{
			// We have some candidates that failed for some reason
			// And we have a suspect: the function literal argument
			// Now, we try to remove this argument and see if it helps
			ResolutionTask<D, F> newTask = new ResolutionTask<D, F>(task.getCandidates(), task.reference, TemporaryBindingTrace.create(task.trace), task.scope, new DelegatingCall(task.call)
			{
				@NotNull
				@Override
				public List<NapileExpression> getFunctionLiteralArguments()
				{
					return Collections.emptyList();
				}
			}, task.expectedType, task.dataFlowInfo);
			OverloadResolutionResultsImpl<F> resultsWithFunctionLiteralsStripped = performResolution(newTask, callTransformer, traceForResolutionCache, bindReference);
			if(resultsWithFunctionLiteralsStripped.isSuccess() || resultsWithFunctionLiteralsStripped.isAmbiguity())
			{
				task.tracing.danglingFunctionLiteralArgumentSuspected(task.trace, task.call.getFunctionLiteralArguments());
			}
		}

		return results;
	}

	@NotNull
	private <D extends CallableDescriptor, F extends D> OverloadResolutionResultsImpl<F> performResolution(@NotNull ResolutionTask<D, F> task, @NotNull CallTransformer<D, F> callTransformer, @NotNull BindingTrace traceForResolutionCache, boolean bindReference)
	{

		for(ResolutionCandidate<D> resolutionCandidate : task.getCandidates())
		{
			TemporaryBindingTrace candidateTrace = TemporaryBindingTrace.create(task.trace);
			Collection<CallResolutionContext<D, F>> contexts = callTransformer.createCallContexts(resolutionCandidate, task, candidateTrace);
			for(CallResolutionContext<D, F> context : contexts)
			{
				performResolutionForCandidateCall(context, task);

				if(bindReference)
					task.tracing.bindReference(context.candidateCall.getTrace(), context.candidateCall);

				Collection<ResolvedCallWithTrace<F>> calls = Collections.singleton((ResolvedCallWithTrace<F>) context.candidateCall);

				for(ResolvedCallWithTrace<F> call : calls)
				{
					if(bindReference)
					{
						task.tracing.bindReference(call.getTrace(), call);
						task.tracing.bindResolvedCall(call.getTrace(), call);
					}
					task.getResolvedCalls().add(call);
				}

				context.candidateCall.getTrace().addAllMyDataTo(traceForResolutionCache, new Predicate<WritableSlice>()
				{
					@Override
					public boolean apply(@Nullable WritableSlice slice)
					{
						return slice == BindingTraceKeys.RESOLUTION_RESULTS_FOR_FUNCTION ||
								slice == BindingTraceKeys.RESOLUTION_RESULTS_FOR_PROPERTY ||
								slice == BindingTraceKeys.TRACE_DELTAS_CACHE;
					}
				}, false);
			}
		}

		Set<ResolvedCallWithTrace<F>> successfulCandidates = Sets.newLinkedHashSet();
		Set<ResolvedCallWithTrace<F>> failedCandidates = Sets.newLinkedHashSet();
		for(ResolvedCallWithTrace<F> candidateCall : task.getResolvedCalls())
		{
			ResolutionStatus status = candidateCall.getStatus();
			if(status.isSuccess())
			{
				successfulCandidates.add(candidateCall);
			}
			else
			{
				assert status != ResolutionStatus.UNKNOWN_STATUS : "No resolution for " + candidateCall.getCandidateDescriptor();
				if(candidateCall.getStatus() != ResolutionStatus.STRONG_ERROR)
				{
					failedCandidates.add(candidateCall);
				}
			}
		}

		OverloadResolutionResultsImpl<F> results = computeResultAndReportErrors(task.trace, task.tracing, successfulCandidates, failedCandidates);
		if(!results.isSingleResult() && results.getResultCode() != OverloadResolutionResults.Code.INCOMPLETE_TYPE_INFERENCE)
		{
			checkTypesWithNoCallee(task.toBasic());
		}
		return results;
	}

	private <D extends CallableDescriptor, F extends D> boolean isImmutableInvisible(@NotNull CallResolutionContext<D, F> context)
	{
		ResolvedCallImpl<D> candidateCall = context.candidateCall;
		D candidate = candidateCall.getCandidateDescriptor();
		if(candidateCall.getThisObject().exists())
		{
			if(AnnotationUtils.hasAnnotation(candidate, NapileAnnotationPackage.IMMUTABLE_TARGET))
			{
				NapileType type = candidateCall.getThisObject().getType();
				if(AnnotationUtils.hasAnnotation(type, NapileAnnotationPackage.IMMUTABLE))
					return true;

				if(candidateCall.getThisObject() instanceof ExpressionReceiver)
				{
					ExpressionReceiver expressionReceiver = (ExpressionReceiver) candidateCall.getThisObject();
					NapileExpression expression = expressionReceiver.getExpression();

					if(expression instanceof NapileDotQualifiedExpressionImpl)
					{
						NapileDotQualifiedExpressionImpl dotQualifiedExpression = (NapileDotQualifiedExpressionImpl) expression;
						if(hasAnnotationOnType(dotQualifiedExpression.getSelectorExpression(), context, NapileAnnotationPackage.INHERIT_IMMUTABLE) && hasAnnotationOnType(dotQualifiedExpression.getReceiverExpression(), context, NapileAnnotationPackage.IMMUTABLE))
							return true;
					}
				}
			}
		}
		return false;
	}

	private static boolean hasAnnotationOnType(@Nullable NapileExpression expression, CallResolutionContext<?, ?> context, FqName fqName)
	{
		if(expression == null)
			return false;
		NapileType type = context.trace.get(BindingTraceKeys.EXPRESSION_TYPE, expression);
		return type != null && AnnotationUtils.hasAnnotation(type, fqName);
	}

	private static boolean isNotExpression(@Nullable NapileExpression expression, CallResolutionContext<?, ?> context)
	{
		return expression instanceof NapileSimpleNameExpression && context.trace.get(BindingTraceKeys.REFERENCE_TARGET, (NapileSimpleNameExpression) expression) instanceof MutableClassDescriptor;
	}

	@Nullable
	private static NapileSimpleNameExpression getSimpleOrReceiverOfDotExpression(@Nullable NapileElement element)
	{
		return element instanceof NapileSimpleNameExpression ? (NapileSimpleNameExpression) element : element instanceof NapileDotQualifiedExpressionImpl ? getSimpleOrReceiverOfDotExpression(((NapileDotQualifiedExpressionImpl) element).getReceiverExpression()) : null;
	}

	private <D extends CallableDescriptor, F extends D> void performResolutionForCandidateCall(@NotNull CallResolutionContext<D, F> context, @NotNull ResolutionTask<D, F> task)
	{

		ResolvedCallImpl<D> candidateCall = context.candidateCall;
		D candidate = candidateCall.getCandidateDescriptor();

		if(ErrorUtils.isError(candidate))
		{
			candidateCall.addStatus(ResolutionStatus.SUCCESS);
			checkTypesWithNoCallee(context.toBasic());
			return;
		}

		/*if(!candidate.isStatic())
		{
			if(candidateCall.getThisObject() instanceof ExpressionReceiver)
			{
				// Calls like
				// Array.length = 1
				// napile.lang.Array.length = 1
				NapileExpression exp = getSimpleOrReceiverOfDotExpression(((ExpressionReceiver) candidateCall.getThisObject()).getExpression());
				if(isNotExpression(exp, context))
				{
					candidateCall.addStatus(ResolutionStatus.OTHER_ERROR);
					context.tracing.staticCallExpectInstanceCall(context.trace, candidate);
					return;
				}
			}
		}  */

		if(isImmutableInvisible(context))
		{
			candidateCall.addStatus(ResolutionStatus.OTHER_ERROR);
			context.tracing.immutableInvisibleMember(context.trace, candidate);
			return;
		}

		if(!Visibilities.isVisible(candidate, context.scope.getContainingDeclaration()))
		{
			candidateCall.addStatus(ResolutionStatus.OTHER_ERROR);
			context.tracing.invisibleMember(context.trace, candidate);
			return;
		}

		Set<ValueArgument> unmappedArguments = Sets.newLinkedHashSet();
		ValueArgumentsToParametersMapper.Status argumentMappingStatus = ValueArgumentsToParametersMapper.mapValueArgumentsToParameters(context.call, context.tracing, candidateCall, unmappedArguments);
		if(!argumentMappingStatus.isSuccess())
		{
			if(argumentMappingStatus == ValueArgumentsToParametersMapper.Status.STRONG_ERROR)
			{
				candidateCall.addStatus(ResolutionStatus.STRONG_ERROR);
			}
			else
			{
				candidateCall.addStatus(ResolutionStatus.OTHER_ERROR);
			}
			if((argumentMappingStatus == ValueArgumentsToParametersMapper.Status.ERROR && candidate.getTypeParameters().isEmpty()) || argumentMappingStatus == ValueArgumentsToParametersMapper.Status.STRONG_ERROR)
			{
				checkTypesWithNoCallee(context.toBasic());
				return;
			}
			checkUnmappedArgumentTypes(context.toBasic(), unmappedArguments);
		}

		List<? extends NapileTypeReference> jetTypeArguments = context.call.getTypeArguments();
		if(jetTypeArguments.isEmpty())
		{
			if(!candidate.getTypeParameters().isEmpty())
			{
				ResolutionStatus status = inferTypeArguments(context);
				candidateCall.addStatus(status);
			}
			else
			{
				candidateCall.addStatus(checkAllValueArguments(context).status);
			}
		}
		else
		{
			// Explicit type arguments passed

			List<NapileType> typeArguments = new ArrayList<NapileType>();
			for(NapileTypeReference typeReference : jetTypeArguments)
				typeArguments.add(typeResolver.resolveType(context.scope, typeReference, context.trace, true));

			int expectedTypeArgumentCount = candidate.getTypeParameters().size();
			if(expectedTypeArgumentCount == jetTypeArguments.size())
			{

				checkGenericBoundsInAFunctionCall(jetTypeArguments, typeArguments, candidate, context.trace);

				Map<TypeConstructor, NapileType> substitutionContext = MethodDescriptorUtil.createSubstitutionContext((MethodDescriptor) candidate, typeArguments);
				candidateCall.setResultingSubstitutor(TypeSubstitutor.create(substitutionContext));

				List<TypeParameterDescriptor> typeParameters = candidateCall.getCandidateDescriptor().getTypeParameters();
				for(int i = 0; i < typeParameters.size(); i++)
				{
					TypeParameterDescriptor typeParameterDescriptor = typeParameters.get(i);
					candidateCall.recordTypeArgument(typeParameterDescriptor, typeArguments.get(i));
				}
				candidateCall.addStatus(checkAllValueArguments(context).status);
			}
			else
			{
				candidateCall.addStatus(ResolutionStatus.OTHER_ERROR);
				context.tracing.wrongNumberOfTypeArguments(context.trace, expectedTypeArgumentCount);
			}
		}

		task.performAdvancedChecks(candidate, context.trace, context.tracing);

		recordAutoCastIfNecessary(candidateCall.getThisObject(), candidateCall.getTrace());
	}

	private <D extends CallableDescriptor, F extends D> ResolutionStatus inferTypeArguments(CallResolutionContext<D, F> context)
	{
		ResolvedCallImpl<D> candidateCall = context.candidateCall;
		final D candidate = candidateCall.getCandidateDescriptor();

		ResolutionDebugInfo.Data debugInfo = context.trace.get(ResolutionDebugInfo.RESOLUTION_DEBUG_INFO, context.call.getCallElement());

		ConstraintSystemImpl constraintsSystem = new ConstraintSystemImpl();

		// If the call is recursive, e.g.
		//   fun foo<T>(t : T) : T = foo(t)
		// we can't use same descriptor objects for T's as actual type values and same T's as unknowns,
		// because constraints become trivial (T :< T), and inference fails
		//
		// Thus, we replace the parameters of our descriptor with fresh objects (perform alpha-conversion)
		CallableDescriptor candidateWithFreshVariables = MethodDescriptorUtil.alphaConvertTypeParameters(candidate);


		for(TypeParameterDescriptor typeParameterDescriptor : candidateWithFreshVariables.getTypeParameters())
		{
			constraintsSystem.registerTypeVariable(typeParameterDescriptor); // TODO: variance of the occurrences
		}

		TypeSubstitutor substituteDontCare = ConstraintSystemWithPriorities.makeConstantSubstitutor(candidateWithFreshVariables.getTypeParameters(), ConstraintSystemImpl.DONT_CARE);

		// Value parameters
		for(Map.Entry<CallParameterDescriptor, ResolvedValueArgument> entry : candidateCall.getValueArguments().entrySet())
		{
			ResolvedValueArgument resolvedValueArgument = entry.getValue();
			CallParameterDescriptor parameterDescriptor = candidateWithFreshVariables.getValueParameters().get(entry.getKey().getIndex());


			for(ValueArgument valueArgument : resolvedValueArgument.getArguments())
			{
				if(NapilePsiUtil.isFunctionLiteralWithoutDeclaredParameterTypes(valueArgument.getArgumentExpression()))
					continue;
				// TODO : more attempts, with different expected types

				// Here we type check expecting an error type (DONT_CARE, substitution with substituteDontCare)
				// and throw the results away
				// We'll type check the arguments later, with the inferred types expected
				boolean success = addConstraintForValueArgument(valueArgument, parameterDescriptor, substituteDontCare, constraintsSystem, context);
				if(!success)
				{
					candidateCall.argumentHasNoType();
				}
			}
		}

		// Receiver
		// Error is already reported if something is missing

		ConstraintSystem constraintSystemWithRightTypeParameters = constraintsSystem.replaceTypeVariables(new Function<TypeParameterDescriptor, TypeParameterDescriptor>()
		{
			@Override
			public TypeParameterDescriptor apply(@Nullable TypeParameterDescriptor typeParameterDescriptor)
			{
				assert typeParameterDescriptor != null;
				return candidate.getTypeParameters().get(typeParameterDescriptor.getIndex());
			}
		});
		candidateCall.setConstraintSystem(constraintSystemWithRightTypeParameters);


		// Solution
		if(!constraintsSystem.hasContradiction())
		{
			candidateCall.setHasUnknownTypeParameters(true);
			return ResolutionStatus.SUCCESS;
		}
		else
		{
			ValueArgumentsCheckingResult checkingResult = checkAllValueArguments(context);
			ResolutionStatus argumentsStatus = checkingResult.status;
			List<NapileType> argumentTypes = checkingResult.argumentTypes;
			context.tracing.typeInferenceFailed(context.trace, InferenceErrorData.create(candidate, constraintSystemWithRightTypeParameters, argumentTypes, context.expectedType), constraintSystemWithRightTypeParameters);
			return ResolutionStatus.TYPE_INFERENCE_ERROR.combine(argumentsStatus);
		}
	}

	private boolean addConstraintForValueArgument(ValueArgument valueArgument, @NotNull CallParameterDescriptor parameterDescriptor, @NotNull TypeSubstitutor substitutor, @NotNull ConstraintSystem constraintSystem, @NotNull ResolutionContext context)
	{

		NapileType effectiveExpectedType = parameterDescriptor.getType();
		TemporaryBindingTrace traceForUnknown = TemporaryBindingTrace.create(context.trace);
		NapileExpression argumentExpression = valueArgument.getArgumentExpression();
		NapileType type = argumentExpression != null ? expressionTypingServices.getType(context.scope, argumentExpression, substitutor.substitute(parameterDescriptor.getType(), null), context.dataFlowInfo, traceForUnknown) : null;
		constraintSystem.addSupertypeConstraint(effectiveExpectedType, type, ConstraintPosition.getValueParameterPosition(parameterDescriptor.getIndex()));
		//todo no return
		if(type == null || ErrorUtils.isErrorType(type))
			return false;
		return true;
	}

	private static void recordAutoCastIfNecessary(ReceiverDescriptor receiver, BindingTrace trace)
	{
		if(receiver instanceof AutoCastReceiver)
		{
			AutoCastReceiver autoCastReceiver = (AutoCastReceiver) receiver;
			ReceiverDescriptor original = autoCastReceiver.getOriginal();
			if(original instanceof ExpressionReceiver)
			{
				ExpressionReceiver expressionReceiver = (ExpressionReceiver) original;
				if(autoCastReceiver.canCast())
				{
					trace.record(AUTOCAST, expressionReceiver.getExpression(), autoCastReceiver.getType());
				}
				else
				{
					trace.report(AUTOCAST_IMPOSSIBLE.on(expressionReceiver.getExpression(), autoCastReceiver.getType(), expressionReceiver.getExpression().getText()));
				}
			}
			else
			{
				assert autoCastReceiver.canCast() : "A non-expression receiver must always be autocastabe: " + original;
			}
		}
	}

	private void checkTypesWithNoCallee(BasicResolutionContext context)
	{
		for(ValueArgument valueArgument : context.call.getValueArguments())
		{
			NapileExpression argumentExpression = valueArgument.getArgumentExpression();
			if(argumentExpression != null)
			{
				expressionTypingServices.getType(context.scope, argumentExpression, TypeUtils.NO_EXPECTED_TYPE, context.dataFlowInfo, context.trace);
			}
		}

		for(NapileExpression expression : context.call.getFunctionLiteralArguments())
		{
			expressionTypingServices.getType(context.scope, expression, TypeUtils.NO_EXPECTED_TYPE, context.dataFlowInfo, context.trace);
		}

		for(NapileTypeReference typeReference : context.call.getTypeArguments())
			typeResolver.resolveType(context.scope, typeReference, context.trace, true);
	}

	private void checkUnmappedArgumentTypes(BasicResolutionContext context, Set<ValueArgument> unmappedArguments)
	{
		for(ValueArgument valueArgument : unmappedArguments)
		{
			NapileExpression argumentExpression = valueArgument.getArgumentExpression();
			if(argumentExpression != null)
			{
				expressionTypingServices.getType(context.scope, argumentExpression, TypeUtils.NO_EXPECTED_TYPE, context.dataFlowInfo, context.trace);
			}
		}
	}


	private <D extends CallableDescriptor, F extends D> ValueArgumentsCheckingResult checkAllValueArguments(CallResolutionContext<D, F> context)
	{
		ValueArgumentsCheckingResult checkingResult = checkValueArgumentTypes(context, context.candidateCall);
		ResolutionStatus resultStatus = checkingResult.status;
		ResolvedCall<D> candidateCall = context.candidateCall;

		// Comment about a very special case.
		// Call 'b.foo(1)' where class 'Foo' has an extension member 'fun B.invoke(Int)' should be checked two times for safe call (in 'checkReceiver'), because
		// both 'b' (receiver) and 'foo' (this object) might be nullable. In the first case we mark dot, in the second 'foo'.
		// Class 'CallForImplicitInvoke' helps up to recognise this case, and parameter 'implicitInvokeCheck' helps us to distinguish whether we check receiver or this object.

		//resultStatus = resultStatus.combine(checkReceiver(context, candidateCall, candidateCall.getResultingDescriptor().getReceiverParameter(), candidateCall.getReceiverArgument(), candidateCall.getExplicitReceiverKind().isReceiver(), false));

		resultStatus = resultStatus.combine(checkReceiver(context, candidateCall, candidateCall.getResultingDescriptor().getExpectedThisObject(), candidateCall.getThisObject(), candidateCall.getExplicitReceiverKind().isThisObject(), false));
		return new ValueArgumentsCheckingResult(resultStatus, checkingResult.argumentTypes);
	}

	private <D extends CallableDescriptor, F extends D> ResolutionStatus checkReceiver(CallResolutionContext<D, F> context, ResolvedCall<D> candidateCall, ReceiverDescriptor receiverParameter, ReceiverDescriptor receiverArgument, boolean isExplicitReceiver, boolean implicitInvokeCheck)
	{

		ResolutionStatus result = ResolutionStatus.SUCCESS;
		if(receiverParameter.exists() && receiverArgument.exists())
		{
			boolean safeAccess = isExplicitReceiver && !implicitInvokeCheck && candidateCall.isSafeCall();
			NapileType receiverArgumentType = receiverArgument.getType();
			AutoCastServiceImpl autoCastService = new AutoCastServiceImpl(context.dataFlowInfo, context.candidateCall.getTrace());
			if(!safeAccess && !receiverParameter.getType().isNullable() && !autoCastService.isNotNull(receiverArgument))
			{

				context.tracing.unsafeCall(context.candidateCall.getTrace(), receiverArgumentType, implicitInvokeCheck);
				result = ResolutionStatus.UNSAFE_CALL_ERROR;
			}
			else
			{
				NapileType effectiveReceiverArgumentType = safeAccess ? TypeUtils.makeNotNullable(receiverArgumentType) : receiverArgumentType;
				if(!TypeUtils.dependsOnTypeParameters(receiverParameter.getType(), candidateCall.getCandidateDescriptor().getTypeParameters()) && !typeChecker.isSubtypeOf(effectiveReceiverArgumentType, receiverParameter.getType()))
				{
					context.tracing.wrongReceiverType(context.candidateCall.getTrace(), receiverParameter, receiverArgument);
					result = ResolutionStatus.OTHER_ERROR;
				}
			}
			if(safeAccess && (!receiverArgumentType.isNullable() || DescriptorUtils.isAnyMethod(candidateCall.getCandidateDescriptor())))
			{
				context.tracing.unnecessarySafeCall(context.candidateCall.getTrace(), receiverArgumentType);
			}
		}
		return result;
	}

	private static class ValueArgumentsCheckingResult
	{
		public final List<NapileType> argumentTypes;
		public final ResolutionStatus status;

		private ValueArgumentsCheckingResult(@NotNull ResolutionStatus status, @NotNull List<NapileType> argumentTypes)
		{
			this.status = status;
			this.argumentTypes = argumentTypes;
		}
	}

	private <D extends CallableDescriptor> ValueArgumentsCheckingResult checkValueArgumentTypes(ResolutionContext context, ResolvedCallImpl<D> candidateCall)
	{
		return checkValueArgumentTypes(context, candidateCall, candidateCall.getTrace());
	}

	private <D extends CallableDescriptor> ValueArgumentsCheckingResult checkValueArgumentTypes(ResolutionContext context, ResolvedCallImpl<D> candidateCall, BindingTrace trace)
	{
		ResolutionStatus resultStatus = ResolutionStatus.SUCCESS;
		DataFlowInfo dataFlowInfo = context.dataFlowInfo;
		List<NapileType> argumentTypes = Lists.newArrayList();
		for(Map.Entry<CallParameterDescriptor, ResolvedValueArgument> entry : candidateCall.getValueArguments().entrySet())
		{
			CallParameterDescriptor parameterDescriptor = entry.getKey();
			ResolvedValueArgument resolvedArgument = entry.getValue();


			for(ValueArgument argument : resolvedArgument.getArguments())
			{
				NapileExpression expression = argument.getArgumentExpression();
				if(expression == null)
					continue;

				NapileType expectedType = parameterDescriptor.getType();
				if(TypeUtils.dependsOnTypeParameters(expectedType, candidateCall.getCandidateDescriptor().getTypeParameters()))
				{
					expectedType = TypeUtils.NO_EXPECTED_TYPE;
				}
				NapileTypeInfo typeInfo = expressionTypingServices.getTypeInfo(context.scope, expression, expectedType, dataFlowInfo, trace);
				NapileType type = typeInfo.getType();
				argumentTypes.add(type);
				dataFlowInfo = dataFlowInfo.and(typeInfo.getDataFlowInfo());
				if(type == null || ErrorUtils.isErrorType(type))
				{
					candidateCall.argumentHasNoType();
				}
				else if(expectedType != TypeUtils.NO_EXPECTED_TYPE && !typeChecker.isSubtypeOf(type, expectedType))
				{
					//                    VariableDescriptor variableDescriptor = AutoCastUtils.getVariableDescriptorFromSimpleName(temporaryTrace.getBindingContext(), argument);
					//                    if (variableDescriptor != null) {
					//                        JetType autoCastType = null;
					//                        for (JetType possibleType : dataFlowInfo.getPossibleTypesForVariable(variableDescriptor)) {
					//                            if (semanticServices.getTypeChecker().isSubtypeOf(type, parameterType)) {
					//                                autoCastType = possibleType;
					//                                break;
					//                            }
					//                        }
					//                        if (autoCastType != null) {
					//                            if (AutoCastUtils.isStableVariable(variableDescriptor)) {
					//                                temporaryTrace.record(AUTOCAST, argument, autoCastType);
					//                            }
					//                            else {
					//                                temporaryTrace.report(AUTOCAST_IMPOSSIBLE.on(argument, autoCastType, variableDescriptor));
					//                                resultStatus = false;
					//                            }
					//                        }
					//                    }
					//                    else {
					resultStatus = ResolutionStatus.OTHER_ERROR;
				}
			}
		}
		return new ValueArgumentsCheckingResult(resultStatus, argumentTypes);
	}

	@NotNull
	private <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> computeResultAndReportErrors(@NotNull BindingTrace trace, @NotNull TracingStrategy tracing, @NotNull Set<ResolvedCallWithTrace<D>> successfulCandidates, @NotNull Set<ResolvedCallWithTrace<D>> failedCandidates)
	{
		// TODO : maybe it's better to filter overrides out first, and only then look for the maximally specific

		if(successfulCandidates.size() > 0)
		{
			OverloadResolutionResultsImpl<D> results = chooseAndReportMaximallySpecific(successfulCandidates, true);
			if(results.isAmbiguity())
			{
				// This check is needed for the following case:
				//    x.foo(unresolved) -- if there are multiple foo's, we'd report an ambiguity, and it does not make sense here
				if(allClean(results.getResultingCalls()))
				{
					tracing.ambiguity(trace, results.getResultingCalls());
				}
				tracing.recordAmbiguity(trace, results.getResultingCalls());
			}
			return results;
		}
		else if(!failedCandidates.isEmpty())
		{
			if(failedCandidates.size() != 1)
			{
				// This is needed when there are several overloads some of which are OK but for nullability of the receiver,
				// and some are not OK at all. In this case we'd like to say "unsafe call" rather than "none applicable"
				// Used to be: weak errors. Generalized for future extensions
				for(EnumSet<ResolutionStatus> severityLevel : ResolutionStatus.SEVERITY_LEVELS)
				{
					Set<ResolvedCallWithTrace<D>> thisLevel = Sets.newLinkedHashSet();
					for(ResolvedCallWithTrace<D> candidate : failedCandidates)
					{
						if(severityLevel.contains(candidate.getStatus()))
						{
							thisLevel.add(candidate);
						}
					}
					if(!thisLevel.isEmpty())
					{
						OverloadResolutionResultsImpl<D> results = chooseAndReportMaximallySpecific(thisLevel, false);
						if(results.isSuccess())
						{
							results.getResultingCall().getTrace().commit();
							return OverloadResolutionResultsImpl.singleFailedCandidate(results.getResultingCall());
						}

						tracing.noneApplicable(trace, results.getResultingCalls());
						tracing.recordAmbiguity(trace, results.getResultingCalls());
						return OverloadResolutionResultsImpl.manyFailedCandidates(results.getResultingCalls());
					}
				}

				assert false : "Should not be reachable, cause every status must belong to some level";

				Set<ResolvedCallWithTrace<D>> noOverrides = OverridingUtil.filterOverrides(failedCandidates, MAP_TO_CANDIDATE);
				if(noOverrides.size() != 1)
				{
					tracing.noneApplicable(trace, noOverrides);
					tracing.recordAmbiguity(trace, noOverrides);
					return OverloadResolutionResultsImpl.manyFailedCandidates(noOverrides);
				}

				failedCandidates = noOverrides;
			}

			ResolvedCallWithTrace<D> failed = failedCandidates.iterator().next();
			failed.getTrace().commit();
			if(failed.getStatus() != ResolutionStatus.STRONG_ERROR && failed.hasUnknownTypeParameters())
			{
				return OverloadResolutionResultsImpl.incompleteTypeInference(failed);
			}
			return OverloadResolutionResultsImpl.singleFailedCandidate(failed);
		}
		else
		{
			tracing.unresolvedReference(trace);
			return OverloadResolutionResultsImpl.nameNotFound();
		}
	}

	private static <D extends CallableDescriptor> boolean allClean(Collection<ResolvedCallWithTrace<D>> results)
	{
		for(ResolvedCallWithTrace<D> result : results)
		{
			if(result.isDirty())
				return false;
		}
		return true;
	}

	private <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> chooseAndReportMaximallySpecific(Set<ResolvedCallWithTrace<D>> candidates, boolean discriminateGenerics)
	{
		if(candidates.size() != 1)
		{
			boolean dirty = false;
			Set<ResolvedCallWithTrace<D>> cleanCandidates = Sets.newLinkedHashSet(candidates);
			for(Iterator<ResolvedCallWithTrace<D>> iterator = cleanCandidates.iterator(); iterator.hasNext(); )
			{
				ResolvedCallWithTrace<D> candidate = iterator.next();
				if(candidate.hasUnknownTypeParameters())
				{
					dirty = true;
				}
				if(candidate.isDirty())
				{
					iterator.remove();
				}
			}

			if(cleanCandidates.isEmpty())
			{
				cleanCandidates = candidates;
			}
			ResolvedCallWithTrace<D> maximallySpecific = overloadingConflictResolver.findMaximallySpecific(cleanCandidates, false);
			if(maximallySpecific != null)
			{
				return OverloadResolutionResultsImpl.success(maximallySpecific);
			}

			if(discriminateGenerics)
			{
				ResolvedCallWithTrace<D> maximallySpecificGenericsDiscriminated = overloadingConflictResolver.findMaximallySpecific(cleanCandidates, true);
				if(maximallySpecificGenericsDiscriminated != null)
				{
					return OverloadResolutionResultsImpl.success(maximallySpecificGenericsDiscriminated);
				}
			}

			Set<ResolvedCallWithTrace<D>> noOverrides = OverridingUtil.filterOverrides(candidates, MAP_TO_RESULT);

			if(dirty)
			{
				return OverloadResolutionResultsImpl.incompleteTypeInference(candidates);
			}

			return OverloadResolutionResultsImpl.ambiguity(noOverrides);
		}
		else
		{
			ResolvedCallWithTrace<D> result = candidates.iterator().next();

			TemporaryBindingTrace temporaryTrace = result.getTrace();
			temporaryTrace.commit();
			if(result.hasUnknownTypeParameters())
			{
				return OverloadResolutionResultsImpl.incompleteTypeInference(result);
			}

			return OverloadResolutionResultsImpl.success(result);
		}
	}

	private void checkGenericBoundsInAFunctionCall(List<? extends NapileTypeReference> jetTypeArguments, List<NapileType> typeArguments, CallableDescriptor functionDescriptor, BindingTrace trace)
	{
		Map<TypeConstructor, NapileType> context = Maps.newHashMap();

		List<TypeParameterDescriptor> typeParameters = functionDescriptor.getOriginal().getTypeParameters();
		for(int i = 0, typeParametersSize = typeParameters.size(); i < typeParametersSize; i++)
		{
			TypeParameterDescriptor typeParameter = typeParameters.get(i);
			NapileType typeArgument = typeArguments.get(i);
			context.put(typeParameter.getTypeConstructor(), typeArgument);
		}
		TypeSubstitutor substitutor = TypeSubstitutor.create(context);
		for(int i = 0, typeParametersSize = typeParameters.size(); i < typeParametersSize; i++)
		{
			TypeParameterDescriptor typeParameterDescriptor = typeParameters.get(i);
			NapileType typeArgument = typeArguments.get(i);
			NapileTypeReference typeReference = jetTypeArguments.get(i);
			if(typeReference != null)
			{
				descriptorResolver.checkBounds(typeReference, typeArgument, typeParameterDescriptor, substitutor, trace);
			}
		}
	}

	@NotNull
	public OverloadResolutionResults<MethodDescriptor> resolveExactSignature(@NotNull NapileScope scope, @NotNull ReceiverDescriptor receiver, @NotNull Name name, @NotNull List<NapileType> parameterTypes)
	{
		List<ResolutionCandidate<MethodDescriptor>> candidates = findCandidatesByExactSignature(scope, receiver, name, parameterTypes);

		BindingTraceImpl trace = new BindingTraceImpl();
		TemporaryBindingTrace temporaryBindingTrace = TemporaryBindingTrace.create(trace);
		Set<ResolvedCallWithTrace<MethodDescriptor>> calls = Sets.newLinkedHashSet();
		for(ResolutionCandidate<MethodDescriptor> candidate : candidates)
		{
			ResolvedCallImpl<MethodDescriptor> call = ResolvedCallImpl.create(candidate, temporaryBindingTrace);
			calls.add(call);
		}
		return computeResultAndReportErrors(trace, TracingStrategy.EMPTY, calls, Collections.<ResolvedCallWithTrace<MethodDescriptor>>emptySet());
	}

	private List<ResolutionCandidate<MethodDescriptor>> findCandidatesByExactSignature(NapileScope scope, ReceiverDescriptor receiver, Name name, List<NapileType> parameterTypes)
	{
		List<ResolutionCandidate<MethodDescriptor>> result = Lists.newArrayList();
		if(receiver.exists())
		{
			Collection<ResolutionCandidate<MethodDescriptor>> extensionFunctionDescriptors = ResolutionCandidate.convertCollection(scope.getMethods(name), false);
			List<ResolutionCandidate<MethodDescriptor>> nonlocal = Lists.newArrayList();
			List<ResolutionCandidate<MethodDescriptor>> local = Lists.newArrayList();
			TaskPrioritizer.splitLexicallyLocalDescriptors(extensionFunctionDescriptors, scope.getContainingDeclaration(), local, nonlocal);


			Collection<ResolutionCandidate<MethodDescriptor>> functionDescriptors = ResolutionCandidate.convertCollection(receiver.getType().getMemberScope().getMethods(name), false);
			if(lookupExactSignature(functionDescriptors, parameterTypes, result))
				return result;

			return result;
		}
		else
		{
			lookupExactSignature(ResolutionCandidate.convertCollection(scope.getMethods(name), false), parameterTypes, result);
			return result;
		}
	}

	private static boolean lookupExactSignature(Collection<ResolutionCandidate<MethodDescriptor>> candidates, List<NapileType> parameterTypes, List<ResolutionCandidate<MethodDescriptor>> result)
	{
		boolean found = false;
		for(ResolutionCandidate<MethodDescriptor> resolvedCall : candidates)
		{
			MethodDescriptor methodDescriptor = resolvedCall.getDescriptor();

			if(!methodDescriptor.getTypeParameters().isEmpty())
				continue;
			if(!checkValueParameters(methodDescriptor, parameterTypes))
				continue;
			result.add(resolvedCall);
			found = true;
		}
		return found;
	}

	private static boolean checkValueParameters(@NotNull MethodDescriptor methodDescriptor, @NotNull List<NapileType> parameterTypes)
	{
		List<CallParameterDescriptor> valueParameters = methodDescriptor.getValueParameters();
		if(valueParameters.size() != parameterTypes.size())
			return false;
		for(int i = 0; i < valueParameters.size(); i++)
		{
			CallParameterDescriptor valueParameter = valueParameters.get(i);
			NapileType expectedType = parameterTypes.get(i);
			if(!TypeUtils.equalTypes(expectedType, valueParameter.getType()))
				return false;
		}
		return true;
	}
}
