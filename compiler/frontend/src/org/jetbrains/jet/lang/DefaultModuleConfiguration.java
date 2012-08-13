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

package org.jetbrains.jet.lang;

import java.util.Collection;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetImportDirective;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import com.intellij.openapi.project.Project;

/**
 * @author svtk
 */
public class DefaultModuleConfiguration implements ModuleConfiguration
{
	public static final ImportPath[] DEFAULT_JET_IMPORTS = new ImportPath[]
	{
		new ImportPath("napile.lang.*")
	};

	@NotNull
	private Project project;
	@NotNull
	private BuiltinsScopeExtensionMode builtinsScopeExtensionMode;

	public static DefaultModuleConfiguration createStandardConfiguration(Project project, @NotNull BuiltinsScopeExtensionMode builtinsScopeExtensionMode)
	{
		DefaultModuleConfiguration defaultModuleConfiguration = new DefaultModuleConfiguration();
		defaultModuleConfiguration.setProject(project);
		defaultModuleConfiguration.setBuiltinsScopeExtensionMode(builtinsScopeExtensionMode);
		return defaultModuleConfiguration;
	}

	@Inject
	public void setProject(@NotNull Project project)
	{
		this.project = project;
	}

	@Inject
	public void setBuiltinsScopeExtensionMode(@NotNull BuiltinsScopeExtensionMode builtinsScopeExtensionMode)
	{
		this.builtinsScopeExtensionMode = builtinsScopeExtensionMode;
	}

	@Override
	public void addDefaultImports(@NotNull Collection<JetImportDirective> directives)
	{
		for(ImportPath defaultJetImport : DEFAULT_JET_IMPORTS)
		{
			directives.add(JetPsiFactory.createImportDirective(project, defaultJetImport));
		}
	}

	@Override
	public void extendNamespaceScope(@NotNull BindingTrace trace, @NotNull NamespaceDescriptor namespaceDescriptor, @NotNull WritableScope namespaceMemberScope)
	{

	}
}
