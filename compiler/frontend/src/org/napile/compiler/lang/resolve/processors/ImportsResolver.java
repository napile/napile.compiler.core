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

package org.napile.compiler.lang.resolve.processors;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.resolve.ImportPath;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.NapileLanguage;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.NamespaceDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.diagnostics.Errors;
import org.napile.compiler.lang.psi.NapileImportDirective;
import org.napile.compiler.lang.psi.NapilePsiFactory;
import org.napile.compiler.lang.psi.NapilePsiUtil;
import org.napile.compiler.lang.psi.NapileSimpleNameExpression;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.Importer;
import org.napile.compiler.lang.resolve.TemporaryBindingTrace;
import org.napile.compiler.lang.resolve.TopDownAnalysisContext;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.resolve.scopes.WritableScope;
import org.napile.compiler.psi.NapileExpression;
import org.napile.compiler.psi.NapileFile;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.intellij.openapi.project.Project;

/**
 * @author abreslav
 * @author svtk
 */
public class ImportsResolver
{
	@NotNull
	private TopDownAnalysisContext context;
	@NotNull
	private QualifiedExpressionResolver qualifiedExpressionResolver;
	@NotNull
	private BindingTrace trace;
	@NotNull
	private Project project;

	@Inject
	public void setContext(@NotNull TopDownAnalysisContext context)
	{
		this.context = context;
	}

	@Inject
	public void setProject(@NotNull Project project)
	{
		this.project = project;
	}

	@Inject
	public void setTrace(@NotNull BindingTrace trace)
	{
		this.trace = trace;
	}

	@Inject
	public void setQualifiedExpressionResolver(@NotNull QualifiedExpressionResolver qualifiedExpressionResolver)
	{
		this.qualifiedExpressionResolver = qualifiedExpressionResolver;
	}

	public void processTypeImports(@NotNull JetScope rootScope)
	{
		processImports(true, rootScope);
	}

	public void processMembersImports(@NotNull JetScope rootScope)
	{
		processImports(false, rootScope);
	}

	private void processImports(boolean onlyClasses, @NotNull JetScope rootScope)
	{
		for(NapileFile file : context.getNamespaceDescriptors().keySet())
		{
			WritableScope namespaceScope = context.getNamespaceScopes().get(file);
			processImportsInFile(onlyClasses, namespaceScope, file.getImportDirectives(), rootScope);
		}
	}

	private void processImportsInFile(boolean classes, WritableScope scope, List<NapileImportDirective> directives, JetScope rootScope)
	{
		processImportsInFile(classes, scope, directives, rootScope, trace, qualifiedExpressionResolver, project);
	}

	public static void processImportsInFile(boolean onlyClasses, @NotNull WritableScope namespaceScope, @NotNull List<NapileImportDirective> importDirectives, @NotNull JetScope rootScope, @NotNull BindingTrace trace, @NotNull QualifiedExpressionResolver qualifiedExpressionResolver, @NotNull Project project)
	{

		Importer.DelayedImporter delayedImporter = new Importer.DelayedImporter(namespaceScope);
		if(!onlyClasses)
		{
			namespaceScope.clearImports();
		}
		Map<NapileImportDirective, DeclarationDescriptor> resolvedDirectives = Maps.newHashMap();
		Collection<NapileImportDirective> defaultImportDirectives = Lists.newArrayList();
		for(ImportPath path : NapileLanguage.DEFAULT_IMPORTS)
			defaultImportDirectives.add(NapilePsiFactory.createImportDirective(project, path));

		for(NapileImportDirective defaultImportDirective : defaultImportDirectives)
		{
			TemporaryBindingTrace temporaryTrace = TemporaryBindingTrace.create(trace); //not to trace errors of default imports
			qualifiedExpressionResolver.processImportReference(defaultImportDirective, rootScope, namespaceScope, delayedImporter, temporaryTrace, onlyClasses);
		}

		for(NapileImportDirective importDirective : importDirectives)
		{
			Collection<? extends DeclarationDescriptor> descriptors = qualifiedExpressionResolver.processImportReference(importDirective, rootScope, namespaceScope, delayedImporter, trace, onlyClasses);
			if(descriptors.size() == 1)
			{
				resolvedDirectives.put(importDirective, descriptors.iterator().next());
			}
		}
		delayedImporter.processImports();

		if(!onlyClasses)
		{
			for(NapileImportDirective importDirective : importDirectives)
			{
				reportUselessImport(importDirective, namespaceScope, resolvedDirectives, trace);
			}
		}
	}

	private static void reportUselessImport(@NotNull NapileImportDirective importDirective, @NotNull WritableScope namespaceScope, @NotNull Map<NapileImportDirective, DeclarationDescriptor> resolvedDirectives, @NotNull BindingTrace trace)
	{

		NapileExpression importedReference = importDirective.getImportedReference();
		if(importedReference == null || !resolvedDirectives.containsKey(importDirective))
		{
			return;
		}
		Name aliasName = NapilePsiUtil.getAliasName(importDirective);
		if(aliasName == null)
		{
			return;
		}

		DeclarationDescriptor wasResolved = resolvedDirectives.get(importDirective);
		DeclarationDescriptor isResolved = null;
		if(wasResolved instanceof ClassDescriptor)
		{
			isResolved = namespaceScope.getClassifier(aliasName);
		}
		else if(wasResolved instanceof VariableDescriptor)
		{
			isResolved = namespaceScope.getLocalVariable(aliasName);
		}
		else if(wasResolved instanceof NamespaceDescriptor)
		{
			isResolved = namespaceScope.getNamespace(aliasName);
		}
		if(isResolved != null && isResolved != wasResolved)
		{
			trace.report(Errors.USELESS_HIDDEN_IMPORT.on(importedReference));
		}
		if(!importDirective.isAllUnder() &&
				importedReference instanceof NapileSimpleNameExpression &&
				importDirective.getAliasName() == null)
		{
			trace.report(Errors.USELESS_SIMPLE_IMPORT.on(importedReference));
		}
	}
}
