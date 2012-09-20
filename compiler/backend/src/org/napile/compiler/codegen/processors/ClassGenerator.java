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

import org.jetbrains.annotations.NotNull;
import org.napile.asm.adapters.InstructionAdapter;
import org.napile.asm.resolve.name.FqName;
import org.napile.asm.tree.members.ClassNode;
import org.napile.asm.tree.members.ConstructorNode;
import org.napile.asm.tree.members.MethodNode;
import org.napile.asm.tree.members.Node;
import org.napile.asm.tree.members.StaticConstructorNode;
import org.napile.asm.tree.members.TypeParameterNode;
import org.napile.asm.tree.members.VariableNode;
import org.napile.asm.tree.members.bytecode.Instruction;
import org.napile.asm.tree.members.bytecode.impl.GetStaticVariableInstruction;
import org.napile.asm.tree.members.bytecode.impl.GetVariableInstruction;
import org.napile.asm.tree.members.bytecode.impl.LoadInstruction;
import org.napile.asm.tree.members.bytecode.impl.PutToStaticVariableInstruction;
import org.napile.asm.tree.members.bytecode.impl.PutToVariableInstruction;
import org.napile.asm.tree.members.bytecode.impl.ReturnInstruction;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.compiler.codegen.processors.codegen.TypeConstants;
import org.napile.compiler.codegen.processors.codegen.stackValue.StackValue;
import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.ParameterDescriptor;
import org.napile.compiler.lang.descriptors.PropertyDescriptor;
import org.napile.compiler.lang.descriptors.ReferenceParameterDescriptor;
import org.napile.compiler.lang.descriptors.SimpleMethodDescriptor;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.psi.NapileAnonymClass;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileConstructor;
import org.napile.compiler.lang.psi.NapileElement;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileNamedFunction;
import org.napile.compiler.lang.psi.NapileProperty;
import org.napile.compiler.lang.psi.NapileRetellEntry;
import org.napile.compiler.lang.psi.NapileTreeVisitor;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.types.JetType;
import com.intellij.util.containers.MultiMap;

/**
 * @author VISTALL
 * @date 10:58/04.09.12
 */
public class ClassGenerator extends NapileTreeVisitor<Node>
{
	private static class Triple<A, B, C>
	{
		final A a;
		final B b;
		final C c;

		private Triple(A a, B b, C c)
		{
			this.a = a;
			this.b = b;
			this.c = c;
		}
	}

	private final Map<FqName, ClassNode> classNodes;
	private final BindingTrace bindingTrace;

	private final MultiMap<ClassNode, Triple<NapileConstructor, ConstructorNode, ConstructorDescriptor>> constructors = new MultiMap<ClassNode, Triple<NapileConstructor, ConstructorNode, ConstructorDescriptor>>();
	private final MultiMap<ClassNode, InstructionAdapter> propertiesStaticInit = new MultiMap<ClassNode, InstructionAdapter>();
	private final MultiMap<ClassNode, InstructionAdapter> propertiesInit = new MultiMap<ClassNode, InstructionAdapter>();

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

		FqName fqName = bindingTrace.safeGet(BindingContext2.DECLARATION_TO_FQ_NAME, klass);

		ClassNode classNode = new ClassNode(ModifierGenerator.gen(classDescriptor), fqName);

		for(JetType superType : classDescriptor.getSupertypes())
			classNode.supers.add(TypeTransformer.toAsmType(superType));

		for(TypeParameterDescriptor typeParameterDescriptor : classDescriptor.getTypeConstructor().getParameters())
		{
			TypeParameterNode typeParameterNode = new TypeParameterNode(typeParameterDescriptor.getName().getName());
			for(JetType superType : typeParameterDescriptor.getUpperBounds())
				typeParameterNode.supers.add(TypeTransformer.toAsmType(superType));

			classNode.typeParameters.add(typeParameterNode);
		}

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

		ConstructorNode constructorNode = MethodGenerator.gen(constructor, methodDescriptor);

		classNode.members.add(constructorNode);

		constructors.putValue(classNode, new Triple<NapileConstructor, ConstructorNode, ConstructorDescriptor>(constructor, constructorNode, methodDescriptor));

