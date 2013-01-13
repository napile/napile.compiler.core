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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.descriptors.ClassifierDescriptor;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.types.ErrorUtils;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.MultiTypeConstructor;
import org.napile.compiler.lang.types.MultiTypeEntry;
import org.napile.compiler.lang.types.TypeConstructor;
import com.google.common.collect.Lists;

/**
 * @author abreslav
 */
public class CallableDescriptorCollectors
{

	static CallableDescriptorCollector<MethodDescriptor> METHODS = new CallableDescriptorCollector<MethodDescriptor>()
	{

		@NotNull
		@Override
		public Collection<MethodDescriptor> getNonExtensionsByName(JetScope scope, Name name)
		{
			return fromScope(scope, name);
		}

		@NotNull
		@Override
		public Collection<MethodDescriptor> getMembersByName(@NotNull JetType receiverType, Name name)
		{
			return fromScope(receiverType.getMemberScope(), name);
		}

		@NotNull
		@Override
		public Collection<MethodDescriptor> getNonMembersByName(JetScope scope, Name name)
		{
			/*Collection<MethodDescriptor> methodsByName = scope.getMethods(name);
			List<MethodDescriptor> extensionList = new ArrayList<MethodDescriptor>(methodsByName.size());
			for(MethodDescriptor methodDescriptor : methodsByName)
				if(AnnotationUtils.hasAnnotation(methodDescriptor, NapileAnnotationPackage.EXTENSION))
					extensionList.add(methodDescriptor);
			return extensionList;  */
			return Collections.emptyList();
		}

		private Collection<MethodDescriptor> fromScope(JetScope scope, Name name)
		{
			Collection<MethodDescriptor> methodDescriptors = scope.getMethods(name);
			Collection<ConstructorDescriptor> constructorDescriptors = Collections.emptyList();

			ClassifierDescriptor classifier = scope.getClassifier(name);
			if(classifier != null && !ErrorUtils.isError(classifier.getTypeConstructor()))
			{
				Collection<ConstructorDescriptor> d = classifier.getConstructors();
				constructorDescriptors = new ArrayList<ConstructorDescriptor>(d.size());
				for(ConstructorDescriptor descriptor : d)
					if(!descriptor.isStatic())
						constructorDescriptors.add(descriptor);
			}

			Set<MethodDescriptor> members = new HashSet<MethodDescriptor>(methodDescriptors.size() + constructorDescriptors.size());
			members.addAll(methodDescriptors);
			members.addAll(constructorDescriptors);

			return members;
		}
	};

	static CallableDescriptorCollector<VariableDescriptor> VARIABLES = new CallableDescriptorCollector<VariableDescriptor>()
	{

		@NotNull
		@Override
		public Collection<VariableDescriptor> getNonExtensionsByName(JetScope scope, Name name)
		{
			VariableDescriptor descriptor = scope.getLocalVariable(name);
			if(descriptor == null)
				return scope.getVariables(name);
			else
				return Collections.singleton(descriptor);
		}

		@NotNull
		@Override
		public Collection<VariableDescriptor> getMembersByName(@NotNull JetType receiverType, Name name)
		{
			TypeConstructor typeConstructor = receiverType.getConstructor();
			if(typeConstructor instanceof MultiTypeConstructor)
			{
				List<MultiTypeEntry> variableDescriptors = ((MultiTypeConstructor) typeConstructor).getEntries();
				for(MultiTypeEntry entry : variableDescriptors)
					if(entry.name != null && entry.name.equals(name))
						return Collections.singletonList(entry.descriptor);
			}
			return receiverType.getMemberScope().getVariables(name);
		}

		@NotNull
		@Override
		public Collection<VariableDescriptor> getNonMembersByName(JetScope scope, Name name)
		{
			return Collections.emptyList();
		}
	};

	static List<CallableDescriptorCollector<? extends CallableDescriptor>> ALL = Lists.<CallableDescriptorCollector<? extends CallableDescriptor>> newArrayList(METHODS, VARIABLES);
}
