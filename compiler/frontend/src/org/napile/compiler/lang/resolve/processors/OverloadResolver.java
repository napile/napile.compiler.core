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
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.descriptors.CallableMemberDescriptor;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.MutableClassDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptorImpl;
import org.napile.compiler.lang.diagnostics.Errors;
import org.napile.compiler.lang.psi.NapileAnonymClass;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileClassLike;
import org.napile.compiler.lang.psi.NapileDeclaration;
import org.napile.compiler.lang.resolve.BindingContextUtils;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.OverloadUtil;
import org.napile.compiler.lang.resolve.TopDownAnalysisContext;
import com.google.common.collect.Sets;
import com.intellij.openapi.util.Pair;
import com.intellij.util.containers.MultiMap;

/**
 * @author Stepan Koltsov
 */
public class OverloadResolver
{
	private TopDownAnalysisContext context;
	private BindingTrace trace;


	@Inject
	public void setContext(TopDownAnalysisContext context)
	{
		this.context = context;
	}

	@Inject
	public void setTrace(BindingTrace trace)
	{
		this.trace = trace;
	}


	public void process()
	{
		checkOverloads();
	}

	private void checkOverloads()
	{
		for(Map.Entry<NapileClass, MutableClassDescriptor> entry : context.getClasses().entrySet())
			checkOverloadsInAClass(entry.getValue(), entry.getKey());

		for(Map.Entry<NapileAnonymClass, MutableClassDescriptor> entry : context.getAnonymous().entrySet())
			checkOverloadsInAClass(entry.getValue(), entry.getKey());
	}

	private String nameForErrorMessage(ClassDescriptor classDescriptor, NapileClassLike jetClass)
	{
		String name = jetClass.getName();
		if(name != null)
		{
			return name;
		}
		if(jetClass instanceof NapileAnonymClass)
		{
			// must be class object
			name = classDescriptor.getContainingDeclaration().getName().getName();
			return "class object " + name;
		}
		// safe
		return "<unknown>";
	}

	private void checkOverloadsInAClass(MutableClassDescriptor classDescriptor, NapileClassLike klass)
	{
		MultiMap<Name, CallableMemberDescriptor> functionsByName = MultiMap.create();

		for(CallableMemberDescriptor function : classDescriptor.getDeclaredCallableMembers())
		{
			functionsByName.putValue(function.getName(), function);
		}

		for(Map.Entry<Name, Collection<CallableMemberDescriptor>> e : functionsByName.entrySet())
		{
			checkOverloadsWithSameName(e.getKey(), e.getValue(), nameForErrorMessage(classDescriptor, klass));
		}
	}

	private void checkOverloadsWithSameName(@NotNull Name name, Collection<CallableMemberDescriptor> functions, @NotNull String functionContainer)
	{
		if(functions.size() == 1)
		{
			// microoptimization
			return;
		}
		Set<Pair<NapileDeclaration, CallableMemberDescriptor>> redeclarations = findRedeclarations(functions);
		reportRedeclarations(functionContainer, redeclarations);
	}

	@NotNull
	private Set<Pair<NapileDeclaration, CallableMemberDescriptor>> findRedeclarations(@NotNull Collection<CallableMemberDescriptor> functions)
	{
		Set<Pair<NapileDeclaration, CallableMemberDescriptor>> redeclarations = Sets.newHashSet();
		for(CallableMemberDescriptor member : functions)
		{
			for(CallableMemberDescriptor member2 : functions)
			{
				if(member == member2)
				{
					continue;
				}

				OverloadUtil.OverloadCompatibilityInfo overloadable = OverloadUtil.isOverloadable(member, member2);
				if(!overloadable.isSuccess())
				{
					NapileDeclaration jetDeclaration = (NapileDeclaration) BindingContextUtils.descriptorToDeclaration(trace.getBindingContext(), member);
					redeclarations.add(Pair.create(jetDeclaration, member));
				}
			}
		}
		return redeclarations;
	}

	private void reportRedeclarations(@NotNull String functionContainer, @NotNull Set<Pair<NapileDeclaration, CallableMemberDescriptor>> redeclarations)
	{
		for(Pair<NapileDeclaration, CallableMemberDescriptor> redeclaration : redeclarations)
		{
			CallableMemberDescriptor memberDescriptor = redeclaration.getSecond();
			NapileDeclaration jetDeclaration = redeclaration.getFirst();
			if(memberDescriptor instanceof VariableDescriptorImpl)
			{
				trace.report(Errors.REDECLARATION.on(jetDeclaration, memberDescriptor.getName().getName()));
			}
			else
			{
				trace.report(Errors.CONFLICTING_OVERLOADS.on(jetDeclaration, memberDescriptor, functionContainer));
			}
		}
	}
}
