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

import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.tree.members.ClassNode;
import org.napile.asm.tree.members.Node;
import org.napile.asm.tree.members.VariableNode;
import org.napile.asm.tree.members.types.ClassTypeNode;
import org.napile.asm.tree.members.types.TypeConstructorNode;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.ClassifierDescriptor;
import org.napile.compiler.lang.descriptors.PropertyDescriptor;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileElement;
import org.napile.compiler.lang.psi.NapileRetellEntry;
import org.napile.compiler.lang.psi.NapileTreeVisitor;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.DescriptorUtils;
import org.napile.compiler.lang.resolve.name.FqName;
import org.napile.compiler.lang.types.JetType;

/**
 * @author VISTALL
 * @date 10:58/04.09.12
 */
public class ClassGenerator extends NapileTreeVisitor<Node>
{
	private final Map<FqName, ClassNode> classNodes;
	private final BindingTrace bindingTrace;

	public ClassGenerator(BindingTrace bindingTrace, Map<FqName, ClassNode> classNodes)
	{
		this.bindingTrace = bindingTrace;
		this.classNodes = classNodes;
	}

	@Override
	public Void visitJetElement(NapileElement element, Node data)
	{
		element.acceptChildren(this, data);
		return null;
	}

	@Override
	public Void visitClass(NapileClass klass, Node parent)
	{
		ClassDescriptor classDescriptor = (ClassDescriptor) bindingTrace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, klass);

		assert classDescriptor != null;

		FqName fqName = bindingTrace.get(BindingContext2.DECLARATION_TO_FQ_NAME, klass);

		assert fqName != null;

		ClassNode classNode = new ClassNode(ModifierGenerator.toModifiers(classDescriptor), fqName);

		classNodes.put(fqName, classNode);

		return super.visitClass(klass, classNode);
	}

	@Override
	public Void visitRetellEntry(NapileRetellEntry retellEntry, Node parent)
	{
		assert parent instanceof ClassNode;

		PropertyDescriptor propertyDescriptor = (PropertyDescriptor) bindingTrace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, retellEntry);

		assert propertyDescriptor != null;

		ClassNode classNode = (ClassNode) parent;

		VariableNode variableNode = new VariableNode(ModifierGenerator.toModifiers(propertyDescriptor), retellEntry.getNameAsSafeName().getName());
		variableNode.returnType = toAsmType(propertyDescriptor.getType());
		classNode.members.add(variableNode);

		return super.visitRetellEntry(retellEntry, variableNode);
	}

	@NotNull
	private TypeNode toAsmType(@NotNull JetType jetType)
	{
		TypeConstructorNode typeConstructorNode = null;
		ClassifierDescriptor owner = jetType.getConstructor().getDeclarationDescriptor();
		if(owner instanceof ClassDescriptor)
			typeConstructorNode = new ClassTypeNode(DescriptorUtils.getFQName(owner).toSafe());
		else
			throw new RuntimeException("invalid " + owner);

		TypeNode typeNode = new TypeNode(jetType.isNullable(), typeConstructorNode);

		//TODO [VISTALL] annotations & type parameters
		return typeNode;
	}

	public Map<FqName, ClassNode> getClassNodes()
	{
		return classNodes;
	}
}
