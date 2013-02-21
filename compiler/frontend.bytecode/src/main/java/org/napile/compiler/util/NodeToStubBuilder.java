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

package org.napile.compiler.util;

import java.util.HashMap;
import java.util.Map;

import org.napile.asm.AsmConstants;
import org.napile.asm.Modifier;
import org.napile.asm.resolve.name.Name;
import org.napile.asm.tree.members.AbstractMemberNode;
import org.napile.asm.tree.members.ClassNode;
import org.napile.asm.tree.members.MacroNode;
import org.napile.asm.tree.members.MethodNode;
import org.napile.asm.tree.members.MethodParameterNode;
import org.napile.asm.tree.members.TypeParameterNode;
import org.napile.asm.tree.members.VariableNode;
import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.compiler.lang.psi.stubs.*;
import org.napile.compiler.lang.psi.stubs.elements.NapileModifierListElementType;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.ArrayUtil;

/**
 * @author VISTALL
 * @date 13:19/16.02.13
 */
public class NodeToStubBuilder extends DummyNodeVisitor<StubElement>
{
	private static final Map<Modifier, IElementType> MODIFIER_TO_ELEMENT = new HashMap<Modifier, IElementType>();

	static
	{
		final IElementType[] types = NapileTokens.MODIFIER_KEYWORDS.getTypes();

		for(Modifier modifier : Modifier.values())
		{
			String modifierName = modifier.name().toLowerCase();

			for(IElementType elementType : types)
			{
				if(elementType.toString().equals(modifierName))
				{
					MODIFIER_TO_ELEMENT.put(modifier, elementType);
					break;
				}
			}
		}
	}

	@Override
	public Void visitClassNode(ClassNode classNode, StubElement a2)
	{
		if(classNode.name.shortName().getName().contains(AsmConstants.ANONYM_SPLITTER))
			return null;

		NapilePsiClassStub classStub = new NapilePsiClassStub(a2, classNode.name.getFqName(), classNode.name.shortName().getName());

		for(AbstractMemberNode<?> memberNode : classNode.getMembers())
		{
			memberNode.accept(this, classStub);
		}

		acceptTypeParameterList(classNode, classStub);

		acceptModifierList(classNode, classStub);

		return null;
	}

	@Override
	public Void visitMethodNode(MethodNode methodNode, StubElement a2)
	{
		final Name name = methodNode.name;

		StubElement<?> methodStub = null;
		if(name.equals(MethodNode.CONSTRUCTOR_NAME) || name.equals(MethodNode.STATIC_CONSTRUCTOR_NAME))
		{
			methodStub = new NapilePsiConstructorStub(a2);
		}
		else if(!name.getName().contains(AsmConstants.ANONYM_SPLITTER))
		{
			methodStub = new NapilePsiMethodStub(a2, name.getName());

			acceptTypeParameterList(methodNode, methodStub);
		}

		NapilePsiCallParameterListStub list = new NapilePsiCallParameterListStub(methodStub);
		for(MethodParameterNode node : methodNode.parameters)
		{
			new NapilePsiCallParameterAsVariableStub(list, node.name.getName());
		}

		acceptModifierList(methodNode, methodStub);

		return null;
	}

	@Override
	public Void visitMacroNode(MacroNode methodNode, StubElement a2)
	{
		NapilePsiMacroStub macroStub = new NapilePsiMacroStub(a2, methodNode.name.getName());

		acceptTypeParameterList(methodNode, macroStub);

		NapilePsiCallParameterListStub list = new NapilePsiCallParameterListStub(macroStub);
		for(MethodParameterNode node : methodNode.parameters)
		{
			new NapilePsiCallParameterAsVariableStub(list, node.name.getName());
		}

		acceptModifierList(methodNode, macroStub);

		return null;
	}

	@Override
	public Void visitVariableNode(VariableNode variableNode, StubElement a2)
	{
		if(ArrayUtil.contains(Modifier.ENUM, variableNode.modifiers))
		{
			final NapilePsiEnumValueStub variableStub = new NapilePsiEnumValueStub(a2, variableNode.name.getName());

			acceptModifierList(variableNode, variableStub);
		}
		else
		{
			final NapilePsiVariableStub variableStub = new NapilePsiVariableStub(a2, variableNode.name.getName());

			acceptModifierList(variableNode, variableStub);
		}

		return null;
	}

	private void acceptModifierList(AbstractMemberNode<?> memberNode, StubElement parent)
	{
		int modifiers = 0;

		for(Modifier modifier : memberNode.modifiers)
		{
			IElementType elementType = MODIFIER_TO_ELEMENT.get(modifier);
			if(elementType != null)
				modifiers |= NapileModifierListElementType.ELEMENT_TO_MASK.get(elementType);
		}
		new NapilePsiModifierListStub(parent, modifiers);
	}

	private void acceptTypeParameterList(AbstractMemberNode<?> memberNode, StubElement parent)
	{
		NapilePsiTypeParameterListStub stub = new NapilePsiTypeParameterListStub(parent);
		for(TypeParameterNode typeParameterNode : memberNode.typeParameters)
		{
			typeParameterNode.accept(this, stub);
		}
	}

	@Override
	public Void visitTypeParameter(TypeParameterNode typeParameterNode, StubElement a2)
	{
		NapilePsiTypeParameterStub stub = new NapilePsiTypeParameterStub(a2, typeParameterNode.name.getName());
		return null;
	}
}
