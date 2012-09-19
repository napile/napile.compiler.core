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

package org.napile.compiler.lang.resolve.calls;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.ClassifierDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.PropertyDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.resolve.DescriptorUtils;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.types.ErrorUtils;
import org.napile.compiler.lang.types.JetType;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author abreslav
 */
public class CallableDescriptorCollectors
{

	/*package*/ static CallableDescriptorCollector<MethodDescriptor> FUNCTIONS = new CallableDescriptorCollector<MethodDescriptor>()
	{

		@NotNull
		@Override
		public Collection<MethodDescriptor> getNonExtensionsByName(JetScope scope, Name name)
		{
			Set<MethodDescriptor> methods = Sets.newLinkedHashSet(scope.getFunctions(name));
			for(Iterator<MethodDescriptor> iterator = methods.iterator(); iterator.hasNext(); )
			{
				MethodDescriptor methodDescriptor = iterator.next();
				if(methodDescriptor.getReceiverParameter().exists())
				{
					iterator.remove();
				}
			}
			addConstructors(scope, name, methods);
			return methods;
		}

		@NotNull
		@Override
		public Collection<MethodDescriptor> getMembersByName(@NotNull JetType receiverType, Name name)
		{
			JetScope receiverScope = receiverType.getMemberScope();
			Set<MethodDescriptor> members = Sets.newHashSet(receiverScope.getFunctions(name));
			addConstructors(receiverScope, name, members);
			return members;
		}

		@NotNull
		@Override
		public Collection<MethodDescriptor> getNonMembersByName(JetScope scope, Name name)
		{
			return scope.getFunctions(name);
		}

		private void addConstructors(JetScope scope, Name name, Collection<MethodDescriptor> methods)
		{
			ClassifierDescriptor classifier = scope.getClassifier(name);
			if(classifier instanceof ClassDescriptor && !ErrorUtils.isError(classifier.getTypeConstructor()))
			{
				ClassDescriptor classDescriptor = (ClassDescriptor) classifier;
				methods.addAll(classDescriptor.getConstructors());
			}
		}
	};

	/*package*/ static CallableDescriptorCollector<VariableDescriptor> VARIABLES = new CallableDescriptorCollector<VariableDescriptor>()
	{

		@NotNull
		@Override
		public Collection<VariableDescriptor> getNonExtensionsByName(JetScope scope, Name name)
		{
			VariableDescriptor descriptor = scope.getLocalVariable(name);
			if(descriptor == null)
			{
				descriptor = DescriptorUtils.filterNonExtensionProperty(scope.getProperties(name));
			}
			if(descriptor == null)
				return Collections.emptyList();
			return Collections.singleton(descriptor);
		}

		@NotNull
		@Override
		public Collection<VariableDescriptor> getMembersByName(@NotNull JetType receiverType, Name name)
		{
			return receiverType.getMemberScope().getProperties(name);
		}

		@NotNull
		@Override
		public Collection<VariableDescriptor> getNonMembersByName(JetScope scope, Name name)
		{
			Collection<VariableDescriptor> result = Sets.newLinkedHashSet();

			VariableDescriptor localVariable = scope.getLocalVariable(name);
			if(localVariable != null)
			{
				result.add(localVariable);
			}
			result.addAll(scope.getProperties(name));
			return result;
		}
	};

	/*package*/ static CallableDescriptorCollector<VariableDescriptor> PROPERTIES = new CallableDescriptorCollector<VariableDescriptor>()
	{
		private Collection<VariableDescriptor> filterProperties(Collection<? extends VariableDescriptor> variableDescriptors)
		{
			ArrayList<VariableDescriptor> properties = Lists.newArrayList();
			for(VariableDescriptor descriptor : variableDescriptors)
			{
				if(descriptor instanceof PropertyDescriptor)
				{
					properties.add(descriptor);
				}
			}
			return properties;
		}

		@NotNull
		@Override
		public Collection<VariableDescriptor> getNonExtensionsByName(JetScope scope, Name name)
		{
			return filterProperties(VARIABLES.getNonExtensionsByName(scope, name));
		}

		@NotNull
		@Override
		public Collection<VariableDescriptor> getMembersByName(@NotNull JetType receiver, Name name)
		{
			return filterProperties(VARIABLES.getMembersByName(receiver, name));
		}

		@NotNull
		@Override
		public Collection<VariableDescriptor> getNonMembersByName(JetScope scope, Name name)
		{
			return filterProperties(VARIABLES.getNonMembersByName(scope, name));
		}
	};

	/*package*/ static List<CallableDescriptorCollector<? extends CallableDescriptor>> FUNCTIONS_AND_VARIABLES = Lists.newArrayList(FUNCTIONS, VARIABLES);
}
