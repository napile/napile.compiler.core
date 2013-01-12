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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.lib.NapileLangPackage;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.descriptors.*;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.psi.NapileAnonymClassExpression;
import org.napile.compiler.lang.psi.NapileAnonymMethodExpression;
import org.napile.compiler.lang.psi.NapileAnonymMethodImpl;
import org.napile.compiler.lang.psi.NapileBlockExpression;
import org.napile.compiler.lang.psi.NapileCallParameterAsVariable;
import org.napile.compiler.lang.psi.NapileElement;
import org.napile.compiler.lang.psi.NapileTypeReference;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingContextUtils;
import org.napile.compiler.lang.resolve.TemporaryBindingTrace;
import org.napile.compiler.lang.resolve.processors.AnonymClassResolver;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.lang.types.ErrorUtils;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.JetTypeInfo;
import org.napile.compiler.lang.types.MethodTypeConstructor;
import org.napile.compiler.lang.types.TypeUtils;
import org.napile.compiler.lang.types.impl.JetTypeImpl;
import org.napile.compiler.lang.types.impl.MethodTypeConstructorImpl;
import org.napile.compiler.util.slicedmap.WritableSlice;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

/**
 * @author abreslav
 * @author svtk
 */
public class ClosureExpressionsTypingVisitor extends ExpressionTypingVisitor
{
	@NotNull
	private final AnonymClassResolver anonymClassResolver;

	protected ClosureExpressionsTypingVisitor(@NotNull ExpressionTypingInternals facade, @NotNull ExpressionTypingServices services)
	{
		super(facade);
		anonymClassResolver = services.getAnonymClassResolver();
	}

	@Override
	public JetTypeInfo visitAnonymClassExpression(final NapileAnonymClassExpression expression, final ExpressionTypingContext context)
	{
		ClassDescriptor classDescriptor = context.trace.get(BindingContext.CLASS, expression.getAnonymClass());
		if(classDescriptor == null)
			classDescriptor = anonymClassResolver.resolveAnonymClass(context.scope.getContainingDeclaration(), context.scope, context.trace, expression.getAnonymClass());

		return DataFlowUtils.checkType(classDescriptor.getDefaultType(), expression, context, context.dataFlowInfo);
	}

