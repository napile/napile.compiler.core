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

package org.napile.compiler.lang.psi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.NapileLanguage;
import org.napile.compiler.lang.psi.impl.NXmlFileImpl;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.PsiElementBase;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ReflectionCache;

/**
 * @author VISTALL
 * @date 19:36/09.10.12
 */
public abstract class NXmlElementBase extends PsiElementBase implements PsiCompiledElement, NapileElement
{
	public static final Key<PsiCompiledElement> COMPILED_ELEMENT = Key.create("COMPILED_ELEMENT");
	private static final List EMPTY = Collections.emptyList();

	protected static class InvalidMirrorException extends RuntimeException
	{
		public InvalidMirrorException(@NotNull @NonNls String message)
		{
			super(message);
		}

		public InvalidMirrorException(@Nullable PsiElement stubElement, @Nullable PsiElement mirrorElement)
		{
			this("stub:" + stubElement + "; mirror:" + mirrorElement);
		}

		public InvalidMirrorException(@NotNull PsiElement[] stubElements, @NotNull PsiElement[] mirrorElements)
		{
			this("stub:" + Arrays.toString(stubElements) + "; mirror:" + Arrays.toString(mirrorElements));
		}

		public InvalidMirrorException(@NotNull List<? extends PsiElement> stubElements, @NotNull PsiElement[] mirrorElements)
		{
			this("stub:" + stubElements + "; mirror:" + Arrays.toString(mirrorElements));
		}
	}

	private static final Logger LOGGER = Logger.getInstance(NXmlElementBase.class);

	private volatile TreeElement myMirror = null;

	@Override
	public final void accept(@NotNull PsiElementVisitor visitor)
	{
		if(visitor instanceof NapileVisitorVoid)
		{
			accept((NapileVisitorVoid) visitor);
		}
		else
		{
			visitor.visitElement(this);
		}
	}

	@Override
	public <D> void acceptChildren(@NotNull NapileTreeVisitor<D> visitor, D data)
	{
		PsiElement child = getFirstChild();
		while(child != null)
		{
			if(child instanceof NapileElement)
				((NapileElement) child).accept(visitor, data);
			child = child.getNextSibling();
		}
	}


	public abstract void setMirror(@NotNull TreeElement element) throws InvalidMirrorException;

	@Override
	@NotNull
	public PsiElement getMirror()
	{
		TreeElement mirror = myMirror;
		if(mirror == null)
		{
			((NXmlFileImpl) getContainingFile()).getMirror();
			mirror = myMirror;
		}
		return SourceTreeToPsiMap.treeElementToPsi(mirror);
	}

	@NotNull
	@Override
	public Language getLanguage()
	{
		return NapileLanguage.INSTANCE;
	}

	@Override
	public final boolean isWritable()
	{
		return false;
	}

	@Override
	public boolean isPhysical()
	{
		return true;
	}

	@Override
	public boolean isValid()
	{
		return true;
	}

	@Override
	public PsiManager getManager()
	{
		final PsiFile file = getContainingFile();
		if(file == null)
			throw new PsiInvalidElementAccessException(this);
		return file.getManager();
	}

	@Override
	public abstract PsiElement getParent();

	@Override
	public final TextRange getTextRange()
	{
		PsiElement mirror = getMirror();
		return mirror != null ? mirror.getTextRange() : TextRange.EMPTY_RANGE;
	}

	@Override
	public final int getStartOffsetInParent()
	{
		PsiElement mirror = getMirror();
		return mirror != null ? mirror.getStartOffsetInParent() : -1;
	}

	@Override
	public int getTextLength()
	{
		String text = getText();
		if(text == null)
		{
			LOGGER.error("getText() == null, element = " + this + ", parent = " + getParent());
			return 0;
		}
		return text.length();
	}

	@Override
	public PsiElement findElementAt(int offset)
	{
		PsiElement mirrorAt = getMirror().findElementAt(offset);
		while(true)
		{
			if(mirrorAt == null)
				return null;
			PsiElement elementAt = mirrorToElement(mirrorAt);
			if(elementAt != null)
				return elementAt;
			mirrorAt = mirrorAt.getParent();
		}
	}

	@Override
	public PsiReference findReferenceAt(int offset)
	{
		PsiReference mirrorRef = getMirror().findReferenceAt(offset);
		if(mirrorRef == null)
			return null;
		PsiElement mirrorElement = mirrorRef.getElement();
		PsiElement element = mirrorToElement(mirrorElement);
		if(element == null)
			return null;
		return element.getReference();
	}

