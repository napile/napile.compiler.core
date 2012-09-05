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

import static org.napile.compiler.lang.diagnostics.Errors.EXPECTED_CONDITION;
import static org.napile.compiler.lang.diagnostics.Errors.INCOMPATIBLE_TYPES;
import static org.napile.compiler.lang.diagnostics.Errors.SENSELESS_NULL_IN_WHEN;
import static org.napile.compiler.lang.diagnostics.Errors.TYPE_MISMATCH_IN_BINDING_PATTERN;
import static org.napile.compiler.lang.diagnostics.Errors.TYPE_MISMATCH_IN_CONDITION;
import static org.napile.compiler.lang.diagnostics.Errors.TYPE_MISMATCH_IN_RANGE;
import static org.napile.compiler.lang.diagnostics.Errors.UNSUPPORTED;
import static org.napile.compiler.lang.types.expressions.ExpressionTypingUtils.newWritableScopeImpl;

import java.util.Collections;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.diagnostics.Errors;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.lang.resolve.calls.autocasts.DataFlowInfo;
import org.napile.compiler.lang.resolve.calls.autocasts.DataFlowValue;
import org.napile.compiler.lang.resolve.calls.autocasts.DataFlowValueFactory;
import org.napile.compiler.lang.resolve.calls.autocasts.Nullability;
import org.napile.compiler.lang.resolve.scopes.WritableScope;
import org.napile.compiler.lang.resolve.scopes.WritableScopeImpl;
import org.napile.compiler.lang.rt.NapileLangPackage;
import org.napile.compiler.lang.types.CommonSupertypes;
import org.napile.compiler.lang.types.ErrorUtils;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.JetTypeInfo;
import org.napile.compiler.lang.types.TypeUtils;
import org.napile.compiler.lang.types.checker.JetTypeChecker;
import com.google.common.collect.Sets;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;

/**
 * @author abreslav
 */
public class PatternMatchingTypingVisitor extends ExpressionTypingVisitor
{
	protected PatternMatchingTypingVisitor(@NotNull ExpressionTypingInternals facade)
	{
		super(facade);
	}

	@Override
	public JetTypeInfo visitIsExpression(NapileIsExpression expression, ExpressionTypingContext contextWithExpectedType)
	{
		ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
		NapileExpression leftHandSide = expression.getLeftHandSide();
		JetType knownType = facade.safeGetTypeInfo(leftHandSide, context.replaceScope(context.scope)).getType();
		NapilePattern pattern = expression.getPattern();
		DataFlowInfo newDataFlowInfo = context.dataFlowInfo;
		if(pattern != null)
		{
			WritableScopeImpl scopeToExtend = newWritableScopeImpl(context, "Scope extended in 'is'");
			DataFlowValue dataFlowValue = DataFlowValueFactory.INSTANCE.createDataFlowValue(leftHandSide, knownType, context.trace.getBindingContext());
			newDataFlowInfo = checkPatternType(pattern, knownType, false, scopeToExtend, context, dataFlowValue).first;
			context.patternsToDataFlowInfo.put(pattern, newDataFlowInfo);
			context.patternsToBoundVariableLists.put(pattern, scopeToExtend.getDeclaredVariables());
		}
		return DataFlowUtils.checkType(TypeUtils.getTypeOfClassOrErrorType(context.scope, NapileLangPackage.BOOL, false), expression, contextWithExpectedType, newDataFlowInfo);
	}

	@Override
	public JetTypeInfo visitWhenExpression(final NapileWhenExpression expression, ExpressionTypingContext context)
	{
		return visitWhenExpression(expression, context, false);
	}

