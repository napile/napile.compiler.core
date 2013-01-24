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
import java.util.Collections;
import java.util.List;

import org.napile.asm.AsmConstants;
import org.napile.asm.lib.NapileLangPackage;
import org.napile.asm.resolve.name.Name;
import org.napile.asm.tree.members.bytecode.MethodRef;
import org.napile.asm.tree.members.bytecode.adapter.InstructionAdapter;
import org.napile.asm.tree.members.bytecode.adapter.ReservedInstruction;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.compiler.codegen.CompilationException;
import org.napile.compiler.codegen.processors.ExpressionCodegen;
import org.napile.compiler.codegen.processors.TypeTransformer;
import org.napile.compiler.codegen.processors.codegen.stackValue.StackValue;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileIfExpression;
import org.napile.compiler.lang.psi.NapileIsExpression;
import org.napile.compiler.lang.psi.NapileWhenCondition;
import org.napile.compiler.lang.psi.NapileWhenConditionInRange;
import org.napile.compiler.lang.psi.NapileWhenConditionIsPattern;
import org.napile.compiler.lang.psi.NapileWhenConditionWithExpression;
import org.napile.compiler.lang.psi.NapileWhenEntry;
import org.napile.compiler.lang.psi.NapileWhenExpression;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.types.JetType;

/**
 * @author VISTALL
 * @date 10:31/24.01.13
 */
public class ConditionCodegenVisitor extends CodegenVisitor
{
	public ConditionCodegenVisitor(ExpressionCodegen gen)
	{
		super(gen);
	}

	@Override
	public StackValue visitIsExpression(NapileIsExpression expression, StackValue data)
	{
		gen.gen(expression.getLeftHandSide(), TypeTransformer.toAsmType(gen.bindingTrace, gen.bindingTrace.safeGet(BindingContext.EXPRESSION_TYPE, expression.getLeftHandSide()), gen.classNode));

		JetType rightType = gen.bindingTrace.safeGet(BindingContext.TYPE, expression.getTypeRef());

		gen.instructs.is(TypeTransformer.toAsmType(gen.bindingTrace, rightType, gen.classNode));

		if(expression.isNegated())
			gen.instructs.invokeVirtual(new MethodRef(NapileLangPackage.BOOL.child(Name.identifier("not")), Collections.<TypeNode>emptyList(), Collections.<TypeNode>emptyList(), AsmConstants.BOOL_TYPE), false);

		return StackValue.onStack(AsmConstants.BOOL_TYPE);
	}

	@Override
	public StackValue visitIfExpression(NapileIfExpression expression, StackValue receiver)
	{
		TypeNode asmType = gen.expressionType(expression);

		NapileExpression thenExpression = expression.getThen();
		NapileExpression elseExpression = expression.getElse();

		if(thenExpression == null && elseExpression == null)
			throw new CompilationException("Both brunches of if/else are null", null, expression);

		if(ExpressionCodegen.isEmptyExpression(thenExpression))
		{
			if(ExpressionCodegen.isEmptyExpression(elseExpression))
			{
				if(!asmType.equals(AsmConstants.NULL_TYPE))
					throw new CompilationException("Completely empty 'if' is expected to have Null type", null, expression);

				gen.instructs.putNull();
				return StackValue.onStack(asmType);
			}
			StackValue condition = gen.gen(expression.getCondition());
			return generateSingleBranchIf(condition, elseExpression, false);
		}
		else
		{
			if(ExpressionCodegen.isEmptyExpression(elseExpression))
			{
				StackValue condition = gen.gen(expression.getCondition());
				return generateSingleBranchIf(condition, thenExpression, true);
			}
		}


		StackValue condition = gen.gen(expression.getCondition());

		condition.put(AsmConstants.BOOL_TYPE, gen.instructs);

		gen.instructs.putTrue();

		ReservedInstruction ifSlot = gen.instructs.reserve();

		gen.gen(thenExpression, asmType);

		ReservedInstruction afterIfSlot = gen.instructs.reserve();

		int elseStartIndex = gen.instructs.size();

		gen.gen(elseExpression, asmType);

		int afterIfStartIndex = gen.instructs.size();

		// replace ifSlot - by jump_if - index is start 'else' block
		gen.instructs.replace(ifSlot).jumpIf(elseStartIndex);
		// at end of 'then' block ignore 'else' block
		gen.instructs.replace(afterIfSlot).jump(afterIfStartIndex);

		return StackValue.onStack(asmType);
	}

