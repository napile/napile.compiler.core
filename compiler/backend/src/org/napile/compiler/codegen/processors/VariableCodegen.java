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

import org.jetbrains.annotations.NotNull;
import org.napile.asm.AsmConstants;
import org.napile.asm.Modifier;
import org.napile.asm.resolve.name.FqName;
import org.napile.asm.resolve.name.Name;
import org.napile.asm.tree.members.ClassNode;
import org.napile.asm.tree.members.MethodNode;
import org.napile.asm.tree.members.MethodParameterNode;
import org.napile.asm.tree.members.bytecode.adapter.InstructionAdapter;
import org.napile.asm.tree.members.bytecode.impl.GetStaticVariableInstruction;
import org.napile.asm.tree.members.bytecode.impl.GetVariableInstruction;
import org.napile.asm.tree.members.bytecode.impl.LoadInstruction;
import org.napile.asm.tree.members.bytecode.impl.ReturnInstruction;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.PropertyDescriptor;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.DescriptorUtils;

/**
 * @author VISTALL
 * @date 13:32/29.09.12
 */
public class VariableCodegen
{
	public static void getSetterAndGetter(@NotNull PropertyDescriptor propertyDescriptor, @NotNull ClassNode classNode, @NotNull BindingTrace bindingTrace)
	{
		FqName fqName = DescriptorUtils.getFQName(propertyDescriptor).toSafe();

		getSetter(propertyDescriptor, classNode, bindingTrace, fqName);

		genGetter(propertyDescriptor, classNode, bindingTrace, fqName);
	}

	private static void getSetter(PropertyDescriptor propertyDescriptor, ClassNode classNode, BindingTrace bindingTrace, FqName fqName)
	{
		FqName setterFq = fqName.parent().child(Name.identifier(fqName.shortName() + AsmConstants.ANONYM_SPLITTER + "set"));

		DeclarationDescriptor setter = bindingTrace.get(BindingContext.FQNAME_TO_DESCRIPTOR, setterFq);
		if(setter == null)
		{
			MethodNode setterMethodNode = new MethodNode(ModifierCodegen.gen(propertyDescriptor), setterFq.shortName());
			setterMethodNode.returnType = AsmConstants.NULL_TYPE;
			setterMethodNode.parameters.add(new MethodParameterNode(Modifier.list(Modifier.FINAL), Name.identifier("value"), TypeTransformer.toAsmType(propertyDescriptor.getType())));

			InstructionAdapter instructions = new InstructionAdapter();
			if(propertyDescriptor.isStatic())
			{
				instructions.load(0);
				instructions.putToStaticVar(NodeRefUtil.ref(propertyDescriptor));
				instructions.putNull();
				instructions.returnVal();
			}
			else
			{
				instructions.load(0);
				instructions.load(1);
				instructions.putToVar(NodeRefUtil.ref(propertyDescriptor));
				instructions.putNull();
				instructions.returnVal();
			}

			setterMethodNode.instructions.addAll(instructions.getInstructions());
			setterMethodNode.maxLocals = propertyDescriptor.isStatic() ? 1 : 2;

			classNode.members.add(setterMethodNode);
		}
	}

	private static void genGetter(PropertyDescriptor propertyDescriptor, ClassNode classNode, BindingTrace bindingTrace, FqName fqName)
	{
		FqName getterFq = fqName.parent().child(Name.identifier(fqName.shortName() + AsmConstants.ANONYM_SPLITTER + "get"));
		DeclarationDescriptor getter = bindingTrace.get(BindingContext.FQNAME_TO_DESCRIPTOR, getterFq);
		if(getter == null)
		{
			//TODO [VISTALL] make LazyType, current version is not thread safe
			MethodDescriptor lazyMethodDescriptor = null;//PropertyAccessUtil.getPropertyDescriptor(bindingTrace, propertyDescriptor, NapileTokens.LAZY_KEYWORD);
			if(lazyMethodDescriptor == null)
			{
				MethodNode getterMethodNode = new MethodNode(ModifierCodegen.gen(propertyDescriptor), getterFq.shortName());
				getterMethodNode.returnType = TypeTransformer.toAsmType(propertyDescriptor.getType());

				if(propertyDescriptor.isStatic())
				{
					getterMethodNode.instructions.add(new GetStaticVariableInstruction(NodeRefUtil.ref(propertyDescriptor)));
					getterMethodNode.instructions.add(new ReturnInstruction());
				}
				else
				{
					getterMethodNode.instructions.add(new LoadInstruction(0));
					getterMethodNode.instructions.add(new GetVariableInstruction(NodeRefUtil.ref(propertyDescriptor)));
					getterMethodNode.instructions.add(new ReturnInstruction());
				}

				getterMethodNode.maxLocals = propertyDescriptor.isStatic() ? 0 : 1;

				classNode.members.add(getterMethodNode);
			}
			/*else
			{
				MethodNode getterMethodNode = new MethodNode(ModifierCodegen.gen(propertyDescriptor), getterFq.shortName());
				getterMethodNode.returnType = TypeTransformer.toAsmType(propertyDescriptor.getType());

				InstructionAdapter adapter = new InstructionAdapter();
				if(!propertyDescriptor.isStatic())
					adapter.visitLocalVariable("this");

				final StackValue varStackValue = StackValue.variable(propertyDescriptor);

				if(!propertyDescriptor.isStatic())
					adapter.load(0);

				varStackValue.put(getterMethodNode.returnType, adapter);

				adapter.dup();

				adapter.putNull();

				adapter.invokeVirtual(BinaryOperationCodegen.ANY_EQUALS, false);

				adapter.putTrue();

				ReservedInstruction reservedInstruction = adapter.reserve();

				if(!propertyDescriptor.isStatic())
				{
					adapter.load(0);
					adapter.dup();
				}

				if(lazyMethodDescriptor.getVisibility() == Visibility.LOCAL)
					adapter.invokeSpecial(NodeRefUtil.ref(lazyMethodDescriptor), false);
				else
					adapter.invokeVirtual(NodeRefUtil.ref(lazyMethodDescriptor), false);

				varStackValue.store(getterMethodNode.returnType, adapter);

				if(!propertyDescriptor.isStatic())
					adapter.load(0);

				varStackValue.put(getterMethodNode.returnType, adapter);

				adapter.returnVal();

				adapter.replace(reservedInstruction, new JumpIfInstruction(adapter.size()));

				adapter.returnVal();

				getterMethodNode.putInstructions(adapter);
				classNode.members.add(getterMethodNode);
			} */
		}
	}
}
