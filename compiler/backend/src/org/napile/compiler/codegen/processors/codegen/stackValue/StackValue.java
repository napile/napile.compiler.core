/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.napile.compiler.codegen.processors.codegen.stackValue;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.resolve.name.FqName;
import org.napile.asm.tree.members.bytecode.adapter.InstructionAdapter;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.compiler.codegen.processors.ExpressionGenerator;
import org.napile.compiler.codegen.processors.TypeTransformer;
import org.napile.compiler.codegen.processors.codegen.CallableMethod;
import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.PropertyDescriptor;
import org.napile.compiler.lang.resolve.DescriptorUtils;
import org.napile.compiler.lang.resolve.calls.ResolvedCall;

/**
 * @author yole
 * @author alex.tkachman
 */
public abstract class StackValue
{
	public static StackValue none()
	{
		return None.INSTANCE;
	}

	public static Local local(int index, TypeNode type)
	{
		return new Local(index, type);
	}

	public static Constant constant(Object value, TypeNode typeNode)
	{
		return new Constant(value, typeNode);
	}

	public static StackValue receiver(ResolvedCall<? extends CallableDescriptor> resolvedCall, StackValue receiver, ExpressionGenerator codegen, @Nullable CallableMethod callableMethod)
	{
		if(resolvedCall.getThisObject().exists())
			return new CallReceiver(resolvedCall, receiver, codegen, callableMethod);

		return receiver;
	}

	public static StackValue thisOrOuter(ExpressionGenerator codegen, ClassDescriptor descriptor, boolean isSuper)
	{
		return new ThisOuter(codegen, descriptor, isSuper);
	}

	public static StackValue onStack(TypeNode type)
	{
		return new OnStack(type);
	}

	@NotNull
	public static StackValue variable(@NotNull PropertyDescriptor propertyDescriptor)
	{
		return new Variable(DescriptorUtils.getFQName(propertyDescriptor).toSafe(), TypeTransformer.toAsmType(propertyDescriptor.getType()), propertyDescriptor.isStatic());
	}

	@NotNull
	public static StackValue variable(@NotNull FqName fqName, @NotNull TypeNode type, boolean staticVar)
	{
		return new Variable(fqName, type, staticVar);
	}

	public static StackValue property(@NotNull PropertyDescriptor propertyDescriptor)
	{
		return property(DescriptorUtils.getFQName(propertyDescriptor).toSafe(), TypeTransformer.toAsmType(propertyDescriptor.getType()), propertyDescriptor.isStatic());
	}

	public static StackValue property(@NotNull FqName fqName, @NotNull TypeNode type, boolean staticVar)
	{
		return new Property(fqName, type, staticVar);
	}

	public static StackValue collectionElement(@NotNull TypeNode typeNode, ResolvedCall<MethodDescriptor> getCall, ResolvedCall<MethodDescriptor> setCall, ExpressionGenerator expressionGenerator)
	{
		return new CollectionElement(typeNode, getCall, setCall, expressionGenerator);
	}


	private final TypeNode type;

	protected StackValue(TypeNode type)
	{
		this.type = type;
	}

	public abstract void put(TypeNode type, InstructionAdapter instructionAdapter);

	/**
	 * This method is called to put the value on the top of the JVM stack if <code>depth</code> other values have been put on the
	 * JVM stack after this value was generated.
	 *
	 * @param type  the type as which the value should be put
	 * @param v     the visitor used to generate the instructions
	 * @param depth the number of new values put onto the stack
	 */
	protected void moveToTopOfStack(TypeNode type, InstructionAdapter v, int depth)
	{
		put(type, v);
	}

	public void store(TypeNode topOfStackType, InstructionAdapter instructionAdapter)
	{
		throw new UnsupportedOperationException("cannot store to value " + this);
	}

	public int receiverSize()
	{
		return 0;
	}

	public void dupReceiver(InstructionAdapter v)
	{
	}

	protected void castTo(TypeNode toType, InstructionAdapter v)
	{
		castTo(this.type, toType, v);
	}

	public static void castTo(TypeNode fromType, TypeNode toType, InstructionAdapter v)
	{

	}

	public TypeNode getType()
	{
		return type;
	}
}
