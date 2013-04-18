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

import static org.napile.compiler.lang.resolve.BindingTraceKeys.DEFERRED_TYPE;

import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.*;
import org.napile.compiler.lang.diagnostics.Errors;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.lang.resolve.BindingTraceKeys;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.BodiesResolveContext;
import org.napile.compiler.lang.resolve.ObservableBindingTrace;
import org.napile.compiler.lang.resolve.TopDownAnalysisParameters;
import org.napile.compiler.lang.resolve.calls.CallMaker;
import org.napile.compiler.lang.resolve.calls.CallResolver;
import org.napile.compiler.lang.resolve.calls.autocasts.DataFlowInfo;
import org.napile.compiler.lang.resolve.processors.checkers.DeclarationsChecker;
import org.napile.compiler.lang.resolve.processors.checkers.ModifiersChecker;
import org.napile.compiler.lang.resolve.scopes.NapileScope;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.lang.types.DeferredType;
import org.napile.compiler.lang.types.NapileType;
import org.napile.compiler.lang.types.TypeUtils;
import org.napile.compiler.lang.types.expressions.ExpressionTypingServices;
import org.napile.compiler.lang.types.expressions.VariableAccessorResolver;
import org.napile.compiler.util.Box;
import org.napile.compiler.util.lazy.ReenteringLazyValueComputationException;
import org.napile.compiler.util.slicedmap.WritableSlice;
import com.intellij.util.containers.Queue;

/**
 * @author abreslav
 */
public class BodyResolver
{
	@NotNull
	private BodiesResolveContext context;
	@NotNull
	private TopDownAnalysisParameters topDownAnalysisParameters;
	@NotNull
	private DescriptorResolver descriptorResolver;
	@NotNull
	private ExpressionTypingServices expressionTypingServices;
	@NotNull
	private CallResolver callResolver;
	@NotNull
	private ObservableBindingTrace trace;
	@NotNull
	private ControlFlowAnalyzer controlFlowAnalyzer;
	@NotNull
	private DeclarationsChecker declarationsChecker;
	@NotNull
	private ModifiersChecker modifiersChecker;

	@Inject
	public void setTopDownAnalysisParameters(@NotNull TopDownAnalysisParameters topDownAnalysisParameters)
	{
		this.topDownAnalysisParameters = topDownAnalysisParameters;
	}

	@Inject
	public void setDescriptorResolver(@NotNull DescriptorResolver descriptorResolver)
	{
		this.descriptorResolver = descriptorResolver;
	}

	@Inject
	public void setExpressionTypingServices(@NotNull ExpressionTypingServices expressionTypingServices)
	{
		this.expressionTypingServices = expressionTypingServices;
	}

	@Inject
	public void setCallResolver(@NotNull CallResolver callResolver)
	{
		this.callResolver = callResolver;
	}

	@Inject
	public void setTrace(@NotNull BindingTrace trace)
	{
		this.trace = new ObservableBindingTrace(trace);
	}

	@Inject
	public void setControlFlowAnalyzer(@NotNull ControlFlowAnalyzer controlFlowAnalyzer)
	{
		this.controlFlowAnalyzer = controlFlowAnalyzer;
	}

	@Inject
	public void setDeclarationsChecker(@NotNull DeclarationsChecker declarationsChecker)
	{
		this.declarationsChecker = declarationsChecker;
	}

	@Inject
	public void setModifiersChecker(@NotNull ModifiersChecker modifiersChecker)
	{
		this.modifiersChecker = modifiersChecker;
	}

	private void resolveBehaviorDeclarationBodies(@NotNull BodiesResolveContext bodiesResolveContext)
	{
		// Initialize context
		context = bodiesResolveContext;

		resolveDelegationSpecifierLists();

		resolveBodies();

		if(!topDownAnalysisParameters.isDeclaredLocally())
		{
			computeDeferredTypes();
		}
	}

	public void resolveBodies(@NotNull BodiesResolveContext bodiesResolveContext)
	{
		resolveBehaviorDeclarationBodies(bodiesResolveContext);

		controlFlowAnalyzer.process(bodiesResolveContext);
		modifiersChecker.process(bodiesResolveContext);
		declarationsChecker.process(bodiesResolveContext);
	}

