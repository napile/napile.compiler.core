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

package org.napile.compiler.lang.types.expressions;

import static org.napile.compiler.lang.diagnostics.Errors.CANNOT_INFER_PARAMETER_TYPE;
import static org.napile.compiler.lang.resolve.BindingContext.AUTO_CREATED_IT;
import static org.napile.compiler.lang.resolve.BindingContext.CLASS;
import static org.napile.compiler.lang.resolve.BindingContext.EXPRESSION_TYPE;
import static org.napile.compiler.lang.resolve.BindingContext.PROCESSED;
import static org.napile.compiler.lang.resolve.BindingContext.TRACE_DELTAS_CACHE;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.descriptors.*;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.psi.NapileBlockExpression;
import org.napile.compiler.lang.psi.NapileElement;
import org.napile.compiler.lang.psi.NapileFunctionLiteral;
import org.napile.compiler.lang.psi.NapileFunctionLiteralExpression;
import org.napile.compiler.lang.psi.NapileObjectLiteralExpression;
import org.napile.compiler.lang.psi.NapilePropertyParameter;
import org.napile.compiler.lang.psi.NapileTypeReference;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingContextUtils;
import org.napile.compiler.lang.resolve.BindingTraceContext;
import org.napile.compiler.lang.resolve.DelegatingBindingTrace;
import org.napile.compiler.lang.resolve.ObservableBindingTrace;
import org.napile.compiler.lang.resolve.TemporaryBindingTrace;
import org.napile.compiler.lang.resolve.TopDownAnalyzer;
import org.napile.compiler.lang.resolve.name.Name;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.lang.rt.NapileLangPackage;
import org.napile.compiler.lang.types.DeferredType;
import org.napile.compiler.lang.types.ErrorUtils;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.JetTypeInfo;
import org.napile.compiler.lang.types.MethodTypeConstructor;
import org.napile.compiler.lang.types.TypeUtils;
import org.napile.compiler.lang.types.impl.JetTypeImpl;
import org.napile.compiler.lang.types.impl.MethodTypeConstructorImpl;
import org.napile.compiler.util.lazy.LazyValueWithDefault;
import org.napile.compiler.util.slicedmap.WritableSlice;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.intellij.psi.PsiElement;

/**
 * @author abreslav
 * @author svtk
 */
public class ClosureExpressionsTypingVisitor extends ExpressionTypingVisitor
{
	protected ClosureExpressionsTypingVisitor(@NotNull ExpressionTypingInternals facade)
	{
		super(facade);
	}

	@Override
	public JetTypeInfo visitObjectLiteralExpression(final NapileObjectLiteralExpression expression, final ExpressionTypingContext context)
	{
		DelegatingBindingTrace delegatingBindingTrace = context.trace.get(TRACE_DELTAS_CACHE, expression.getObjectDeclaration());
		if(delegatingBindingTrace != null)
		{
			delegatingBindingTrace.addAllMyDataTo(context.trace);
			JetType type = context.trace.get(EXPRESSION_TYPE, expression);
			return DataFlowUtils.checkType(type, expression, context, context.dataFlowInfo);
		}
		final JetType[] result = new JetType[1];
		final TemporaryBindingTrace temporaryTrace = TemporaryBindingTrace.create(context.trace);
		ObservableBindingTrace.RecordHandler<PsiElement, ClassDescriptor> handler = new ObservableBindingTrace.RecordHandler<PsiElement, ClassDescriptor>()
		{

			@Override
			public void handleRecord(WritableSlice<PsiElement, ClassDescriptor> slice, PsiElement declaration, final ClassDescriptor descriptor)
			{
				if(slice == CLASS && declaration == expression.getObjectDeclaration())
				{
					JetType defaultType = DeferredType.create(context.trace, new LazyValueWithDefault<JetType>(ErrorUtils.createErrorType("Recursive dependency"))
					{
						@Override
						protected JetType compute()
						{
							return descriptor.getDefaultType();
						}
					});
					result[0] = defaultType;
					if(!context.trace.get(PROCESSED, expression))
					{
						temporaryTrace.record(EXPRESSION_TYPE, expression, defaultType);
						temporaryTrace.record(PROCESSED, expression);
					}
				}
			}
		};
		ObservableBindingTrace traceAdapter = new ObservableBindingTrace(temporaryTrace);
		traceAdapter.addHandler(CLASS, handler);
		TopDownAnalyzer.processClassOrObject(context.expressionTypingServices.getProject(), traceAdapter, context.scope, context.scope.getContainingDeclaration(), expression.getObjectDeclaration());

		DelegatingBindingTrace cloneDelta = new DelegatingBindingTrace(new BindingTraceContext().getBindingContext());
		temporaryTrace.addAllMyDataTo(cloneDelta);
		context.trace.record(TRACE_DELTAS_CACHE, expression.getObjectDeclaration(), cloneDelta);
		temporaryTrace.commit();
		return DataFlowUtils.checkType(result[0], expression, context, context.dataFlowInfo);
	}