	public JetTypeInfo visitWhenExpression(final NapileWhenExpression expression, ExpressionTypingContext contextWithExpectedType, boolean isStatement)
	{
		ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
		// TODO :change scope according to the bound value in the when header
		final NapileExpression subjectExpression = expression.getSubjectExpression();

		final JetType subjectType = subjectExpression != null ? context.expressionTypingServices.safeGetType(context.scope, subjectExpression, TypeUtils.NO_EXPECTED_TYPE, context.dataFlowInfo, context.trace) : ErrorUtils.createErrorType("Unknown type");
		final DataFlowValue variableDescriptor = subjectExpression != null ? DataFlowValueFactory.INSTANCE.createDataFlowValue(subjectExpression, subjectType, context.trace.getBindingContext()) : new DataFlowValue(new Object(), TypeUtils.getTypeOfClassOrErrorType(context.scope, NapileLangPackage.NULL, true), false, Nullability.NULL);

		// TODO : exhaustive patterns

		Set<JetType> expressionTypes = Sets.newHashSet();
		DataFlowInfo commonDataFlowInfo = null;
		DataFlowInfo elseDataFlowInfo = context.dataFlowInfo;
		for(NapileWhenEntry whenEntry : expression.getEntries())
		{
			NapileWhenCondition[] conditions = whenEntry.getConditions();
			DataFlowInfo newDataFlowInfo;
			WritableScope scopeToExtend;
			if(whenEntry.isElse())
			{
				scopeToExtend = newWritableScopeImpl(context, "Scope extended in when-else entry");
				newDataFlowInfo = elseDataFlowInfo;
			}
			else if(conditions.length == 1)
			{
				scopeToExtend = newWritableScopeImpl(context, "Scope extended in when entry");
				newDataFlowInfo = context.dataFlowInfo;
				NapileWhenCondition condition = conditions[0];
				if(condition != null)
				{
					Pair<DataFlowInfo, DataFlowInfo> infos = checkWhenCondition(subjectExpression, subjectExpression == null, subjectType, condition, scopeToExtend, context, variableDescriptor);
					newDataFlowInfo = infos.first;
					elseDataFlowInfo = elseDataFlowInfo.and(infos.second);
				}
			}
			else
			{
				scopeToExtend = newWritableScopeImpl(context, "pattern matching"); // We don't write to this scope
				newDataFlowInfo = null;
				for(NapileWhenCondition condition : conditions)
				{
					Pair<DataFlowInfo, DataFlowInfo> infos = checkWhenCondition(subjectExpression, subjectExpression == null, subjectType, condition, newWritableScopeImpl(context, ""), context, variableDescriptor);
					if(newDataFlowInfo == null)
					{
						newDataFlowInfo = infos.first;
					}
					else
					{
						newDataFlowInfo = newDataFlowInfo.or(infos.first);
					}
					elseDataFlowInfo = elseDataFlowInfo.and(infos.second);
				}
				if(newDataFlowInfo == null)
				{
					newDataFlowInfo = context.dataFlowInfo;
				}
			}
			NapileExpression bodyExpression = whenEntry.getExpression();
			if(bodyExpression != null)
			{
				ExpressionTypingContext newContext = contextWithExpectedType.replaceScope(scopeToExtend).replaceDataFlowInfo(newDataFlowInfo);
				CoercionStrategy coercionStrategy = isStatement ? CoercionStrategy.COERCION_TO_UNIT : CoercionStrategy.NO_COERCION;
				JetTypeInfo typeInfo = context.expressionTypingServices.getBlockReturnedTypeWithWritableScope(scopeToExtend, Collections.singletonList(bodyExpression), coercionStrategy, newContext, context.trace);
				JetType type = typeInfo.getType();
				if(type != null)
				{
					expressionTypes.add(type);
				}
				if(commonDataFlowInfo == null)
				{
					commonDataFlowInfo = typeInfo.getDataFlowInfo();
				}
				else
				{
					commonDataFlowInfo = commonDataFlowInfo.or(typeInfo.getDataFlowInfo());
				}
			}
		}

		if(commonDataFlowInfo == null)
		{
			commonDataFlowInfo = context.dataFlowInfo;
		}

		if(!expressionTypes.isEmpty())
		{
			return DataFlowUtils.checkImplicitCast(CommonSupertypes.commonSupertype(expressionTypes), expression, contextWithExpectedType, isStatement, commonDataFlowInfo);
		}
		return JetTypeInfo.create(null, commonDataFlowInfo);
	}

