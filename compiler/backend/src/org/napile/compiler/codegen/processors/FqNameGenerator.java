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

package org.napile.compiler.codegen.processors;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.AsmConstants;
import org.napile.asm.resolve.name.FqName;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.ClassKind;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.DescriptorUtils;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * @author VISTALL
 * @date 19:49/03.09.12
 */
public class FqNameGenerator extends NapileTreeVisitor<FqName>
{
	@NotNull
	private final BindingTrace bindingTrace;

	private final Map<FqName, Integer> anonymClassCounts = new HashMap<FqName, Integer>();

	public FqNameGenerator(@NotNull BindingTrace bindingTrace)
	{
		this.bindingTrace = bindingTrace;
	}

	@Override
	public Void visitJetElement(NapileElement element, FqName fqName)
	{
		element.acceptChildren(this, fqName);
		return null;
	}

	@Override
	public Void visitClass(NapileClass klass, FqName fqName)
	{
		// owner is file
		if(fqName == null)
			fqName = klass.getFqName();
		else
		{
			final Name shortName = fqName.shortName();
			fqName = fqName.parent().child(Name.identifier(shortName.getName() + AsmConstants.ANONYM_SPLITTER + klass.getName()));
		}

		record(bindingTrace.safeGet(BindingContext.CLASS, klass), klass, fqName);
		return super.visitClass(klass, fqName);
	}

	@Override
	public Void visitVariable(NapileVariable property, FqName data)
	{
		NapileMethod method = PsiTreeUtil.getParentOfType(property, NapileMethod.class);
		if(method == null)
			record(bindingTrace.safeGet(BindingContext.VARIABLE, property), property, data.child(NapilePsiUtil.safeName(property.getName())));
		return super.visitVariable(property, data);
	}

	@Override
	public Void visitEnumValue(NapileEnumValue enumValue, FqName data)
	{
		NapileMethod method = PsiTreeUtil.getParentOfType(enumValue, NapileMethod.class);
		if(method == null)
			record(bindingTrace.safeGet(BindingContext.VARIABLE, enumValue), enumValue, data.child(NapilePsiUtil.safeName(enumValue.getName())));
		return super.visitVariable(enumValue, data);
	}

	@Override
	public Void visitVariableAccessor(NapileVariableAccessor accessor, FqName data)
	{
		NapileVariable variable = PsiTreeUtil.getParentOfType(accessor, NapileVariable.class);

		FqName fqName = bindingTrace.safeGet(BindingContext2.DECLARATION_TO_FQ_NAME, variable);

		record(bindingTrace.safeGet(BindingContext.DECLARATION_TO_DESCRIPTOR, accessor), accessor, fqName.child(Name.identifier(fqName.shortName().getName() + AsmConstants.ANONYM_SPLITTER + accessor.getAccessorElement().getText())));
		return super.visitVariableAccessor(accessor, data);
	}

	@Override
	public Void visitNamedMethodOrMacro(NapileNamedMethodOrMacro function, FqName data)
	{
		record(bindingTrace.safeGet(BindingContext.METHOD, function), function, data.child(NapilePsiUtil.safeName(function.getName())));
		return super.visitNamedMethodOrMacro(function, data);
	}

	@Override
	public Void visitAnonymClass(NapileAnonymClass element, FqName data)
	{
		Integer anonymCount = anonymClassCounts.get(data);
		if(anonymCount == null)
			anonymClassCounts.put(data, anonymCount = 0);
		else
			anonymClassCounts.put(data, ++ anonymCount);

		data = data.parent().child(Name.identifier(data.shortName() + AsmConstants.ANONYM_SPLITTER + anonymCount));
		record(bindingTrace.safeGet(BindingContext.CLASS, element), element, data);
		return super.visitAnonymClass(element, data);
	}

	private void record(DeclarationDescriptor descriptor, NapileDeclaration declaration, FqName fqName)
	{
		bindingTrace.record(BindingContext2.DECLARATION_TO_FQ_NAME, declaration, fqName);
		bindingTrace.record(BindingContext2.DESCRIPTOR_TO_FQ_NAME, descriptor, fqName);
	}

	public static FqName getFqName(DeclarationDescriptor declarationDescriptor, BindingTrace bindingTrace)
	{
		if(declarationDescriptor instanceof ClassDescriptor)
		{
			if(((ClassDescriptor)declarationDescriptor).getKind() == ClassKind.ANONYM_CLASS)
				return bindingTrace.safeGet(BindingContext2.DESCRIPTOR_TO_FQ_NAME, declarationDescriptor);
			else
				return DescriptorUtils.getFQName(declarationDescriptor).toSafe();
		}
		else
		{
			ClassDescriptor ownerClass = DescriptorUtils.getParentOfType(declarationDescriptor, ClassDescriptor.class);
			assert ownerClass != null;

			FqName classFqName = getFqName(ownerClass, bindingTrace);

			return classFqName.child(declarationDescriptor.getName());
		}

	}
}
