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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.napile.asm.tree.members.CodeInfo;
import org.napile.asm.tree.members.bytecode.adapter.InstructionAdapter;
import org.napile.asm.util.IntIntPair;
import org.napile.compiler.codegen.processors.AsmNodeUtil;
import org.napile.compiler.codegen.processors.ExpressionCodegen;
import org.napile.compiler.codegen.processors.TypeTransformer;
import org.napile.compiler.codegen.processors.codegen.stackValue.StackValue;
import org.napile.compiler.lang.descriptors.CallParameterDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.LocalVariableDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.SimpleMethodDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.psi.NapileAnonymMethodExpression;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileLinkMethodExpression;
import org.napile.compiler.lang.psi.NapileSimpleNameExpression;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import org.napile.compiler.lang.resolve.BindingTraceKeys;
import org.napile.compiler.lang.types.JetType;
import com.intellij.psi.PsiElement;

/**
 * @author VISTALL
 * @since 15:19/21.01.13
 */
public class ClosureCodegenVisitor extends CodegenVisitor
{
	public ClosureCodegenVisitor(ExpressionCodegen gen)
	{
		super(gen);
	}

	@Override
	public StackValue visitLinkMethodExpression(NapileLinkMethodExpression expression, StackValue data)
	{
		JetType jetType = gen.bindingTrace.safeGet(BindingTraceKeys.EXPRESSION_TYPE, expression);

		MethodDescriptor target = (MethodDescriptor) gen.bindingTrace.safeGet(BindingTraceKeys.REFERENCE_TARGET, expression.getTarget());

		InstructionAdapter adapter = new InstructionAdapter();

		int index = 0;
		if(!target.isStatic())
		{
			adapter.localGet(index);
			adapter.visitLocalVariable("l" + index);
			index ++;
		}

		for(CallParameterDescriptor descriptor : target.getValueParameters())
		{
			adapter.localGet(index);
			adapter.visitLocalVariable("l" + index);
			index ++;
		}

		if(target.isStatic())
			adapter.invokeStatic(AsmNodeUtil.ref(target, gen.bindingTrace, gen.classNode), false);
		else
			adapter.invokeVirtual(AsmNodeUtil.ref(target, gen.bindingTrace, gen.classNode), false);

		adapter.returnValues(1);

		gen.marker(expression).putAnonym(Collections.<IntIntPair>emptyList(), new CodeInfo(adapter));

		return StackValue.onStack(TypeTransformer.toAsmType(gen.bindingTrace, jetType, gen.classNode));
	}

	@Override
	public StackValue visitAnonymMethodExpression(NapileAnonymMethodExpression expression, StackValue data)
	{
		final SimpleMethodDescriptor methodDescriptor = gen.bindingTrace.safeGet(BindingTraceKeys.METHOD, expression);

		InstructionAdapter adapter = new InstructionAdapter();

		final Set<VariableDescriptor> vars = new LinkedHashSet<VariableDescriptor>();

		NapileExpression bodyExpression = expression.getAnonymMethod().getBodyExpression();

		assert bodyExpression != null;

		NapileVisitorVoid visitorVoid = new NapileVisitorVoid()
		{
			@Override
			public void visitElement(PsiElement element)
			{
				element.acceptChildren(this);
			}

			@Override
			public void visitSimpleNameExpression(NapileSimpleNameExpression expression)
			{
				DeclarationDescriptor descriptor = gen.bindingTrace.get(BindingTraceKeys.REFERENCE_TARGET, expression);
				if(descriptor instanceof LocalVariableDescriptor || descriptor instanceof CallParameterDescriptor)
				{
					if(descriptor.getContainingDeclaration() != methodDescriptor)
					{
						vars.add(((VariableDescriptor) descriptor));
					}
				}
			}
		};
		bodyExpression.accept(visitorVoid);

		ExpressionCodegen expCodegen = new ExpressionCodegen(gen.bindingTrace, methodDescriptor, gen.classNode,  gen.context.clone(), adapter);
		for(VariableDescriptor v : vars)
		{
			expCodegen.frameMap.enter(v);
			adapter.visitLocalVariable(v.getName().getName());
		}

		expCodegen.returnExpression(bodyExpression, methodDescriptor.isMacro());

		List<IntIntPair> pairs = new ArrayList<IntIntPair>(vars.size());

		for(VariableDescriptor v : vars)
		{
			pairs.add(new IntIntPair(gen.frameMap.getIndex(v), expCodegen.frameMap.getIndex(v)));
		}

		gen.marker(expression).putAnonym(pairs, new CodeInfo(adapter));

		JetType jetType = gen.bindingTrace.safeGet(BindingTraceKeys.EXPRESSION_TYPE, expression);

		return StackValue.onStack(gen.toAsmType(jetType));
	}
}
