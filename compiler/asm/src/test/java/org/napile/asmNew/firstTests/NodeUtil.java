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

package org.napile.asmNew.firstTests;

import org.napile.compiler.lang.rt.NapileLangPackage;
import org.napile.asm.tree.members.AnnotationNode;
import org.napile.asm.tree.members.ClassNode;
import org.napile.asm.tree.members.types.ClassTypeNode;
import org.napile.asmNew.Modifier;

/**
 * @author VISTALL
 * @date 1:12/14.08.12
 */
public class NodeUtil
{
	static ClassNode createTestNode()
	{
		ClassNode classNode = new ClassNode();
		classNode.visit(Modifier.list(Modifier.ABSTRACT), NapileLangPackage.INT, null);

		classNode.visitSuper(new ClassTypeNode(NapileLangPackage.ANY).addTypeParameter(new ClassTypeNode(NapileLangPackage.BOOL, true).addAnnotation(new AnnotationNode(NapileLangPackage.STRING))));

		return classNode;
	}
}
