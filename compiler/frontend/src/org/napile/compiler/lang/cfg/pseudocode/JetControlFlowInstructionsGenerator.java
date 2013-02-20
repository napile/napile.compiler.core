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

package org.napile.compiler.lang.cfg.pseudocode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.cfg.BlockInfo;
import org.napile.compiler.lang.cfg.BreakableBlockInfo;
import org.napile.compiler.lang.cfg.GenerationTrigger;
import org.napile.compiler.lang.cfg.JetControlFlowBuilder;
import org.napile.compiler.lang.cfg.JetControlFlowBuilderAdapter;
import org.napile.compiler.lang.cfg.Label;
import org.napile.compiler.lang.cfg.LoopInfo;
import org.napile.compiler.lang.psi.NapileAnonymMethod;
import org.napile.compiler.lang.psi.NapileCallParameterAsVariable;
import org.napile.compiler.lang.psi.NapileDeclaration;
import org.napile.compiler.lang.psi.NapileElement;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileThrowExpression;
import org.napile.compiler.lang.psi.NapileVariable;

/**
 * @author abreslav
 */
public class JetControlFlowInstructionsGenerator extends JetControlFlowBuilderAdapter
{

	private final Stack<BreakableBlockInfo> loopInfo = new Stack<BreakableBlockInfo>();
	private final Map<NapileElement, BreakableBlockInfo> elementToBlockInfo = new HashMap<NapileElement, BreakableBlockInfo>();
	private int labelCount = 0;

	private final Stack<JetControlFlowInstructionsGeneratorWorker> builders = new Stack<JetControlFlowInstructionsGeneratorWorker>();

	private final Stack<BlockInfo> allBlocks = new Stack<BlockInfo>();

	private void pushBuilder(NapileElement scopingElement, NapileElement subroutine)
	{
		JetControlFlowInstructionsGeneratorWorker worker = new JetControlFlowInstructionsGeneratorWorker(scopingElement, subroutine);
		builders.push(worker);
		builder = worker;
	}

	private JetControlFlowInstructionsGeneratorWorker popBuilder(@NotNull NapileElement element)
	{
		JetControlFlowInstructionsGeneratorWorker worker = builders.pop();
		if(!builders.isEmpty())
		{
			builder = builders.peek();
		}
		else
		{
			builder = null;
		}
		return worker;
	}

	@Override
	public void enterSubroutine(@NotNull NapileDeclaration subroutine)
	{
		if(builder != null && subroutine instanceof NapileAnonymMethod)
		{
			pushBuilder(subroutine, builder.getReturnSubroutine());
		}
		else
		{
			pushBuilder(subroutine, subroutine);
		}
		assert builder != null;
		builder.enterSubroutine(subroutine);
	}

	@Override
	public Pseudocode exitSubroutine(@NotNull NapileDeclaration subroutine)
	{
		super.exitSubroutine(subroutine);
		JetControlFlowInstructionsGeneratorWorker worker = popBuilder(subroutine);
		if(!builders.empty())
		{
			JetControlFlowInstructionsGeneratorWorker builder = builders.peek();
			LocalDeclarationInstruction instruction = new LocalDeclarationInstruction(subroutine, worker.getPseudocode());
			builder.add(instruction);
		}
		return worker.getPseudocode();
	}

	private class JetControlFlowInstructionsGeneratorWorker implements JetControlFlowBuilder
	{

		private final PseudocodeImpl pseudocode;
		private final Label error;
		private final Label sink;
		private final NapileElement returnSubroutine;

		private JetControlFlowInstructionsGeneratorWorker(@NotNull NapileElement scopingElement, @NotNull NapileElement returnSubroutine)
		{
			this.pseudocode = new PseudocodeImpl(scopingElement);
			this.error = pseudocode.createLabel("error");
			this.sink = pseudocode.createLabel("sink");
			this.returnSubroutine = returnSubroutine;
		}

		public PseudocodeImpl getPseudocode()
		{
			return pseudocode;
		}

		private void add(@NotNull Instruction instruction)
		{
			pseudocode.addInstruction(instruction);
		}

