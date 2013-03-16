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

package org.napile.doc.compiler.info;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.resolve.BindingContext;
import org.pegdown.PegDownProcessor;
import com.intellij.psi.PsiElement;

/**
 * @author VISTALL
 * @since 14:54/01.02.13
 */
public abstract class DocableInfo<E extends PsiElement> implements Comparable<DocableInfo<?>>
{
	protected static final PegDownProcessor PEG_DOWN_PROCESSOR = new PegDownProcessor();

	protected final BindingContext bindingContext;

	public final E element;

	public DocableInfo(BindingContext bindingContext, E element)
	{
		this.bindingContext = bindingContext;
		this.element = element;
	}

	@NotNull
	public abstract String getName();

	@NotNull
	public abstract String getDoc();

	@Override
	public int compareTo(DocableInfo<?> o)
	{
		return getName().compareTo(o.getName());
	}

	public BindingContext getBindingContext()
	{
		return bindingContext;
	}
}
