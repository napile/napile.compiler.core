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

package org.napile.compiler.lang.descriptors;

import org.jetbrains.annotations.NotNull;

/**
 * @author abreslav
 */
public interface DescriptorBuilder
{
	@NotNull
	DeclarationDescriptor getOwnerForChildren();

	void addClassifierDescriptor(@NotNull MutableClassDescriptorLite classDescriptor);

	void addAnonymClassDescriptor(@NotNull MutableClassDescriptorLite objectDescriptor);

	void addMethodDescriptor(@NotNull MethodDescriptor functionDescriptor);

	void addVariableDescriptor(@NotNull VariableDescriptor propertyDescriptor);

	void addConstructorDescriptor(@NotNull ConstructorDescriptor constructorDescriptor);

	void addStaticConstructorDescriptor(@NotNull ConstructorDescriptor constructorDescriptor);
}