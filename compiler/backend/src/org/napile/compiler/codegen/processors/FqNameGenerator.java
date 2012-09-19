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
import org.napile.asm.resolve.name.FqName;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.psi.NapileAnonymClass;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileDeclaration;
import org.napile.compiler.lang.psi.NapileElement;
import org.napile.compiler.lang.psi.NapileFunctionLiteralExpression;
import org.napile.compiler.lang.psi.NapileNamedFunction;
import org.napile.compiler.lang.psi.NapileProperty;
import org.napile.compiler.lang.psi.NapilePsiUtil;
import org.napile.compiler.lang.psi.NapileTreeVisitor;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingTrace;

/**
 * @author VISTALL
 * @date 19:49/03.09.12
 */
public class FqNameGenerator extends NapileTreeVisitor<FqName>
{
	public static final String SEPARATOR = "$";

	@NotNull
	private final BindingTrace bindingTrace;

	private final Map<FqName, Integer> anonymClassCounts = new HashMap<FqName, Integer>();
	private final Map<FqName, Integer> anonymMethodCounts = new HashMap<FqName, Integer>();

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
			fqName = fqName.parent().child(Name.identifier(shortName.getName() + SEPARATOR + klass.getName()));
		}

		record(klass, fqName);
		return super.visitClass(klass, fqName);
	}

	@Override
	public Void visitProperty(NapileProperty property, FqName data)
	{
		record(property, data.child(NapilePsiUtil.safeName(property.getName())));
		return super.visitProperty(property, data);
	}

	@Override
	public Void visitNamedFunction(NapileNamedFunction function, FqName data)
	{
		record(function, data.child(NapilePsiUtil.safeName(function.getName())));
		return super.visitNamedFunction(function, data);
	}

	@Override
	public Void visitFunctionLiteralExpression(NapileFunctionLiteralExpression expression, FqName data)
	{
		Integer anonymCount = anonymMethodCounts.get(data);
		if(anonymCount == null)
			anonymMethodCounts.put(data, anonymCount = 0);
		else
			anonymMethodCounts.put(data, ++ anonymCount);

		data = data.child(Name.identifier("anonym" + SEPARATOR + anonymCount));
		record(expression, data);
		return super.visitFunctionLiteralExpression(expression, data);
	}

	@Override
	public Void visitAnonymClass(NapileAnonymClass element, FqName data)
	{
		Integer anonymCount = anonymClassCounts.get(data);
		if(anonymCount == null)
			anonymClassCounts.put(data, anonymCount = 0);
		else
			anonymClassCounts.put(data, ++ anonymCount);

		data = data.parent().child(Name.identifier(data.shortName() + SEPARATOR + anonymCount));
		record(element, data);
		return super.visitAnonymClass(element, data);
	}

	private void record(NapileDeclaration declaration, FqName fqName)
	{
		DeclarationDescriptor descriptor = bindingTrace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration);

		bindingTrace.record(BindingContext2.DESCRIPTOR_TO_FQ_NAME, descriptor, fqName);
		bindingTrace.record(BindingContext2.DECLARATION_TO_FQ_NAME, declaration, fqName);
	}
}