	private void resolveDelegationSpecifierLists()
	{
		for(Map.Entry<NapileConstructor, ConstructorDescriptor> entry : context.getConstructors().entrySet())
			resolveDelegationSpecifierList(entry.getKey(), entry.getValue(), entry.getValue().getParametersScope(), false);

		// anonym resolved later
		//for(Map.Entry<NapileAnonymClass, MutableClassDescriptor> entry : context.getAnonymous().entrySet())
		//	resolveDelegationSpecifierList(entry.getKey(), entry.getValue(), entry.getValue().getScopeForSupertypeResolution());

		for(Map.Entry<NapileEnumValue, MutableClassDescriptor> entry : context.getEnumValues().entrySet())
			resolveDelegationSpecifierList(entry.getKey(), entry.getValue(), entry.getValue().getScopeForSupertypeResolution(), true);
	}

	public void resolveDelegationSpecifierList(final NapileDelegationSpecifierListOwner owner, @NotNull final DeclarationDescriptor declarationDescriptor, final @NotNull NapileScope napileScope, boolean canSuperTraitedClass)
	{
		if(!context.completeAnalysisNeeded(owner))
			return;

		for(NapileDelegationToSuperCall call : owner.getDelegationSpecifiers())
		{
			NapileTypeReference typeReference = call.getTypeReference();
			if(typeReference == null)
			{
				continue;
			}

			NapileType type = trace.get(BindingTraceKeys.TYPE, typeReference);
			if(type == null)
			{
				continue;
			}

			if(!canSuperTraitedClass)
			{
				callResolver.resolveFunctionCall(trace, napileScope, CallMaker.makeCall(ReceiverDescriptor.NO_RECEIVER, null, call), TypeUtils.NO_EXPECTED_TYPE, DataFlowInfo.EMPTY);
			}
			else
			{
				final ClassifierDescriptor typeOwner = type.getConstructor().getDeclarationDescriptor();
				final boolean isTypeOwnerIsTraited = typeOwner instanceof ClassDescriptor && ((ClassDescriptor) typeOwner).isTraited();

				if(isTypeOwnerIsTraited)
				{
					NapileValueArgumentList valueArgumentList = call.getValueArgumentList();
					if(valueArgumentList != null)
					{
						trace.report(Errors.FROM_TRAITED_CLASS_CANT_CALL_CONSTRUCTOR.on(typeReference));
					}
					continue;
				}

				callResolver.resolveFunctionCall(trace, napileScope, CallMaker.makeCall(ReceiverDescriptor.NO_RECEIVER, null, call), TypeUtils.NO_EXPECTED_TYPE, DataFlowInfo.EMPTY);
			}
		}
	}

	public void resolvePropertyInitializer(NapileVariable property, VariableDescriptor propertyDescriptor, NapileExpression initializer, NapileScope scope)
	{
		//JetFlowInformationProvider flowInformationProvider = context.getDescriptorResolver().computeFlowData(property, initializer); // TODO : flow JET-15
		NapileType expectedTypeForInitializer = property.getType() != null ? propertyDescriptor.getType() : TypeUtils.NO_EXPECTED_TYPE;
		NapileScope propertyDeclarationInnerScope = descriptorResolver.getPropertyDeclarationInnerScope(scope, propertyDescriptor.getTypeParameters(), trace);
		NapileType type = expressionTypingServices.getType(propertyDeclarationInnerScope, initializer, expectedTypeForInitializer, DataFlowInfo.EMPTY, trace);
		//
		//        JetType expectedType = propertyDescriptor.getInType();
		//        if (expectedType == null) {
		//            expectedType = propertyDescriptor.getType();
		//        }
		//        if (type != null && expectedType != null
		//            && !context.getSemanticServices().getTypeChecker().isSubtypeOf(type, expectedType)) {
		////            trace.report(TYPE_MISMATCH.on(initializer, expectedType, type));
		//        }
	}