	@Override
	public JetTypeInfo visitFunctionLiteralExpression(NapileFunctionLiteralExpression expression, ExpressionTypingContext context)
	{
		NapileFunctionLiteral functionLiteral = expression.getFunctionLiteral();
		NapileBlockExpression bodyExpression = functionLiteral.getBodyExpression();
		if(bodyExpression == null)
			return null;

		JetType expectedType = context.expectedType;
		boolean functionTypeExpected = expectedType != TypeUtils.NO_EXPECTED_TYPE && expectedType.getConstructor() instanceof MethodTypeConstructor;

		SimpleMethodDescriptorImpl functionDescriptor = createFunctionDescriptor(expression, context, functionTypeExpected);

		List<ParameterDescriptor> valueParameters = functionDescriptor.getValueParameters();
		Map<Name, JetType> parameterTypes = new LinkedHashMap<Name, JetType>(valueParameters.size());
		for(ParameterDescriptor valueParameter : valueParameters)
			parameterTypes.put(valueParameter.getName(), valueParameter.getType());

		ReceiverDescriptor receiverParameter = functionDescriptor.getReceiverParameter();

		JetType returnType = TypeUtils.NO_EXPECTED_TYPE;
		JetScope functionInnerScope = FunctionDescriptorUtil.getFunctionInnerScope(context.scope, functionDescriptor, context.trace);
		NapileTypeReference returnTypeRef = functionLiteral.getReturnTypeRef();
		TemporaryBindingTrace temporaryTrace = TemporaryBindingTrace.create(context.trace);
		if(returnTypeRef != null)
		{
			returnType = context.expressionTypingServices.getTypeResolver().resolveType(context.scope, returnTypeRef, context.trace, true);
			context.expressionTypingServices.checkFunctionReturnType(expression, context.replaceScope(functionInnerScope).
					replaceExpectedType(returnType).replaceBindingTrace(temporaryTrace), temporaryTrace);
		}
		else
		{
			if(functionTypeExpected)
			{

				returnType = ((MethodTypeConstructor) expectedType.getConstructor()).getReturnType();
			}
			returnType = context.expressionTypingServices.getBlockReturnedType(functionInnerScope, bodyExpression, CoercionStrategy.COERCION_TO_UNIT, context.replaceExpectedType(returnType).replaceBindingTrace(temporaryTrace), temporaryTrace).getType();
		}
		temporaryTrace.commit(new Predicate<WritableSlice>()
		{
			@Override
			public boolean apply(@Nullable WritableSlice slice)
			{
				return (slice != BindingContext.RESOLUTION_RESULTS_FOR_FUNCTION &&
						slice != BindingContext.RESOLUTION_RESULTS_FOR_PROPERTY &&
						slice != BindingContext.TRACE_DELTAS_CACHE);
			}
		}, true);
		JetType safeReturnType = returnType == null ? ErrorUtils.createErrorType("<return type>") : returnType;
		functionDescriptor.setReturnType(safeReturnType);

		boolean hasDeclaredValueParameters = functionLiteral.getValueParameterList() != null;
		if(!hasDeclaredValueParameters && functionTypeExpected)
		{

			JetType expectedReturnType = ((MethodTypeConstructor) expectedType.getConstructor()).getReturnType();
			if(TypeUtils.isEqualFqName(expectedReturnType, NapileLangPackage.NULL))
			{
				functionDescriptor.setReturnType(TypeUtils.getTypeOfClassOrErrorType(context.scope, NapileLangPackage.NULL));
				return DataFlowUtils.checkType(new JetTypeImpl(new MethodTypeConstructorImpl(functionDescriptor.getReturnType(), parameterTypes), context.scope), expression, context, context.dataFlowInfo);
			}
		}
		return DataFlowUtils.checkType(new JetTypeImpl(new MethodTypeConstructorImpl(safeReturnType, parameterTypes), context.scope), expression, context, context.dataFlowInfo);
	}

