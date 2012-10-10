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

package org.napile.compiler.lang.resolve.processors.members;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.descriptors.ClassifierDescriptor;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.ParameterDescriptor;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.diagnostics.Errors;
import org.napile.compiler.lang.psi.NapileAnnotationEntry;
import org.napile.compiler.lang.psi.NapileConstantExpression;
import org.napile.compiler.psi.NapileElement;
import org.napile.compiler.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileModifierList;
import org.napile.compiler.lang.psi.NapileParenthesizedExpression;
import org.napile.compiler.lang.psi.NapileStringTemplateExpression;
import org.napile.compiler.lang.psi.NapileVisitor;
import org.napile.compiler.lang.psi.ValueArgument;
import org.napile.compiler.lang.resolve.AnnotationUtils;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.calls.CallMaker;
import org.napile.compiler.lang.resolve.calls.CallResolver;
import org.napile.compiler.lang.resolve.calls.OverloadResolutionResults;
import org.napile.compiler.lang.resolve.calls.ResolvedValueArgument;
import org.napile.compiler.lang.resolve.calls.autocasts.DataFlowInfo;
import org.napile.compiler.lang.resolve.constants.CompileTimeConstant;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.lang.types.ErrorUtils;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.TypeUtils;
import org.napile.compiler.lang.types.expressions.ExpressionTypingServices;
import com.google.common.collect.Lists;

/**
 * @author abreslav
 */
public class AnnotationResolver
{

	private ExpressionTypingServices expressionTypingServices;
	private CallResolver callResolver;

	@Inject
	public void setExpressionTypingServices(ExpressionTypingServices expressionTypingServices)
	{
		this.expressionTypingServices = expressionTypingServices;
	}

	@Inject
	public void setCallResolver(CallResolver callResolver)
	{
		this.callResolver = callResolver;
	}

	@NotNull
	public List<AnnotationDescriptor> resolveAnnotations(@NotNull JetScope scope, @Nullable NapileModifierList modifierList, BindingTrace trace)
	{
		if(modifierList == null)
			return Collections.emptyList();

		return resolveAnnotations(scope, modifierList.getAnnotationEntries(), trace);
	}

	@NotNull
	public List<AnnotationDescriptor> resolveAnnotations(@NotNull JetScope scope, @NotNull List<NapileAnnotationEntry> annotationEntryElements, BindingTrace trace)
	{
		if(annotationEntryElements.isEmpty())
			return Collections.emptyList();
		List<AnnotationDescriptor> result = Lists.newArrayList();
		for(NapileAnnotationEntry entryElement : annotationEntryElements)
		{
			AnnotationDescriptor descriptor = new AnnotationDescriptor();
			resolveAnnotationStub(scope, entryElement, descriptor, trace);
			trace.record(BindingContext.ANNOTATION, entryElement, descriptor);
			result.add(descriptor);
		}
		return result;
	}

	public void resolveAnnotationStub(@NotNull JetScope scope, @NotNull NapileAnnotationEntry entryElement, @NotNull AnnotationDescriptor annotationDescriptor, BindingTrace trace)
	{
		OverloadResolutionResults<MethodDescriptor> results = callResolver.resolveFunctionCall(trace, scope, CallMaker.makeCall(ReceiverDescriptor.NO_RECEIVER, null, entryElement), TypeUtils.NO_EXPECTED_TYPE, DataFlowInfo.EMPTY);
		if(results.isSuccess())
		{
			MethodDescriptor descriptor = results.getResultingDescriptor();
			if(!ErrorUtils.isError(descriptor))
			{
				if(descriptor instanceof ConstructorDescriptor)
				{
					ConstructorDescriptor constructor = (ConstructorDescriptor) descriptor;
					ClassifierDescriptor classDescriptor = constructor.getContainingDeclaration();
					if(!AnnotationUtils.isAnnotation(classDescriptor))
					{
						trace.report(Errors.NOT_AN_ANNOTATION_CLASS.on(entryElement, classDescriptor.getName().getName()));
					}
				}
				else
				{
					trace.report(Errors.NOT_AN_ANNOTATION_CLASS.on(entryElement, descriptor.getName().getName()));
				}
			}
		}
		if(results.isSuccess())
		{
			JetType annotationType = results.getResultingDescriptor().getReturnType();
			annotationDescriptor.setAnnotationType(annotationType);
			resolveArguments(results, annotationDescriptor, trace);
		}
		else
		{
			annotationDescriptor.setAnnotationType(ErrorUtils.createErrorType("Unresolved annotation type"));
		}
	}

