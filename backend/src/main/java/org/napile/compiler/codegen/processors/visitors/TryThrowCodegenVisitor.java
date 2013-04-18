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

import java.util.ArrayList;
import java.util.List;

import org.napile.asm.tree.members.bytecode.adapter.ReservedInstruction;
import org.napile.asm.tree.members.bytecode.tryCatch.CatchBlock;
import org.napile.asm.tree.members.bytecode.tryCatch.TryBlock;
import org.napile.asm.tree.members.bytecode.tryCatch.TryCatchBlockNode;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.compiler.codegen.processors.ExpressionCodegen;
import org.napile.compiler.codegen.processors.TypeTransformer;
import org.napile.compiler.codegen.processors.codegen.TypeConstants;
import org.napile.compiler.codegen.processors.codegen.stackValue.StackValue;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.psi.NapileCatchClause;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileThrowExpression;
import org.napile.compiler.lang.psi.NapileTryExpression;
import org.napile.compiler.lang.resolve.BindingTraceKeys;
import org.napile.compiler.lang.types.NapileType;

/**
 * @author VISTALL
 * @since 10:46/24.01.13
 */
public class TryThrowCodegenVisitor extends CodegenVisitor
{
	public TryThrowCodegenVisitor(ExpressionCodegen gen)
	{
		super(gen);
	}

	@Override
	public StackValue visitTryExpression(NapileTryExpression expression, StackValue data)
	{
		NapileType napileType = gen.bindingTrace.safeGet(BindingTraceKeys.EXPRESSION_TYPE, expression);

		TypeNode expectedAsmType = TypeTransformer.toAsmType(gen.bindingTrace, napileType, gen.classNode);

		final int tryStartIndex = gen.instructs.size();

		gen.gen(expression.getTryBlock());

		List<ReservedInstruction> jumpOutInstructions = new ArrayList<ReservedInstruction>(2);
		jumpOutInstructions.add(gen.instructs.reserve());

		TryBlock tryBlock = new TryBlock(tryStartIndex, gen.instructs.size());
		List<CatchBlock> catchBlocks = new ArrayList<CatchBlock>(2);

		for(NapileCatchClause catchClause : expression.getCatchClauses())
		{
			VariableDescriptor catchParameter = (VariableDescriptor) gen.bindingTrace.safeGet(BindingTraceKeys.DECLARATION_TO_DESCRIPTOR, catchClause.getCatchParameter());

			int index = gen.frameMap.enter(catchParameter);

			gen.instructs.visitLocalVariable(catchClause.getName());

			int startCatchIndex = gen.instructs.size();

			gen.gen(catchClause.getCatchBody());

			jumpOutInstructions.add(gen.instructs.reserve());

			catchBlocks.add(new CatchBlock(startCatchIndex, gen.instructs.size(), index, TypeTransformer.toAsmType(gen.bindingTrace, catchParameter.getType(), gen.classNode)));

			gen.frameMap.leave(catchParameter);
		}

		final int nextIndex = gen.instructs.size();
		for(ReservedInstruction r : jumpOutInstructions)
			gen.instructs.replace(r).jump(nextIndex);

		gen.instructs.tryCatch(new TryCatchBlockNode(tryBlock, catchBlocks));

		return StackValue.onStack(expectedAsmType);
	}

	@Override
	public StackValue visitThrowExpression(NapileThrowExpression expression, StackValue data)
	{
		NapileExpression throwExp = expression.getThrownExpression();

		assert throwExp != null;

		gen.gen(throwExp, TypeConstants.EXCEPTION);

		gen.instructs.throwVal();

		return StackValue.onStack(TypeConstants.EXCEPTION);
	}
}