	private SimpleMethodDescriptorImpl createFunctionDescriptor(NapileFunctionLiteralExpression expression, ExpressionTypingContext context, boolean functionTypeExpected)
	{
		NapileFunctionLiteral functionLiteral = expression.getFunctionLiteral();
		NapileTypeReference receiverTypeRef = functionLiteral.getReceiverTypeRef();
		SimpleMethodDescriptorImpl functionDescriptor = new SimpleMethodDescriptorImpl(context.scope.getContainingDeclaration(), Collections.<AnnotationDescriptor>emptyList(), Name.special("<anonymous>"), CallableMemberDescriptor.Kind.DECLARATION, false, false);

		List<ParameterDescriptor> parameterDescriptors = createValueParameterDescriptors(context, functionLiteral, functionDescriptor, functionTypeExpected);

		JetType effectiveReceiverType;
		if(receiverTypeRef == null)
		{
			if(functionTypeExpected)
			{
				effectiveReceiverType = null;
			}
			else
			{
				effectiveReceiverType = null;
			}
		}
		else
		{
			effectiveReceiverType = context.expressionTypingServices.getTypeResolver().resolveType(context.scope, receiverTypeRef, context.trace, true);
		}
		functionDescriptor.initialize(effectiveReceiverType, ReceiverDescriptor.NO_RECEIVER, Collections.<TypeParameterDescriptorImpl>emptyList(), parameterDescriptors,
                                      /*unsubstitutedReturnType = */ null, Modality.FINAL, Visibility.LOCAL2
                                      /*isInline = */);
		context.trace.record(BindingContext.METHOD, expression, functionDescriptor);
		BindingContextUtils.recordFunctionDeclarationToDescriptor(context.trace, expression, functionDescriptor);
		return functionDescriptor;
	}

	private List<ParameterDescriptor> createValueParameterDescriptors(ExpressionTypingContext context, NapileFunctionLiteral functionLiteral, MethodDescriptorImpl functionDescriptor, boolean functionTypeExpected)
	{
		List<ParameterDescriptor> parameterDescriptors = Lists.newArrayList();
		List<NapileElement> declaredValueParameters = functionLiteral.getValueParameters();

		List<ParameterDescriptor> expectedValueParameters = (functionTypeExpected) ? FunctionDescriptorUtil.getValueParameters(functionDescriptor, context.expectedType) : null;

		boolean hasDeclaredValueParameters = functionLiteral.getValueParameterList() != null;
		if(functionTypeExpected && !hasDeclaredValueParameters && expectedValueParameters.size() == 1)
		{
			ParameterDescriptor parameterDescriptor = expectedValueParameters.get(0);
			ParameterDescriptor it = new PropertyParameterDescriptorImpl(functionDescriptor, 0, Collections.<AnnotationDescriptor>emptyList(), Name.identifier("it"), PropertyKind.VAL, parameterDescriptor.getType(), parameterDescriptor.hasDefaultValue(), parameterDescriptor.getVarargElementType());
			parameterDescriptors.add(it);
			context.trace.record(AUTO_CREATED_IT, it);
		}
		else
		{
			for(int i = 0; i < declaredValueParameters.size(); i++)
			{
				NapileElement declaredParameter = declaredValueParameters.get(i);
				if(!(declaredParameter instanceof NapilePropertyParameter))
					continue;
				NapilePropertyParameter propertyParameter = (NapilePropertyParameter) declaredParameter;
				NapileTypeReference typeReference = propertyParameter.getTypeReference();

				JetType type;
				if(typeReference != null)
				{
					type = context.expressionTypingServices.getTypeResolver().resolveType(context.scope, typeReference, context.trace, true);
				}
				else
				{
					if(expectedValueParameters != null && i < expectedValueParameters.size())
					{
						type = expectedValueParameters.get(i).getType();
					}
					else
					{
						context.trace.report(CANNOT_INFER_PARAMETER_TYPE.on(propertyParameter));
						type = ErrorUtils.createErrorType("Cannot be inferred");
					}
				}
				ParameterDescriptor parameterDescriptor = context.expressionTypingServices.getDescriptorResolver().resolveValueParameterDescriptor(context.scope, functionDescriptor, propertyParameter, i, type, context.trace);
				parameterDescriptors.add(parameterDescriptor);
			}
		}
		return parameterDescriptors;
	}
}
