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

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.AsmConstants;
import org.napile.asm.Modifier;
import org.napile.asm.resolve.name.Name;
import org.napile.asm.tree.members.VariableNode;
import org.napile.compiler.codegen.processors.codegen.stackValue.StackValue;
import org.napile.compiler.codegen.processors.codegen.stackValue.WrappedVar;
import org.napile.compiler.lang.descriptors.CallableMemberDescriptor;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptorImpl;
import org.napile.compiler.lang.descriptors.Visibility;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.scopes.receivers.ClassReceiver;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;

/**
 * @author VISTALL
 * @date 9:54/13.01.13
 */
public class ExpressionCodegenContext
{
	@NotNull
	public final Map<VariableDescriptor, WrappedVar> wrappedVariables = new HashMap<VariableDescriptor, WrappedVar>();

	@NotNull
	public final Map<ClassDescriptor, StackValue> wrappedOuterClasses = new HashMap<ClassDescriptor, StackValue>();

	public static ExpressionCodegenContext empty()
	{
		return new ExpressionCodegenContext();
	}

	@NotNull
	public ExpressionCodegen gen;

	private ExpressionCodegenContext()
	{

	}

	public boolean wrapVariableIfNeed(VariableDescriptor variableDescriptor)
	{
		boolean wrappedInClosure = gen.bindingTrace.safeGet(BindingContext.CAPTURED_IN_CLOSURE, variableDescriptor);
		if(wrappedInClosure)
		{
			MethodDescriptor ownerMethod = (MethodDescriptor) variableDescriptor.getContainingDeclaration();
			ClassDescriptor ownerClass = (ClassDescriptor) ownerMethod.getContainingDeclaration();

			Name name = Name.identifier(ownerMethod.getName() + AsmConstants.ANONYM_SPLITTER + variableDescriptor.getName());
			VariableDescriptorImpl newVariableDescriptor = new VariableDescriptorImpl(ownerClass, variableDescriptor.getAnnotations(), variableDescriptor.getModality(), Visibility.LOCAL, name, CallableMemberDescriptor.Kind.DECLARATION, ownerMethod.isStatic(), true);
			newVariableDescriptor.setType(variableDescriptor.getType(), variableDescriptor.getTypeParameters(), ownerMethod.isStatic() ? ReceiverDescriptor.NO_RECEIVER : new ClassReceiver(ownerClass));

			wrappedVariables.put(variableDescriptor, new WrappedVar(gen, newVariableDescriptor));

			VariableCodegen.getSetterAndGetter(newVariableDescriptor, null, gen.classNode, gen.bindingTrace, false);
			VariableNode variableNode = new VariableNode(newVariableDescriptor.isStatic() ? Modifier.list(Modifier.STATIC, Modifier.MUTABLE) : Modifier.list(Modifier.MUTABLE), newVariableDescriptor.getName(), gen.toAsmType(newVariableDescriptor.getType()));
			gen.classNode.addMember(variableNode);
			return true;
		}
		else
			return false;
	}

	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("{wrappedVariables=").append(wrappedVariables.size());
		sb.append(", wrappedOuterClasses=").append(wrappedOuterClasses.size());
		sb.append('}');
		return sb.toString();
	}

	@Override
	public ExpressionCodegenContext clone()
	{
		ExpressionCodegenContext context = empty();
		context.wrappedVariables.putAll(wrappedVariables);
		context.wrappedOuterClasses.putAll(wrappedOuterClasses);
		return context;
	}
}
