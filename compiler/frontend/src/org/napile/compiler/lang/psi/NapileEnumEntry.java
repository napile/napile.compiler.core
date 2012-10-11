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
import org.napile.compiler.lang.psi.stubs.NapilePsiEnumEntryStub;
import org.napile.compiler.lang.psi.stubs.elements.NapileStubElementTypes;
import org.napile.compiler.psi.NapileClassLike;
import org.napile.compiler.psi.NapileDeclaration;
import org.napile.compiler.psi.NapileElement;
import org.napile.compiler.psi.NapileExpression;
import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * @author max
 */
public class NapileEnumEntry extends NapileNamedDeclarationStub<NapilePsiEnumEntryStub> implements NapileClassLike, NapileCallElement
{
	public NapileEnumEntry(@NotNull ASTNode node)
	{
		super(node);
	}

	public NapileEnumEntry(@NotNull NapilePsiEnumEntryStub stub)
	{
		super(stub, NapileStubElementTypes.ENUM_ENTRY);
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitEnumEntry(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitEnumEntry(this, data);
	}

	@Override
	public FqName getFqName()
	{
		return NapilePsiUtil.getFQName(this);
	}

	@Override
	public NapileObjectDeclarationName getNameAsDeclaration()
	{
		return (NapileObjectDeclarationName) findChildByType(NapileNodeTypes.OBJECT_DECLARATION_NAME);
	}

	@Override
	public NapileElement getExtendTypeListElement()
	{
		return null;
	}

	@NotNull
	@Override
	public List<NapileTypeReference> getExtendTypeList()
	{
		return Collections.emptyList();
	}

	@Override
	public NapileClassBody getBody()
	{
		return (NapileClassBody) findChildByType(NapileNodeTypes.CLASS_BODY);
	}

	@NotNull
	@Override
	public List<NapileDeclaration> getDeclarations()
	{
		NapileClassBody body = getBody();
		if(body == null)
			return Collections.emptyList();

		return body.getDeclarations();
	}

	@Nullable
	@Override
	public NapileExpression getCalleeExpression()
	{
		return (NapileExpression) findChildByType(NapileNodeTypes.CONSTRUCTOR_CALLEE);
	}

	@Override
	@Nullable
	public NapileValueArgumentList getValueArgumentList()
	{
		NapileExpression expression = getCalleeExpression();
		if(expression == null)
			return null;
		return PsiTreeUtil.findChildOfType(expression, NapileValueArgumentList.class);
	}

	@Override
	@NotNull
	public List<? extends ValueArgument> getValueArguments()
	{
		NapileValueArgumentList list = getValueArgumentList();
		return list != null ? list.getArguments() : Collections.<NapileValueArgument>emptyList();
	}

	@NotNull
	@Override
	public List<NapileExpression> getFunctionLiteralArguments()
	{
		return Collections.emptyList();
	}

	@NotNull
	@Override
	public List<NapileTypeReference> getTypeArguments()
	{
		NapileTypeArgumentList typeArgumentList = getTypeArgumentList();
		return typeArgumentList != null ? typeArgumentList.getArguments() : Collections.<NapileTypeReference>emptyList();
	}

	@Nullable
	@Override
	public NapileTypeArgumentList getTypeArgumentList()
	{
		return (NapileTypeArgumentList) findChildByType(NapileNodeTypes.TYPE_ARGUMENT_LIST);
	}
}
