/*
 * Copyright 2010-2013 napile.org
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

package org.napile.idea.plugin.editor.highlight.postHighlight;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.lib.NapileAnnotationPackage;
import org.napile.compiler.lang.descriptors.annotations.Annotated;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import org.napile.compiler.lang.resolve.AnnotationUtils;
import org.napile.compiler.lang.resolve.BindingTrace;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.HighlightInfoType;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;

/**
 * @author VISTALL
 * @since 20:25/26.02.13
 */
public class PostHighlightVisitor extends NapileVisitorVoid
{
	protected final BindingTrace bindingTrace;
	protected final Collection<HighlightInfo> holder;

	public PostHighlightVisitor(BindingTrace context, Collection<HighlightInfo> holder)
	{
		this.bindingTrace = context;
		this.holder = holder;
	}

	protected void highlight(HighlightInfoType type, PsiElement element, @Nullable String text, @Nullable TextAttributesKey key)
	{
		HighlightInfo.Builder builder = HighlightInfo.newHighlightInfo(type);
		builder.range(element);
		if(text != null)
			builder.escapedToolTip(text);
		if(key != null)
			builder.textAttributes(key);

		final HighlightInfo e = builder.create();
		if(e != null)
			holder.add(e);
	}

	protected void highlight(HighlightInfoType type, TextRange range, @Nullable String text, @Nullable TextAttributesKey key)
	{
		HighlightInfo.Builder builder = HighlightInfo.newHighlightInfo(type);
		builder.range(range);
		if(text != null)
			builder.escapedToolTip(text);
		if(key != null)
			builder.textAttributes(key);

		final HighlightInfo e = builder.create();
		if(e != null)
			holder.add(e);
	}

	protected void highlightInfo(PsiElement element, @Nullable String text, @Nullable TextAttributesKey key)
	{
		highlight(HighlightInfoType.INFORMATION, element, text, key);
	}

	protected void highlightInfo(TextRange textRange, @Nullable String text, @Nullable TextAttributesKey key)
	{
		highlight(HighlightInfoType.INFORMATION, textRange, text, key);
	}

	protected void highlightInfo(ASTNode element, @Nullable String text, @Nullable TextAttributesKey key)
	{
		HighlightInfo.Builder builder = HighlightInfo.newHighlightInfo(HighlightInfoType.INFORMATION);
		builder.range(element);
		if(text != null)
			builder.escapedToolTip(text);
		if(key != null)
			builder.textAttributes(key);

		final HighlightInfo e = builder.create();
		if(e != null)
			holder.add(e);
	}

	protected void highlightName(@NotNull PsiElement element, @NotNull TextAttributesKey attributesKey, Annotated annotated)
	{
		highlightInfo(element, null, attributesKey);

		if(annotated != null && AnnotationUtils.hasAnnotation(annotated, NapileAnnotationPackage.DEPRECATED))
			highlight(HighlightInfoType.DEPRECATED, element, null, null);
	}

	@Override
	public void visitElement(PsiElement element)
	{
		element.acceptChildren(this);
	}
}