	private Pair<DataFlowInfo, DataFlowInfo> checkWhenCondition(@Nullable final NapileExpression subjectExpression, final boolean expectedCondition, final JetType subjectType, NapileWhenCondition condition, final WritableScope scopeToExtend, final ExpressionTypingContext context, final DataFlowValue... subjectVariables)
	{
		final Ref<Pair<DataFlowInfo, DataFlowInfo>> newDataFlowInfo = new Ref<Pair<DataFlowInfo, DataFlowInfo>>(Pair.create(context.dataFlowInfo, context.dataFlowInfo));
		condition.accept(new NapileVisitorVoid()
		{

			@Override
			public void visitWhenConditionInRange(NapileWhenConditionInRange condition)
			{
				NapileExpression rangeExpression = condition.getRangeExpression();
				if(rangeExpression == null)
					return;
				if(expectedCondition)
				{
					context.trace.report(EXPECTED_CONDITION.on(condition));
					facade.getTypeInfo(rangeExpression, context);
					return;
				}
				if(!facade.checkInExpression(condition, condition.getOperationReference(), subjectExpression, rangeExpression, context))
				{
					context.trace.report(TYPE_MISMATCH_IN_RANGE.on(condition));
				}
			}

			@Override
			public void visitWhenConditionIsPattern(NapileWhenConditionIsPattern condition)
			{
				NapilePattern pattern = condition.getPattern();
				if(expectedCondition)
				{
					context.trace.report(EXPECTED_CONDITION.on(condition));
				}
				if(pattern != null)
				{
					Pair<DataFlowInfo, DataFlowInfo> result = checkPatternType(pattern, subjectType, subjectExpression == null, scopeToExtend, context, subjectVariables);
					if(condition.isNegated())
					{
						newDataFlowInfo.set(Pair.create(result.second, result.first));
					}
					else
					{
						newDataFlowInfo.set(result);
					}
				}
			}

			@Override
			public void visitWhenConditionWithExpression(NapileWhenConditionWithExpression condition)
			{
				NapilePattern pattern = condition.getPattern();
				if(pattern != null)
				{
					newDataFlowInfo.set(checkPatternType(pattern, subjectType, subjectExpression == null, scopeToExtend, context, subjectVariables));
				}
			}

			@Override
			public void visitJetElement(NapileElement element)
			{
				context.trace.report(UNSUPPORTED.on(element, getClass().getCanonicalName()));
			}
		});
		return newDataFlowInfo.get();
	}

