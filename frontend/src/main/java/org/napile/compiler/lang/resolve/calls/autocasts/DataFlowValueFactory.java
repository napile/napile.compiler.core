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

package org.napile.compiler.lang.resolve.calls.autocasts;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.lib.NapileLangPackage;
import org.napile.compiler.lang.descriptors.*;
import org.napile.compiler.lang.lexer.NapileNodes;
import org.napile.compiler.lang.psi.NapileConstantExpression;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileParenthesizedExpression;
import org.napile.compiler.lang.psi.NapileQualifiedExpressionImpl;
import org.napile.compiler.lang.psi.NapileSimpleNameExpression;
import org.napile.compiler.lang.psi.NapileThisExpression;
import org.napile.compiler.lang.resolve.BindingTraceKeys;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.scopes.receivers.ThisReceiverDescriptor;
import org.napile.compiler.lang.types.NapileType;
import org.napile.compiler.lang.types.TypeUtils;
import com.intellij.openapi.util.Pair;

/**
 * @author abreslav
 */
public class DataFlowValueFactory
{
	public static final DataFlowValueFactory INSTANCE = new DataFlowValueFactory();

	private DataFlowValueFactory()
	{
	}

	@NotNull
	public DataFlowValue createDataFlowValue(@NotNull NapileExpression expression, @NotNull NapileType type, @NotNull BindingTrace bindingTrace)
	{
		if(expression instanceof NapileConstantExpression)
		{
			NapileConstantExpression constantExpression = (NapileConstantExpression) expression;
			if(constantExpression.getNode().getElementType() == NapileNodes.NULL)
				return new DataFlowValue(new Object(), TypeUtils.getTypeOfClassOrErrorType(type.getMemberScope(), NapileLangPackage.NULL, true), false, Nullability.NULL);
		}
		if(TypeUtils.isEqualFqName(type, NapileLangPackage.NULL))
			return new DataFlowValue(new Object(), TypeUtils.getTypeOfClassOrErrorType(type.getMemberScope(), NapileLangPackage.NULL, true), false, Nullability.NULL);

		Pair<Object, Boolean> result = getIdForStableIdentifier(expression, bindingTrace, false);
		return new DataFlowValue(result.first == null ? expression : result.first, type, result.second, getImmanentNullability(type));
	}

	@NotNull
	public DataFlowValue createDataFlowValue(@NotNull ThisReceiverDescriptor receiver)
	{
		NapileType type = receiver.getType();
		return new DataFlowValue(receiver, type, true, getImmanentNullability(type));
	}

	/*@NotNull
	public DataFlowValue createDataFlowValue(@NotNull VariableDescriptor variableDescriptor)
	{
		JetType type = variableDescriptor.getType();
		return new DataFlowValue(variableDescriptor, type, isStableVariable(variableDescriptor), getImmanentNullability(type));
	}  */

	private Nullability getImmanentNullability(NapileType type)
	{
		return type.isNullable() ? Nullability.UNKNOWN : Nullability.NOT_NULL;
	}

	@NotNull
	private static Pair<Object, Boolean> getIdForStableIdentifier(@NotNull NapileExpression expression, @NotNull BindingTrace bindingTrace, boolean allowNamespaces)
	{
		if(expression instanceof NapileParenthesizedExpression)
		{
			NapileParenthesizedExpression parenthesizedExpression = (NapileParenthesizedExpression) expression;
			NapileExpression innerExpression = parenthesizedExpression.getExpression();
			if(innerExpression == null)
			{
				return Pair.create(null, false);
			}
			return getIdForStableIdentifier(innerExpression, bindingTrace, allowNamespaces);
		}
		else if(expression instanceof NapileQualifiedExpressionImpl)
		{
			NapileQualifiedExpressionImpl qualifiedExpression = (NapileQualifiedExpressionImpl) expression;
			NapileExpression selectorExpression = qualifiedExpression.getSelectorExpression();
			if(selectorExpression == null)
			{
				return Pair.create(null, false);
			}
			final NapileExpression receiverExpression = qualifiedExpression.getReceiverExpression();
			if(receiverExpression == null)
			{
				return Pair.create(null, false);
			}
			Pair<Object, Boolean> receiverId = getIdForStableIdentifier(receiverExpression, bindingTrace, true);
			Pair<Object, Boolean> selectorId = getIdForStableIdentifier(selectorExpression, bindingTrace, allowNamespaces);
			return receiverId.second ? selectorId : Pair.create(receiverId.first, false);
		}
		if(expression instanceof NapileSimpleNameExpression)
		{
			NapileSimpleNameExpression simpleNameExpression = (NapileSimpleNameExpression) expression;
			DeclarationDescriptor declarationDescriptor = bindingTrace.get(BindingTraceKeys.REFERENCE_TARGET, simpleNameExpression);
			if(declarationDescriptor instanceof VariableDescriptor || declarationDescriptor instanceof VariableAccessorDescriptor)
			{
				return Pair.create((Object) declarationDescriptor, isStableVariable(declarationDescriptor));
			}
			if(declarationDescriptor instanceof PackageDescriptor)
			{
				return Pair.create((Object) declarationDescriptor, allowNamespaces);
			}
			if(declarationDescriptor instanceof ClassDescriptor)
			{
				ClassDescriptor classDescriptor = (ClassDescriptor) declarationDescriptor;
				return Pair.create((Object) classDescriptor, false);
			}
		}
		else if(expression instanceof NapileThisExpression)
		{
			NapileThisExpression thisExpression = (NapileThisExpression) expression;
			DeclarationDescriptor declarationDescriptor = bindingTrace.get(BindingTraceKeys.REFERENCE_TARGET, thisExpression.getInstanceReference());
			if(declarationDescriptor instanceof CallableDescriptor)
			{
				return Pair.create(null, true);
			}
			if(declarationDescriptor instanceof ClassDescriptor)
			{
				return Pair.create((Object) ((ClassDescriptor) declarationDescriptor).getImplicitReceiver(), true);
			}
			return Pair.create(null, true);
		}
		return Pair.create(null, false);
	}

	public static boolean isStableVariable(@NotNull DeclarationDescriptor declarationDescriptor)
	{
		if(declarationDescriptor instanceof LocalVariableDescriptor || declarationDescriptor instanceof CallParameterDescriptor)
			return true;

		if(declarationDescriptor instanceof VariableDescriptor && !((VariableDescriptor) declarationDescriptor).isMutable())
			if(((VariableDescriptor) declarationDescriptor).getModality() == Modality.FINAL || ((VariableDescriptor) declarationDescriptor).getVisibility() == Visibility.LOCAL)
				return true;

		if(declarationDescriptor instanceof VariableAccessorDescriptor)
			return ((VariableAccessorDescriptor) declarationDescriptor).isDefault() && isStableVariable(((VariableAccessorDescriptor) declarationDescriptor).getVariable());

		return false;
	}
}
