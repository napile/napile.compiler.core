/*
 * Copyright 2010-2012 napile.org
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

package org.napile.compiler.codegen.processors.codegen.loopGen;

import java.util.Collections;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.AsmConstants;
import org.napile.asm.resolve.name.Name;
import org.napile.asm.tree.members.bytecode.MethodRef;
import org.napile.asm.tree.members.bytecode.adapter.InstructionAdapter;
import org.napile.asm.tree.members.bytecode.adapter.ReservedInstruction;
import org.napile.asm.tree.members.bytecode.impl.JumpIfInstruction;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.asm.tree.members.types.constructors.TypeParameterValueTypeNode;
import org.napile.compiler.CodeTodo;
import org.napile.compiler.codegen.processors.ExpressionGenerator;
import org.napile.compiler.codegen.processors.NodeRefUtil;
import org.napile.compiler.codegen.processors.codegen.TypeConstants;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.psi.NapileForExpression;
import org.napile.compiler.lang.resolve.BindingContext;

/**
 * @author VISTALL
 * @date 21:16/02.10.12
 */
public class ForLoopCodegen extends LoopCodegen<NapileForExpression>
{
	private DeclarationDescriptor loopParameterDescriptor;

	private ReservedInstruction jumpIfSlot;

	public ForLoopCodegen(@NotNull NapileForExpression expression)
	{
		super(expression);
	}

	@Override
	protected void beforeLoop(ExpressionGenerator gen, InstructionAdapter instructions)
	{
		loopParameterDescriptor = gen.bindingTrace.safeGet(BindingContext.DECLARATION_TO_DESCRIPTOR, expression.getLoopParameter());
		int loopParameterIndex = gen.frameMap.enter(loopParameterDescriptor);

		// temp var for iterator ref
		int loopIteratorIndex = gen.frameMap.enterTemp();
		instructions.visitLocalVariable("temp$iterator");
		instructions.visitLocalVariable(loopParameterDescriptor.getName().getName());

		// put Iterator instance to stack
		MethodDescriptor methodDescriptor = gen.bindingTrace.safeGet(BindingContext.LOOP_RANGE_ITERATOR, expression.getLoopRange());
		gen.gen(expression.getLoopRange(), TypeConstants.ITERATOR__ANY__);
		instructions.invokeVirtual(NodeRefUtil.ref(methodDescriptor), false);
		instructions.store(loopIteratorIndex);

		firstPos = instructions.size();

		instructions.load(loopIteratorIndex);
		instructions.invokeVirtual(new MethodRef(CodeTodo.ITERATOR.child(Name.identifier("hasNext")), Collections.<TypeNode>emptyList(), Collections.<TypeNode>emptyList(), AsmConstants.BOOL_TYPE), false);
		instructions.putTrue();
		jumpIfSlot = instructions.reserve();

		instructions.load(loopIteratorIndex);
		instructions.invokeVirtual(new MethodRef(CodeTodo.ITERATOR.child(Name.identifier("next")), Collections.<TypeNode>emptyList(), Collections.<TypeNode>emptyList(), new TypeNode(false, new TypeParameterValueTypeNode(Name.identifier("E")))), false);
		instructions.store(loopParameterIndex);
	}

	@Override
	protected void afterLoop(ExpressionGenerator gen, InstructionAdapter instructions)
	{
		instructions.jump(firstPos);

		instructions.replace(jumpIfSlot, new JumpIfInstruction(instructions.size()));

		gen.frameMap.leaveTemp();
		gen.frameMap.leave(loopParameterDescriptor);
	}
}