	@Override
	public StackValue visitWhenExpression(NapileWhenExpression expression, StackValue data)
	{
		InstructionAdapter instructs = gen.instructs;
		JetType expType = gen.bindingTrace.safeGet(BindingContext.EXPRESSION_TYPE, expression);

		NapileExpression subjectExpression = expression.getSubjectExpression();
		if(subjectExpression != null)
			gen.gen(subjectExpression, TypeTransformer.toAsmType(gen.bindingTrace, gen.bindingTrace.safeGet(BindingContext.EXPRESSION_TYPE, subjectExpression), gen.classNode));

		List<NapileWhenEntry> whenEntries = expression.getEntries();
		List<ReservedInstruction> jumpToBlocks = new ArrayList<ReservedInstruction>(whenEntries.size());
		List<ReservedInstruction> jumpOut = new ArrayList<ReservedInstruction>(whenEntries.size());

		for(NapileWhenEntry whenEntry : expression.getEntries())
		{
			if(whenEntry.isElse())
			{
				jumpToBlocks.add(instructs.reserve());
			}
			else
			{
				NapileWhenCondition condition = whenEntry.getCondition();
				if(condition instanceof NapileWhenConditionIsPattern)
				{
					instructs.dup();

					TypeNode typeNode = TypeTransformer.toAsmType(gen.bindingTrace, gen.bindingTrace.safeGet(BindingContext.TYPE, ((NapileWhenConditionIsPattern) condition).getTypeRef()), gen.classNode);

					instructs.is(typeNode);

					if(((NapileWhenConditionIsPattern) condition).isNegated())
						instructs.putFalse();
					else
						instructs.putTrue();

					instructs.jumpIf(instructs.size() + 2);

					jumpToBlocks.add(instructs.reserve());
				}
				else if(condition instanceof NapileWhenConditionWithExpression)
				{
					NapileExpression condExp = ((NapileWhenConditionWithExpression) condition).getExpression();

					if(subjectExpression != null)
						instructs.dup();

					gen.gen(condExp, TypeTransformer.toAsmType(gen.bindingTrace, gen.bindingTrace.safeGet(BindingContext.EXPRESSION_TYPE, condExp), gen.classNode));

					if(subjectExpression != null)
						instructs.invokeVirtual(BinaryCodegenVisitor.ANY_EQUALS, false);

					instructs.putTrue();

					instructs.jumpIf(instructs.size() + 2);

					jumpToBlocks.add(instructs.reserve());
				}
				else if(condition instanceof NapileWhenConditionInRange)
				{
					throw new UnsupportedOperationException("'in' is not supported for now");
				}
			}
		}

		if(whenEntries.size() != jumpToBlocks.size())
			throw new IllegalArgumentException();

		for(int i = 0; i < jumpToBlocks.size(); i++)
		{
			ReservedInstruction reservedInstruction = jumpToBlocks.get(i);
			NapileWhenEntry whenEntry = whenEntries.get(i);

			NapileExpression whenExp = whenEntry.getExpression();

			instructs.replace(reservedInstruction).jump(instructs.size());

			gen.gen(whenExp, TypeTransformer.toAsmType(gen.bindingTrace, gen.bindingTrace.safeGet(BindingContext.EXPRESSION_TYPE, whenExp), gen.classNode));

			jumpOut.add(instructs.reserve());
		}

		for(ReservedInstruction instruction : jumpOut)
			instructs.replace(instruction).jump(instructs.size());

		return StackValue.onStack(TypeTransformer.toAsmType(gen.bindingTrace, expType, gen.classNode));
	}


	private StackValue generateSingleBranchIf(StackValue condition, NapileExpression expression, boolean inverse)
	{
		TypeNode expressionType = gen.expressionType(expression);
		TypeNode targetType = expressionType;
		if(!expressionType.equals(AsmConstants.NULL_TYPE))
			targetType = AsmConstants.ANY_TYPE;

		condition.put(AsmConstants.BOOL_TYPE, gen.instructs);

		if(inverse)
			gen.instructs.putTrue();
		else
			gen.instructs.putFalse();

		ReservedInstruction ifSlot = gen.instructs.reserve();

		gen.gen(expression, expressionType);

		StackValue.castTo(expressionType, targetType, gen.instructs);

		gen.instructs.replace(ifSlot).jumpIf(gen.instructs.size());

		return StackValue.onStack(targetType);
	}
}