	private Pair<DataFlowInfo, DataFlowInfo> checkPatternType(@NotNull NapilePattern pattern, @NotNull final JetType subjectType, final boolean conditionExpected, @NotNull final WritableScope scopeToExtend, final ExpressionTypingContext context, @NotNull final DataFlowValue... subjectVariables)
	{
		final Ref<Pair<DataFlowInfo, DataFlowInfo>> result = new Ref<Pair<DataFlowInfo, DataFlowInfo>>(Pair.create(context.dataFlowInfo, context.dataFlowInfo));
		pattern.accept(new NapileVisitorVoid()
		{
			@Override
			public void visitTypePattern(NapileTypePattern typePattern)
			{
				NapileTypeReference typeReference = typePattern.getTypeReference();
				if(typeReference == null)
					return;
				JetType type = context.expressionTypingServices.getTypeResolver().resolveType(context.scope, typeReference, context.trace, true);
				checkTypeCompatibility(type, subjectType, typePattern);
				result.set(Pair.create(context.dataFlowInfo.establishSubtyping(subjectVariables, type), context.dataFlowInfo));
			}

			@Override
			public void visitExpressionPattern(NapileExpressionPattern pattern)
			{
				NapileExpression expression = pattern.getExpression();
				if(expression == null)
					return;
				JetTypeInfo typeInfo = facade.getTypeInfo(expression, context.replaceScope(scopeToExtend));
				JetType type = typeInfo.getType();
				if(type == null)
					return;
				if(conditionExpected)
				{
					JetType booleanType = TypeUtils.getTypeOfClassOrErrorType(context.scope, NapileLangPackage.BOOL, false);
					if(!JetTypeChecker.INSTANCE.equalTypes(booleanType, type))
					{
						context.trace.report(TYPE_MISMATCH_IN_CONDITION.on(pattern, type));
					}
					else
					{
						DataFlowInfo ifInfo = DataFlowUtils.extractDataFlowInfoFromCondition(expression, true, scopeToExtend, context);
						DataFlowInfo elseInfo = DataFlowUtils.extractDataFlowInfoFromCondition(expression, false, null, context);
						result.set(Pair.create(ifInfo, elseInfo));
					}
					return;
				}
				checkTypeCompatibility(type, subjectType, pattern);
				DataFlowValue expressionDataFlowValue = DataFlowValueFactory.INSTANCE.createDataFlowValue(expression, type, context.trace.getBindingContext());
				for(DataFlowValue subjectVariable : subjectVariables)
				{
					result.set(Pair.create(result.get().first.equate(subjectVariable, expressionDataFlowValue), result.get().second.disequate(subjectVariable, expressionDataFlowValue)));
				}
			}

			@Override
			public void visitBindingPattern(NapileBindingPattern pattern)
			{
				NapileProperty variableDeclaration = pattern.getVariableDeclaration();
				NapileTypeReference propertyTypeRef = variableDeclaration.getPropertyTypeRef();
				JetType type = propertyTypeRef == null ? subjectType : context.expressionTypingServices.getTypeResolver().resolveType(context.scope, propertyTypeRef, context.trace, true);
				VariableDescriptor variableDescriptor = context.expressionTypingServices.getDescriptorResolver().resolveLocalVariableDescriptorWithType(context.scope.getContainingDeclaration(), variableDeclaration, type, context.trace);
				scopeToExtend.addVariableDescriptor(variableDescriptor);
				if(propertyTypeRef != null)
				{
					if(!JetTypeChecker.INSTANCE.isSubtypeOf(subjectType, type))
					{
						context.trace.report(TYPE_MISMATCH_IN_BINDING_PATTERN.on(propertyTypeRef, type, subjectType));
					}
				}

				NapileWhenCondition condition = pattern.getCondition();
				if(condition != null)
				{
					int oldLength = subjectVariables.length;
					DataFlowValue[] newSubjectVariables = new DataFlowValue[oldLength + 1];
					System.arraycopy(subjectVariables, 0, newSubjectVariables, 0, oldLength);
					newSubjectVariables[oldLength] = DataFlowValueFactory.INSTANCE.createDataFlowValue(variableDescriptor);
					result.set(checkWhenCondition(null, false, subjectType, condition, scopeToExtend, context, newSubjectVariables));
				}
			}

			/*
						 * (a: SubjectType) is Type
						 */
			private void checkTypeCompatibility(@Nullable JetType type, @NotNull JetType subjectType, @NotNull NapileElement reportErrorOn)
			{
				// TODO : Take auto casts into account?
				if(type == null)
				{
					return;
				}
				if(TypeUtils.isIntersectionEmpty(type, subjectType))
				{
					context.trace.report(INCOMPATIBLE_TYPES.on(reportErrorOn, type, subjectType));
					return;
				}

				// check if the pattern is essentially a 'null' expression
				if(type.isNullable() && TypeUtils.isEqualFqName(type, NapileLangPackage.NULL) && !subjectType.isNullable())
				{
					context.trace.report(SENSELESS_NULL_IN_WHEN.on(reportErrorOn));
				}

				if(BasicExpressionTypingVisitor.isCastErased(subjectType, type, JetTypeChecker.INSTANCE))
				{
					context.trace.report(Errors.CANNOT_CHECK_FOR_ERASED.on(reportErrorOn, type));
				}
			}

			@Override
			public void visitJetElement(NapileElement element)
			{
				context.trace.report(UNSUPPORTED.on(element, getClass().getCanonicalName()));
			}
		});
		return result.get();
	}
}
