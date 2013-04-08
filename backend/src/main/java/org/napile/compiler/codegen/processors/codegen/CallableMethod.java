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

package org.napile.compiler.codegen.processors.codegen;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.tree.members.bytecode.Instruction;
import org.napile.asm.tree.members.bytecode.MethodRef;
import org.napile.asm.tree.members.bytecode.adapter.InstructionAdapter;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.compiler.codegen.processors.PositionMarker;
import com.intellij.psi.PsiElement;

/**
 * @author VISTALL
 * @since 14:51/18.09.12
 */
public class CallableMethod
{
	public static enum CallType
	{
		SPECIAL,
		STATIC,
		VIRTUAL,
		ANONYM
	}

	private final MethodRef methodRef;
	private final CallType callType;
	private final TypeNode returnType;
	private final List<TypeNode> parameters;
	private final boolean macro;
	private final boolean nullable;

	public CallableMethod(@NotNull MethodRef methodRef, @NotNull CallType callType, TypeNode returnType, List<TypeNode> parameters, boolean macro, boolean nullable)
	{
		this.methodRef = methodRef;
		this.callType = callType;
		this.returnType = returnType;
		this.parameters = parameters;
		this.macro = macro;
		this.nullable = nullable;
	}

	public void newObject(@NotNull InstructionAdapter instructionAdapter, @NotNull PositionMarker marker,  @Nullable PsiElement target, @NotNull TypeNode typeNode)
	{
		Instruction instruction = instructionAdapter.newObject(typeNode, methodRef.parameters);

		marker.mark(instruction, target);
	}

	public void invoke(@NotNull InstructionAdapter instructionAdapter, @NotNull PositionMarker marker, @Nullable PsiElement target)
	{
		Instruction instruction = null;
		switch(callType)
		{
			case SPECIAL:
				if(macro)
					instruction = instructionAdapter.macroJump(methodRef);
				else
					instruction = instructionAdapter.invokeSpecial(methodRef, nullable);
				break;
			case STATIC:
				if(macro)
					instruction = instructionAdapter.macroStaticJump(methodRef);
				else
					instruction = instructionAdapter.invokeStatic(methodRef, nullable);
				break;
			case VIRTUAL:
				instruction = instructionAdapter.invokeVirtual(methodRef, nullable);
				break;
			case ANONYM:
				instruction = instructionAdapter.invokeAnonym(methodRef.parameters, methodRef.typeArguments, methodRef.returnType, nullable);
				break;
		}

		marker.mark(instruction, target);
	}

	public TypeNode getReturnType()
	{
		return returnType;
	}

	public List<TypeNode> getValueParameterTypes()
	{
		return parameters;
	}

	public String getName()
	{
		return methodRef.method.shortName().getName();
	}

	public CallType getCallType()
	{
		return callType;
	}
}
