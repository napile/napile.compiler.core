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
import org.napile.compiler.lang.lexer.NapileNodes;
import org.napile.compiler.lang.lexer.NapileTokens;
import com.google.common.collect.Lists;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceService;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * @author max
 */
public class NapileArrayAccessExpressionImpl extends NapileReferenceExpressionImpl
{
	public NapileArrayAccessExpressionImpl(@NotNull ASTNode node)
	{
		super(node);
	}

	@NotNull
	@Override
	public PsiReference[] getReferences()
	{
		return ReferenceProvidersRegistry.getReferencesFromProviders(this, PsiReferenceService.Hints.NO_HINTS);
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitArrayAccessExpression(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitArrayAccessExpression(this, data);
	}

	@NotNull
	public NapileExpression getArrayExpression()
	{
		NapileExpression baseExpression = findChildByClass(NapileExpression.class);
		assert baseExpression != null;
		return baseExpression;
	}

	@NotNull
	public List<NapileExpression> getIndexExpressions()
	{
		PsiElement container = getIndicesNode();
		return PsiTreeUtil.getChildrenOfTypeAsList(container, NapileExpression.class);
	}

	@NotNull
	public NapileContainerNode getIndicesNode()
	{
		return (NapileContainerNode) findChildByType(NapileNodes.INDICES);
	}

	public List<TextRange> getBracketRanges()
	{
		PsiElement lBracket = getIndicesNode().findChildByType(NapileTokens.LBRACKET);
		PsiElement rBracket = getIndicesNode().findChildByType(NapileTokens.RBRACKET);
		if(lBracket == null || rBracket == null)
		{
			return Collections.emptyList();
		}
		return Lists.newArrayList(lBracket.getTextRange(), rBracket.getTextRange());
	}
}
