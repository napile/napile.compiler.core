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
import org.napile.asm.tree.members.CodeInfo;
import org.napile.asm.tree.members.MethodNode;
import org.napile.asm.tree.members.MethodParameterNode;
import org.napile.asm.tree.members.VariableNode;
import org.napile.asm.tree.members.bytecode.VariableRef;
import org.napile.asm.tree.members.bytecode.adapter.InstructionAdapter;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.asm.tree.members.types.constructors.ClassTypeNode;
import org.napile.compiler.codegen.processors.codegen.stackValue.StackValue;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.SimpleMethodDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.lang.resolve.BindingTraceKeys;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.types.JetType;
import com.intellij.openapi.util.Pair;

/**
 * @author VISTALL
 * @since 19:51/12.01.13
 */
public class ClassCodegen extends NapileVisitorVoid
{
	private final Map<Boolean, InstructionAdapter> constructorsAdapters = new HashMap<Boolean, InstructionAdapter>(2);
	private final BindingTrace bindingTrace;

	private ClassNode classNode;
	private ExpressionCodegenContext context;

	public ClassCodegen(BindingTrace bindingTrace)
	{
		this.bindingTrace = bindingTrace;
	}

	public ClassNode gen(NapileClass napileClass, ExpressionCodegenContext context)
	{
		FqName fqName = bindingTrace.safeGet(BindingTraceKeys2.DECLARATION_TO_FQ_NAME, napileClass);
		ClassDescriptor classDescriptor = bindingTrace.safeGet(BindingTraceKeys.CLASS, napileClass);

		classNode = new ClassNode(ModifierCodegen.gen(classDescriptor), fqName);

		return gen(napileClass, classDescriptor, context);
	}

	public ClassNode gen(NapileAnonymClass anonymClass, ExpressionCodegenContext context)
	{
		FqName fqName = bindingTrace.safeGet(BindingTraceKeys2.DECLARATION_TO_FQ_NAME, anonymClass);
		ClassDescriptor classDescriptor = bindingTrace.safeGet(BindingTraceKeys.CLASS, anonymClass);

		classNode = new ClassNode(Modifier.list(Modifier.FINAL, Modifier.STATIC), fqName);

		return gen(anonymClass, classDescriptor, context);
	}

	public ClassNode gen(NapileClassLike classLike, ClassDescriptor classDescriptor, ExpressionCodegenContext context)
	{
		this.context = context;

		AnnotationCodegen.gen(bindingTrace, classDescriptor, classNode, classNode);

		TypeParameterCodegen.gen(classDescriptor.getTypeConstructor().getParameters(), classNode, bindingTrace, classNode);

		constructorsAdapters.put(Boolean.FALSE, new InstructionAdapter()); //instance
		constructorsAdapters.put(Boolean.TRUE, new InstructionAdapter()); //static

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
			if(declaration instanceof NapileConstructor)
				list.add(declaration);

		for(NapileDeclaration declaration : list)
			declaration.accept(this);

		InstructionAdapter adapter = constructorsAdapters.get(Boolean.TRUE);
		if(!adapter.getInstructions().isEmpty())
		{
			MethodNode methodNode = MethodNode.staticConstructor();
			methodNode.code = new CodeInfo(adapter);

			classNode.addMember(methodNode);
		}

		if(classDescriptor.isTraited())
		{
			adapter = constructorsAdapters.get(Boolean.FALSE);
			MethodNode methodNode = MethodNode.constructor(Modifier.HERITABLE);

			adapter.visitLocalVariable("this");

			adapter.localGet(0);
			adapter.returnValues(1);

			methodNode.code = new CodeInfo(adapter);
			classNode.addMember(methodNode);
		}
		return classNode;
	}

	@Override
	public void visitConstructor(NapileConstructor constructor)
	{
		ConstructorDescriptor constructorDescriptor = bindingTrace.safeGet(BindingTraceKeys.CONSTRUCTOR, constructor);
		if(constructorDescriptor.isStatic())
		{
			NapileExpression expression = constructor.getBodyExpression();
			if(expression != null)
			{
				ExpressionCodegen gen = new ExpressionCodegen(bindingTrace, constructorDescriptor, classNode, context.clone(), constructorsAdapters.get(Boolean.TRUE));
				gen.gen(expression).put(AsmConstants.NULL_TYPE, gen.instructs, PositionMarker.EMPTY);
			}
		}
		else
		{
			Pair<MethodNode, InstructionAdapter> pair = MethodCodegen.genConstructor(constructor, constructorDescriptor, bindingTrace, classNode);

			MethodNode constructorNode = pair.getFirst();
			InstructionAdapter adapter = pair.getSecond();

			InstructionAdapter variableInitCode = constructorsAdapters.get(Boolean.FALSE);

			adapter.getInstructions().addAll(variableInitCode.getInstructions());
			adapter.getTryCatchBlockNodes().addAll(variableInitCode.getTryCatchBlockNodes());

			ExpressionCodegen gen = new ExpressionCodegen(bindingTrace, constructorDescriptor, classNode, context.clone(), adapter);
			NapileExpression expression = constructor.getBodyExpression();
			if(expression != null)
				gen.returnExpression(expression, false);
			else
			{
				adapter.localGet(0);
				adapter.returnValues(1);
			}

			constructorNode.code = new CodeInfo(adapter);
			classNode.addMember(constructorNode);
		}
	}

