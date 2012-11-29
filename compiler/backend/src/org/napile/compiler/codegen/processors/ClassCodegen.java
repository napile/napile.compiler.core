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
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.napile.asm.resolve.name.FqName;
import org.napile.asm.tree.members.ClassNode;
import org.napile.asm.tree.members.MethodNode;
import org.napile.asm.tree.members.Node;
import org.napile.asm.tree.members.VariableNode;
import org.napile.asm.tree.members.bytecode.Instruction;
import org.napile.asm.tree.members.bytecode.adapter.InstructionAdapter;
import org.napile.asm.tree.members.bytecode.impl.LoadInstruction;
import org.napile.asm.tree.members.bytecode.impl.ReturnInstruction;
import org.napile.compiler.codegen.processors.codegen.stackValue.StackValue;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.LocalVariableDescriptor;
import org.napile.compiler.lang.descriptors.PropertyDescriptor;
import org.napile.compiler.lang.descriptors.SimpleMethodDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.compiler.lang.psi.NapileAnonymClass;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileConstructor;
import org.napile.compiler.lang.psi.NapileElement;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileFunctionLiteralExpression;
import org.napile.compiler.lang.psi.NapileNamedMethodOrMacro;
import org.napile.compiler.lang.psi.NapileTreeVisitor;
import org.napile.compiler.lang.psi.NapileVariable;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.PropertyAccessUtil;
import org.napile.compiler.lang.types.JetType;
import com.intellij.util.containers.MultiMap;

/**
 * @author VISTALL
 * @date 10:58/04.09.12
 */
public class ClassCodegen extends NapileTreeVisitor<Node>
{
	private final Map<FqName, ClassNode> classNodes;
	private final BindingTrace bindingTrace;

	private final MultiMap<ClassNode, Triple<NapileConstructor, MethodNode, ConstructorDescriptor>> constructors = new MultiMap<ClassNode, Triple<NapileConstructor, MethodNode, ConstructorDescriptor>>();
	private final MultiMap<ClassNode, InstructionAdapter> propertiesStaticInit = new MultiMap<ClassNode, InstructionAdapter>();
	private final MultiMap<ClassNode, InstructionAdapter> propertiesInit = new MultiMap<ClassNode, InstructionAdapter>();

	public ClassCodegen(BindingTrace bindingTrace, Map<FqName, ClassNode> classNodes)
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

		FqName fqName = bindingTrace.safeGet(BindingContext2.DECLARATION_TO_FQ_NAME, klass);

		ClassNode classNode = new ClassNode(ModifierCodegen.gen(classDescriptor), fqName);
		AnnotationCodegen.convert(bindingTrace, classDescriptor, classNode);

		for(JetType superType : classDescriptor.getSupertypes())
			classNode.supers.add(TypeTransformer.toAsmType(superType));

		TypeParameterCodegen.gen(classDescriptor.getTypeConstructor().getParameters(), classNode);

		classNodes.put(fqName, classNode);