	@Nullable
	private PsiElement mirrorToElement(PsiElement mirror)
	{
		final PsiElement m = getMirror();
		if(m == mirror)
			return this;

		PsiElement[] children = getChildren();
		if(children.length == 0)
			return null;

		for(PsiElement child : children)
		{
			NXmlElementBase elementBase = (NXmlElementBase) child;
			if(PsiTreeUtil.isAncestor(elementBase.getMirror(), mirror, false))
			{
				PsiElement element = elementBase.mirrorToElement(mirror);
				if(element != null)
					return element;
			}
		}

		return null;
	}

	@Override
	public final int getTextOffset()
	{
		PsiElement mirror = getMirror();
		return mirror != null ? mirror.getTextOffset() : -1;
	}

	@Override
	public String getText()
	{
		PsiElement mirror = getMirror();
		return mirror != null ? mirror.getText() : null;
	}

	@Override
	@NotNull
	public char[] textToCharArray()
	{
		return getMirror().textToCharArray();
	}

	@Override
	public boolean textMatches(@NotNull CharSequence text)
	{
		return getText().equals(text.toString());
	}

	@Override
	public boolean textMatches(@NotNull PsiElement element)
	{
		return getText().equals(element.getText());
	}

	@Override
	public PsiElement copy()
	{
		return null;
	}

	@Override
	public PsiElement add(@NotNull PsiElement psiElement) throws IncorrectOperationException
	{
		return null;
	}

	@Override
	public PsiElement addBefore(@NotNull PsiElement psiElement, @Nullable PsiElement psiElement1) throws IncorrectOperationException
	{
		return null;
	}

	@Override
	public PsiElement addAfter(@NotNull PsiElement psiElement, @Nullable PsiElement psiElement1) throws IncorrectOperationException
	{
		return null;
	}

	@Override
	public void checkAdd(@NotNull PsiElement psiElement) throws IncorrectOperationException
	{
	}

	@Override
	public void delete() throws IncorrectOperationException
	{
	}

	@Override
	public void checkDelete() throws IncorrectOperationException
	{
	}

	@Override
	public PsiElement replace(@NotNull PsiElement psiElement) throws IncorrectOperationException
	{
		return null;
	}

	@Override
	public ASTNode getNode()
	{
		return null;
	}

	@Override
	public NapileFile getContainingFile()
	{
		return (NapileFile) super.getContainingFile();
	}

	protected void setMirrorCheckingType(@NotNull TreeElement element, @Nullable IElementType type) throws InvalidMirrorException
	{
		if(type != null && element.getElementType() != type)
		{
			throw new InvalidMirrorException(element.getElementType() + " != " + type);
		}

		element.putUserData(COMPILED_ELEMENT, this);

		myMirror = element;
	}

	protected static <T extends PsiElement> void setMirrors(@NotNull T[] stubs, @NotNull T[] mirrors) throws InvalidMirrorException
	{
		if(stubs.length != mirrors.length)
		{
			//			throw new InvalidMirrorException(stubs, mirrors);
			LOGGER.warn("stub:" + Arrays.toString(stubs) + "; mirror:" + Arrays.toString(mirrors));
			return;
		}
		for(int i = 0; i < stubs.length; i++)
		{
			setMirror(stubs[i], mirrors[i]);
		}
	}

	protected static <T extends PsiElement> void setMirrorIfPresent(@Nullable T stub, @Nullable T mirror) throws InvalidMirrorException
	{
		if((stub == null) != (mirror == null))
		{
			throw new InvalidMirrorException(stub, mirror);
		}
		else if(stub != null)
		{
			((NXmlElementBase) stub).setMirror(SourceTreeToPsiMap.psiToTreeNotNull(mirror));
		}
	}

	protected static <T extends PsiElement> void setMirror(@Nullable T stub, @Nullable T mirror) throws InvalidMirrorException
	{
		if(stub == null || mirror == null)
		{
			throw new InvalidMirrorException(stub, mirror);
		}
		((NXmlElementBase) stub).setMirror(SourceTreeToPsiMap.psiToTreeNotNull(mirror));
	}

	@NotNull
	protected <T> List<T> findChildrenByClassAsList(Class<T> aClass)
	{
		List<T> result = EMPTY;
		for(PsiElement cur = getFirstChild(); cur != null; cur = cur.getNextSibling())
		{
			if(ReflectionCache.isInstance(cur, aClass))
			{
				if(result == EMPTY)
				{
					result = new ArrayList<T>();
				}
				result.add((T) cur);
			}
		}
		return result;
	}
}
