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

package org.napile.idea.plugin;

import java.util.LinkedList;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.ModuleDescriptor;
import org.napile.compiler.lang.types.DeferredType;
import org.napile.compiler.lang.types.JetType;
import com.google.common.collect.Lists;

/**
 * @author svtk
 */
public class JetPluginUtil
{
	@NotNull
	private static LinkedList<String> computeTypeFullNameList(JetType type)
	{
		if(type instanceof DeferredType)
		{
			type = ((DeferredType) type).getActualType();
		}
		DeclarationDescriptor declarationDescriptor = type.getConstructor().getDeclarationDescriptor();

		LinkedList<String> fullName = Lists.newLinkedList();
		while(declarationDescriptor != null && !(declarationDescriptor instanceof ModuleDescriptor))
		{
			fullName.addFirst(declarationDescriptor.getName().getName());
			declarationDescriptor = declarationDescriptor.getContainingDeclaration();
		}
		assert fullName.size() > 0;

		return fullName;
	}

}
