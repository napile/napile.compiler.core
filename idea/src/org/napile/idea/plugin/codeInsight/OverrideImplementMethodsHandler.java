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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.descriptors.*;
import org.napile.compiler.lang.descriptors.SimpleMethodDescriptor;
import org.napile.compiler.lang.psi.NapileClassBody;
import org.napile.compiler.lang.psi.NapileLikeClass;
import org.napile.compiler.lang.psi.NapileElement;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.psi.NapilePsiFactory;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.TypeUtils;
import org.napile.compiler.lang.types.lang.JetStandardClasses;
import org.napile.compiler.lang.rt.NapileLangPackage;
import org.napile.idea.plugin.project.WholeProjectAnalyzerFacade;
import org.napile.compiler.resolve.DescriptorRenderer;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.ide.util.MemberChooser;
import com.intellij.lang.LanguageCodeInsightActionHandler;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * @author yole
 */
public abstract class OverrideImplementMethodsHandler implements LanguageCodeInsightActionHandler
{
	public static List<DescriptorClassMember> membersFromDescriptors(Iterable<CallableMemberDescriptor> missingImplementations)
	{
		List<DescriptorClassMember> members = new ArrayList<DescriptorClassMember>();
		for(CallableMemberDescriptor memberDescriptor : missingImplementations)
		{
			members.add(new DescriptorClassMember(memberDescriptor));
		}
		return members;
	}

