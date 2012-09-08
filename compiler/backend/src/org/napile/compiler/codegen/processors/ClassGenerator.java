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

import java.util.Map;

import org.napile.asm.tree.members.ClassNode;
import org.napile.asm.tree.members.ConstructorNode;
import org.napile.asm.tree.members.MethodNode;
import org.napile.asm.tree.members.Node;
import org.napile.asm.tree.members.VariableNode;
import org.napile.asm.tree.members.bytecode.impl.GetStaticVariableInstruction;
import org.napile.asm.tree.members.bytecode.impl.GetVariableInstruction;
import org.napile.asm.tree.members.bytecode.impl.LoadInstruction;
import org.napile.asm.tree.members.bytecode.impl.PutToStaticVariableInstruction;
import org.napile.asm.tree.members.bytecode.impl.PutToVariableInstruction;
import org.napile.asm.tree.members.bytecode.impl.ReturnInstruction;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.PropertyDescriptor;
import org.napile.compiler.lang.descriptors.SimpleMethodDescriptor;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.name.FqName;

/**
 * @author VISTALL
 * @date 10:58/04.09.12
 */
public class ClassGenerator extends NapileTreeVisitor<Node>
{
	private final Map<FqName, ClassNode> classNodes;
	private final BindingTrace bindingTrace;

	public ClassGenerator(BindingTrace bindingTrace, Map<FqName, ClassNode> classNodes)
	{
		this.bindingTrace = bindingTrace;
		this.classNodes = classNodes;
	}

	@Override
	public Void visitJetElement(NapileElement element, Node data)
	{
		element.acceptChildren(this, data);
		return null;
	}

	@Override
	public Void visitClass(NapileClass klass, Node parent)
	{
		ClassDescriptor classDescriptor = (ClassDescriptor) bindingTrace.safeGet(BindingContext.DECLARATION_TO_DESCRIPTOR, klass);

		FqName fqName = bindingTrace.get(BindingContext2.DECLARATION_TO_FQ_NAME, klass);

		assert fqName != null;

		ClassNode classNode = new ClassNode(ModifierGenerator.gen(classDescriptor), fqName);

		classNodes.put(fqName, classNode);

		return super.visitClass(klass, classNode);
	}

	@Override
	public Void visitAnonymClass(NapileAnonymClass element, Node data)
	{
		ClassDescriptor classDescriptor = (ClassDescriptor) bindingTrace.safeGet(BindingContext.DECLARATION_TO_DESCRIPTOR, element);

		FqName fqName = bindingTrace.get(BindingContext2.DECLARATION_TO_FQ_NAME, element);

		assert fqName != null;

		ClassNode classNode = new ClassNode(ModifierGenerator.gen(classDescriptor), fqName);

		classNodes.put(fqName, classNode);

		return null; //return super.visitAnonymClass(element, classNode);
	}

	@Override
	public Void visitConstructor(NapileConstructor constructor, Node parent)
	{
		assert parent instanceof ClassNode;

		ConstructorDescriptor methodDescriptor = (ConstructorDescriptor) bindingTrace.safeGet(BindingContext.DECLARATION_TO_DESCRIPTOR, constructor);

		ClassNode classNode = (ClassNode) parent;

		ConstructorNode constructorNode = MethodGenerator.gen(methodDescriptor);

		classNode.members.add(constructorNode);

		NapileClass napileClass = (NapileClass) constructor.getParent().getParent();

		int i = 0;

		//TODO [VISTALL] call supers

		/*PropertyInitializerGenerator gen = new PropertyInitializerGenerator(bindingTrace);
		for(NapileDeclaration decl : napileClass.getDeclarations())
			decl.accept(gen, null);

		constructorNode.instructions.addAll(gen.getInstructions());

		NapileExpression expression = constructor.getBodyExpression();
		if(expression != null)
		{
			ExpressionGenerator expressionGenerator = new ExpressionGenerator(bindingTrace, methodDescriptor.isStatic());

			expression.accept(expressionGenerator);

			constructorNode.instructions.addAll(expressionGenerator.getInstructions());

			i += expressionGenerator.localsSize();
		}    */

		constructorNode.visitMaxs(i, i);

		return null;
	}

