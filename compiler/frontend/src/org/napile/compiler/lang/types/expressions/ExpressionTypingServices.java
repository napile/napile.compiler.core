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

import static org.napile.compiler.lang.diagnostics.Errors.TYPE_INFERENCE_ERRORS;
import static org.napile.compiler.lang.diagnostics.Errors.TYPE_MISMATCH;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.lib.NapileLangPackage;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptorUtil;
import org.napile.compiler.lang.diagnostics.Diagnostic;
import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.ObservableBindingTrace;
import org.napile.compiler.lang.resolve.TemporaryBindingTrace;
import org.napile.compiler.lang.resolve.TraceBasedRedeclarationHandler;
import org.napile.compiler.lang.resolve.calls.CallResolver;
import org.napile.compiler.lang.resolve.calls.autocasts.DataFlowInfo;
import org.napile.compiler.lang.resolve.processors.AnonymClassResolver;
import org.napile.compiler.lang.resolve.processors.DescriptorResolver;
import org.napile.compiler.lang.resolve.processors.TypeResolver;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.resolve.scopes.WritableScope;
import org.napile.compiler.lang.resolve.scopes.WritableScopeImpl;
import org.napile.compiler.lang.types.CommonSupertypes;
import org.napile.compiler.lang.types.ErrorUtils;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.JetTypeInfo;
import org.napile.compiler.lang.types.TypeUtils;
import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * @author abreslav
 */
public class ExpressionTypingServices
{
	private ExpressionTypingFacade expressionTypingFacade;

	@NotNull
	private Project project;
	@NotNull
	private CallResolver callResolver;
	@NotNull
	private DescriptorResolver descriptorResolver;
	@NotNull
	private TypeResolver typeResolver;
	@NotNull
	private AnonymClassResolver anonymClassResolver;

	@NotNull
	public Project getProject()
	{
		return project;
	}

	@Inject
	public void setProject(@NotNull Project project)
	{
		this.project = project;
	}

	@Inject
	public void setAnonymClassResolver(@NotNull AnonymClassResolver anonymClassResolver)
	{
		this.anonymClassResolver = anonymClassResolver;
	}

	@NotNull
	public AnonymClassResolver getAnonymClassResolver()
	{
		return anonymClassResolver;
	}

	@NotNull
	public CallResolver getCallResolver()
	{
		return callResolver;
	}

	@Inject
	public void setCallResolver(@NotNull CallResolver callResolver)
	{
		this.callResolver = callResolver;
	}

	@NotNull
	public DescriptorResolver getDescriptorResolver()
	{
		return descriptorResolver;
	}

	@Inject
	public void setDescriptorResolver(@NotNull DescriptorResolver descriptorResolver)
	{
		this.descriptorResolver = descriptorResolver;
	}

	@NotNull
	public TypeResolver getTypeResolver()
	{
		return typeResolver;
	}

	@Inject
	public void setTypeResolver(@NotNull TypeResolver typeResolver)
	{
		this.typeResolver = typeResolver;
	}

	@NotNull
	public ExpressionTypingFacade getExpressionTypingFacade()
	{
		if(expressionTypingFacade == null)
			expressionTypingFacade = ExpressionTypingVisitorDispatcher.create(this);
		return expressionTypingFacade;
	}

	@NotNull
	public JetType safeGetType(@NotNull JetScope scope, @NotNull NapileExpression expression, @NotNull JetType expectedType, @NotNull DataFlowInfo dataFlowInfo, @NotNull BindingTrace trace)
	{
		JetType type = getType(scope, expression, expectedType, dataFlowInfo, trace);
		if(type != null)
		{
			return type;
		}
		return ErrorUtils.createErrorType("Type for " + expression.getText());
	}

	@NotNull
	public JetTypeInfo getTypeInfo(@NotNull final JetScope scope, @NotNull NapileExpression expression, @NotNull JetType expectedType, @NotNull DataFlowInfo dataFlowInfo, @NotNull BindingTrace trace)
	{
		ExpressionTypingContext context = ExpressionTypingContext.newContext(this, trace, scope, dataFlowInfo, expectedType, false);
		return getExpressionTypingFacade().getTypeInfo(expression, context);
	}

	@Nullable
	public JetType getType(@NotNull final JetScope scope, @NotNull NapileExpression expression, @NotNull JetType expectedType, @NotNull DataFlowInfo dataFlowInfo, @NotNull BindingTrace trace)
	{
		return getTypeInfo(scope, expression, expectedType, dataFlowInfo, trace).getType();
	}