	private void resolveArguments(@NotNull OverloadResolutionResults<MethodDescriptor> results, @NotNull AnnotationDescriptor descriptor, BindingTrace trace)
	{
		List<CompileTimeConstant<?>> arguments = Lists.newArrayList();
		for(Map.Entry<ParameterDescriptor, ResolvedValueArgument> descriptorToArgument : results.getResultingCall().getValueArguments().entrySet())
		{
			// TODO: are varargs supported here?
			List<ValueArgument> valueArguments = descriptorToArgument.getValue().getArguments();
			ParameterDescriptor parameterDescriptor = descriptorToArgument.getKey();
			for(ValueArgument argument : valueArguments)
			{
				arguments.add(resolveAnnotationArgument(argument.getArgumentExpression(), parameterDescriptor.getType(), trace));
			}
		}
		descriptor.setValueArguments(arguments);
	}

	@Nullable
	public CompileTimeConstant<?> resolveAnnotationArgument(@NotNull NapileExpression expression, @NotNull final JetType expectedType, final BindingTrace trace)
	{
		NapileVisitor<CompileTimeConstant<?>, Void> visitor = new NapileVisitor<CompileTimeConstant<?>, Void>()
		{
			@Override
			public CompileTimeConstant<?> visitConstantExpression(NapileConstantExpression expression, Void nothing)
			{
				JetType type = expressionTypingServices.getType(JetScope.EMPTY, expression, expectedType, DataFlowInfo.EMPTY, trace);
				if(type == null)
				{
					// TODO:
					//  trace.report(ANNOTATION_PARAMETER_SHOULD_BE_CONSTANT.on(expression));
				}
				return trace.get(BindingContext.COMPILE_TIME_VALUE, expression);
			}


			// @Override
			//            public CompileTimeConstant visitAnnotationList(NapileAnnotation annotation, Void nothing) {
			//                super.visitAnnotationList(annotation, null); // TODO
			//            }
			//
			//            @Override
			//            public CompileTimeConstant visitAnnotationEntry(NapileAnnotationEntry annotationEntry, Void nothing) {
			//                return super.visitAnnotationEntry(annotationEntry, null); // TODO
			//            }

			@Override
			public CompileTimeConstant<?> visitParenthesizedExpression(NapileParenthesizedExpression expression, Void nothing)
			{
				NapileExpression innerExpression = expression.getExpression();
				if(innerExpression == null)
					return null;
				return innerExpression.accept(this, null);
			}

			@Override
			public CompileTimeConstant<?> visitStringTemplateExpression(NapileStringTemplateExpression expression, Void nothing)
			{
				return trace.get(BindingContext.COMPILE_TIME_VALUE, expression);
			}

			@Override
			public CompileTimeConstant<?> visitJetElement(NapileElement element, Void nothing)
			{
				// TODO:
				//trace.report(ANNOTATION_PARAMETER_SHOULD_BE_CONSTANT.on(element));
				return null;
			}
		};
		return expression.accept(visitor, null);
	}

	@NotNull
	public List<AnnotationDescriptor> createAnnotationStubs(@Nullable NapileModifierList modifierList, BindingTrace trace)
	{
		if(modifierList == null)
		{
			return Collections.emptyList();
		}
		return createAnnotationStubs(modifierList.getAnnotationEntries(), trace);
	}

	@NotNull
	public List<AnnotationDescriptor> createAnnotationStubs(List<NapileAnnotationEntry> annotations, BindingTrace trace)
	{
		List<AnnotationDescriptor> result = Lists.newArrayList();
		for(NapileAnnotationEntry annotation : annotations)
		{
			AnnotationDescriptor annotationDescriptor = new AnnotationDescriptor();
			result.add(annotationDescriptor);
			trace.record(BindingContext.ANNOTATION, annotation, annotationDescriptor);
		}
		return result;
	}
}
