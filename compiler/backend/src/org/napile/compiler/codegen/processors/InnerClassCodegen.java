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
import java.util.Collections;
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
import org.napile.asm.tree.members.bytecode.adapter.InstructionAdapter;
import org.napile.asm.tree.members.bytecode.impl.LoadInstruction;
import org.napile.asm.tree.members.bytecode.impl.PutToVariableInstruction;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.asm.tree.members.types.constructors.ClassTypeNode;
import org.napile.compiler.codegen.processors.codegen.stackValue.Outer;
import org.napile.compiler.codegen.processors.codegen.stackValue.StackValue;
import org.napile.compiler.lang.descriptors.CallableMemberDescriptor;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.Modality;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptorImpl;
import org.napile.compiler.lang.descriptors.Visibility;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.psi.NapileAnonymClass;
import org.napile.compiler.lang.psi.NapileAnonymClassExpression;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
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

		ClassCodegen classCodegen = new ClassCodegen(gen.bindingTrace);

		FqName fqName = gen.bindingTrace.safeGet(BindingContext2.DECLARATION_TO_FQ_NAME, anonymClass);
		ClassDescriptor classDescriptor = gen.bindingTrace.safeGet(BindingContext.CLASS, anonymClass);
		TypeNode anonymClassType = new TypeNode(false, new ClassTypeNode(fqName));

		List<ClassDescriptor> outerClasses = findOuterClasses(anonymClass, gen);

		List<VariableNode> thisVariableNodes = new ArrayList<VariableNode>(outerClasses.size());

		ExpressionCodegenContext codegenContext = gen.context.clone();
		for(ClassDescriptor outer : outerClasses)
		{
			TypeNode typeNode = gen.toAsmType(outer.getDefaultType());
			FqName outerFq = FqNameGenerator.getFqName(outer, gen.bindingTrace);

			Name name = Name.identifier(outerFq.shortName() + AsmConstants.ANONYM_SPLITTER + "this");

			VariableNode variableNode = new VariableNode(Modifier.EMPTY, name, typeNode);
			thisVariableNodes.add(variableNode);

			codegenContext.wrappedOuterClasses.put(outer, new Outer(anonymClassType, fqName, variableNode));
		}

		ClassNode anonymClassNode = classCodegen.gen(expression.getAnonymClass(), codegenContext);

		InstructionAdapter adapter = classCodegen.getConstructorsAdapters().get(Boolean.FALSE);

		// make constructor
		MethodNode constructorNode = MethodNode.constructor(Modifier.EMPTY);
		constructorNode.maxLocals = 1 + adapter.getMaxLocals();

		// gen super calls
		MethodCodegen.genSuperCalls(constructorNode, anonymClass, gen.bindingTrace, anonymClassNode);
		constructorNode.instructions.addAll(adapter.getInstructions());

		for(ClassDescriptor owner : outerClasses)
		{
			int i = outerClasses.indexOf(owner);

			VariableNode variableNode = thisVariableNodes.get(i);
			anonymClassNode.addMember(variableNode);

			// add parameter to constructor
			constructorNode.maxLocals ++;
			constructorNode.parameters.add(new MethodParameterNode(Modifier.EMPTY, variableNode.name, variableNode.returnType));

			// create getter & setter
			VariableDescriptorImpl varDesc = new VariableDescriptorImpl(classDescriptor, Collections.<AnnotationDescriptor>emptyList(), Modality.FINAL, Visibility.PUBLIC, variableNode.name, CallableMemberDescriptor.Kind.DECLARATION, false, false);
			varDesc.setType(classDescriptor.getDefaultType(), Collections.<TypeParameterDescriptor>emptyList(), ReceiverDescriptor.NO_RECEIVER);

			VariableCodegen.getSetterAndGetter(varDesc, null, anonymClassNode, gen.bindingTrace, false);

			// put data from parameters to variables
			constructorNode.instructions.add(new LoadInstruction(0));
			constructorNode.instructions.add(new LoadInstruction(i + 1));
			constructorNode.instructions.add(new PutToVariableInstruction(new VariableRef(fqName.child(variableNode.name), variableNode.returnType)));
		}

		anonymClassNode.addMember(constructorNode);

		for(ClassDescriptor owner : outerClasses)
		{
			StackValue stackValue = gen.context.wrappedOuterClasses.get(owner);
			if(stackValue == null)
				gen.instructs.load(0);
			else
				stackValue.put(AsmConstants.ANY_TYPE, gen.instructs);
		}

		List<TypeNode> parameters = new ArrayList<TypeNode>(constructorNode.parameters.size());
		for(MethodParameterNode p : constructorNode.parameters)
			parameters.add(p.returnType);
		gen.instructs.newObject(anonymClassType, parameters);

		gen.classNode.addMember(anonymClassNode);

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