	private void resolveBodies()
	{
		for(Map.Entry<NapileVariable, VariableDescriptor> entry : this.context.getVariables().entrySet())
		{
			NapileVariable variable = entry.getKey();
			if(!context.completeAnalysisNeeded(variable))
				continue;

			final VariableDescriptor propertyDescriptor = entry.getValue();

			computeDeferredType(propertyDescriptor.getReturnType());

			NapileScope declaringScope = this.context.getDeclaringScopes().get(variable);

			NapileExpression initializer = variable.getInitializer();
			if(initializer != null)
				resolvePropertyInitializer(variable, propertyDescriptor, initializer, declaringScope);

			for(NapileVariableAccessor accessor : variable.getAccessors())
			{
				final VariableAccessorDescriptor descriptor = trace.get(VariableAccessorResolver.getSliceForAccessor(accessor), accessor);
				if(descriptor == null || accessor.getBodyExpression() == null)
				{
					continue;
				}

				resolveBody(trace, accessor, descriptor, declaringScope, true);
			}
		}

		for(Map.Entry<NapileNamedMethodOrMacro, SimpleMethodDescriptor> entry : this.context.getMethods().entrySet())
		{
			NapileNamedMethodOrMacro declaration = entry.getKey();
			SimpleMethodDescriptor descriptor = entry.getValue();

			computeDeferredType(descriptor.getReturnType());

			NapileScope declaringScope = this.context.getDeclaringScopes().get(declaration);
			assert declaringScope != null;

			resolveBody(trace, declaration, descriptor, declaringScope, false);
		}

		for(Map.Entry<NapileConstructor, ConstructorDescriptor> entry : context.getConstructors().entrySet())
		{
			NapileConstructor declaration = entry.getKey();
			ConstructorDescriptor descriptor = entry.getValue();

			NapileScope declaringScope = context.getDeclaringScopes().get(declaration);
			assert declaringScope != null;

			resolveBody(trace, declaration, descriptor, declaringScope, false);
		}
	}

	public void resolveBody(@NotNull BindingTrace trace, @NotNull NapileDeclarationWithBody function, @NotNull MethodDescriptor methodDescriptor, @NotNull NapileScope declaringScope, boolean variableAccessor)
	{
		if(!context.completeAnalysisNeeded(function))
			return;

		NapileExpression bodyExpression = function.getBodyExpression();
		NapileScope functionInnerScope = MethodDescriptorUtil.getMethodInnerScope(declaringScope, methodDescriptor, trace, variableAccessor);
		if(bodyExpression != null)
			expressionTypingServices.checkFunctionReturnType(functionInnerScope, function, methodDescriptor, DataFlowInfo.EMPTY, null, trace);
	}

	private static void computeDeferredType(NapileType type)
	{
		// handle type inference loop: function or property body contains a reference to itself
		// fun f() = { f() }
		// val x = x
		// type resolution must be started before body resolution
		if(type instanceof DeferredType)
		{
			DeferredType deferredType = (DeferredType) type;
			if(!deferredType.isComputed())
			{
				deferredType.getActualType();
			}
		}
	}

	private void computeDeferredTypes()
	{
		Collection<Box<DeferredType>> deferredTypes = trace.getKeys(DEFERRED_TYPE);
		if(deferredTypes != null)
		{
			// +1 is a work around agains new Queue(0).addLast(...) bug // stepan.koltsov@ 2011-11-21
			final Queue<DeferredType> queue = new Queue<DeferredType>(deferredTypes.size() + 1);
			trace.addHandler(DEFERRED_TYPE, new ObservableBindingTrace.RecordHandler<Box<DeferredType>, Boolean>()
			{
				@Override
				public void handleRecord(WritableSlice<Box<DeferredType>, Boolean> deferredTypeKeyDeferredTypeWritableSlice, Box<DeferredType> key, Boolean value)
				{
					queue.addLast(key.getData());
				}
			});
			for(Box<DeferredType> deferredType : deferredTypes)
			{
				queue.addLast(deferredType.getData());
			}
			while(!queue.isEmpty())
			{
				DeferredType deferredType = queue.pullFirst();
				if(!deferredType.isComputed())
				{
					try
					{
						deferredType.getActualType(); // to compute
					}
					catch(ReenteringLazyValueComputationException e)
					{
						// A problem should be reported while computing the type
					}
				}
			}
		}
	}

	@NotNull
	public BodiesResolveContext getContext()
	{
		return context;
	}
}
