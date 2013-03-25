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

import static org.napile.compiler.lang.diagnostics.Errors.LABEL_NAME_CLASH;
import static org.napile.compiler.lang.diagnostics.Errors.UNRESOLVED_REFERENCE;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.psi.NapileBreakExpression;
import org.napile.compiler.lang.psi.NapileElement;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileSimpleNameExpression;
import org.napile.compiler.lang.resolve.BindingTraceKeys;

/**
 * @author abreslav
 */
public class LabelResolver
{

	private final Map<Name, Stack<NapileElement>> labeledElements = new HashMap<Name, Stack<NapileElement>>();

	public LabelResolver()
	{
	}

	public void enterLabeledElement(@NotNull Name labelName, @NotNull NapileExpression labeledExpression)
	{
		Stack<NapileElement> stack = labeledElements.get(labelName);
		if(stack == null)
		{
			stack = new Stack<NapileElement>();
			labeledElements.put(labelName, stack);
		}
		stack.push(labeledExpression);
	}

	public void exitLabeledElement(@NotNull NapileExpression expression)
	{
		// TODO : really suboptimal
		for(Iterator<Map.Entry<Name, Stack<NapileElement>>> mapIter = labeledElements.entrySet().iterator(); mapIter.hasNext(); )
		{
			Map.Entry<Name, Stack<NapileElement>> entry = mapIter.next();
			Stack<NapileElement> stack = entry.getValue();
			for(Iterator<NapileElement> stackIter = stack.iterator(); stackIter.hasNext(); )
			{
				NapileElement recorded = stackIter.next();
				if(recorded == expression)
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
	private NapileElement resolveControlLabel(@NotNull Name labelName, @NotNull NapileSimpleNameExpression labelExpression, boolean reportUnresolved, ExpressionTypingContext context)
	{
		return resolveNamedLabel(labelName, labelExpression, reportUnresolved, context);
	}

	@Nullable
	public NapileElement resolveLabel(NapileBreakExpression expression, ExpressionTypingContext context)
	{
		NapileSimpleNameExpression labelElement = expression.getTargetLabel();
		if(labelElement != null)
		{
			Name labelName = Name.identifier(labelElement.getText());
			return resolveControlLabel(labelName, labelElement, true, context);
		}
		return null;
	}

	private NapileElement resolveNamedLabel(@NotNull Name labelName, @NotNull NapileSimpleNameExpression labelExpression, boolean reportUnresolved, ExpressionTypingContext context)
	{
		Stack<NapileElement> stack = labeledElements.get(labelName);
		if(stack == null || stack.isEmpty())
		{
			if(reportUnresolved)
			{
				context.trace.report(UNRESOLVED_REFERENCE.on(labelExpression, labelExpression.getText()));
			}
			return null;
		}
		else if(stack.size() > 1)
		{
			context.trace.report(LABEL_NAME_CLASH.on(labelExpression));
		}

		NapileElement result = stack.peek();
		context.trace.record(BindingTraceKeys.LABEL_TARGET, labelExpression, result);
		return result;
	}
}
