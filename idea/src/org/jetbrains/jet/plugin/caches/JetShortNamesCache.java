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

package org.jetbrains.jet.plugin.caches;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingUtils;
import org.jetbrains.jet.plugin.project.WholeProjectAnalyzerFacade;
import org.jetbrains.jet.plugin.stubindex.JetExtensionFunctionNameIndex;
import org.jetbrains.jet.plugin.stubindex.JetFullClassNameIndex;
import org.jetbrains.jet.plugin.stubindex.JetShortClassNameIndex;
import org.jetbrains.jet.plugin.stubindex.JetShortFunctionNameIndex;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.HashSet;

/**
 * Will provide both java elements from jet context and some special declarations special to jet.
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

	/**
	 * Return jet class names form jet project sources which should be visible from java.
	 */
	@NotNull
	public String[] getAllClassNames()
	{
		Collection<String> classNames = JetShortClassNameIndex.getInstance().getAllKeys(project);
		return ArrayUtil.toStringArray(classNames);
	}

	/**
	 * Return class names form jet sources in given scope which should be visible as Java classes.
	 */
	@NotNull
	public JetClassOrObject[] getClassesByName(@NotNull @NonNls String name, @NotNull GlobalSearchScope scope)
	{
		// Quick check for classes from getAllClassNames()
		Collection<JetClassOrObject> classOrObjects = JetShortClassNameIndex.getInstance().get(name, project, scope);
		return classOrObjects.isEmpty() ? JetClassOrObject.EMPTY_ARRAY : classOrObjects.toArray(JetClassOrObject.EMPTY_ARRAY);
	}

	/**
	 * Get jet non-extension top-level function names. Method is allowed to give invalid names - all result should be
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
	public Collection<FunctionDescriptor> getTopLevelFunctionDescriptorsByName(@NotNull String name, @NotNull JetSimpleNameExpression expression, @NotNull GlobalSearchScope scope)
	{
		HashSet<FunctionDescriptor> result = new HashSet<FunctionDescriptor>();

		JetFile jetFile = (JetFile) expression.getContainingFile();
		BindingContext context = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile(jetFile).getBindingContext();
		JetScope jetScope = context.get(BindingContext.RESOLUTION_SCOPE, expression);

		if(jetScope == null)
		{
			return result;
		}

		Collection<JetNamedFunction> jetNamedFunctions = JetShortFunctionNameIndex.getInstance().get(name, project, scope);
		for(JetNamedFunction jetNamedFunction : jetNamedFunctions)
		{
			SimpleFunctionDescriptor functionDescriptor = context.get(BindingContext.FUNCTION, jetNamedFunction);
			if(functionDescriptor != null)
			{
				result.add(functionDescriptor);
			}
		}

		return result;
	}

	/**
	 * Get jet extensions top-level function names. Method is allowed to give invalid names - all result should be
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
	public Collection<DeclarationDescriptor> getJetCallableExtensions(@NotNull Condition<String> acceptedNameCondition, @NotNull JetSimpleNameExpression expression, @NotNull GlobalSearchScope searchScope)
	{
		Collection<DeclarationDescriptor> resultDescriptors = new ArrayList<DeclarationDescriptor>();

		JetFile jetFile = (JetFile) expression.getContainingFile();

		BindingContext context = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile(jetFile).getBindingContext();
		JetExpression receiverExpression = expression.getReceiverExpression();

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
							if(extensionFunction instanceof JetNamedFunction)
							{
								functionFQNs.add(JetPsiUtil.getFQName((JetNamedFunction) extensionFunction));
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

	public Collection<ClassDescriptor> getJetClassesDescriptors(@NotNull Condition<String> acceptedShortNameCondition, @NotNull JetFile jetFile)
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