	public JetType getTypeWithNamespaces(@NotNull final JetScope scope, @NotNull NapileExpression expression, @NotNull BindingTrace trace)
	{
		ExpressionTypingContext context = ExpressionTypingContext.newContext(this, trace, scope, DataFlowInfo.EMPTY, TypeUtils.NO_EXPECTED_TYPE, true);
		return getExpressionTypingFacade().getTypeInfo(expression, context).getType();
		//        return ((ExpressionTypingContext) ExpressionTyperVisitorWithNamespaces).INSTANCE.getType(expression, ExpressionTypingContext.newRootContext(semanticServices, trace, scope, DataFlowInfo.getEmpty(), TypeUtils.NO_EXPECTED_TYPE, TypeUtils.NO_EXPECTED_TYPE));
	}

	@NotNull
	public JetType inferFunctionReturnType(@NotNull JetScope outerScope, @NotNull NapileDeclarationWithBody function, @NotNull MethodDescriptor methodDescriptor, @NotNull BindingTrace trace)
	{
		Map<NapileExpression, JetType> typeMap = collectReturnedExpressionsWithTypes(trace, outerScope, function, methodDescriptor);
		Collection<JetType> types = typeMap.values();
		return types.isEmpty() ? TypeUtils.getTypeOfClassOrErrorType(outerScope, NapileLangPackage.NULL, false) : CommonSupertypes.commonSupertype(types);
	}


	/////////////////////////////////////////////////////////

	public void checkFunctionReturnType(@NotNull JetScope functionInnerScope, @NotNull NapileDeclarationWithBody function, @NotNull MethodDescriptor methodDescriptor, @NotNull DataFlowInfo dataFlowInfo, @Nullable JetType expectedReturnType, BindingTrace trace)
	{
		if(expectedReturnType == null)
		{
			expectedReturnType = methodDescriptor.getReturnType();
			if(!function.hasBlockBody() && !function.hasDeclaredReturnType())
			{
				expectedReturnType = TypeUtils.NO_EXPECTED_TYPE;
			}
		}
		checkFunctionReturnType(function, ExpressionTypingContext.newContext(this, trace, functionInnerScope, dataFlowInfo, expectedReturnType != null ? expectedReturnType : TypeUtils.NO_EXPECTED_TYPE, false), trace);
	}

	/*package*/ void checkFunctionReturnType(NapileDeclarationWithBody function, ExpressionTypingContext context, BindingTrace trace)
	{
		NapileExpression bodyExpression = function.getBodyExpression();
		if(bodyExpression == null)
			return;

		final boolean blockBody = function.hasBlockBody();
		final ExpressionTypingContext newContext = blockBody ? context.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE) : context;