		@NotNull
		@Override
		public final Label createUnboundLabel()
		{
			return pseudocode.createLabel("l" + labelCount++);
		}

		@Override
		public final LoopInfo enterLoop(@NotNull NapileExpression expression, Label loopExitPoint, Label conditionEntryPoint)
		{
			Label label = createUnboundLabel();
			bindLabel(label);
			LoopInfo blockInfo = new LoopInfo(expression, label, loopExitPoint != null ? loopExitPoint : createUnboundLabel(), createUnboundLabel(), conditionEntryPoint != null ? conditionEntryPoint : createUnboundLabel());
			loopInfo.push(blockInfo);
			elementToBlockInfo.put(expression, blockInfo);
			allBlocks.push(blockInfo);
			pseudocode.recordLoopInfo(expression, blockInfo);
			return blockInfo;
		}

		@Override
		public final void exitLoop(@NotNull NapileExpression expression)
		{
			BreakableBlockInfo info = loopInfo.pop();
			elementToBlockInfo.remove(expression);
			allBlocks.pop();
			bindLabel(info.getExitPoint());
		}

		@Override
		public NapileElement getCurrentLoop()
		{
			return loopInfo.empty() ? null : loopInfo.peek().getElement();
		}

		@Override
		public void enterSubroutine(@NotNull NapileDeclaration subroutine)
		{
			Label entryPoint = createUnboundLabel();
			BreakableBlockInfo blockInfo = new BreakableBlockInfo(subroutine, entryPoint, createUnboundLabel());
			//            subroutineInfo.push(blockInfo);
			elementToBlockInfo.put(subroutine, blockInfo);
			allBlocks.push(blockInfo);
			bindLabel(entryPoint);
			add(new SubroutineEnterInstruction(subroutine));
		}

		@NotNull
		@Override
		public NapileElement getCurrentSubroutine()
		{
			return pseudocode.getCorrespondingElement();
		}

		@Override
		public NapileElement getReturnSubroutine()
		{
			return returnSubroutine;// subroutineInfo.empty() ? null : subroutineInfo.peek().getElement();
		}

		@Override
		public Label getEntryPoint(@NotNull NapileElement labelElement)
		{
			return elementToBlockInfo.get(labelElement).getEntryPoint();
		}

		@Override
		public Label getExitPoint(@NotNull NapileElement labelElement)
		{
			BreakableBlockInfo blockInfo = elementToBlockInfo.get(labelElement);
			assert blockInfo != null : labelElement.getText();
			return blockInfo.getExitPoint();
		}

		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		private void handleJumpInsideTryFinally(Label jumpTarget)
		{
			List<TryFinallyBlockInfo> finallyBlocks = new ArrayList<TryFinallyBlockInfo>();

			for(int i = allBlocks.size() - 1; i >= 0; i--)
			{
				BlockInfo blockInfo = allBlocks.get(i);
				if(blockInfo instanceof BreakableBlockInfo)
				{
					BreakableBlockInfo breakableBlockInfo = (BreakableBlockInfo) blockInfo;
					if(jumpTarget == breakableBlockInfo.getExitPoint() || jumpTarget == breakableBlockInfo.getEntryPoint())
					{
						for(int j = finallyBlocks.size() - 1; j >= 0; j--)
						{
							finallyBlocks.get(j).generateFinallyBlock();
						}
						break;
					}
				}
				else if(blockInfo instanceof TryFinallyBlockInfo)
				{
					TryFinallyBlockInfo tryFinallyBlockInfo = (TryFinallyBlockInfo) blockInfo;
					finallyBlocks.add(tryFinallyBlockInfo);
				}
			}
		}

		@Override
		public Pseudocode exitSubroutine(@NotNull NapileDeclaration subroutine)
		{
			bindLabel(getExitPoint(subroutine));
			pseudocode.addExitInstruction(new SubroutineExitInstruction(subroutine, "<END>"));
			bindLabel(error);
			pseudocode.addErrorInstruction(new SubroutineExitInstruction(subroutine, "<ERROR>"));
			bindLabel(sink);
			pseudocode.addSinkInstruction(new SubroutineSinkInstruction(subroutine, "<SINK>"));
			elementToBlockInfo.remove(subroutine);
			allBlocks.pop();
			return null;
		}

