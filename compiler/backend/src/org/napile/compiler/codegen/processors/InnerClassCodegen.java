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

package org.napile.compiler.codegen.processors;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.AsmConstants;
import org.napile.asm.Modifier;
import org.napile.asm.resolve.name.FqName;
import org.napile.asm.resolve.name.Name;
import org.napile.asm.tree.members.ClassNode;
import org.napile.asm.tree.members.MethodNode;
import org.napile.asm.tree.members.MethodParameterNode;
import org.napile.asm.tree.members.VariableNode;
import org.napile.asm.tree.members.bytecode.VariableRef;
import org.napile.asm.tree.members.bytecode.impl.LoadInstruction;
import org.napile.asm.tree.members.bytecode.impl.PutToVariableInstruction;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.compiler.codegen.processors.codegen.stackValue.StackValue;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.psi.NapileAnonymClass;
import org.napile.compiler.lang.psi.NapileAnonymClassExpression;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.resolve.BindingContext;
import com.intellij.psi.PsiElement;

/**
 * @author VISTALL
 * @date 15:59/23.12.12
 */
public class InnerClassCodegen
{
	public static StackValue genAnonym(@NotNull NapileAnonymClassExpression expression, @NotNull ExpressionCodegen gen)
	{
		NapileAnonymClass anonymClass = expression.getAnonymClass();

		FqName fqName = gen.bindingTrace.safeGet(BindingContext2.DECLARATION_TO_FQ_NAME, anonymClass);

		ClassNode anonymClassNode = new ClassNode(Modifier.list(Modifier.FINAL, Modifier.STATIC), fqName);
		gen.classNode.addMember(anonymClassNode);

		List<ClassDescriptor> classLikes = findOuterClasses(anonymClass, gen);
		List<VariableNode> thisVariableNodes = new ArrayList<VariableNode>(classLikes.size());

		MethodNode constructorNode = MethodNode.constructor();
		constructorNode.maxLocals = 1;

		for(ClassDescriptor classDescriptor : classLikes)
		{
			TypeNode typeNode = gen.toAsmType(classDescriptor.getDefaultType());
			Name name = Name.identifier(classDescriptor.getName() + AsmConstants.ANONYM_SPLITTER + "this");

			constructorNode.maxLocals ++;
			// add parameters to constructor
			constructorNode.parameters.add(new MethodParameterNode(Modifier.EMPTY, name, typeNode));

			VariableNode variableNode = new VariableNode(Modifier.EMPTY, name, typeNode);
			anonymClassNode.addMember(variableNode);

			thisVariableNodes.add(variableNode);
		}

		MethodCodegen.genSuperCalls(constructorNode, anonymClass, gen.bindingTrace, anonymClassNode);

		for(ClassDescriptor classDescriptor : classLikes)
		{
			int i = classLikes.indexOf(classDescriptor);

			VariableNode variableNode = thisVariableNodes.get(i);

			constructorNode.instructions.add(new LoadInstruction(0));
			constructorNode.instructions.add(new LoadInstruction(i + 1));
			constructorNode.instructions.add(new PutToVariableInstruction(new VariableRef(fqName.child(variableNode.name), variableNode.returnType)));
		}

		anonymClassNode.addMember(constructorNode);

		gen.instructs.putNull();

		return StackValue.none();
	}

	private static List<ClassDescriptor> findOuterClasses(PsiElement element, ExpressionCodegen gen)
	{
		List<ClassDescriptor> classLikes = new ArrayList<ClassDescriptor>(1);
		PsiElement p = element.getParent();
		while(p != null)
		{
			DeclarationDescriptor descriptor = gen.bindingTrace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, p);
			if(!(descriptor instanceof ClassDescriptor))
			{}
			else if(p instanceof NapileAnonymClass)
				classLikes.add((ClassDescriptor) descriptor);
			else if(p instanceof NapileClass)
			{
				classLikes.add((ClassDescriptor) descriptor);
				if(((ClassDescriptor) descriptor).isStatic())
					break;
			}

			p = p.getParent();
		}
		return classLikes;
	}
}
