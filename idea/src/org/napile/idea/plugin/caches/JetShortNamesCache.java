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

package org.napile.idea.plugin.caches;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.psi.NapileClassLike;
import org.napile.compiler.lang.resolve.name.FqName;
import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.SimpleMethodDescriptor;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileElement;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.psi.NapileNamedFunction;
import org.napile.compiler.lang.psi.NapilePsiUtil;
import org.napile.compiler.lang.psi.NapileSimpleNameExpression;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.JetFilesProvider;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.expressions.ExpressionTypingUtils;
import org.napile.idea.plugin.project.WholeProjectAnalyzerFacade;
import org.napile.idea.plugin.stubindex.JetExtensionFunctionNameIndex;
import org.napile.idea.plugin.stubindex.JetFullClassNameIndex;
import org.napile.idea.plugin.stubindex.JetShortClassNameIndex;
import org.napile.idea.plugin.stubindex.JetShortFunctionNameIndex;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.HashSet;

/**
 * All those declaration are planned to be used in completion.
 *
 * @author Nikolay Krasko
 */
public class JetShortNamesCache
{
	private final Project project;

	@NotNull
	public static JetShortNamesCache getInstance(final Project project)
	{
		return ServiceManager.getService(project, JetShortNamesCache.class);
	}

	public JetShortNamesCache(Project project)
	{
		this.project = project;
	}

