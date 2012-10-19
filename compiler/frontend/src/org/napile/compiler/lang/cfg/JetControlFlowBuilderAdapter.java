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

package org.napile.compiler.lang.cfg;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.cfg.pseudocode.Pseudocode;
import org.napile.compiler.lang.psi.NapileDeclaration;
import org.napile.compiler.lang.psi.NapileElement;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapilePropertyParameter;
import org.napile.compiler.lang.psi.NapileThrowExpression;
import org.napile.compiler.lang.psi.NapileVariable;

/**
 * @author abreslav
 */
public class JetControlFlowBuilderAdapter implements JetControlFlowBuilder
{
	protected
	@Nullable
	JetControlFlowBuilder builder;

	@Override
	public void read(@NotNull NapileElement element)
	{
		assert builder != null;
		builder.read(element);
	}

	@Override
	public void readUnit(@NotNull NapileExpression expression)
	{
		assert builder != null;
		builder.readUnit(expression);
	}

	@Override
	@NotNull
	public Label createUnboundLabel()
	{
		assert builder != null;
		return builder.createUnboundLabel();
	}

	@Override
	public void bindLabel(@NotNull Label label)
	{
		assert builder != null;
		builder.bindLabel(label);
	}

	@Override
	public void allowDead()
	{
		assert builder != null;
		builder.allowDead();
	}

	@Override
	public void stopAllowDead()
	{
		assert builder != null;
		builder.stopAllowDead();
	}

	@Override
	public void jump(@NotNull Label label)
	{
		assert builder != null;
		builder.jump(label);
	}

	@Override
	public void jumpOnFalse(@NotNull Label label)
	{
		assert builder != null;
		builder.jumpOnFalse(label);
	}

	@Override
	public void jumpOnTrue(@NotNull Label label)
	{
		assert builder != null;
		builder.jumpOnTrue(label);
	}

	@Override
	public void nondeterministicJump(Label label)
	{
		assert builder != null;
		builder.nondeterministicJump(label);
	}

	@Override
	public void nondeterministicJump(List<Label> labels)
	{
		assert builder != null;
		builder.nondeterministicJump(labels);
	}

	@Override
	public void jumpToError(NapileThrowExpression expression)
	{
		assert builder != null;
		builder.jumpToError(expression);
	}

	@Override
	public void jumpToError(NapileExpression nothingExpression)
	{
		assert builder != null;
		builder.jumpToError(nothingExpression);
	}

	@Override
	public Label getEntryPoint(@NotNull NapileElement labelElement)
	{
		assert builder != null;
		return builder.getEntryPoint(labelElement);
	}

	@Override
	public Label getExitPoint(@NotNull NapileElement labelElement)
	{
		assert builder != null;
		return builder.getExitPoint(labelElement);
	}

	@Override
	public LoopInfo enterLoop(@NotNull NapileExpression expression, Label loopExitPoint, Label conditionEntryPoint)
	{
		assert builder != null;
		return builder.enterLoop(expression, loopExitPoint, conditionEntryPoint);
	}

	@Override
	public void exitLoop(@NotNull NapileExpression expression)
	{
		assert builder != null;
		builder.exitLoop(expression);
	}

	@Override
	@Nullable
	public NapileElement getCurrentLoop()
	{
		assert builder != null;
		return builder.getCurrentLoop();
	}

	@Override
	public void enterTryFinally(@NotNull GenerationTrigger trigger)
	{
		assert builder != null;
		builder.enterTryFinally(trigger);
	}

	@Override
	public void exitTryFinally()
	{
		assert builder != null;
		builder.exitTryFinally();
	}

	@Override
	public void enterSubroutine(@NotNull NapileDeclaration subroutine)
	{
		assert builder != null;
		builder.enterSubroutine(subroutine);
	}

	@Override
	public Pseudocode exitSubroutine(@NotNull NapileDeclaration subroutine)
	{
		assert builder != null;
		return builder.exitSubroutine(subroutine);
	}

	@NotNull
	@Override
	public NapileElement getCurrentSubroutine()
	{
		assert builder != null;
		return builder.getCurrentSubroutine();
	}

	@Override
	@Nullable
	public NapileElement getReturnSubroutine()
	{
		assert builder != null;
		return builder.getReturnSubroutine();
	}

	@Override
	public void returnValue(@NotNull NapileExpression returnExpression, @NotNull NapileElement subroutine)
	{
		assert builder != null;
		builder.returnValue(returnExpression, subroutine);
	}

	@Override
	public void returnNoValue(@NotNull NapileElement returnExpression, @NotNull NapileElement subroutine)
	{
		assert builder != null;
		builder.returnNoValue(returnExpression, subroutine);
	}

	@Override
	public void unsupported(NapileElement element)
	{
		assert builder != null;
		builder.unsupported(element);
	}

	@Override
	public void write(@NotNull NapileElement assignment, @NotNull NapileElement lValue)
	{
		assert builder != null;
		builder.write(assignment, lValue);
	}

	@Override
	public void declare(@NotNull NapilePropertyParameter parameter)
	{
		assert builder != null;
		builder.declare(parameter);
	}

	@Override
	public void declare(@NotNull NapileVariable property)
	{
		assert builder != null;
		builder.declare(property);
	}
}
