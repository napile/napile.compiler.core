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

/*
 * @author max
 */
package org.napile.compiler.psi;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.NapileFileType;
import org.napile.compiler.NapileLanguage;
import org.napile.compiler.NapileNodeTypes;
import org.napile.compiler.lang.psi.NapileImportDirective;
import org.napile.compiler.lang.psi.NapileNamespaceHeader;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import org.napile.compiler.lang.psi.stubs.NapilePsiFileStub;
import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;

public class NapileFileImpl extends PsiFileBase implements NapileFile
{
	public NapileFileImpl(FileViewProvider viewProvider)
	{
		super(viewProvider, NapileLanguage.INSTANCE);
	}

	@Override
	@NotNull
	public FileType getFileType()
	{
		return NapileFileType.INSTANCE;
	}

	@Override
	public String toString()
	{
		return "NapileFileImpl: " + getName();
	}

	@NotNull
	@Override
	public List<NapileClass> getDeclarations()
	{
		return PsiTreeUtil.getChildrenOfTypeAsList(this, NapileClass.class);
	}

	@NotNull
	@Override
	public List<NapileImportDirective> getImportDirectives()
	{
		return PsiTreeUtil.getChildrenOfTypeAsList(this, NapileImportDirective.class);
	}

	@Nullable
	@Override
	public NapileImportDirective findImportByAlias(@NotNull String name)
	{
		for(NapileImportDirective directive : getImportDirectives())
		{
			if(name.equals(directive.getAliasName()))
			{
				return directive;
			}
		}
		return null;
	}

	// scripts has no namespace header
	@Nullable
	@Override
	public NapileNamespaceHeader getNamespaceHeader()
	{
		ASTNode ast = getNode().findChildByType(NapileNodeTypes.NAMESPACE_HEADER);
		return ast != null ? (NapileNamespaceHeader) ast.getPsi() : null;
	}

	@Nullable
	@Override
	public String getPackageName()
	{
		NapilePsiFileStub stub = (NapilePsiFileStub) getStub();
		if(stub != null)
			return stub.getPackageName();

		NapileNamespaceHeader statement = getNamespaceHeader();
		return statement != null ? statement.getQualifiedName() : null;
	}

	@Override
	public void accept(@NotNull PsiElementVisitor visitor)
	{
		if(visitor instanceof NapileVisitorVoid)
		{
			((NapileVisitorVoid) visitor).visitNapileFile(this);
		}
		else
		{
			visitor.visitFile(this);
		}
	}
}
