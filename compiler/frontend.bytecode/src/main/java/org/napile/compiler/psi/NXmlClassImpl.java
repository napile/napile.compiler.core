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

package org.napile.compiler.psi;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.resolve.name.FqName;
import org.napile.asm.resolve.name.Name;
import org.napile.asm.tree.members.ClassNode;
import org.napile.compiler.lang.descriptors.ClassKind;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.lexer.NapileToken;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.util.IncorrectOperationException;

/**
 * @author VISTALL
 * @date 22:04/09.10.12
 */
public class NXmlClassImpl extends NXmlElementImpl implements NapileClass
{
	public NXmlClassImpl(PsiManager psiManager, ClassNode classNode)
	{
		super(psiManager);
	}

	@Override
	public void appendMirrorText(int indent, StringBuilder builder)
	{
	}

	@Override
	public String getQualifiedName()
	{
		return null;
	}

	@Override
	public ClassKind getKind()
	{
		return null;
	}

	@NotNull
	@Override
	public NapileConstructor[] getConstructors()
	{
		return new NapileConstructor[0];
	}

	@NotNull
	@Override
	public NapileStaticConstructor[] getStaticConstructors()
	{
		return new NapileStaticConstructor[0];
	}

	@NotNull
	@Override
	public List<String> getSuperNames()
	{
		return Collections.emptyList();
	}

	@Override
	public FqName getFqName()
	{
		return null;
	}

	@Nullable
	@Override
	public NapileObjectDeclarationName getNameAsDeclaration()
	{
		return null;
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

	@Nullable
	@Override
	public NapileClassBody getBody()
	{
		return null;
	}

	@NotNull
	@Override
	public List<NapileDeclaration> getDeclarations()
	{
		return Collections.emptyList();
	}

	@Nullable
	@Override
	public NapileTypeParameterList getTypeParameterList()
	{
		return null;
	}

	@NotNull
	@Override
	public List<NapileTypeParameter> getTypeParameters()
	{
		return Collections.emptyList();
	}

	@NotNull
	@Override
	public Name getNameAsSafeName()
	{
		return null;
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitClass(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitClass(this, data);
	}

	@Nullable
	@Override
	public NapileModifierList getModifierList()
	{
		return null;
	}

	@Override
	public boolean hasModifier(NapileToken modifier)
	{
		return false;
	}

	@Override
	public ASTNode getModifierNode(NapileToken token)
	{
		return null;
	}

	@Nullable
	@Override
	public Name getNameAsName()
	{
		return null;
	}

	@Nullable
	@Override
	public PsiElement getNameIdentifier()
	{
		return null;
	}

	@Override
	public PsiElement setName(@NonNls @NotNull String s) throws IncorrectOperationException
	{
		return null;
	}
}