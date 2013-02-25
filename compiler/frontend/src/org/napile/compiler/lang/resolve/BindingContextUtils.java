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

package org.napile.compiler.lang.resolve;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.descriptors.CallableMemberDescriptor;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.PackageDescriptor;
import org.napile.compiler.lang.descriptors.SimpleMethodDescriptor;
import org.napile.compiler.lang.descriptors.VariableAccessorDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.util.slicedmap.ReadOnlySlice;
import org.napile.compiler.util.slicedmap.Slices;
import com.google.common.collect.Lists;
import com.intellij.psi.PsiElement;

/**
 * @author abreslav
 * @author svtk
 */
public class BindingContextUtils
{
	private BindingContextUtils()
	{
	}

	private static final Slices.KeyNormalizer<DeclarationDescriptor> DECLARATION_DESCRIPTOR_NORMALIZER = new Slices.KeyNormalizer<DeclarationDescriptor>()
	{
		@Override
		public DeclarationDescriptor normalize(DeclarationDescriptor declarationDescriptor)
		{
			if(declarationDescriptor instanceof CallableMemberDescriptor)
			{
				CallableMemberDescriptor callable = (CallableMemberDescriptor) declarationDescriptor;
				if(callable.getKind() != CallableMemberDescriptor.Kind.DECLARATION)
				{
					throw new IllegalStateException("non-declaration descriptors should be filtered out earlier: " + callable);
				}
			}
			//if (declarationDescriptor instanceof VariableAsFunctionDescriptor) {
			//    VariableAsFunctionDescriptor descriptor = (VariableAsFunctionDescriptor) declarationDescriptor;
			//    if (descriptor.getOriginal() != descriptor) {
			//        throw new IllegalStateException("original should be resolved earlier: " + descriptor);
			//    }
			//}
			return declarationDescriptor.getOriginal();
		}
	};

	/*package*/ static final ReadOnlySlice<DeclarationDescriptor, PsiElement> DESCRIPTOR_TO_DECLARATION = Slices.<DeclarationDescriptor, PsiElement>sliceBuilder().setKeyNormalizer(DECLARATION_DESCRIPTOR_NORMALIZER).setDebugName("DESCRIPTOR_TO_DECLARATION").build();

	@Nullable
	public static PsiElement resolveToDeclarationPsiElement(@NotNull BindingContext bindingContext, @Nullable NapileReferenceExpression referenceExpression)
	{
		DeclarationDescriptor declarationDescriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, referenceExpression);
		if(declarationDescriptor == null)
		{
			return bindingContext.get(BindingContext.LABEL_TARGET, referenceExpression);
		}

		PsiElement element = descriptorToDeclaration(bindingContext, declarationDescriptor);
		if(element != null)
		{
			return element;
		}