		@Override
		public void returnValue(@NotNull NapileExpression returnExpression, @NotNull NapileElement subroutine)
		{
			Label exitPoint = getExitPoint(subroutine);
			handleJumpInsideTryFinally(exitPoint);
			add(new ReturnValueInstruction(returnExpression, exitPoint));
		}

		@Override
		public void returnNoValue(@NotNull NapileElement returnExpression, @NotNull NapileElement subroutine)
		{
			Label exitPoint = getExitPoint(subroutine);
			handleJumpInsideTryFinally(exitPoint);
			add(new ReturnNoValueInstruction(returnExpression, exitPoint));
		}

		@Override
		public void write(@NotNull NapileElement assignment, @NotNull NapileElement lValue)
		{
			add(new WriteValueInstruction(assignment, lValue));
		}

		@Override
		public void declare(@NotNull NapileCallParameterAsVariable parameter)
		{
			add(new VariableDeclarationInstruction(parameter));
		}

		@Override
		public void declare(@NotNull NapileVariable property)
		{
			add(new VariableDeclarationInstruction(property));
		}

		@Override
		public void read(@NotNull NapileElement element)
		{
			add(new ReadValueInstruction(element));
		}

		@Override
		public void readUnit(@NotNull NapileExpression expression)
		{
			add(new ReadUnitValueInstruction(expression));
		}

		@Override
		public void jump(@NotNull Label label)
		{
			handleJumpInsideTryFinally(label);
			add(new UnconditionalJumpInstruction(label));
		}

		@Override
		public void jumpOnFalse(@NotNull Label label)
		{
			handleJumpInsideTryFinally(label);
			add(new ConditionalJumpInstruction(false, label));
		}

		@Override
		public void jumpOnTrue(@NotNull Label label)
		{
			handleJumpInsideTryFinally(label);
			add(new ConditionalJumpInstruction(true, label));
		}

		@Override
		public void bindLabel(@NotNull Label label)
		{
			pseudocode.bindLabel(label);
		}

		@Override
		public void allowDead()
		{
			Label allowedDeadLabel = createUnboundLabel();
			bindLabel(allowedDeadLabel);
			pseudocode.allowDead(allowedDeadLabel);
		}

		@Override
		public void stopAllowDead()
		{
			Label allowedDeadLabel = createUnboundLabel();
			bindLabel(allowedDeadLabel);
			pseudocode.stopAllowDead(allowedDeadLabel);
		}

		@Override
		public void nondeterministicJump(Label label)
		{
			handleJumpInsideTryFinally(label);
			add(new NondeterministicJumpInstruction(label));
		}

		@Override
		public void nondeterministicJump(List<Label> labels)
		{
			//todo
			//handleJumpInsideTryFinally(label);
			add(new NondeterministicJumpInstruction(labels));
		}

		@Override
		public void jumpToError(NapileThrowExpression expression)
		{
			add(new UnconditionalJumpInstruction(error));
		}

		@Override
		public void jumpToError(NapileExpression nothingExpression)
		{
			add(new UnconditionalJumpInstruction(error));
		}

		@Override
		public void enterTryFinally(@NotNull GenerationTrigger generationTrigger)
		{
			allBlocks.push(new TryFinallyBlockInfo(generationTrigger));
		}

		@Override
		public void exitTryFinally()
		{
			BlockInfo pop = allBlocks.pop();
			assert pop instanceof TryFinallyBlockInfo;
		}

		@Override
		public void unsupported(NapileElement element)
		{
			add(new UnsupportedElementInstruction(element));
		}
	}

	public static class TryFinallyBlockInfo extends BlockInfo
	{
		private final GenerationTrigger finallyBlock;

		private TryFinallyBlockInfo(GenerationTrigger finallyBlock)
		{
			this.finallyBlock = finallyBlock;
		}

		public void generateFinallyBlock()
		{
			finallyBlock.generate();
		}
	}
}
