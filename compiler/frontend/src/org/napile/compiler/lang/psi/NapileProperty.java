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

import static org.napile.compiler.NapileNodeTypes.PROPERTY_ACCESSOR;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.NapileNodeTypes;
import org.napile.compiler.lang.psi.stubs.PsiJetPropertyStub;
import org.napile.compiler.lang.psi.stubs.elements.JetStubElementTypes;
import org.napile.compiler.lexer.NapileTokens;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * @author max
 */
public class NapileProperty extends NapileTypeParameterListOwnerStub<PsiJetPropertyStub> implements NapileWithExpressionInitializer
{
	public NapileProperty(@NotNull ASTNode node)
	{
		super(node);
	}

	public NapileProperty(@NotNull PsiJetPropertyStub stub, @NotNull IStubElementType nodeType)
	{
		super(stub, nodeType);
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitProperty(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitProperty(this, data);
	}

	@NotNull
	@Override
	public IStubElementType getElementType()
	{
		return JetStubElementTypes.PROPERTY;
	}

	public boolean isLocal()
	{
		PsiElement parent = getParent();
		return !(parent instanceof NapileFile || parent instanceof NapileClassBody || parent instanceof NapileNamespaceBody);
	}

	@NotNull
	@Override
	public SearchScope getUseScope()
	{
		if(isLocal())
		{
			@SuppressWarnings("unchecked") PsiElement block = PsiTreeUtil.getParentOfType(this, NapileBlockExpression.class);
			if(block == null)
			{
				return super.getUseScope();
			}
			else
			{
				return new LocalSearchScope(block);
			}
		}
		else
		{
			return super.getUseScope();
		}
	}

	@Nullable
	public NapileTypeReference getPropertyTypeRef()
	{
		ASTNode node = getNode().getFirstChildNode();
		boolean passedColon = false;
		while(node != null)
		{
			IElementType tt = node.getElementType();
			if(tt == NapileTokens.COLON)
			{
				passedColon = true;
			}
			else if(tt == NapileNodeTypes.TYPE_REFERENCE && passedColon)
			{
				return (NapileTypeReference) node.getPsi();
			}
			node = node.getTreeNext();
		}

		return null;
	}

	@NotNull
	public List<NapilePropertyAccessor> getAccessors()
	{
		return findChildrenByType(PROPERTY_ACCESSOR);
	}

	@Nullable
	public NapilePropertyAccessor getGetter()
	{
		for(NapilePropertyAccessor accessor : getAccessors())
		{
			if(accessor.isGetter())
				return accessor;
		}

		return null;
	}

	@Nullable
	public NapilePropertyAccessor getSetter()
	{
		for(NapilePropertyAccessor accessor : getAccessors())
		{
			if(accessor.isSetter())
				return accessor;
		}

		return null;
	}

	@Override
	@Nullable
	public NapileExpression getInitializer()
	{
		return PsiTreeUtil.getNextSiblingOfType(findChildByType(NapileTokens.EQ), NapileExpression.class);
	}

	@NotNull
	public ASTNode getVarNode()
	{
		ASTNode node = getNode().findChildByType(NapileTokens.VAR_KEYWORD);
		assert node != null : "Var should always exist for property";
		return node;
	}
}
