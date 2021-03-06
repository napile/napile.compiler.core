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

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.ClassKind;
import org.napile.compiler.lang.descriptors.ClassifierDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.PackageDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.resolve.scopes.WritableScope;
import com.google.common.collect.Lists;
import com.intellij.openapi.util.Pair;

/**
 * @author svtk
 */
public interface Importer
{

	void addAllUnderImport(@NotNull DeclarationDescriptor descriptor);

	void addAliasImport(@NotNull DeclarationDescriptor descriptor, @NotNull Name aliasName);

	Importer DO_NOTHING = new Importer()
	{
		@Override
		public void addAllUnderImport(@NotNull DeclarationDescriptor descriptor)
		{
		}

		@Override
		public void addAliasImport(@NotNull DeclarationDescriptor descriptor, @NotNull Name aliasName)
		{
		}
	};

	class StandardImporter implements Importer
	{
		private final WritableScope namespaceScope;

		public StandardImporter(WritableScope namespaceScope)
		{
			this.namespaceScope = namespaceScope;
		}

		@Override
		public void addAllUnderImport(@NotNull DeclarationDescriptor descriptor)
		{
			importAllUnderDeclaration(descriptor);
		}

		@Override
		public void addAliasImport(@NotNull DeclarationDescriptor descriptor, @NotNull Name aliasName)
		{
			importDeclarationAlias(descriptor, aliasName);
		}

		protected void importAllUnderDeclaration(@NotNull DeclarationDescriptor descriptor)
		{
			if(descriptor instanceof PackageDescriptor)
			{
				namespaceScope.importScope(((PackageDescriptor) descriptor).getMemberScope());
			}
			if(descriptor instanceof ClassDescriptor && ((ClassDescriptor) descriptor).getKind() != ClassKind.ANONYM_CLASS)
			{
				ClassDescriptor classDescriptor = (ClassDescriptor) descriptor;
				namespaceScope.importScope(classDescriptor.getStaticOuterScope());
			}
		}

		protected void importDeclarationAlias(@NotNull DeclarationDescriptor descriptor, @NotNull Name aliasName)
		{
			if(descriptor instanceof ClassifierDescriptor)
			{
				namespaceScope.importClassifierAlias(aliasName, (ClassifierDescriptor) descriptor);
			}
			else if(descriptor instanceof PackageDescriptor)
			{
				namespaceScope.importNamespaceAlias(aliasName, (PackageDescriptor) descriptor);
			}
			else if(descriptor instanceof MethodDescriptor)
			{
				namespaceScope.importFunctionAlias(aliasName, (MethodDescriptor) descriptor);
			}
			else if(descriptor instanceof VariableDescriptor)
			{
				namespaceScope.importVariableAlias(aliasName, (VariableDescriptor) descriptor);
			}
		}
	}

	class DelayedImporter extends StandardImporter
	{
		private final List<Pair<DeclarationDescriptor, Name>> imports = Lists.newArrayList();

		public DelayedImporter(@NotNull WritableScope namespaceScope)
		{
			super(namespaceScope);
		}

		@Override
		public void addAllUnderImport(@NotNull DeclarationDescriptor descriptor)
		{
			imports.add(Pair.<DeclarationDescriptor, Name>create(descriptor, null));
		}

		@Override
		public void addAliasImport(@NotNull DeclarationDescriptor descriptor, @NotNull Name aliasName)
		{
			imports.add(Pair.create(descriptor, aliasName));
		}

		public void processImports()
		{
			for(Pair<DeclarationDescriptor, Name> anImport : imports)
			{
				DeclarationDescriptor descriptor = anImport.getFirst();
				Name aliasName = anImport.getSecond();
				boolean allUnderImport = aliasName == null;
				if(allUnderImport)
				{
					importAllUnderDeclaration(descriptor);
				}
				else
				{
					importDeclarationAlias(descriptor, aliasName);
				}
			}
		}
	}
}