		if(function instanceof NapileAnonymMethodExpression)
		{
			NapileAnonymMethodExpression functionLiteralExpression = (NapileAnonymMethodExpression) function;
			NapileBlockExpression blockExpression = functionLiteralExpression.getAnonymMethod().getBodyExpression();
			assert blockExpression != null;
			getBlockReturnedType(newContext.scope, blockExpression, CoercionStrategy.COERCION_TO_UNIT, context, trace);
		}
		else
		{
			getExpressionTypingFacade().getTypeInfo(bodyExpression, newContext, !blockBody);
		}
	}

	@NotNull
	public JetTypeInfo getBlockReturnedType(@NotNull JetScope outerScope, @NotNull NapileBlockExpression expression, @NotNull CoercionStrategy coercionStrategyForLastExpression, ExpressionTypingContext context, BindingTrace trace)
	{
		NapileElement[] blocks = expression.getStatements();

		DeclarationDescriptor containingDescriptor = outerScope.getContainingDeclaration();

		WritableScope scope = new WritableScopeImpl(outerScope, containingDescriptor, new TraceBasedRedeclarationHandler(context.trace), "getBlockReturnedType");
		scope.changeLockLevel(WritableScope.LockLevel.BOTH);

		JetTypeInfo r;
		if(blocks.length == 0)
		{
			r = DataFlowUtils.checkType(TypeUtils.getTypeOfClassOrErrorType(scope, NapileLangPackage.NULL), expression, context, context.dataFlowInfo);
		}
		else
		{
			r = getBlockReturnedTypeWithWritableScope(scope, Arrays.asList(blocks), coercionStrategyForLastExpression, context, trace);
		}
		scope.changeLockLevel(WritableScope.LockLevel.READING);

		return r;
	}

	private Map<NapileExpression, JetType> collectReturnedExpressionsWithTypes(final @NotNull BindingTrace trace, JetScope outerScope, final NapileDeclarationWithBody function, MethodDescriptor methodDescriptor)
	{
		NapileExpression bodyExpression = function.getBodyExpression();
		assert bodyExpression != null;
		JetScope functionInnerScope = MethodDescriptorUtil.getMethodInnerScope(outerScope, methodDescriptor, function, trace);
		getExpressionTypingFacade().getTypeInfo(bodyExpression, ExpressionTypingContext.newContext(this, trace, functionInnerScope, DataFlowInfo.EMPTY, TypeUtils.NO_EXPECTED_TYPE, false), !function.hasBlockBody());
		//todo function literals
		final Collection<NapileExpression> returnedExpressions = Lists.newArrayList();
		if(function.hasBlockBody())
		{
			//now this code is never invoked!, it should be invoked for inference of return type of function literal with local returns
			bodyExpression.accept(new NapileTreeVisitor<NapileDeclarationWithBody>()
			{
				@Override
				public Void visitReturnExpression(NapileReturnExpression expression, NapileDeclarationWithBody outerFunction)
				{
					returnedExpressions.add(expression);

					return null;
				}

				@Override
				public Void visitAnonymMethodExpression(NapileAnonymMethodExpression expression, NapileDeclarationWithBody outerFunction)
				{
					return super.visitAnonymMethodExpression(expression, expression.getAnonymMethod());
				}

				@Override
				public Void visitNamedMethodOrMacro(NapileNamedMethodOrMacro function, NapileDeclarationWithBody outerFunction)
				{
					return super.visitNamedMethodOrMacro(function, function);
				}
			}, function);
		}
		else
		{
			returnedExpressions.add(bodyExpression);
		}
		Map<NapileExpression, JetType> typeMap = new HashMap<NapileExpression, JetType>();
		for(NapileExpression returnedExpression : returnedExpressions)
		{
			JetType cachedType = trace.getBindingContext().get(BindingContext.EXPRESSION_TYPE, returnedExpression);
			trace.record(BindingContext.STATEMENT, returnedExpression, false);
			if(cachedType != null)
			{
				typeMap.put(returnedExpression, cachedType);
			}
			else
			{
				typeMap.put(returnedExpression, ErrorUtils.createErrorType("Error function type"));
			}
		}
		return typeMap;
	}

	/*package*/
	@SuppressWarnings("SuspiciousMethodCalls")
	JetTypeInfo getBlockReturnedTypeWithWritableScope(@NotNull WritableScope scope, @NotNull List<? extends NapileElement> block, @NotNull CoercionStrategy coercionStrategyForLastExpression, ExpressionTypingContext context, BindingTrace trace)
	{
		if(block.isEmpty())
		{
			return JetTypeInfo.create(TypeUtils.getTypeOfClassOrErrorType(scope, NapileLangPackage.NULL), context.dataFlowInfo);
		}

		ExpressionTypingInternals blockLevelVisitor = ExpressionTypingVisitorDispatcher.createForBlock(scope, this);
		ExpressionTypingContext newContext = createContext(context, trace, scope, context.dataFlowInfo, TypeUtils.NO_EXPECTED_TYPE);

		JetTypeInfo result = JetTypeInfo.create(null, context.dataFlowInfo);
		for(Iterator<? extends NapileElement> iterator = block.iterator(); iterator.hasNext(); )
		{
			final NapileElement statement = iterator.next();
			if(!(statement instanceof NapileExpression))
			{
				continue;
			}
			trace.record(BindingContext.STATEMENT, statement);
			final NapileExpression statementExpression = (NapileExpression) statement;
			//TODO constructor assert context.expectedType != FORBIDDEN : ""
			if(!iterator.hasNext())
			{
				if(context.expectedType != TypeUtils.NO_EXPECTED_TYPE)
				{
					if(coercionStrategyForLastExpression == CoercionStrategy.COERCION_TO_UNIT && TypeUtils.isEqualFqName(context.expectedType, NapileLangPackage.NULL))
					{
						// This implements coercion to Unit
						TemporaryBindingTrace temporaryTraceExpectingUnit = TemporaryBindingTrace.create(trace);
						final boolean[] mismatch = new boolean[1];
						ObservableBindingTrace errorInterceptingTrace = makeTraceInterceptingTypeMismatch(temporaryTraceExpectingUnit, statementExpression, mismatch);
						newContext = createContext(newContext, errorInterceptingTrace, scope, newContext.dataFlowInfo, context.expectedType);
						result = blockLevelVisitor.getTypeInfo(statementExpression, newContext, true);
						if(mismatch[0])
						{
							TemporaryBindingTrace temporaryTraceNoExpectedType = TemporaryBindingTrace.create(trace);
							mismatch[0] = false;
							ObservableBindingTrace interceptingTrace = makeTraceInterceptingTypeMismatch(temporaryTraceNoExpectedType, statementExpression, mismatch);
							newContext = createContext(newContext, interceptingTrace, scope, newContext.dataFlowInfo, TypeUtils.NO_EXPECTED_TYPE);
							result = blockLevelVisitor.getTypeInfo(statementExpression, newContext, true);
							if(mismatch[0])
							{
								temporaryTraceExpectingUnit.commit();
							}
							else
							{
								temporaryTraceNoExpectedType.commit();
							}
						}
						else
						{
							temporaryTraceExpectingUnit.commit();
						}
					}
					else
					{
						newContext = createContext(newContext, trace, scope, newContext.dataFlowInfo, context.expectedType);
						result = blockLevelVisitor.getTypeInfo(statementExpression, newContext, true);
					}
				}
				else
				{
					result = blockLevelVisitor.getTypeInfo(statementExpression, newContext, true);
					if(coercionStrategyForLastExpression == CoercionStrategy.COERCION_TO_UNIT)
					{
						boolean mightBeUnit = false;
						if(statementExpression instanceof NapileDeclaration)
						{
							mightBeUnit = true;
						}
						if(statementExpression instanceof NapileBinaryExpression)
						{
							NapileBinaryExpression binaryExpression = (NapileBinaryExpression) statementExpression;
							IElementType operationType = binaryExpression.getOperationToken();
							if(operationType == NapileTokens.EQ || OperatorConventions.ASSIGNMENT_OPERATION_COUNTERPARTS.containsKey(operationType))
							{
								mightBeUnit = true;
							}
						}
						if(mightBeUnit)
						{
							// ExpressionTypingVisitorForStatements should return only null or Unit for declarations and assignments
							assert result.getType() == null || TypeUtils.isEqualFqName(result.getType(), NapileLangPackage.NULL);
							result = JetTypeInfo.create(TypeUtils.getTypeOfClassOrErrorType(scope, NapileLangPackage.NULL), newContext.dataFlowInfo);
						}
					}
				}
			}
			else
			{
				result = blockLevelVisitor.getTypeInfo(statementExpression, newContext, true);
			}

			DataFlowInfo newDataFlowInfo = result.getDataFlowInfo();
			if(newDataFlowInfo != context.dataFlowInfo)
			{
				newContext = createContext(newContext, trace, scope, newDataFlowInfo, TypeUtils.NO_EXPECTED_TYPE);
			}
			blockLevelVisitor = ExpressionTypingVisitorDispatcher.createForBlock(scope, this);
		}
		return result;
	}

	private ExpressionTypingContext createContext(ExpressionTypingContext oldContext, BindingTrace trace, WritableScope scope, DataFlowInfo dataFlowInfo, JetType expectedType)
	{
		return ExpressionTypingContext.newContext(this, oldContext.patternsToDataFlowInfo, oldContext.patternsToBoundVariableLists, oldContext.labelResolver, trace, scope, dataFlowInfo, expectedType, oldContext.namespacesAllowed);
	}

	private ObservableBindingTrace makeTraceInterceptingTypeMismatch(final BindingTrace trace, final NapileExpression expressionToWatch, final boolean[] mismatchFound)
	{
		return new ObservableBindingTrace(trace)
		{

			@Override
			public void report(@NotNull Diagnostic diagnostic)
			{
				if(diagnostic.getFactory() == TYPE_MISMATCH && diagnostic.getPsiElement() == expressionToWatch)
				{
					mismatchFound[0] = true;
				}
				if(TYPE_INFERENCE_ERRORS.contains(diagnostic.getFactory()) && PsiTreeUtil.getParentOfType(diagnostic.getPsiElement(), NapileQualifiedExpressionImpl.class, false) == expressionToWatch)
				{
					mismatchFound[0] = true;
				}
				super.report(diagnostic);
			}
		};
	}
}
