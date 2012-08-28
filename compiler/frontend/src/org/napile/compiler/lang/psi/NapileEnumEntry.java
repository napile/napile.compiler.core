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
import org.napile.compiler.NapileNodeTypes;
import org.napile.compiler.lang.psi.stubs.NapilePsiEnumEntryStub;
import org.napile.compiler.lang.psi.stubs.elements.JetStubElementTypes;
import org.napile.compiler.lang.resolve.name.FqName;
import com.intellij.lang.ASTNode;

/**
 * @author max
 */
public class NapileEnumEntry extends NapileNamedDeclarationStub<NapilePsiEnumEntryStub> implements NapileClassOrObject, NapileCallElement
{
	public NapileEnumEntry(@NotNull ASTNode node)
	{
		super(node);
	}

	public NapileEnumEntry(@NotNull NapilePsiEnumEntryStub stub)
	{
		super(stub, JetStubElementTypes.ENUM_ENTRY);
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

	@NotNull
	@Override
	public List<NapileClassInitializer> getAnonymousInitializers()
	{
		return Collections.emptyList();
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
	public NapileDelegationSpecifierList getDelegationSpecifierList()
	{
		return null;
	}

	@NotNull
	@Override
	public List<NapileDelegationSpecifier> getDelegationSpecifiers()
	{
		return Collections.emptyList();
	}

	@Nullable
	@Override
	public NapileExpression getCalleeExpression()
	{
		return null;
	}

	@Override
	@Nullable
	public NapileValueArgumentList getValueArgumentList()
	{
		return (NapileValueArgumentList) findChildByType(NapileNodeTypes.VALUE_ARGUMENT_LIST);
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
