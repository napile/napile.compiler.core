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

package org.napile.compiler.codegen.processors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.napile.asm.AsmConstants;
import org.napile.asm.Modifier;
import org.napile.asm.resolve.name.FqName;
import org.napile.asm.resolve.name.Name;
import org.napile.asm.tree.members.ClassNode;
import org.napile.asm.tree.members.MethodNode;
import org.napile.asm.tree.members.VariableNode;
import org.napile.asm.tree.members.bytecode.VariableRef;
import org.napile.asm.tree.members.bytecode.adapter.InstructionAdapter;
import org.napile.asm.tree.members.bytecode.impl.LoadInstruction;
import org.napile.asm.tree.members.bytecode.impl.ReturnInstruction;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.asm.tree.members.types.constructors.ClassTypeNode;
import org.napile.compiler.codegen.processors.codegen.stackValue.StackValue;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.SimpleMethodDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.compiler.lang.psi.NapileClassLike;
import org.napile.compiler.lang.psi.NapileConstructor;
import org.napile.compiler.lang.psi.NapileDeclaration;
import org.napile.compiler.lang.psi.NapileEnumValue;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileNamedMethodOrMacro;
import org.napile.compiler.lang.psi.NapileStaticConstructor;
import org.napile.compiler.lang.psi.NapileVariable;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.DescriptorUtils;
import org.napile.compiler.lang.types.JetType;

/**
 * @author VISTALL
 * @date 19:51/12.01.13
 */
public class ClassCodegen extends NapileVisitorVoid
{
	private final Map<Boolean, InstructionAdapter> constructorsAdapters = new HashMap<Boolean, InstructionAdapter>(2);
	private final BindingTrace bindingTrace;

	private ClassNode classNode;

	public ClassCodegen(BindingTrace bindingTrace)
	{
		this.bindingTrace = bindingTrace;
	}

	public ClassNode gen(NapileClassLike classLike)
	{
		constructorsAdapters.put(Boolean.FALSE, new InstructionAdapter()); //instance
		constructorsAdapters.put(Boolean.TRUE, new InstructionAdapter()); //static

		FqName fqName = bindingTrace.safeGet(BindingContext2.DECLARATION_TO_FQ_NAME, classLike);
		ClassDescriptor classDescriptor = bindingTrace.safeGet(BindingContext.CLASS, classLike);

		classNode = new ClassNode(ModifierCodegen.gen(classDescriptor), fqName);
		AnnotationCodegen.gen(bindingTrace, classDescriptor, classNode, classNode);
		TypeParameterCodegen.gen(classDescriptor.getTypeConstructor().getParameters(), classNode, bindingTrace, classNode);

		for(JetType superType : classDescriptor.getSupertypes())
			classNode.supers.add(TypeTransformer.toAsmType(bindingTrace, superType, classNode));

		NapileDeclaration[] declarations = classLike.getDeclarations();
		List<NapileDeclaration> list = new ArrayList<NapileDeclaration>(declarations.length);

		// FIXME [VISTALL] use comparator>
		// variable or enum value
		// method or macros
		// constructors

		for(NapileDeclaration declaration : declarations)
			if(declaration instanceof NapileVariable)
				list.add(declaration);

		for(NapileDeclaration declaration : declarations)
			if(declaration instanceof NapileNamedMethodOrMacro)
				list.add(declaration);

		for(NapileDeclaration declaration : declarations)
			if(declaration instanceof NapileConstructor || declaration instanceof NapileStaticConstructor)
				list.add(declaration);

		for(NapileDeclaration declaration : list)
			declaration.accept(this);

		InstructionAdapter adapter = constructorsAdapters.get(Boolean.TRUE);
		if(!adapter.getInstructions().isEmpty())
		{
			MethodNode methodNode = MethodNode.staticConstructor();
			methodNode.putInstructions(adapter);

			classNode.addMember(methodNode);
		}
		return classNode;
	}

	@Override
	public void visitConstructor(NapileConstructor constructor)
	{
		ConstructorDescriptor constructorDescriptor = bindingTrace.safeGet(BindingContext.CONSTRUCTOR, constructor);

		InstructionAdapter adapter = new InstructionAdapter();
		adapter.getInstructions().addAll(constructorsAdapters.get(Boolean.FALSE).getInstructions()); // clone

		MethodNode constructorNode = MethodCodegen.gen(constructor, constructorDescriptor, bindingTrace, classNode);
		MethodCodegen.genReferenceParameters(constructor, constructorDescriptor, constructorNode.instructions, bindingTrace, classNode);

		ExpressionCodegen gen = new ExpressionCodegen(bindingTrace, constructorDescriptor, classNode, Collections.<VariableDescriptor, StackValue>emptyMap(), adapter);
		NapileExpression expression = constructor.getBodyExpression();
		if(expression != null)
			gen.returnExpression(expression, false);
		else
		{
			adapter.load(0);
			adapter.returnVal();
		}
		constructorNode.putInstructions(adapter);
		constructorNode.maxLocals += 1 + constructorDescriptor.getValueParameters().size() + adapter.getMaxLocals();

		classNode.addMember(constructorNode);
	}

