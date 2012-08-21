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

import java.io.PrintStream;
import java.util.Map;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.CallableMemberDescriptor;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.MutableClassDescriptor;
import org.napile.compiler.lang.descriptors.NamespaceDescriptorImpl;
import org.napile.compiler.lang.descriptors.PropertyDescriptor;
import org.napile.compiler.lang.descriptors.SimpleFunctionDescriptor;
import org.napile.compiler.lang.descriptors.WithDeferredResolve;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileConstructor;
import org.napile.compiler.lang.psi.NapileDeclaration;
import org.napile.compiler.lang.psi.NapileDeclarationContainer;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.psi.NapileNamedFunction;
import org.napile.compiler.lang.psi.NapileObjectDeclaration;
import org.napile.compiler.lang.psi.NapileProperty;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.resolve.scopes.WritableScope;
import com.google.common.collect.Maps;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

/**
 * @author abreslav
 */
public class TopDownAnalysisContext implements BodiesResolveContext
{

	private final Map<NapileClass, MutableClassDescriptor> classes = Maps.newLinkedHashMap();
	private final Map<NapileObjectDeclaration, MutableClassDescriptor> objects = Maps.newLinkedHashMap();
	protected final Map<NapileFile, NamespaceDescriptorImpl> namespaceDescriptors = Maps.newHashMap();

	private final Map<NapileDeclaration, JetScope> declaringScopes = Maps.newHashMap();
	private final Map<NapileConstructor, ConstructorDescriptor> constructors = Maps.newLinkedHashMap();
	private final Map<NapileNamedFunction, SimpleFunctionDescriptor> functions = Maps.newLinkedHashMap();
	private final Map<NapileProperty, PropertyDescriptor> properties = Maps.newLinkedHashMap();
	private Map<NapileDeclaration, CallableMemberDescriptor> members = null;

	// File scopes - package scope extended with imports
	protected final Map<NapileFile, WritableScope> namespaceScopes = Maps.newHashMap();

	public final Map<NapileDeclarationContainer, WithDeferredResolve> forDeferredResolver = Maps.newHashMap();

	public final Map<NapileDeclarationContainer, JetScope> normalScope = Maps.newHashMap();

	private StringBuilder debugOutput;


	private TopDownAnalysisParameters topDownAnalysisParameters;

	@Override
	@Inject
	public void setTopDownAnalysisParameters(TopDownAnalysisParameters topDownAnalysisParameters)
	{
		this.topDownAnalysisParameters = topDownAnalysisParameters;
	}

	public TopDownAnalysisParameters getTopDownAnalysisParameters()
	{
		return topDownAnalysisParameters;
	}

	public void debug(Object message)
	{
		if(debugOutput != null)
		{
			debugOutput.append(message).append("\n");
		}
	}

	@SuppressWarnings("UnusedDeclaration")
    /*package*/ void enableDebugOutput()
	{
		if(debugOutput == null)
		{
			debugOutput = new StringBuilder();
		}
	}

	/*package*/ void printDebugOutput(PrintStream out)
	{
		if(debugOutput != null)
		{
			out.print(debugOutput);
		}
	}

	@Override
	public boolean completeAnalysisNeeded(@NotNull PsiElement element)
	{
		PsiFile containingFile = element.getContainingFile();
		boolean result = containingFile != null && topDownAnalysisParameters.getAnalyzeCompletely().apply(containingFile);
		if(!result)
		{
			debug(containingFile);
		}
		return result;
	}

	@Override
	public Map<NapileClass, MutableClassDescriptor> getClasses()
	{
		return classes;
	}

	@Override
	public Map<NapileObjectDeclaration, MutableClassDescriptor> getObjects()
	{
		return objects;
	}

	public Map<NapileFile, WritableScope> getNamespaceScopes()
	{
		return namespaceScopes;
	}

	public Map<NapileFile, NamespaceDescriptorImpl> getNamespaceDescriptors()
	{
		return namespaceDescriptors;
	}

	@Override
	public Map<NapileConstructor, ConstructorDescriptor> getConstructors()
	{
		return constructors;
	}

	@Override
	public Map<NapileProperty, PropertyDescriptor> getProperties()
	{
		return properties;
	}

	@Override
	public Map<NapileDeclaration, JetScope> getDeclaringScopes()
	{
		return declaringScopes;
	}

	@Override
	public Map<NapileNamedFunction, SimpleFunctionDescriptor> getFunctions()
	{
		return functions;
	}

	public Map<NapileDeclaration, CallableMemberDescriptor> getMembers()
	{
		if(members == null)
		{
			members = Maps.newHashMap();
			members.putAll(functions);
			members.putAll(properties);
		}
		return members;
	}
}