		return super.visitClass(klass, classNode);
	}

	@Override
	public Void visitAnonymClass(NapileAnonymClass element, Node data)
	{
		ClassDescriptor classDescriptor = (ClassDescriptor) bindingTrace.safeGet(BindingContext.DECLARATION_TO_DESCRIPTOR, element);

		FqName fqName = bindingTrace.get(BindingContext2.DECLARATION_TO_FQ_NAME, element);

		assert fqName != null;

		ClassNode classNode = new ClassNode(ModifierCodegen.gen(classDescriptor), fqName);

		classNodes.put(fqName, classNode);

		return null; //return super.visitAnonymClass(element, classNode);
	}

	@Override
	public Void visitFunctionLiteralExpression(NapileFunctionLiteralExpression expression, Node parent)
	{
		assert parent instanceof ClassNode;

		FqName fqName = bindingTrace.safeGet(BindingContext2.DECLARATION_TO_FQ_NAME, expression.getAnonymMethod());

		SimpleMethodDescriptor methodDescriptor = bindingTrace.safeGet(BindingContext.METHOD, expression);

		// gen method
		MethodNode methodNode = MethodCodegen.gen(methodDescriptor, fqName.shortName(), expression.getAnonymMethod(), bindingTrace);

		ClassNode classNode = (ClassNode) parent;

		classNode.members.add(methodNode);
		return null;
	}

	@Override
	public Void visitConstructor(NapileConstructor constructor, Node parent)
	{
		assert parent instanceof ClassNode;

		ConstructorDescriptor methodDescriptor = (ConstructorDescriptor) bindingTrace.safeGet(BindingContext.DECLARATION_TO_DESCRIPTOR, constructor);

		ClassNode classNode = (ClassNode) parent;

		MethodNode constructorNode = MethodCodegen.gen(constructor, methodDescriptor, bindingTrace);

		classNode.members.add(constructorNode);

		constructors.putValue(classNode, new Triple<NapileConstructor, MethodNode, ConstructorDescriptor>(constructor, constructorNode, methodDescriptor));

		return null;
	}

	@Override
	public Void visitNamedMethodOrMacro(NapileNamedMethodOrMacro function, Node parent)
	{
		assert parent instanceof ClassNode;

		SimpleMethodDescriptor methodDescriptor = (SimpleMethodDescriptor) bindingTrace.safeGet(BindingContext.DECLARATION_TO_DESCRIPTOR, function);

		ClassNode classNode = (ClassNode) parent;

		MethodNode methodNode = MethodCodegen.gen(methodDescriptor, methodDescriptor.getName(), function, bindingTrace);

		classNode.members.add(methodNode);

		return super.visitNamedMethodOrMacro(function, parent);
	}

	@Override
	public Void visitVariable(NapileVariable property, Node parent)
	{
		assert parent instanceof ClassNode;

		VariableDescriptor variableDescriptor = bindingTrace.safeGet(BindingContext.VARIABLE, property);
		if(variableDescriptor instanceof LocalVariableDescriptor)
			return super.visitVariable(property, parent);

		ClassNode classNode = (ClassNode) parent;

		VariableNode variableNode = new VariableNode(ModifierCodegen.gen(variableDescriptor), variableDescriptor.getName());
		variableNode.returnType = TypeTransformer.toAsmType(variableDescriptor.getType());
		classNode.members.add(variableNode);

		VariableCodegen.getSetterAndGetter((PropertyDescriptor)variableDescriptor, classNode, bindingTrace);

		NapileExpression initializer = property.getInitializer();
		if(initializer != null)
		{
			ExpressionCodegen expressionCodegen = new ExpressionCodegen(bindingTrace, variableDescriptor);
			if(!variableDescriptor.isStatic())
				expressionCodegen.getInstructs().load(0);

			expressionCodegen.gen(initializer, variableNode.returnType);

			StackValue.variable((PropertyDescriptor) variableDescriptor).store(variableNode.returnType, expressionCodegen.getInstructs());

			if(variableDescriptor.isStatic())
				propertiesStaticInit.putValue(classNode, expressionCodegen.getInstructs());
			else
				propertiesInit.putValue(classNode, expressionCodegen.getInstructs());
		}
		else
		{
			// if variable has lazy initiator, put null to var
			if(PropertyAccessUtil.getPropertyDescriptor(bindingTrace, variableDescriptor, NapileTokens.LAZY_KEYWORD) != null)
			{
				InstructionAdapter adapter = new InstructionAdapter();

				if(!variableDescriptor.isStatic())
					adapter.load(0);

				adapter.putNull();

				StackValue.variable((PropertyDescriptor) variableDescriptor).store(variableNode.returnType, adapter);

				if(variableDescriptor.isStatic())
					propertiesStaticInit.putValue(classNode, adapter);
				else
					propertiesInit.putValue(classNode, adapter);
			}
		}
		return super.visitVariable(property, parent);
	}

	public void addPropertiesInitToConstructors()
	{
		for(ClassNode classNode : classNodes.values())
		{
			// first instance properties
			int size = 1;
			List<Instruction> instructions = new ArrayList<Instruction>();
			Collection<InstructionAdapter> instructionAdapters = propertiesInit.get(classNode);
			for(InstructionAdapter inst : instructionAdapters)
			{
				instructions.addAll(inst.getInstructions());
				size += inst.getMaxLocals();
			}

			Collection<Triple<NapileConstructor, MethodNode, ConstructorDescriptor>> constrs = constructors.get(classNode);

			for(Triple<NapileConstructor, MethodNode, ConstructorDescriptor> triple : constrs)
			{
				NapileConstructor constructor = triple.a;
				MethodNode constructorNode = triple.b;
				ConstructorDescriptor constructorDescriptor = triple.c;

				constructorNode.instructions.addAll(instructions);

				MethodCodegen.genReferenceParameters(constructorDescriptor, constructorNode.instructions);

				ExpressionCodegen gen = new ExpressionCodegen(bindingTrace, constructorDescriptor);
				NapileExpression expression = constructor.getBodyExpression();
				if(expression != null)
					gen.returnExpression(expression, false);
				else
				{
					constructorNode.instructions.add(new LoadInstruction(0));
					constructorNode.instructions.add(new ReturnInstruction());
				}

				constructorNode.instructions.addAll(gen.getInstructs().getInstructions());
				constructorNode.tryCatchBlockNodes.addAll(gen.getInstructs().getTryCatchBlockNodes());

				constructorNode.maxLocals = size + gen.getInstructs().getMaxLocals() + constructorDescriptor.getValueParameters().size();
			}

			// next static properties
			size = 0;
			instructions.clear();

			instructionAdapters = propertiesStaticInit.get(classNode);
			for(InstructionAdapter inst : instructionAdapters)
			{
				instructions.addAll(inst.getInstructions());
				size += inst.getMaxLocals();
			}

			if(!instructions.isEmpty())
			{
				MethodNode staticConstructorNode = MethodNode.staticConstructor();
				staticConstructorNode.instructions.addAll(instructions);
				staticConstructorNode.maxLocals = size;

				//TODO [VISTALL] add codegen from bodies

				classNode.members.add(staticConstructorNode);
			}
		}
	}

	public Map<FqName, ClassNode> getClassNodes()
	{
		return classNodes;
	}
}
