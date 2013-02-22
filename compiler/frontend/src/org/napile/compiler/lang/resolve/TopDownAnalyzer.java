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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.resolve.name.FqName;
import org.napile.compiler.analyzer.AnalyzeContext;
import org.napile.compiler.lang.descriptors.DescriptorBuilder;
import org.napile.compiler.lang.descriptors.DescriptorBuilderDummy;
import org.napile.compiler.lang.descriptors.ModuleDescriptor;
import org.napile.compiler.lang.descriptors.MutableClassDescriptor;
import org.napile.compiler.lang.descriptors.PackageDescriptorImpl;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.resolve.processors.BodyResolver;
import org.napile.compiler.lang.resolve.processors.DeclarationResolver;
import org.napile.compiler.lang.resolve.processors.OverloadResolver;
import org.napile.compiler.lang.resolve.processors.OverrideResolver;
import org.napile.compiler.lang.resolve.processors.TypeHierarchyResolver;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.resolve.scopes.WritableScope;
import org.napile.compiler.lang.resolve.scopes.WritableScopeImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiCompiledFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

/**
 * @author abreslav
 */
public class TopDownAnalyzer
{

	@NotNull
	private DeclarationResolver declarationResolver;
	@NotNull
	private TypeHierarchyResolver typeHierarchyResolver;
	@NotNull
	private OverrideResolver overrideResolver;
	@NotNull
	private OverloadResolver overloadResolver;
	@NotNull
	private TopDownAnalysisParameters topDownAnalysisParameters;
	@NotNull
	private TopDownAnalysisContext context;
	@NotNull
	private BindingTrace trace;
	@NotNull
	private ModuleDescriptor moduleDescriptor;
	@NotNull
	private NamespaceFactoryImpl namespaceFactory;
	@NotNull
	private BodyResolver bodyResolver;

	@Inject
	public void setDeclarationResolver(@NotNull DeclarationResolver declarationResolver)
	{
		this.declarationResolver = declarationResolver;
	}

	@Inject
	public void setTypeHierarchyResolver(@NotNull TypeHierarchyResolver typeHierarchyResolver)
	{
		this.typeHierarchyResolver = typeHierarchyResolver;
	}

	@Inject
	public void setOverrideResolver(@NotNull OverrideResolver overrideResolver)
	{
		this.overrideResolver = overrideResolver;
	}

	@Inject
	public void setOverloadResolver(@NotNull OverloadResolver overloadResolver)
	{
		this.overloadResolver = overloadResolver;
	}

	@Inject
	public void setTopDownAnalysisParameters(@NotNull TopDownAnalysisParameters topDownAnalysisParameters)
	{
		this.topDownAnalysisParameters = topDownAnalysisParameters;
	}

	@Inject
	public void setTrace(@NotNull BindingTrace trace)
	{
		this.trace = trace;
	}

	@Inject
	public void setContext(@NotNull TopDownAnalysisContext context)
	{
		this.context = context;
	}

	@Inject
	public void setModuleDescriptor(@NotNull ModuleDescriptor moduleDescriptor)
	{
		this.moduleDescriptor = moduleDescriptor;
	}

	@Inject
	public void setNamespaceFactory(@NotNull NamespaceFactoryImpl namespaceFactory)
	{
		this.namespaceFactory = namespaceFactory;
	}

	@Inject
	public void setBodyResolver(@NotNull BodyResolver bodyResolver)
	{
		this.bodyResolver = bodyResolver;
	}

	public void doProcessSource(JetScope outerScope, DescriptorBuilder owner, Collection<? extends PsiElement> declarations)
	{
		//        context.enableDebugOutput();
		context.debug("Enter");

		typeHierarchyResolver.process(outerScope, owner, declarations);
		declarationResolver.process(outerScope);
		overrideResolver.process();

		lockScopes();

		overloadResolver.process();

		bodyResolver.resolveBodies(context);

		context.debug("Exit");
		context.printDebugOutput(System.out);
	}

	private void lockScopes()
	{
		for(MutableClassDescriptor mutableClassDescriptor : context.getClasses().values())
		{
			mutableClassDescriptor.lockScopes();
		}
		for(MutableClassDescriptor mutableClassDescriptor : context.getAnonymous().values())
		{
			mutableClassDescriptor.lockScopes();
		}
		for(Map.Entry<NapileFile, WritableScope> namespaceScope : context.getNamespaceScopes().entrySet())
		{
			namespaceScope.getValue().changeLockLevel(WritableScope.LockLevel.READING);
		}
	}

	public void analyzeFiles(@NotNull Project project, @NotNull AnalyzeContext analyzeContext)
	{
		final WritableScope scope = new WritableScopeImpl(JetScope.EMPTY, moduleDescriptor, new TraceBasedRedeclarationHandler(trace), "Root scope in analyzeNamespace");

		scope.changeLockLevel(WritableScope.LockLevel.BOTH);

		PackageDescriptorImpl rootNs = namespaceFactory.createNamespaceDescriptorPathIfNeeded(FqName.ROOT);

		// Import a scope that contains all top-level namespaces that come from dependencies
		// This makes the namespaces visible at all, does not import themselves
		scope.importScope(rootNs.getMemberScope());

		scope.changeLockLevel(WritableScope.LockLevel.READING);

		DescriptorBuilderDummy owner = new DescriptorBuilderDummy();

		PsiManager manager = PsiManager.getInstance(project);
		List<NapileFile> files = new ArrayList<NapileFile>(analyzeContext.getFiles().size());

		for(VirtualFile virtualFile : analyzeContext.getBootpath())
		{
			collect(files, virtualFile, manager);
		}

		files.addAll(analyzeContext.getFiles());


		// dummy builder is used because "root" is module descriptor,
		// namespaces added to module explicitly in
		doProcessSource(scope, owner, files);
	}

	private void collect(@NotNull List<NapileFile> list, @NotNull VirtualFile virtualFile, @NotNull PsiManager manager)
	{
		PsiFile file = manager.findFile(virtualFile);
		if(file instanceof NapileFile)
			list.add(((NapileFile) file));
		else
		{
			final VirtualFile[] children = virtualFile.getChildren();
			if(children == null)
				return;

			for(VirtualFile child : children)
			{
				collect(list, child, manager);
			}
		}
	}
}