	@NotNull
	public Map<NapileClassLike, ClassDescriptor> getAllClassesAndDescriptors(@NotNull NapileElement napileElement, @NotNull GlobalSearchScope globalSearchScope)
	{
		BindingContext context = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile(napileElement.getContainingFile()).getBindingContext();

		JetFilesProvider jetFilesProvider = JetFilesProvider.getInstance(project);

		Map<NapileClassLike, ClassDescriptor> result = new HashMap<NapileClassLike, ClassDescriptor>();

		for(NapileFile temp : jetFilesProvider.allInScope(globalSearchScope))
		{
			for(NapileClass napileClass : temp.getDeclarations())
			{
				DeclarationDescriptor declarationDescriptor = context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, napileClass);

				result.put(napileClass, (ClassDescriptor) declarationDescriptor);
			}
		}
		return result;
	}

	/**
	 * Return idea class names form idea project sources which should be visible from java.
	 */
	@NotNull
	public String[] getAllClassNames()
	{
		Collection<String> classNames = JetShortClassNameIndex.getInstance().getAllKeys(project);
		return ArrayUtil.toStringArray(classNames);
	}

	/**
	 * Return class names form idea sources in given scope which should be visible as Java classes.
	 */
	@NotNull
	public NapileClassLike[] getClassesByName(@NotNull @NonNls String name, @NotNull GlobalSearchScope scope)
	{
		// Quick check for classes from getAllClassNames()
		Collection<NapileClassLike> classOrObjects = JetShortClassNameIndex.getInstance().get(name, project, scope);
		return classOrObjects.isEmpty() ? NapileClassLike.EMPTY_ARRAY : classOrObjects.toArray(NapileClassLike.EMPTY_ARRAY);
	}

	/**
	 * Get idea non-extension top-level function names. Method is allowed to give invalid names - all result should be
	 * checked with getTopLevelFunctionDescriptorsByName().
	 *
	 * @return
	 */
	@NotNull
	public Collection<String> getAllTopLevelFunctionNames()
	{
		Set<String> functionNames = new HashSet<String>();
		functionNames.addAll(JetShortFunctionNameIndex.getInstance().getAllKeys(project));
		return functionNames;
	}

	// TODO: Make it work for properties
	@NotNull
	public Collection<MethodDescriptor> getTopLevelFunctionDescriptorsByName(@NotNull String name, @NotNull NapileSimpleNameExpression expression, @NotNull GlobalSearchScope scope)
	{
		HashSet<MethodDescriptor> result = new HashSet<MethodDescriptor>();

		NapileFile jetFile = (NapileFile) expression.getContainingFile();
		BindingContext context = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile(jetFile).getBindingContext();
		JetScope jetScope = context.get(BindingContext.RESOLUTION_SCOPE, expression);

		if(jetScope == null)
		{
			return result;
		}

		Collection<NapileNamedFunction> jetNamedFunctions = JetShortFunctionNameIndex.getInstance().get(name, project, scope);
		for(NapileNamedFunction jetNamedFunction : jetNamedFunctions)
		{
			SimpleMethodDescriptor functionDescriptor = context.get(BindingContext.METHOD, jetNamedFunction);
			if(functionDescriptor != null)
			{
				result.add(functionDescriptor);
			}
		}

		return result;
	}

	/**
	 * Get idea extensions top-level function names. Method is allowed to give invalid names - all result should be
	 * checked with getAllJetExtensionFunctionsByName().
	 *
	 * @return
	 */
	@NotNull
	public Collection<String> getAllJetExtensionFunctionsNames(@NotNull GlobalSearchScope scope)
	{
		Set<String> extensionFunctionNames = new HashSet<String>();

		extensionFunctionNames.addAll(JetExtensionFunctionNameIndex.getInstance().getAllKeys(project));

		return extensionFunctionNames;
	}

	public Collection<PsiElement> getJetExtensionFunctionsByName(@NotNull String name, @NotNull GlobalSearchScope scope)
	{
		HashSet<PsiElement> functions = new HashSet<PsiElement>();
		functions.addAll(JetExtensionFunctionNameIndex.getInstance().get(name, project, scope));

		return functions;
	}

	// TODO: Make it work for properties
	public Collection<DeclarationDescriptor> getJetCallableExtensions(@NotNull Condition<String> acceptedNameCondition, @NotNull NapileSimpleNameExpression expression, @NotNull GlobalSearchScope searchScope)
	{
		Collection<DeclarationDescriptor> resultDescriptors = new ArrayList<DeclarationDescriptor>();

		NapileFile jetFile = (NapileFile) expression.getContainingFile();

		BindingContext context = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile(jetFile).getBindingContext();
		NapileExpression receiverExpression = expression.getReceiverExpression();

		if(receiverExpression != null)
		{
			JetType expressionType = context.get(BindingContext.EXPRESSION_TYPE, receiverExpression);
			JetScope scope = context.get(BindingContext.RESOLUTION_SCOPE, receiverExpression);

			if(expressionType != null && scope != null)
			{
				Collection<String> extensionFunctionsNames = getAllJetExtensionFunctionsNames(searchScope);

				Set<FqName> functionFQNs = new java.util.HashSet<FqName>();

				// Collect all possible extension function qualified names
				for(String name : extensionFunctionsNames)
				{
					if(acceptedNameCondition.value(name))
					{
						Collection<PsiElement> extensionFunctions = getJetExtensionFunctionsByName(name, searchScope);

						for(PsiElement extensionFunction : extensionFunctions)
						{
							if(extensionFunction instanceof NapileNamedFunction)
							{
								functionFQNs.add(NapilePsiUtil.getFQName((NapileNamedFunction) extensionFunction));
							}
						}
					}
				}

				// Iterate through the function with attempt to resolve found functions
				for(FqName functionFQN : functionFQNs)
				{
					for(CallableDescriptor functionDescriptor : ExpressionTypingUtils.canFindSuitableCall(functionFQN, project, receiverExpression, expressionType, scope))
					{

						resultDescriptors.add(functionDescriptor);
					}
				}
			}
		}

		return resultDescriptors;
	}

	public Collection<ClassDescriptor> getJetClassesDescriptors(@NotNull Condition<String> acceptedShortNameCondition, @NotNull NapileFile jetFile)
	{
		BindingContext context = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile(jetFile).getBindingContext();
		Collection<ClassDescriptor> classDescriptors = new ArrayList<ClassDescriptor>();

		for(String fqName : JetFullClassNameIndex.getInstance().getAllKeys(project))
		{
			FqName classFQName = new FqName(fqName);
			if(acceptedShortNameCondition.value(classFQName.shortName().getName()))
			{
				ClassDescriptor descriptor = context.get(BindingContext.FQNAME_TO_CLASS_DESCRIPTOR, classFQName);
				if(descriptor != null)
				{
					classDescriptors.add(descriptor);
				}
			}
		}

		return classDescriptors;
	}
}