	@Override
	public void visitStaticConstructor(NapileStaticConstructor constructor)
	{
		ConstructorDescriptor constructorDescriptor = bindingTrace.safeGet(BindingContext.CONSTRUCTOR, constructor);

		NapileExpression expression = constructor.getBodyExpression();
		if(expression != null)
			new ExpressionCodegen(bindingTrace, constructorDescriptor, classNode, Collections.<VariableDescriptor, StackValue>emptyMap(), constructorsAdapters.get(Boolean.TRUE)).returnExpression(expression, false);
	}

	@Override
	public void visitVariable(NapileVariable variable)
	{
		VariableDescriptor variableDescriptor = bindingTrace.safeGet(BindingContext.VARIABLE, variable);

		TypeNode type = TypeTransformer.toAsmType(bindingTrace, variableDescriptor.getType(), classNode);

		VariableCodegen.getSetterAndGetter(variableDescriptor, variable, classNode, bindingTrace, false);

		if(!variable.hasModifier(NapileTokens.OVERRIDE_KEYWORD))
		{
			VariableNode variableNode = new VariableNode(ModifierCodegen.gen(variableDescriptor), variableDescriptor.getName(), type);
			classNode.addMember(variableNode);

			NapileExpression initializer = variable.getInitializer();
			if(initializer != null)
			{
				InstructionAdapter adapter = constructorsAdapters.get(variableDescriptor.isStatic());

				if(!variableDescriptor.isStatic())
					adapter.load(0);

				// if var has lazy modifier - need put null
				if(variable.hasModifier(NapileTokens.LAZY_KEYWORD))
					adapter.putNull();
				else
					new ExpressionCodegen(bindingTrace, variableDescriptor, classNode, Collections.<VariableDescriptor, StackValue>emptyMap(), adapter).gen(initializer, type);

				StackValue.variable(bindingTrace, classNode, variableDescriptor).store(type, adapter);
			}
		}
	}

	@Override
	public void visitEnumValue(NapileEnumValue value)
	{
		VariableDescriptor variableDescriptor = bindingTrace.safeGet(BindingContext.VARIABLE, value);

		FqName classFqName = classNode.name.parent().child(Name.identifier(classNode.name.shortName() + AsmConstants.ANONYM_SPLITTER + variableDescriptor.getName()));

		TypeNode type = new TypeNode(false, new ClassTypeNode(classFqName));

		VariableNode variableNode = new VariableNode(ModifierCodegen.gen(variableDescriptor), variableDescriptor.getName(), type);
		classNode.addMember(variableNode);

		VariableCodegen.getSetterAndGetter(variableDescriptor, value, classNode, bindingTrace, true);

		ClassDescriptor classDescriptor = bindingTrace.safeGet(BindingContext.CLASS, value);
		ClassNode innerClassNode = new ClassNode(Modifier.list(Modifier.STATIC, Modifier.FINAL), classFqName);
		for(JetType superType : classDescriptor.getSupertypes())
			innerClassNode.supers.add(TypeTransformer.toAsmType(bindingTrace, superType, classNode));

		MethodNode methodNode = MethodNode.constructor(Modifier.list(Modifier.LOCAL));
		methodNode.maxLocals = 1;

		MethodCodegen.genSuperCalls(methodNode, value, bindingTrace, classNode);

		methodNode.instructions.add(new LoadInstruction(0));
		methodNode.instructions.add(new ReturnInstruction());

		innerClassNode.addMember(methodNode);

		classNode.addMember(innerClassNode);

		InstructionAdapter adapter = constructorsAdapters.get(Boolean.TRUE);

		adapter.newObject(type, Collections.<TypeNode>emptyList());
		adapter.putToStaticVar(new VariableRef(DescriptorUtils.getFQName(variableDescriptor).toSafe(), type));
	}

	@Override
	public void visitNamedMethodOrMacro(NapileNamedMethodOrMacro method)
	{
		SimpleMethodDescriptor methodDescriptor = (SimpleMethodDescriptor) bindingTrace.safeGet(BindingContext.DECLARATION_TO_DESCRIPTOR, method);

		MethodNode methodNode = MethodCodegen.gen(methodDescriptor, methodDescriptor.getName(), method, bindingTrace, classNode, Collections.<VariableDescriptor, StackValue>emptyMap());

		classNode.addMember(methodNode);
	}
}
