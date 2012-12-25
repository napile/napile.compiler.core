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

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.Modifier;
import org.napile.asm.resolve.name.FqName;
import org.napile.asm.tree.members.ClassNode;
import org.napile.compiler.codegen.processors.codegen.stackValue.StackValue;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.psi.NapileAnonymClass;
import org.napile.compiler.lang.psi.NapileAnonymClassExpression;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.resolve.BindingContext;
import com.intellij.psi.PsiElement;

/**
 * @author VISTALL
 * @date 15:59/23.12.12
 */
public class InnerClassCodegen
{
	public static StackValue genAnonym(@NotNull NapileAnonymClassExpression expression, @NotNull ExpressionCodegen gen)
	{
		NapileAnonymClass anonymClass = expression.getAnonymClass();

		FqName fqName = gen.bindingTrace.safeGet(BindingContext2.DECLARATION_TO_FQ_NAME, anonymClass);

		ClassNode anonymClassNode = new ClassNode(Modifier.EMPTY, fqName);
		gen.classNode.addMember(anonymClassNode);

		List<ClassDescriptor> classLikes = new ArrayList<ClassDescriptor>(1);

		PsiElement p = expression.getParent();
		while(p != null)
		{
			DeclarationDescriptor descriptor = gen.bindingTrace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, p);
			if(!(descriptor instanceof ClassDescriptor))
			{}
			else if(p instanceof NapileAnonymClass)
				classLikes.add((ClassDescriptor) descriptor);
			else if(p instanceof NapileClass)
			{
				classLikes.add((ClassDescriptor) descriptor);
				if(((ClassDescriptor) descriptor).isStatic())
					break;
			}

			p = p.getParent();
		}

		gen.instructs.putNull();

		for(ClassDescriptor n : classLikes)
			System.out.println(n.getName());

		return StackValue.none();
	}
}
