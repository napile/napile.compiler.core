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
import org.napile.compiler.lang.psi.NapileCallParameterAsVariable;
import org.napile.compiler.lang.psi.NapileDeclaration;
import org.napile.compiler.lang.psi.NapileElement;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileThrowExpression;
import org.napile.compiler.lang.psi.NapileVariable;

/**
 * @author abreslav
 */
public interface NapileControlFlowBuilder
{
	void read(@NotNull NapileElement element);

	void readUnit(@NotNull NapileExpression expression);

	// General label management
	@NotNull
	Label createUnboundLabel();

	void bindLabel(@NotNull Label label);

	void allowDead();

	void stopAllowDead();

	// Jumps
	void jump(@NotNull Label label);

	void jumpOnFalse(@NotNull Label label);

	void jumpOnTrue(@NotNull Label label);

	void nondeterministicJump(Label label); // Maybe, jump to label

	void nondeterministicJump(List<Label> label);

	void jumpToError(NapileThrowExpression expression);

	void jumpToError(NapileExpression nothingExpression);

	// Entry/exit points
	Label getEntryPoint(@NotNull NapileElement labelElement);

	Label getExitPoint(@NotNull NapileElement labelElement);

	// Loops
	LoopInfo enterLoop(@NotNull NapileExpression expression, @Nullable Label loopExitPoint, @Nullable Label conditionEntryPoint);

	void exitLoop(@NotNull NapileExpression expression);

	@Nullable
	NapileElement getCurrentLoop();

	// Finally
	void enterTryFinally(@NotNull GenerationTrigger trigger);

	void exitTryFinally();

	// Subroutines
	void enterSubroutine(@NotNull NapileDeclaration subroutine);

	Pseudocode exitSubroutine(@NotNull NapileDeclaration subroutine);

	@NotNull
	NapileElement getCurrentSubroutine();

	@Nullable
	NapileElement getReturnSubroutine();

	void returnValue(@NotNull NapileExpression returnExpression, @NotNull NapileElement subroutine);

	void returnNoValue(@NotNull NapileElement returnExpression, @NotNull NapileElement subroutine);

	void write(@NotNull NapileElement assignment, @NotNull NapileElement lValue);

	void declare(@NotNull NapileCallParameterAsVariable parameter);

	void declare(@NotNull NapileVariable property);

	// Other
	void unsupported(NapileElement element);
}
