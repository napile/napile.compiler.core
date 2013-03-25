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

package org.napile.idea.plugin.codeInsight;

import java.util.List;

import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.ClassifierDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.lang.resolve.BindingTraceKeys;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.DescriptorUtils;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.idea.plugin.module.ModuleAnalyzerUtil;
import org.napile.idea.plugin.quickfix.ImportInsertHelper;

public class ReferenceToClassesShortening
{
	private ReferenceToClassesShortening()
	{
	}

	public static void compactReferenceToClasses(List<? extends NapileElement> elementsToCompact)
	{
		if(elementsToCompact.isEmpty())
		{
			return;
		}
		final NapileFile file = elementsToCompact.get(0).getContainingFile();
		final BindingTrace bc = ModuleAnalyzerUtil.lastAnalyze(file).getBindingTrace();
		for(NapileElement element : elementsToCompact)
		{
			element.accept(new NapileVisitorVoid()
			{
				@Override
				public void visitJetElement(NapileElement element)
				{
					element.acceptChildren(this);
				}

				@Override
				public void visitTypeReference(NapileTypeReference typeReference)
				{
					super.visitTypeReference(typeReference);

					NapileTypeElement typeElement = typeReference.getTypeElement();
					if(typeElement instanceof NapileNullableType)
					{
						typeElement = ((NapileNullableType) typeElement).getInnerType();
					}
					if(typeElement instanceof NapileUserType)
					{
						NapileUserType userType = (NapileUserType) typeElement;
						DeclarationDescriptor target = bc.get(BindingTraceKeys.REFERENCE_TARGET, userType.getReferenceExpression());
						if(target instanceof ClassDescriptor)
						{
							ClassDescriptor targetClass = (ClassDescriptor) target;
							ClassDescriptor targetTopLevelClass = ImportInsertHelper.getTopLevelClass(targetClass);

							JetScope scope = bc.get(BindingTraceKeys.TYPE_RESOLUTION_SCOPE, typeReference);
							ClassifierDescriptor classifier = scope.getClassifier(targetTopLevelClass.getName());
							if(targetTopLevelClass == classifier)
							{
								compactReferenceToClass(userType, targetClass);
							}
							else if(classifier == null)
							{
								ImportInsertHelper.addImportDirective(DescriptorUtils.getFQName(targetTopLevelClass).toSafe(), file);
								compactReferenceToClass(userType, targetClass);
							}
							else
							{
								// leave FQ name
							}
						}
					}
				}

				private void compactReferenceToClass(NapileUserType userType, ClassDescriptor targetClass)
				{
					String name = targetClass.getName().getName();
					DeclarationDescriptor parent = targetClass.getContainingDeclaration();
					while(parent instanceof ClassDescriptor)
					{
						name = parent.getName() + "." + name;
						parent = parent.getContainingDeclaration();
					}
					NapileTypeArgumentList typeArgumentList = userType.getTypeArgumentList();
					NapileTypeElement typeElement = NapilePsiFactory.createType(userType.getProject(), name + (typeArgumentList == null ? "" : typeArgumentList.getText())).getTypeElement();
					assert typeElement != null;
					userType.replace(typeElement);
				}
			});
		}
	}
}