	@Override
	public void visitVariable(NapileVariable variable)
	{
		VariableDescriptor variableDescriptor = bindingTrace.safeGet(BindingTraceKeys.VARIABLE, variable);

		TypeNode type = TypeTransformer.toAsmType(bindingTrace, variableDescriptor.getType(), classNode);



		VariableCodegen.getSetterAndGetter(variableDescriptor, variable, classNode, bindingTrace, false);

		if(!variable.hasModifier(NapileTokens.OVERRIDE_KEYWORD))
		{
			VariableNode variableNode = new VariableNode(ModifierCodegen.gen(variableDescriptor), variableDescriptor.getName(), type);

			classNode.addMember(variableNode);

			AnnotationCodegen.gen(bindingTrace, variableDescriptor, variableNode, classNode);

			NapileExpression initializer = variable.getInitializer();
			if(initializer != null)
			{
				InstructionAdapter adapter = constructorsAdapters.get(variableDescriptor.isStatic());

				if(!variableDescriptor.isStatic())
					adapter.localGet(0);

				// if var has lazy modifier - need put null
				if(variable.hasModifier(NapileTokens.LAZY_KEYWORD))
					adapter.putNull();
				else
					new ExpressionCodegen(bindingTrace, null, classNode, context.clone(), adapter).gen(initializer, type);

				StackValue.variable(bindingTrace, classNode, variableDescriptor).store(type, adapter, PositionMarker.EMPTY);
			}
		}
	}

	@Override
	public void visitEnumValue(NapileEnumValue value)
	{
		VariableDescriptor variableDescriptor = bindingTrace.safeGet(BindingTraceKeys.VARIABLE, value);

		FqName classFqName = classNode.name.parent().child(Name.identifier(classNode.name.shortName() + AsmConstants.ANONYM_SPLITTER + variableDescriptor.getName()));

		TypeNode type = new TypeNode(false, new ClassTypeNode(classFqName));

		VariableNode variableNode = new VariableNode(ModifierCodegen.gen(variableDescriptor), variableDescriptor.getName(), type);

		AnnotationCodegen.gen(bindingTrace, variableDescriptor, variableNode, classNode);

		classNode.addMember(variableNode);

		VariableCodegen.getSetterAndGetter(variableDescriptor, value, classNode, bindingTrace, true);

		ClassDescriptor classDescriptor = bindingTrace.safeGet(BindingTraceKeys.CLASS, value);
		ClassNode innerClassNode = new ClassNode(Modifier.list(Modifier.STATIC, Modifier.FINAL), classFqName);
		for(JetType superType : classDescriptor.getSupertypes())
			innerClassNode.supers.add(TypeTransformer.toAsmType(bindingTrace, superType, classNode));

		MethodNode enumClassConstructorNode = MethodNode.constructor(Modifier.list(Modifier.LOCAL));

		InstructionAdapter constructorAdapter = new InstructionAdapter();
		constructorAdapter.visitLocalVariable("this");

		MethodCodegen.genSuperCalls(constructorAdapter, value, bindingTrace, classNode);

		constructorAdapter.localGet(0);
		constructorAdapter.returnValues(1);

		enumClassConstructorNode.code = new CodeInfo(constructorAdapter);

		innerClassNode.addMember(enumClassConstructorNode);

		classNode.addMember(innerClassNode);

		InstructionAdapter adapter = constructorsAdapters.get(Boolean.TRUE);

		adapter.newObject(type, Collections.<MethodParameterNode>emptyList());
		adapter.putToStaticVar(new VariableRef(FqNameGenerator.getFqName(variableDescriptor, bindingTrace), type));
	}

	@Override
	public void visitNamedMethodOrMacro(NapileNamedMethodOrMacro method)
	{
		SimpleMethodDescriptor methodDescriptor = (SimpleMethodDescriptor) bindingTrace.safeGet(BindingTraceKeys.DECLARATION_TO_DESCRIPTOR, method);

		MethodNode methodNode = MethodCodegen.genMethodOrMacro(method, methodDescriptor, bindingTrace, classNode, context.clone());

		classNode.addMember(methodNode);
	}

	public Map<Boolean, InstructionAdapter> getConstructorsAdapters()
	{
		return constructorsAdapters;
	}
}
