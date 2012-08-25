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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.resolve.name.FqName;
import org.napile.compiler.lang.resolve.name.Name;
import org.napile.compiler.di.InjectorForTopDownAnalyzerBasic;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.ModuleDescriptor;
import org.napile.compiler.lang.descriptors.MutableClassDescriptor;
import org.napile.compiler.lang.descriptors.MutableClassDescriptorLite;
import org.napile.compiler.lang.descriptors.NamespaceDescriptorImpl;
import org.napile.compiler.lang.descriptors.NamespaceLikeBuilder;
import org.napile.compiler.lang.descriptors.NamespaceLikeBuilderDummy;
import org.napile.compiler.lang.descriptors.PropertyDescriptor;
import org.napile.compiler.lang.descriptors.SimpleFunctionDescriptor;
import org.napile.compiler.lang.psi.NapileClassOrObject;
import org.napile.compiler.lang.psi.NapileDeclaration;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.resolve.scopes.WritableScope;
import org.napile.compiler.lang.resolve.scopes.WritableScopeImpl;
import org.napile.compiler.lang.types.lang.JetStandardClasses;
import com.google.common.base.Predicates;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;

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


	public void doProcess(JetScope outerScope, NamespaceLikeBuilder owner, Collection<? extends PsiElement> declarations)
	{
		//        context.enableDebugOutput();
		context.debug("Enter");

		typeHierarchyResolver.process(outerScope, owner, declarations);
		declarationResolver.process(outerScope);
		overrideResolver.process();

		lockScopes();

		overloadResolver.process();

		if(!topDownAnalysisParameters.isAnalyzingBootstrapLibrary())
		{
			bodyResolver.resolveBodies(context);
		}

		context.debug("Exit");
		context.printDebugOutput(System.out);
	}

	private void lockScopes()
	{
		for(MutableClassDescriptor mutableClassDescriptor : context.getClasses().values())
		{
			mutableClassDescriptor.lockScopes();
		}
		for(MutableClassDescriptor mutableClassDescriptor : context.getObjects().values())
		{
			mutableClassDescriptor.lockScopes();
		}
		for(Map.Entry<NapileFile, WritableScope> namespaceScope : context.getNamespaceScopes().entrySet())
		{
			namespaceScope.getValue().changeLockLevel(WritableScope.LockLevel.READING);
		}
	}

	public static void processStandardLibraryNamespace(@NotNull Project project, @NotNull BindingTrace trace, @NotNull WritableScope outerScope, @NotNull NamespaceDescriptorImpl standardLibraryNamespace, @NotNull List<NapileFile> files)
	{

		TopDownAnalysisParameters topDownAnalysisParameters = new TopDownAnalysisParameters(Predicates.<NapileFile>alwaysFalse(), true, false, Collections.<AnalyzerScriptParameter>emptyList());
		InjectorForTopDownAnalyzerBasic injector = new InjectorForTopDownAnalyzerBasic(project, topDownAnalysisParameters, new ObservableBindingTrace(trace), JetStandardClasses.FAKE_STANDARD_CLASSES_MODULE);

		injector.getTopDownAnalyzer().doProcessStandardLibraryNamespace(outerScope, standardLibraryNamespace, files);
	}

	private void doProcessStandardLibraryNamespace(WritableScope outerScope, NamespaceDescriptorImpl standardLibraryNamespace, List<NapileFile> files)
	{
		ArrayList<NapileDeclaration> toAnalyze = new ArrayList<NapileDeclaration>();
		for(NapileFile file : files)
		{
			context.getNamespaceDescriptors().put(file, standardLibraryNamespace);
			context.getNamespaceScopes().put(file, standardLibraryNamespace.getMemberScope());
			toAnalyze.addAll(file.getDeclarations());
		}
		//        context.getDeclaringScopes().put(file, outerScope);

		doProcess(outerScope, standardLibraryNamespace.getBuilder(), toAnalyze);
	}

	public static void processClassOrObject(@NotNull Project project, @NotNull final BindingTrace trace, @NotNull JetScope outerScope, @NotNull final DeclarationDescriptor containingDeclaration, @NotNull NapileClassOrObject object)
	{
		ModuleDescriptor moduleDescriptor = new ModuleDescriptor(Name.special("<dummy for object>"));

		TopDownAnalysisParameters topDownAnalysisParameters = new TopDownAnalysisParameters(Predicates.equalTo(object.getContainingFile()), false, true, Collections.<AnalyzerScriptParameter>emptyList());

		InjectorForTopDownAnalyzerBasic injector = new InjectorForTopDownAnalyzerBasic(project, topDownAnalysisParameters, new ObservableBindingTrace(trace), moduleDescriptor);

		injector.getTopDownAnalyzer().doProcess(outerScope, new NamespaceLikeBuilder()
		{

			@NotNull
			@Override
			public DeclarationDescriptor getOwnerForChildren()
			{
				return containingDeclaration;
			}

			@Override
			public void addClassifierDescriptor(@NotNull MutableClassDescriptorLite classDescriptor)
			{

			}

			@Override
			public void addObjectDescriptor(@NotNull MutableClassDescriptorLite objectDescriptor)
			{

			}

			@Override
			public void addFunctionDescriptor(@NotNull SimpleFunctionDescriptor functionDescriptor)
			{
				throw new UnsupportedOperationException();
			}

			@Override
			public void addPropertyDescriptor(@NotNull PropertyDescriptor propertyDescriptor)
			{

			}

			@Override
			public ClassObjectStatus setClassObjectDescriptor(@NotNull MutableClassDescriptorLite classObjectDescriptor)
			{
				return ClassObjectStatus.NOT_ALLOWED;
			}
		}, Collections.<PsiElement>singletonList(object));
	}

	public void analyzeFiles(@NotNull Collection<NapileFile> files, @NotNull List<AnalyzerScriptParameter> scriptParameters)
	{
		final WritableScope scope = new WritableScopeImpl(JetScope.EMPTY, moduleDescriptor, new TraceBasedRedeclarationHandler(trace), "Root scope in analyzeNamespace");

		scope.changeLockLevel(WritableScope.LockLevel.BOTH);

		NamespaceDescriptorImpl rootNs = namespaceFactory.createNamespaceDescriptorPathIfNeeded(FqName.ROOT);

		// Import a scope that contains all top-level namespaces that come from dependencies
		// This makes the namespaces visible at all, does not import themselves
		scope.importScope(rootNs.getMemberScope());

		scope.changeLockLevel(WritableScope.LockLevel.READING);

		// dummy builder is used because "root" is module descriptor,
		// namespaces added to module explicitly in
		doProcess(scope, new NamespaceLikeBuilderDummy(), files);
	}
}