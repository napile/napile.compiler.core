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

package org.napile.compiler.lang.psi.impl;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.psi.NXmlParentedElementBase;
import org.napile.compiler.lang.psi.NapileCallParameter;
import org.napile.compiler.lang.psi.NapileCallParameterList;
import org.napile.compiler.lang.psi.NapileVisitor;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.impl.source.tree.TreeElement;

/**
 * @author VISTALL
 * @since 14:50/17.02.13
 */
public class NXmlCallParameterListImpl extends NXmlParentedElementBase implements NapileCallParameterList
{
	private NapileCallParameter[] parameters;

	public NXmlCallParameterListImpl(PsiElement parent, PsiElement mirror)
	{
		super(parent, mirror);
	}

	@Override
	public void setMirror(@NotNull TreeElement element) throws InvalidMirrorException
	{
		NapileCallParameterList mirror = SourceTreeToPsiMap.treeToPsiNotNull(element);

		setMirrorCheckingType(element, null);

		final NapileCallParameter[] mirrorParameters = mirror.getParameters();
		parameters = new NapileCallParameter[mirrorParameters.length];
		for(int i = 0; i < parameters.length; i++)
		{
			parameters[i] = new NXmlCallParameterAsVariableImpl(this, mirrorParameters[i]);
		}
	}

	@Override
	@NotNull
	public NapileCallParameter[] getParameters()
	{
		return parameters;
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitCallParameterList(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitCallParameterList(this, data);
	}

	@NotNull
	@Override
	public PsiElement[] getChildren()
	{
		return getParameters();
	}
}
