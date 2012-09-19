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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.napile.compiler.lang.psi.NapileConstructor;
import org.napile.compiler.lang.psi.NapileDeclaration;
import org.napile.compiler.lang.psi.NapileElement;
import org.napile.compiler.lang.psi.NapileMethod;
import org.napile.compiler.lang.psi.NapileProperty;
import org.napile.compiler.lang.psi.NapilePropertyParameter;
import org.napile.compiler.lang.psi.NapileReferenceParameter;
import org.napile.compiler.lang.psi.NapileTypeReference;
import org.napile.idea.plugin.formatter.JetCodeStyleSettings;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.AbstractPsiBasedNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;

/**
 * User: Alefas
 * Date: 15.02.12
 */
public class JetDeclarationTreeNode extends AbstractPsiBasedNode<NapileDeclaration>
{
	protected JetDeclarationTreeNode(Project project, NapileDeclaration jetDeclaration, ViewSettings viewSettings)
	{
		super(project, jetDeclaration, viewSettings);
	}

	@Override
	protected PsiElement extractPsiFromValue()
	{
		return getValue();
	}

	@Override
	protected Collection<AbstractTreeNode> getChildrenImpl()
	{
		return Collections.emptyList();
	}

	@Override
	protected void updateImpl(PresentationData data)
	{
		NapileDeclaration declaration = getValue();
		if(declaration != null)
		{
			String text = declaration.getName();
			if(text == null)
				return;
			JetCodeStyleSettings settings = CodeStyleSettingsManager.getInstance(getProject()).getCurrentSettings().getCustomSettings(JetCodeStyleSettings.class);
			if(declaration instanceof NapileProperty)
			{
				NapileProperty property = (NapileProperty) declaration;
				NapileTypeReference ref = property.getPropertyTypeRef();
				if(ref != null)
				{
					if(settings.SPACE_BEFORE_TYPE_COLON)
						text += " ";
					text += ":";
					if(settings.SPACE_AFTER_TYPE_COLON)
						text += " ";
					text += ref.getText();
				}
			}
			else if(declaration instanceof NapileMethod)
			{
				NapileMethod function = (NapileMethod) declaration;
				NapileTypeReference receiverTypeRef = function.getReceiverTypeRef();
				if(receiverTypeRef != null)
				{
					text = receiverTypeRef.getText() + "." + text;
				}
				text += "(";
				List<NapileElement> parameters = function.getValueParameters();
				for(NapileElement parameter : parameters)
				{
					if(parameter instanceof NapilePropertyParameter)
					{
						if(parameter.getName() != null)
						{
							text += parameter.getName();
							if(settings.SPACE_BEFORE_TYPE_COLON)
								text += " ";
							text += ":";
							if(settings.SPACE_AFTER_TYPE_COLON)
								text += " ";
						}
						NapileTypeReference typeReference = ((NapilePropertyParameter) parameter).getTypeReference();
						if(typeReference != null)
						{
							text += typeReference.getText();
						}
					}
					else if(parameter instanceof NapileReferenceParameter)
						text += parameter.getText();
					text += ", ";
				}
				if(parameters.size() > 0)
					text = text.substring(0, text.length() - 2);
				text += ")";
				NapileTypeReference typeReference = function.getReturnTypeRef();
				if(typeReference != null)
				{
					if(settings.SPACE_BEFORE_TYPE_COLON)
						text += " ";
					text += ":";
					if(settings.SPACE_AFTER_TYPE_COLON)
						text += " ";
					text += typeReference.getText();
				}
			}
			else if(declaration instanceof NapileConstructor)
			{
				NapileConstructor function = (NapileConstructor) declaration;
				text += "(";
				List<NapileElement> parameters = function.getValueParameters();
				for(NapileElement parameter : parameters)
				{
					if(parameter instanceof NapilePropertyParameter)
					{
						if(parameter.getName() != null)
						{
							text += parameter.getName();
							if(settings.SPACE_BEFORE_TYPE_COLON)
								text += " ";
							text += ":";
							if(settings.SPACE_AFTER_TYPE_COLON)
								text += " ";
						}
						NapileTypeReference typeReference = ((NapilePropertyParameter) parameter).getTypeReference();
						if(typeReference != null)
						{
							text += typeReference.getText();
						}
					}
					else if(parameter instanceof NapileReferenceParameter)
						text += parameter.getText();
					text += ", ";
				}
				if(parameters.size() > 0)
					text = text.substring(0, text.length() - 2);
				text += ")";
			}

			data.setPresentableText(text);
		}
	}
}
