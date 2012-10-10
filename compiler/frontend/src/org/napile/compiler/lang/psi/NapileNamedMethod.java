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

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.resolve.name.FqName;
import org.napile.compiler.NapileNodeTypes;
import org.napile.compiler.lang.psi.stubs.PsiJetFunctionStub;
import org.napile.compiler.lang.psi.stubs.elements.JetStubElementTypes;
import org.napile.compiler.lexer.NapileTokens;
import org.napile.compiler.psi.NapileElement;
import org.napile.compiler.psi.NapileExpression;
import org.napile.compiler.psi.NapileFile;
import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.ItemPresentationProviders;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * @author max
 */
public class NapileNamedMethod extends NapileTypeParameterListOwnerStub<PsiJetFunctionStub> implements NapileMethod, NapileWithExpressionInitializer
{
	private static final TokenSet SET_AND_GET_KEYWORDS = TokenSet.create(NapileTokens.SET_KEYWORD, NapileTokens.GET_KEYWORD);

	public NapileNamedMethod(@NotNull ASTNode node)
	{
		super(node);
	}

	public NapileNamedMethod(@NotNull PsiJetFunctionStub stub)
	{
		super(stub, JetStubElementTypes.METHOD);
	}

	@Override
	public String getName()
	{
		PsiJetFunctionStub stub = getStub();
		if(stub != null)
			return stub.getName();

		PsiElement psiElement = findChildByType(SET_AND_GET_KEYWORDS);
		if(psiElement != null)
		{
			NapileSimpleNameExpression ref = getVariableRef();
			assert ref != null;
			return ref.getReferencedName() + "$" + psiElement.getText();
		}
		else
		{
			PsiElement identifier = getNameIdentifier();
			if(identifier != null)
			{
				String text = identifier.getText();
				return text != null ? NapilePsiUtil.unquoteIdentifier(text) : null;
			}
			else
				return null;
		}
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitNamedMethod(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitNamedFunction(this, data);
	}

	@Override
	public boolean hasBlockBody()
	{
		return getEqualsToken() == null;
	}

	@Nullable
	public PsiElement getEqualsToken()
	{
		return findChildByType(NapileTokens.EQ);
	}

	@Override
	@Nullable
	public NapileExpression getInitializer()
	{
		return PsiTreeUtil.getNextSiblingOfType(getEqualsToken(), NapileExpression.class);
	}

	/**
	 * Returns full qualified name for function "package_fqn.function_name"
	 * Not null for top level functions.
	 *
	 * @return
	 */
	@Nullable
	public FqName getQualifiedName()
	{
		PsiElement parent = getParent();
		if(parent instanceof NapileFile)
		{
			// fqname is different in scripts
			if(((NapileFile) parent).getNamespaceHeader() == null)
			{
				return null;
			}
			NapileFile jetFile = (NapileFile) parent;
			final FqName fileFQN = NapilePsiUtil.getFQName(jetFile);
			return fileFQN.child(getNameAsName());
		}

		return null;
	}

	@NotNull
	@Override
	public IStubElementType getElementType()
	{
		return JetStubElementTypes.METHOD;
	}

	@Override
	public ItemPresentation getPresentation()
	{
		return ItemPresentationProviders.getItemPresentation(this);
	}

	@Nullable
	@Override
	public NapileSimpleNameExpression getVariableRef()
	{
		return (NapileSimpleNameExpression) findChildByType(NapileNodeTypes.VARIABLE_REFERENCE);
	}

	@Override
	@Nullable
	public NapileParameterList getValueParameterList()
	{
		return (NapileParameterList) findChildByType(NapileNodeTypes.VALUE_PARAMETER_LIST);
	}

	@Override
	@NotNull
	public List<NapileElement> getValueParameters()
	{
		NapileParameterList list = getValueParameterList();
		return list != null ? list.getParameters() : Collections.<NapileElement>emptyList();
	}

	@Override
	@Nullable
	public NapileExpression getBodyExpression()
	{
		NapileExpression[] ex = findChildrenByClass(NapileExpression.class);
		for(NapileExpression e : ex)
			if(e.getNode().getElementType() != NapileNodeTypes.VARIABLE_REFERENCE)
				return e;
		return null;
	}

	@Override
	public boolean hasDeclaredReturnType()
	{
		return getReturnTypeRef() != null;
	}

	@Override
	@Nullable
	public NapileTypeReference getReturnTypeRef()
	{
		boolean colonPassed = false;
		PsiElement child = getFirstChild();
		while(child != null)
		{
			IElementType tt = child.getNode().getElementType();
			if(tt == NapileTokens.COLON)
			{
				colonPassed = true;
			}
			if(colonPassed && child instanceof NapileTypeReference)
			{
				return (NapileTypeReference) child;
			}
			child = child.getNextSibling();
		}

		return null;
	}

	@NotNull
	@Override
	public NapileElement asElement()
	{
		return this;
	}

	@Override
	public boolean isLocal()
	{
		return false;
	}
}
