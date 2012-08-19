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

package org.napile.idea.plugin.projectView;

import static org.napile.idea.plugin.projectView.JetProjectViewUtil.canRepresentPsiElement;
import static org.napile.idea.plugin.projectView.JetProjectViewUtil.getClassOrObjectChildren;

import java.util.Collection;

import org.napile.compiler.lang.psi.NapileClassOrObject;
import org.napile.compiler.lang.psi.NapileTypeParameter;
import org.napile.compiler.lang.psi.NapileTypeParameterListOwner;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.AbstractPsiBasedNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.util.Function;

/**
 * User: Alefas
 * Date: 15.02.12
 */
public class JetClassOrObjectTreeNode extends AbstractPsiBasedNode<NapileClassOrObject>
{
	protected JetClassOrObjectTreeNode(Project project, NapileClassOrObject jetClassOrObject, ViewSettings viewSettings)
	{
		super(project, jetClassOrObject, viewSettings);
	}

	@Override
	protected PsiElement extractPsiFromValue()
	{
		return getValue();
	}

	@Override
	protected Collection<AbstractTreeNode> getChildrenImpl()
	{
		return getClassOrObjectChildren(getValue(), getProject(), getSettings());
	}

	@Override
	protected void updateImpl(PresentationData data)
	{
		NapileClassOrObject classOrObject = getValue();
		if(classOrObject != null)
		{
			if(classOrObject instanceof NapileTypeParameterListOwner && !((NapileTypeParameterListOwner) classOrObject).getTypeParameters().isEmpty())
			{
				StringBuilder builder = new StringBuilder(classOrObject.getName());
				builder.append("<");
				builder.append(StringUtil.join(((NapileTypeParameterListOwner) classOrObject).getTypeParameters(), new Function<NapileTypeParameter, String>()
				{
					@Override
					public String fun(NapileTypeParameter jetTypeParameter)
					{
						return jetTypeParameter.getName();
					}
				}, ", "));
				builder.append(">");
				data.setPresentableText(builder.toString());
			}
			else
				data.setPresentableText(classOrObject.getName());

			ProjectView.getInstance(getProject()).getCurrentProjectViewPane().getTreeBuilder().addSubtreeToUpdateByElement(data);
		}
	}

	@Override
	public boolean canRepresent(Object element)
	{
		if(!isValid())
		{
			return false;
		}

		return super.canRepresent(element) || canRepresentPsiElement(getValue(), element, getSettings());
	}

	@Override
	public int getWeight()
	{
		return 20;
	}
}
