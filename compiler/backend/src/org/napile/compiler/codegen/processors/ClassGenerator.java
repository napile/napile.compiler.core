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

import org.napile.asm.tree.members.ClassNode;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.name.FqName;

/**
 * @author VISTALL
 * @date 10:58/04.09.12
 */
public class ClassGenerator extends NapileVisitorVoid
{
	private final Map<FqName, ClassNode> classNodes;
	private final BindingTrace bindingTrace;

	public ClassGenerator(BindingTrace bindingTrace, Map<FqName, ClassNode> classNodes)
	{
		this.bindingTrace = bindingTrace;
		this.classNodes = classNodes;
	}

	@Override
	public void visitClass(NapileClass klass)
	{
		ClassDescriptor classDescriptor = (ClassDescriptor) bindingTrace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, klass);

		assert classDescriptor != null;

		FqName fqName = bindingTrace.get(BindingContext2.DECLARATION_TO_FQ_NAME, klass);

		assert fqName != null;

		ClassNode classNode = new ClassNode(ModifierGenerator.toModifiers(classDescriptor), fqName);

		classNodes.put(fqName, classNode);
	}

	public Map<FqName, ClassNode> getClassNodes()
	{
		return classNodes;
	}
}