	@Override
	public Void visitNamedFunction(NapileNamedFunction function, Node parent)
	{
		assert parent instanceof ClassNode;

		SimpleMethodDescriptor methodDescriptor = (SimpleMethodDescriptor) bindingTrace.safeGet(BindingContext.DECLARATION_TO_DESCRIPTOR, function);

		ClassNode classNode = (ClassNode) parent;

		MethodNode methodNode = MethodGenerator.gen(methodDescriptor);

		classNode.members.add(methodNode);

		NapileExpression expression = function.getBodyExpression();
		if(expression != null)
		{
			ExpressionGenerator expressionGenerator = new ExpressionGenerator(bindingTrace, methodDescriptor.isStatic());

			expression.accept(expressionGenerator);

		//	methodNode.instructions.addAll(expressionGenerator.getInstructions());
		//	methodNode.visitMaxs(expressionGenerator.localsSize(), expressionGenerator.localsSize());
		}

		return null; //return super.visitNamedFunction(function, parent);
	}

	@Override
	public Void visitProperty(NapileProperty property, Node parent)
	{
		assert parent instanceof ClassNode;

		PropertyDescriptor propertyDescriptor = (PropertyDescriptor) bindingTrace.safeGet(BindingContext.DECLARATION_TO_DESCRIPTOR, property);

		ClassNode classNode = (ClassNode) parent;

		VariableNode variableNode = new VariableNode(ModifierGenerator.gen(propertyDescriptor), propertyDescriptor.getName().getName());
		variableNode.returnType = TypeGenerator.toAsmType(propertyDescriptor.getType());
		classNode.members.add(variableNode);

		assert propertyDescriptor.getGetter() != null;
		assert propertyDescriptor.getSetter() != null;

		MethodNode setterNode = MethodGenerator.gen(propertyDescriptor.getSetter());
		if(propertyDescriptor.getSetter().isDefault())
		{
			if(propertyDescriptor.isStatic())
			{
				setterNode.instructions.add(new LoadInstruction(0)); // push method parameter
				setterNode.instructions.add(new PutToStaticVariableInstruction(NodeRefUtil.ref(classNode, variableNode))); // pop this & parameter
			}
			else
			{
				setterNode.instructions.add(new LoadInstruction(0)); // push this
				setterNode.instructions.add(new LoadInstruction(1)); // push method parameter
				setterNode.instructions.add(new PutToVariableInstruction(NodeRefUtil.ref(classNode, variableNode))); // pop this & parameter
			}
			setterNode.visitMaxs(propertyDescriptor.isStatic() ? 1 : 2, propertyDescriptor.isStatic() ? 1 : 2);
		}
		classNode.members.add(setterNode);

		MethodNode getterNode = MethodGenerator.gen(propertyDescriptor.getGetter());
		if(propertyDescriptor.getGetter().isDefault())
		{
			if(propertyDescriptor.isStatic())
			{
				getterNode.instructions.add(new GetStaticVariableInstruction(NodeRefUtil.ref(classNode, variableNode)));
				getterNode.instructions.add(new ReturnInstruction());
			}
			else
			{
				getterNode.instructions.add(new LoadInstruction(0)); // push this
				getterNode.instructions.add(new GetVariableInstruction(NodeRefUtil.ref(classNode, variableNode)));
				getterNode.instructions.add(new ReturnInstruction());
			}
			getterNode.visitMaxs(propertyDescriptor.isStatic() ? 0 : 1, propertyDescriptor.isStatic() ? 0 : 1);
		}
		classNode.members.add(getterNode);

		return null;//return super.visitProperty(property, parent);
	}

	@Override
	public Void visitRetellEntry(NapileRetellEntry retellEntry, Node parent)
	{
		assert parent instanceof ClassNode;

		PropertyDescriptor propertyDescriptor = (PropertyDescriptor) bindingTrace.safeGet(BindingContext.DECLARATION_TO_DESCRIPTOR, retellEntry);

		ClassNode classNode = (ClassNode) parent;

		VariableNode variableNode = new VariableNode(ModifierGenerator.gen(propertyDescriptor), propertyDescriptor.getName().getName());
		variableNode.returnType = TypeGenerator.toAsmType(propertyDescriptor.getType());
		classNode.members.add(variableNode);

		return null;// super.visitRetellEntry(retellEntry, variableNode);
	}

	public Map<FqName, ClassNode> getClassNodes()
	{
		return classNodes;
	}
}