		return null;
	}

	@Override
	public Void visitNamedFunction(NapileNamedFunction function, Node parent)
	{
		assert parent instanceof ClassNode;

		SimpleMethodDescriptor methodDescriptor = (SimpleMethodDescriptor) bindingTrace.safeGet(BindingContext.DECLARATION_TO_DESCRIPTOR, function);

		ClassNode classNode = (ClassNode) parent;

		MethodNode methodNode = MethodGenerator.gen(methodDescriptor, function, bindingTrace);

		classNode.members.add(methodNode);

		return null; //return super.visitNamedFunction(function, parent);
	}

	@Override
	public Void visitProperty(NapileProperty property, Node parent)
	{
		assert parent instanceof ClassNode;

		PropertyDescriptor propertyDescriptor = (PropertyDescriptor) bindingTrace.safeGet(BindingContext.DECLARATION_TO_DESCRIPTOR, property);

		ClassNode classNode = (ClassNode) parent;

		VariableNode variableNode = new VariableNode(ModifierGenerator.gen(propertyDescriptor), propertyDescriptor.getName().getName());
		variableNode.returnType = TypeTransformer.toAsmType(propertyDescriptor.getType());
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

		NapileExpression initializer = property.getInitializer();
		if(initializer != null)
		{
			ExpressionGenerator expressionGenerator = new ExpressionGenerator(bindingTrace, propertyDescriptor);
			if(!propertyDescriptor.isStatic())
				expressionGenerator.getInstructs().load(0);

			expressionGenerator.gen(initializer, variableNode.returnType);

			StackValue.property(propertyDescriptor).store(variableNode.returnType, expressionGenerator.getInstructs());

			if(propertyDescriptor.isStatic())
				propertiesStaticInit.putValue(classNode, expressionGenerator.getInstructs());
			else
				propertiesInit.putValue(classNode, expressionGenerator.getInstructs());
		}
		return null;//return super.visitProperty(property, parent);
	}

	@Override
	public Void visitRetellEntry(NapileRetellEntry retellEntry, Node parent)
	{
		assert parent instanceof ClassNode;

		PropertyDescriptor propertyDescriptor = (PropertyDescriptor) bindingTrace.safeGet(BindingContext.DECLARATION_TO_DESCRIPTOR, retellEntry);

		ClassNode classNode = (ClassNode) parent;

		VariableNode variableNode = new VariableNode(ModifierGenerator.gen(propertyDescriptor), propertyDescriptor.getName().getName());
		variableNode.returnType = TypeTransformer.toAsmType(propertyDescriptor.getType());
		classNode.members.add(variableNode);

		return null;// super.visitRetellEntry(retellEntry, variableNode);
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

			Collection<Triple<NapileConstructor, ConstructorNode, ConstructorDescriptor>> constrs = constructors.get(classNode);

			for(Triple<NapileConstructor, ConstructorNode, ConstructorDescriptor> triple : constrs)
			{
				NapileConstructor constructor = triple.a;
				ConstructorNode constructorNode = triple.b;
				ConstructorDescriptor constructorDescriptor = triple.c;

				constructorNode.instructions.addAll(instructions);

				genReferenceParameters(constructorDescriptor, constructorNode.instructions);

				ExpressionGenerator gen = new ExpressionGenerator(bindingTrace, constructorDescriptor);
				NapileExpression expression = constructor.getBodyExpression();
				if(expression != null)
					gen.returnExpression(expression);
				else
				{
					constructorNode.instructions.add(new LoadInstruction(0));
					constructorNode.instructions.add(new ReturnInstruction());
				}

				constructorNode.instructions.addAll(gen.getInstructs().getInstructions());

				final int maxLocals = size + gen.getInstructs().getMaxLocals() + constructorDescriptor.getValueParameters().size();
				constructorNode.visitMaxs(maxLocals, maxLocals);
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
				StaticConstructorNode staticConstructorNode = new StaticConstructorNode();
				staticConstructorNode.instructions.addAll(instructions);
				staticConstructorNode.visitMaxs(size, size);

				//TODO [VISTALL] add codegen from bodies

				classNode.members.add(staticConstructorNode);
			}
		}
	}

	private static void genReferenceParameters(@NotNull CallableDescriptor callableDescriptor, List<Instruction> instructions)
	{
		InstructionAdapter adapter = new InstructionAdapter();
		for(ParameterDescriptor parameterDescriptor : callableDescriptor.getValueParameters())
			if(parameterDescriptor instanceof ReferenceParameterDescriptor)
			{
				PropertyDescriptor propertyDescriptor = ((ReferenceParameterDescriptor) parameterDescriptor).getReferenceProperty();
				if(propertyDescriptor == null)
					continue;

				TypeNode typeNode = TypeTransformer.toAsmType(propertyDescriptor.getType());

				if(!propertyDescriptor.isStatic())
					StackValue.local(0, typeNode).put(TypeConstants.ANY, adapter);
				StackValue.local(callableDescriptor.isStatic() ? 0 : 1 + parameterDescriptor.getIndex(), typeNode).put(typeNode, adapter);
				StackValue.property(propertyDescriptor).store(typeNode, adapter);
			}

		instructions.addAll(adapter.getInstructions());
	}

	public Map<FqName, ClassNode> getClassNodes()
	{
		return classNodes;
	}
}
