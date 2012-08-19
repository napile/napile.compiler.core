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
import org.napile.compiler.lang.psi.stubs.PsiJetParameterStub;
import org.napile.compiler.lang.psi.stubs.elements.JetStubElementTypes;
import org.napile.compiler.lexer.JetTokens;
import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.util.ArrayFactory;

/**
 * @author max
 */
public class NapileParameter extends NapileNamedDeclarationStub<PsiJetParameterStub>
{
	public static final NapileParameter[] EMPTY_ARRAY = new NapileParameter[0];

	public static final ArrayFactory<NapileParameter> ARRAY_FACTORY = new ArrayFactory<NapileParameter>()
	{
		@Override
		public NapileParameter[] create(final int count)
		{
			return count == 0 ? EMPTY_ARRAY : new NapileParameter[count];
		}
	};

	public NapileParameter(@NotNull ASTNode node)
	{
		super(node);
	}

	public NapileParameter(@NotNull PsiJetParameterStub stub, @NotNull IStubElementType nodeType)
	{
		super(stub, nodeType);
	}

	@NotNull
	@Override
	public IStubElementType getElementType()
	{
		return JetStubElementTypes.VALUE_PARAMETER;
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitParameter(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitParameter(this, data);
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
			if(child.getElementType() == JetTokens.EQ)
				passedEQ = true;
			if(passedEQ && child.getPsi() instanceof NapileExpression)
			{
				return (NapileExpression) child.getPsi();
			}
			child = child.getTreeNext();
		}

		return null;
	}

	public boolean isMutable()
	{
		PsiJetParameterStub stub = getStub();
		if(stub != null)
		{
			return stub.isMutable();
		}

		return findChildByType(JetTokens.VAR_KEYWORD) != null;
	}

	public boolean isVarArg()
	{
		PsiJetParameterStub stub = getStub();
		if(stub != null)
		{
			return stub.isVarArg();
		}

		NapileModifierList modifierList = getModifierList();
		return modifierList != null && modifierList.getModifierNode(JetTokens.VARARG_KEYWORD) != null;
	}

	@Nullable
	public ASTNode getValOrVarNode()
	{
		ASTNode val = getNode().findChildByType(JetTokens.VAL_KEYWORD);
		if(val != null)
			return val;

		return getNode().findChildByType(JetTokens.VAR_KEYWORD);
	}
}