		return null;
	}

	@NotNull
	public static List<PsiElement> resolveToDeclarationPsiElements(@NotNull BindingContext bindingContext, @Nullable NapileReferenceExpression referenceExpression)
	{
		DeclarationDescriptor declarationDescriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, referenceExpression);
		if(declarationDescriptor == null)
		{
			return Lists.newArrayList(bindingContext.get(BindingContext.LABEL_TARGET, referenceExpression));
		}

		List<PsiElement> elements = descriptorToDeclarations(bindingContext, declarationDescriptor);
		if(elements.size() > 0)
		{
			return elements;
		}

		return Lists.newArrayList();
	}


	@Nullable
	public static VariableDescriptor extractVariableDescriptorIfAny(@NotNull BindingContext bindingContext, @Nullable NapileElement element, boolean onlyReference)
	{
		DeclarationDescriptor descriptor = null;
		if(!onlyReference && (element instanceof NapileVariable || element instanceof NapileCallParameterAsVariable))
		{
			descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element);
		}
		else if(element instanceof NapileSimpleNameExpression)
		{
			descriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, (NapileSimpleNameExpression) element);
		}
		else if(element instanceof NapileQualifiedExpressionImpl)
		{
			descriptor = extractVariableDescriptorIfAny(bindingContext, ((NapileQualifiedExpressionImpl) element).getSelectorExpression(), onlyReference);
		}
		if(descriptor instanceof VariableDescriptor)
			return (VariableDescriptor) descriptor;
		else if(descriptor instanceof VariableAccessorDescriptor)
			return ((VariableAccessorDescriptor) descriptor).getVariable();
		return null;
	}

	// TODO these helper methods are added as a workaround to some compiler bugs in Kotlin...

	// NOTE this is used by KDoc
	@Nullable
	public static PackageDescriptor namespaceDescriptor(@NotNull BindingContext context, @NotNull NapileFile source)
	{
		return context.get(BindingContext.FILE_TO_NAMESPACE, source);
	}

	@Nullable
	private static PsiElement doGetDescriptorToDeclaration(@NotNull BindingContext context, @NotNull DeclarationDescriptor descriptor)
	{
		return context.get(DESCRIPTOR_TO_DECLARATION, descriptor);
	}

	// NOTE this is also used by KDoc
	@Nullable
	public static PsiElement descriptorToDeclaration(@NotNull BindingContext context, @NotNull DeclarationDescriptor descriptor)
	{
		if(descriptor instanceof CallableMemberDescriptor)
		{
			return callableDescriptorToDeclaration(context, (CallableMemberDescriptor) descriptor);
		}
		else if(descriptor instanceof ClassDescriptor)
		{
			return classDescriptorToDeclaration(context, (ClassDescriptor) descriptor);
		}
		else
		{
			return doGetDescriptorToDeclaration(context, descriptor);
		}
	}

	@NotNull
	public static List<PsiElement> descriptorToDeclarations(@NotNull BindingContext context, @NotNull DeclarationDescriptor descriptor)
	{
		if(descriptor instanceof CallableMemberDescriptor)
		{
			return callableDescriptorToDeclarations(context, (CallableMemberDescriptor) descriptor);
		}
		else
		{
			PsiElement psiElement = descriptorToDeclaration(context, descriptor);
			if(psiElement != null)
			{
				return Lists.newArrayList(psiElement);
			}
			else
			{
				return Lists.newArrayList();
			}
		}
	}

	@Nullable
	public static PsiElement callableDescriptorToDeclaration(@NotNull BindingContext context, @NotNull CallableMemberDescriptor callable)
	{
		if(callable.getKind() != CallableMemberDescriptor.Kind.DECLARATION)
		{
			Set<? extends CallableMemberDescriptor> overriddenDescriptors = callable.getOverriddenDescriptors();
			if(overriddenDescriptors.size() != 1)
			{
				// TODO evil code
				throw new IllegalStateException("cannot find declaration: fake descriptor" +
						" has more then one overridden descriptor: " + callable);
			}

			return callableDescriptorToDeclaration(context, overriddenDescriptors.iterator().next());
		}

		return doGetDescriptorToDeclaration(context, callable.getOriginal());
	}

	private static List<PsiElement> callableDescriptorToDeclarations(@NotNull BindingContext context, @NotNull CallableMemberDescriptor callable)
	{
		if(callable.getKind() != CallableMemberDescriptor.Kind.DECLARATION)
		{
			List<PsiElement> r = new ArrayList<PsiElement>();
			Set<? extends CallableMemberDescriptor> overriddenDescriptors = callable.getOverriddenDescriptors();
			for(CallableMemberDescriptor overridden : overriddenDescriptors)
			{
				r.addAll(callableDescriptorToDeclarations(context, overridden));
			}
			return r;
		}
		PsiElement psiElement = doGetDescriptorToDeclaration(context, callable);
		return psiElement != null ? Lists.newArrayList(psiElement) : Lists.<PsiElement>newArrayList();
	}

	@Nullable
	public static PsiElement classDescriptorToDeclaration(@NotNull BindingContext context, @NotNull ClassDescriptor clazz)
	{
		return doGetDescriptorToDeclaration(context, clazz);
	}

	public static void recordMethodDeclarationToDescriptor(@NotNull BindingTrace trace, @NotNull PsiElement psiElement, @NotNull SimpleMethodDescriptor method)
	{
		if(method.getKind() != CallableMemberDescriptor.Kind.DECLARATION)
			throw new IllegalArgumentException("function of kind " + method.getKind() + " cannot have declaration");

		trace.record(BindingContext.METHOD, psiElement, method);
	}
}
