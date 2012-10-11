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

package org.napile.compiler.lang.psi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.NapileNodeTypes;
import org.napile.compiler.lang.psi.stubs.NapilePsiMethodParameterStub;
import org.napile.compiler.lang.psi.stubs.elements.NapileStubElementTypes;
import org.napile.compiler.lexer.NapileTokens;
import org.napile.compiler.psi.NapileExpression;
import org.napile.compiler.psi.NapileModifierList;
import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.util.ArrayFactory;

/**
 * @author max
 */
public class NapilePropertyParameter extends NapileNamedDeclarationStub<NapilePsiMethodParameterStub>
{
	public static final NapilePropertyParameter[] EMPTY_ARRAY = new NapilePropertyParameter[0];

	public static final ArrayFactory<NapilePropertyParameter> ARRAY_FACTORY = new ArrayFactory<NapilePropertyParameter>()
	{
		@Override
		public NapilePropertyParameter[] create(final int count)
		{
			return count == 0 ? EMPTY_ARRAY : new NapilePropertyParameter[count];
		}
	};

	public NapilePropertyParameter(@NotNull ASTNode node)
	{
		super(node);
	}

	public NapilePropertyParameter(@NotNull NapilePsiMethodParameterStub stub, @NotNull IStubElementType nodeType)
	{
		super(stub, nodeType);
	}

	@NotNull
	@Override
	public IStubElementType getElementType()
	{
		return NapileStubElementTypes.VALUE_PARAMETER;
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitPropertyParameter(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitPropertyParameter(this, data);
	}

	@Nullable
	public NapileTypeReference getTypeReference()
	{
		return (NapileTypeReference) findChildByType(NapileNodeTypes.TYPE_REFERENCE);
	}

	@Nullable
	public NapileExpression getDefaultValue()
	{
		boolean passedEQ = false;
		ASTNode child = getNode().getFirstChildNode();
		while(child != null)
		{
			if(child.getElementType() == NapileTokens.EQ)
				passedEQ = true;
			if(passedEQ && child.getPsi() instanceof NapileExpression)
			{
				return (NapileExpression) child.getPsi();
			}
			child = child.getTreeNext();
		}

		return null;
	}

	public boolean isVarArg()
	{
		NapilePsiMethodParameterStub stub = getStub();
		if(stub != null)
		{
			return stub.isVarArg();
		}

		NapileModifierList modifierList = getModifierList();
		return modifierList != null && modifierList.getModifierNode(NapileTokens.VARARG_KEYWORD) != null;
	}

	@Nullable
	public ASTNode getVarNode()
	{
		return getNode().findChildByType(NapileTokens.VAR_KEYWORD);
	}
}