	@Override
	public JetTypeInfo visitAnonymMethodExpression(NapileAnonymMethodExpression expression, ExpressionTypingContext context)
	{
		NapileAnonymMethodImpl functionLiteral = expression.getAnonymMethod();
		NapileBlockExpression bodyExpression = functionLiteral.getBodyExpression();
		if(bodyExpression == null)
			return null;

		JetType expectedType = context.expectedType;
		boolean functionTypeExpected = expectedType != TypeUtils.NO_EXPECTED_TYPE && expectedType.getConstructor() instanceof MethodTypeConstructor;

		SimpleMethodDescriptorImpl functionDescriptor = createFunctionDescriptor(expression, context, functionTypeExpected);

		List<CallParameterDescriptor> valueParameters = functionDescriptor.getValueParameters();
		Map<Name, JetType> parameterTypes = new LinkedHashMap<Name, JetType>(valueParameters.size());
		for(CallParameterDescriptor valueParameter : valueParameters)
			parameterTypes.put(valueParameter.getName(), valueParameter.getType());

		JetType returnType = TypeUtils.NO_EXPECTED_TYPE;
		JetScope functionInnerScope = MethodDescriptorUtil.getMethodInnerScope(context.scope, functionDescriptor, functionLiteral, context.trace);
		NapileTypeReference returnTypeRef = functionLiteral.getReturnTypeRef();
		TemporaryBindingTrace temporaryTrace = TemporaryBindingTrace.create(context.trace);
		if(returnTypeRef != null)
		{
			returnType = context.expressionTypingServices.getTypeResolver().resolveType(context.scope, returnTypeRef, context.trace, true);
			context.expressionTypingServices.checkFunctionReturnType(expression.getAnonymMethod(), context.replaceScope(functionInnerScope).
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

		boolean hasDeclaredValueParameters = functionLiteral.getCallParameterList() != null;
		if(!hasDeclaredValueParameters && functionTypeExpected)
		{

			JetType expectedReturnType = ((MethodTypeConstructor) expectedType.getConstructor()).getReturnType();
			if(TypeUtils.isEqualFqName(expectedReturnType, NapileLangPackage.NULL))
			{
				functionDescriptor.setReturnType(TypeUtils.getTypeOfClassOrErrorType(context.scope, NapileLangPackage.NULL));
				return DataFlowUtils.checkType(new JetTypeImpl(new MethodTypeConstructorImpl(functionDescriptor.getReturnType(), parameterTypes, context.scope), context.scope), expression, context, context.dataFlowInfo);
			}
		}
		return DataFlowUtils.checkType(new JetTypeImpl(new MethodTypeConstructorImpl(safeReturnType, parameterTypes, context.scope), context.scope), expression, context, context.dataFlowInfo);
	}

	private SimpleMethodDescriptorImpl createFunctionDescriptor(NapileAnonymMethodExpression expression, ExpressionTypingContext context, boolean functionTypeExpected)
	{
		NapileAnonymMethodImpl functionLiteral = expression.getAnonymMethod();

		SimpleMethodDescriptorImpl functionDescriptor = new SimpleMethodDescriptorImpl(context.scope.getContainingDeclaration(), Collections.<AnnotationDescriptor>emptyList(), functionLiteral.getNameAsName(), CallableMemberDescriptor.Kind.DECLARATION, false, false, false);

		List<CallParameterDescriptor> parameterDescriptors = createValueParameterDescriptors(context, functionLiteral, functionDescriptor, functionTypeExpected);

		functionDescriptor.initialize(ReceiverDescriptor.NO_RECEIVER, Collections.<TypeParameterDescriptorImpl>emptyList(), parameterDescriptors, null, Modality.FINAL, Visibility.LOCAL);
		context.trace.record(BindingContext.METHOD, expression, functionDescriptor);
		BindingContextUtils.recordMethodDeclarationToDescriptor(context.trace, expression, functionDescriptor);
		return functionDescriptor;
	}

	private List<CallParameterDescriptor> createValueParameterDescriptors(ExpressionTypingContext context, NapileAnonymMethodImpl functionLiteral, AbstractMethodDescriptorImpl functionDescriptor, boolean functionTypeExpected)
	{
		List<CallParameterDescriptor> parameterDescriptors = Lists.newArrayList();
		NapileElement[] declaredValueParameters = functionLiteral.getValueParameters();

		List<CallParameterDescriptor> expectedValueParameters = (functionTypeExpected) ? MethodDescriptorUtil.getValueParameters(functionDescriptor, context.expectedType) : null;

		boolean hasDeclaredValueParameters = functionLiteral.getCallParameterList() != null;
		if(functionTypeExpected && !hasDeclaredValueParameters && expectedValueParameters.size() == 1)
		{
			CallParameterDescriptor parameterDescriptor = expectedValueParameters.get(0);

			AbstractCallParameterDescriptorImpl value = new CallParameterAsVariableDescriptorImpl(functionDescriptor, 0, Collections.<AnnotationDescriptor>emptyList(), Name.identifier("value"), parameterDescriptor.getType(), Modality.FINAL, false);
			value.setHasDefaultValue(parameterDescriptor.hasDefaultValue());

			parameterDescriptors.add(value);
			context.trace.record(AUTO_CREATED_IT, value);
		}
		else
		{
			for(int i = 0; i < declaredValueParameters.length; i++)
			{
				NapileElement declaredParameter = declaredValueParameters[i];
				if(!(declaredParameter instanceof NapileCallParameterAsVariable))
					continue;
				NapileCallParameterAsVariable propertyParameter = (NapileCallParameterAsVariable) declaredParameter;
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
				CallParameterDescriptor parameterDescriptor = context.expressionTypingServices.getDescriptorResolver().resolveCallParameterDescriptor(context.scope, functionDescriptor, propertyParameter, i, type, context.trace);
				parameterDescriptors.add(parameterDescriptor);
			}
		}
		return parameterDescriptors;
	}
}
