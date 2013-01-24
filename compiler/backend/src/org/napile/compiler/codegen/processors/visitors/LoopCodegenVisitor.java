/*
 * Copyright 2010-2013 napile.org
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

package org.napile.compiler.codegen.processors.visitors;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.codegen.processors.ExpressionCodegen;
import org.napile.compiler.codegen.processors.visitors.loopCodegen.DoWhileLoopCodegen;
import org.napile.compiler.codegen.processors.visitors.loopCodegen.ForLoopCodegen;
import org.napile.compiler.codegen.processors.visitors.loopCodegen.LabelLoopCodegen;
import org.napile.compiler.codegen.processors.visitors.loopCodegen.LoopCodegen;
import org.napile.compiler.codegen.processors.visitors.loopCodegen.WhileLoopCodegen;
import org.napile.compiler.codegen.processors.codegen.stackValue.StackValue;
import org.napile.compiler.lang.psi.NapileBreakExpression;
import org.napile.compiler.lang.psi.NapileContinueExpression;
import org.napile.compiler.lang.psi.NapileDoWhileExpression;
import org.napile.compiler.lang.psi.NapileForExpression;
import org.napile.compiler.lang.psi.NapileLabelExpression;
import org.napile.compiler.lang.psi.NapileLoopExpression;
import org.napile.compiler.lang.psi.NapileSimpleNameExpression;
import org.napile.compiler.lang.psi.NapileWhileExpression;
import com.intellij.openapi.util.Comparing;

/**
 * @author VISTALL
 * @date 10:37/24.01.13
 */
public class LoopCodegenVisitor extends CodegenVisitor
{
	@NotNull
	private final Deque<LoopCodegen<?>> loops = new ArrayDeque<LoopCodegen<?>>();

	public LoopCodegenVisitor(ExpressionCodegen gen)
	{
		super(gen);
	}

	@Override
	public StackValue visitContinueExpression(NapileContinueExpression expression, StackValue data)
	{
		LoopCodegen<?> last = loops.getLast();

		last.addContinue(gen.instructs);

		return StackValue.none();
	}

	@Override
	public StackValue visitBreakExpression(NapileBreakExpression expression, StackValue data)
	{
		LoopCodegen<?> targetLoop = null;
		NapileSimpleNameExpression labelRef = expression.getTargetLabel();
		if(labelRef != null)
		{
			Iterator<LoopCodegen<?>> it = loops.descendingIterator();
			while(it.hasNext())
			{
				LoopCodegen<?> e = it.next();
				if(Comparing.equal(e.getName(), expression.getLabelName()))
				{
					targetLoop = e;
					break;
				}
			}
		}
		else
			targetLoop = loops.getLast();

		assert targetLoop != null;

		targetLoop.addBreak(gen.instructs);

		return StackValue.none();
	}

	@Override
	public StackValue visitForExpression(NapileForExpression expression, StackValue data)
	{
		return loopGen(new ForLoopCodegen(expression));
	}

	@Override
	public StackValue visitWhileExpression(NapileWhileExpression expression, StackValue data)
	{
		return loopGen(new WhileLoopCodegen(expression));
	}

	@Override
	public StackValue visitDoWhileExpression(NapileDoWhileExpression expression, StackValue data)
	{
		return loopGen(new DoWhileLoopCodegen(expression));
	}

	@Override
	public StackValue visitLabelExpression(NapileLabelExpression expression, StackValue data)
	{
		return loopGen(new LabelLoopCodegen(expression));
	}

	private <E extends NapileLoopExpression> StackValue loopGen(LoopCodegen<E> l)
	{
		loops.add(l);

		l.gen(gen);

		loops.getLast();

		return StackValue.none();
	}
}
