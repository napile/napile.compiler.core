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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.NapileNodeTypes;
import com.intellij.lang.ASTNode;

/**
 * Type reference element.
 * Underlying token is {@link org.napile.compiler.NapileNodeTypes#TYPE_REFERENCE}
 *
 * @author max
 */
public class NapileTypeReference extends NapileElementImpl
{
	public NapileTypeReference(@NotNull ASTNode node)
	{
		super(node);
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitTypeReference(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitTypeReference(this, data);
	}

	public List<NapileAnnotationList> getAttributeAnnotations()
	{
		return findChildrenByType(NapileNodeTypes.ANNOTATION_LIST);
	}

	@Nullable
	public NapileTypeElement getTypeElement()
	{
		return findChildByClass(NapileTypeElement.class);
	}

	public List<NapileAnnotationEntry> getAnnotations()
	{
		List<NapileAnnotationEntry> answer = null;
		for(NapileAnnotationList annotationList : getAttributeAnnotations())
		{
			if(answer == null)
				answer = new ArrayList<NapileAnnotationEntry>();
			answer.addAll(annotationList.getEntries());
		}
		return answer != null ? answer : Collections.<NapileAnnotationEntry>emptyList();
	}
}
