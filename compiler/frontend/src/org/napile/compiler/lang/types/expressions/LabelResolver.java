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

import static org.napile.compiler.lang.diagnostics.Errors.AMBIGUOUS_LABEL;
import static org.napile.compiler.lang.diagnostics.Errors.LABEL_NAME_CLASH;
import static org.napile.compiler.lang.diagnostics.Errors.UNRESOLVED_REFERENCE;
import static org.napile.compiler.lang.resolve.BindingContext.LABEL_TARGET;
import static org.napile.compiler.lang.resolve.BindingContext.REFERENCE_TARGET;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.psi.NapilePsiUtil;
import org.napile.compiler.lang.psi.NapileReferenceExpression;
import org.napile.compiler.lang.psi.NapileElement;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileLabelQualifiedExpression;
import org.napile.compiler.lang.psi.NapileSimpleNameExpression;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingContextUtils;
import org.napile.compiler.lang.resolve.name.LabelName;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.lang.psi.NapileFunctionLiteralExpression;
import com.intellij.psi.PsiElement;

/**
 * @author abreslav
 */
public class LabelResolver
{

	private final Map<LabelName, Stack<NapileElement>> labeledElements = new HashMap<LabelName, Stack<NapileElement>>();

	public LabelResolver()
	{
	}

	public void enterLabeledElement(@NotNull LabelName labelName, @NotNull NapileExpression labeledExpression)
	{
		NapileExpression deparenthesized = NapilePsiUtil.deparenthesize(labeledExpression);
		if(deparenthesized != null)
		{
			Stack<NapileElement> stack = labeledElements.get(labelName);
			if(stack == null)
			{
				stack = new Stack<NapileElement>();
				labeledElements.put(labelName, stack);
			}
			stack.push(deparenthesized);
		}
	}

	public void exitLabeledElement(@NotNull NapileExpression expression)
	{
		NapileExpression deparenthesized = NapilePsiUtil.deparenthesize(expression);
		// TODO : really suboptimal
		for(Iterator<Map.Entry<LabelName, Stack<NapileElement>>> mapIter = labeledElements.entrySet().iterator(); mapIter.hasNext(); )
		{
			Map.Entry<LabelName, Stack<NapileElement>> entry = mapIter.next();
			Stack<NapileElement> stack = entry.getValue();
			for(Iterator<NapileElement> stackIter = stack.iterator(); stackIter.hasNext(); )
			{
				NapileElement recorded = stackIter.next();
				if(recorded == deparenthesized)
				{
					stackIter.remove();
				}
			}
			if(stack.isEmpty())
			{
				mapIter.remove();
			}
		}
	}

	@Nullable
	private NapileElement resolveControlLabel(@NotNull LabelName labelName, @NotNull NapileSimpleNameExpression labelExpression, boolean reportUnresolved, ExpressionTypingContext context)
	{
		Collection<DeclarationDescriptor> declarationsByLabel = context.scope.getDeclarationsByLabel(labelName);
		int size = declarationsByLabel.size();

		if(size == 1)
		{
			DeclarationDescriptor declarationDescriptor = declarationsByLabel.iterator().next();
			NapileElement element;
			if(declarationDescriptor instanceof MethodDescriptor || declarationDescriptor instanceof ClassDescriptor)
			{
				element = (NapileElement) BindingContextUtils.descriptorToDeclaration(context.trace.getBindingContext(), declarationDescriptor);
			}
			else
			{
				throw new UnsupportedOperationException(); // TODO
			}
			context.trace.record(LABEL_TARGET, labelExpression, element);
			return element;
		}
		else if(size == 0)
		{
			return resolveNamedLabel(labelName, labelExpression, reportUnresolved, context);
		}
		context.trace.report(AMBIGUOUS_LABEL.on(labelExpression));
		return null;
	}

	@Nullable
	public NapileElement resolveLabel(NapileLabelQualifiedExpression expression, ExpressionTypingContext context)
	{
		NapileSimpleNameExpression labelElement = expression.getTargetLabel();
		if(labelElement != null)
		{
			LabelName labelName = new LabelName(expression.getLabelName());
			return resolveControlLabel(labelName, labelElement, true, context);
		}
		return null;
	}

	private NapileElement resolveNamedLabel(@NotNull LabelName labelName, @NotNull NapileSimpleNameExpression labelExpression, boolean reportUnresolved, ExpressionTypingContext context)
	{
		Stack<NapileElement> stack = labeledElements.get(labelName);
		if(stack == null || stack.isEmpty())
		{
			if(reportUnresolved)
			{
				context.trace.report(UNRESOLVED_REFERENCE.on(labelExpression));
			}
			return null;
		}
		else if(stack.size() > 1)
		{
			context.trace.report(LABEL_NAME_CLASH.on(labelExpression));
		}

		NapileElement result = stack.peek();
		context.trace.record(BindingContext.LABEL_TARGET, labelExpression, result);
		return result;
	}

	public ReceiverDescriptor resolveThisLabel(NapileReferenceExpression thisReference, NapileSimpleNameExpression targetLabel, ExpressionTypingContext context, ReceiverDescriptor thisReceiver, LabelName labelName)
	{
		Collection<DeclarationDescriptor> declarationsByLabel = context.scope.getDeclarationsByLabel(labelName);
		int size = declarationsByLabel.size();
		assert targetLabel != null;
		if(size == 1)
		{
			DeclarationDescriptor declarationDescriptor = declarationsByLabel.iterator().next();
			if(declarationDescriptor instanceof ClassDescriptor)
			{
				ClassDescriptor classDescriptor = (ClassDescriptor) declarationDescriptor;
				thisReceiver = classDescriptor.getImplicitReceiver();
			}
			else if(declarationDescriptor instanceof MethodDescriptor)
			{
				MethodDescriptor methodDescriptor = (MethodDescriptor) declarationDescriptor;
				thisReceiver = methodDescriptor.getReceiverParameter();
			}
			else
			{
				throw new UnsupportedOperationException("Unsupported descriptor: " + declarationDescriptor); // TODO
			}
			PsiElement element = BindingContextUtils.descriptorToDeclaration(context.trace.getBindingContext(), declarationDescriptor);
			assert element != null : "No PSI element for descriptor: " + declarationDescriptor;
			context.trace.record(LABEL_TARGET, targetLabel, element);
			context.trace.record(REFERENCE_TARGET, thisReference, declarationDescriptor);
		}
		else if(size == 0)
		{
			NapileElement element = resolveNamedLabel(labelName, targetLabel, false, context);
			if(element instanceof NapileFunctionLiteralExpression)
			{
				DeclarationDescriptor declarationDescriptor = context.trace.getBindingContext().get(BindingContext.DECLARATION_TO_DESCRIPTOR, element);
				if(declarationDescriptor instanceof MethodDescriptor)
				{
					thisReceiver = ((MethodDescriptor) declarationDescriptor).getReceiverParameter();
					if(thisReceiver.exists())
					{
						context.trace.record(LABEL_TARGET, targetLabel, element);
						context.trace.record(REFERENCE_TARGET, thisReference, declarationDescriptor);
					}
				}
				else
				{
					context.trace.report(UNRESOLVED_REFERENCE.on(targetLabel));
				}
			}
			else
			{
				context.trace.report(UNRESOLVED_REFERENCE.on(targetLabel));
			}
		}
		else
		{
			context.trace.report(AMBIGUOUS_LABEL.on(targetLabel));
		}
		return thisReceiver;
	}
}