	@NotNull
	public Set<CallableMemberDescriptor> collectMethodsToGenerate(@NotNull NapileLikeClass classOrObject)
	{
		BindingContext bindingContext = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile((NapileFile) classOrObject.getContainingFile()).getBindingContext();
		final DeclarationDescriptor descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, classOrObject);
		if(descriptor instanceof MutableClassDescriptor)
		{
			return collectMethodsToGenerate((MutableClassDescriptor) descriptor);
		}
		return Collections.emptySet();
	}

	protected abstract Set<CallableMemberDescriptor> collectMethodsToGenerate(MutableClassDescriptor descriptor);

	public static void generateMethods(Editor editor, NapileLikeClass classOrObject, List<DescriptorClassMember> selectedElements)
	{
		final NapileClassBody body = classOrObject.getBody();
		if(body == null)
		{
			return;
		}

		PsiElement afterAnchor = findInsertAfterAnchor(editor, body);

		if(afterAnchor == null)
		{
			return;
		}

		List<NapileElement> elementsToCompact = new ArrayList<NapileElement>();
		final NapileFile file = (NapileFile) classOrObject.getContainingFile();
		for(NapileElement element : generateOverridingMembers(selectedElements, file))
		{
			PsiElement added = body.addAfter(element, afterAnchor);
			afterAnchor = added;
			elementsToCompact.add((NapileElement) added);
		}
		ReferenceToClassesShortening.compactReferenceToClasses(elementsToCompact);
	}


	@Nullable
	private static PsiElement findInsertAfterAnchor(Editor editor, final NapileClassBody body)
	{
		PsiElement afterAnchor = body.getLBrace();
		if(afterAnchor == null)
		{
			return null;
		}

		int offset = editor.getCaretModel().getOffset();
		PsiElement offsetCursorElement = PsiTreeUtil.findFirstParent(body.getContainingFile().findElementAt(offset), new Condition<PsiElement>()
		{
			@Override
			public boolean value(PsiElement element)
			{
				return element.getParent() == body;
			}
		});

		if(offsetCursorElement != null && offsetCursorElement != body.getRBrace())
		{
			afterAnchor = offsetCursorElement;
		}

		return afterAnchor;
	}

	private static List<NapileElement> generateOverridingMembers(List<DescriptorClassMember> selectedElements, NapileFile file)
	{
		List<NapileElement> overridingMembers = new ArrayList<NapileElement>();
		for(DescriptorClassMember selectedElement : selectedElements)
		{
			final DeclarationDescriptor descriptor = selectedElement.getDescriptor();
			if(descriptor instanceof SimpleMethodDescriptor)
			{
				overridingMembers.add(overrideFunction(file.getProject(), (SimpleMethodDescriptor) descriptor));
			}
			else if(descriptor instanceof PropertyDescriptor)
			{
				overridingMembers.add(overrideProperty(file.getProject(), (PropertyDescriptor) descriptor));
			}
		}
		return overridingMembers;
	}

	private static NapileElement overrideProperty(Project project, PropertyDescriptor descriptor)
	{
		StringBuilder bodyBuilder = new StringBuilder();
		bodyBuilder.append(displayableVisibility(descriptor)).append("override ");
		if(descriptor.getPropertyKind() == PropertyKind.VAR)
		{
			bodyBuilder.append("var ");
		}
		else
		{
			bodyBuilder.append("val ");
		}

		addReceiverParameter(descriptor, bodyBuilder);

		bodyBuilder.append(descriptor.getName()).append(" : ").append(DescriptorRenderer.COMPACT_WITH_MODIFIERS.renderTypeWithShortNames(descriptor.getType()));
		String initializer = defaultInitializer(descriptor.getType());
		if(initializer != null)
		{
			bodyBuilder.append(" = ").append(initializer);
		}
		else
		{
			bodyBuilder.append(" = ?");
		}
		return NapilePsiFactory.createProperty(project, bodyBuilder.toString());
	}

	private static String renderType(JetType type)
	{
		return DescriptorRenderer.TEXT.renderType(type);
	}

	private static NapileElement overrideFunction(Project project, SimpleMethodDescriptor descriptor)
	{
		StringBuilder bodyBuilder = new StringBuilder();
		bodyBuilder.append(displayableVisibility(descriptor));
		bodyBuilder.append("override fun ");

		List<String> whereRestrictions = new ArrayList<String>();
		if(!descriptor.getTypeParameters().isEmpty())
		{
			bodyBuilder.append("<");
			boolean first = true;
			for(TypeParameterDescriptor param : descriptor.getTypeParameters())
			{
				if(!first)
				{
					bodyBuilder.append(", ");
				}

				bodyBuilder.append(param.getName());
				Set<JetType> upperBounds = param.getUpperBounds();
				if(!upperBounds.isEmpty())
				{
					boolean firstUpperBound = true;
					for(JetType upperBound : upperBounds)
					{
						String upperBoundText = " : " + renderType(upperBound);
						if(upperBound != JetStandardClasses.getDefaultBound())
						{
							if(firstUpperBound)
							{
								bodyBuilder.append(upperBoundText);
							}
							else
							{
								whereRestrictions.add(param.getName() + upperBoundText);
							}
						}
						firstUpperBound = false;
					}
				}

				first = false;
			}
			bodyBuilder.append("> ");
		}

		addReceiverParameter(descriptor, bodyBuilder);

		bodyBuilder.append(descriptor.getName()).append("(");
		boolean isAbstractFun = descriptor.getModality() == Modality.ABSTRACT;
		StringBuilder delegationBuilder = new StringBuilder();
		if(isAbstractFun)
		{
			delegationBuilder.append("throw UnsupportedOperationException()");
		}
		else
		{
			delegationBuilder.append("super<").append(descriptor.getContainingDeclaration().getName());
			delegationBuilder.append(">.").append(descriptor.getName()).append("(");
		}
		boolean first = true;
		for(ValueParameterDescriptor parameterDescriptor : descriptor.getValueParameters())
		{
			if(!first)
			{
				bodyBuilder.append(",");
				if(!isAbstractFun)
				{
					delegationBuilder.append(",");
				}
			}
			first = false;
			bodyBuilder.append(parameterDescriptor.getName());
			bodyBuilder.append(" : ");
			bodyBuilder.append(renderType(parameterDescriptor.getType()));

			if(!isAbstractFun)
			{
				delegationBuilder.append(parameterDescriptor.getName());
			}
		}
		bodyBuilder.append(")");
		if(!isAbstractFun)
		{
			delegationBuilder.append(")");
		}
		final JetType returnType = descriptor.getReturnType();

		boolean returnsNotUnit = returnType != null;
		if(returnsNotUnit)
		{
			bodyBuilder.append(" : ").append(renderType(returnType));
		}
		if(!whereRestrictions.isEmpty())
		{
			bodyBuilder.append("\n").append("where ").append(StringUtil.join(whereRestrictions, ", "));
		}
		bodyBuilder.append("{").append(returnsNotUnit && !isAbstractFun ? "return " : "").append(delegationBuilder.toString()).append("}");

		return NapilePsiFactory.createFunction(project, bodyBuilder.toString());
	}

	private static void addReceiverParameter(CallableDescriptor descriptor, StringBuilder bodyBuilder)
	{
		ReceiverDescriptor receiverParameter = descriptor.getReceiverParameter();
		if(receiverParameter.exists())
		{
			bodyBuilder.append(receiverParameter.getType()).append(".");
		}
	}

	//TODO [VISTALL] get from @DefaultValue =
	private static String defaultInitializer(JetType returnType)
	{
		if(returnType.isNullable() || TypeUtils.isEqualFqName(returnType, NapileLangPackage.NULL))
		{
			return "null";
		}
		else if(TypeUtils.isEqualFqName(returnType, NapileLangPackage.BYTE) ||
				TypeUtils.isEqualFqName(returnType, NapileLangPackage.SHORT) ||
				TypeUtils.isEqualFqName(returnType, NapileLangPackage.INT) ||
				TypeUtils.isEqualFqName(returnType, NapileLangPackage.LONG) ||
				TypeUtils.isEqualFqName(returnType, NapileLangPackage.FLOAT) ||
				TypeUtils.isEqualFqName(returnType, NapileLangPackage.DOUBLE) ||
				TypeUtils.isEqualFqName(returnType, NapileLangPackage.CHAR))
		{
			return "0";
		}
		else if(TypeUtils.isEqualFqName(returnType, NapileLangPackage.BOOL))
		{
			return "false";
		}

		return null;
	}

	private static String displayableVisibility(MemberDescriptor descriptor)
	{
		Visibility visibility = descriptor.getVisibility();
		return visibility != Visibility.PUBLIC ? visibility.toString() + " " : "";
	}

	private MemberChooser<DescriptorClassMember> showOverrideImplementChooser(Project project, DescriptorClassMember[] members)
	{
		final MemberChooser<DescriptorClassMember> chooser = new MemberChooser<DescriptorClassMember>(members, true, true, project);
		chooser.setTitle(getChooserTitle());
		chooser.show();
		if(chooser.getExitCode() != DialogWrapper.OK_EXIT_CODE)
			return null;
		return chooser;
	}

	protected abstract String getChooserTitle();

	@Override
	public boolean isValidFor(Editor editor, PsiFile file)
	{
		if(!(file instanceof NapileFile))
		{
			return false;
		}
		final PsiElement elementAtCaret = file.findElementAt(editor.getCaretModel().getOffset());
		final NapileLikeClass classOrObject = PsiTreeUtil.getParentOfType(elementAtCaret, NapileLikeClass.class);
		return classOrObject != null;
	}

	protected abstract String getNoMethodsFoundHint();

	public void invoke(@NotNull final Project project, @NotNull final Editor editor, @NotNull PsiFile file, boolean implementAll)
	{
		final PsiElement elementAtCaret = file.findElementAt(editor.getCaretModel().getOffset());
		final NapileLikeClass classOrObject = PsiTreeUtil.getParentOfType(elementAtCaret, NapileLikeClass.class);

		assert classOrObject != null : "ClassObject should be checked in isValidFor method";

		Set<CallableMemberDescriptor> missingImplementations = collectMethodsToGenerate(classOrObject);
		if(missingImplementations.isEmpty() && !implementAll)
		{
			HintManager.getInstance().showErrorHint(editor, getNoMethodsFoundHint());
			return;
		}
		List<DescriptorClassMember> members = membersFromDescriptors(missingImplementations);

		final List<DescriptorClassMember> selectedElements;
		if(implementAll)
		{
			selectedElements = members;
		}
		else
		{
			final MemberChooser<DescriptorClassMember> chooser = showOverrideImplementChooser(project, members.toArray(new DescriptorClassMember[members.size()]));

			if(chooser == null)
			{
				return;
			}

			selectedElements = chooser.getSelectedElements();
			if(selectedElements == null || selectedElements.isEmpty())
				return;
		}

		ApplicationManager.getApplication().runWriteAction(new Runnable()
		{
			@Override
			public void run()
			{
				generateMethods(editor, classOrObject, selectedElements);
			}
		});
	}

	@Override
	public void invoke(@NotNull final Project project, @NotNull final Editor editor, @NotNull PsiFile file)
	{
		invoke(project, editor, file, false);
	}

	@Override
	public boolean startInWriteAction()
	{
		return false;
	}
}
