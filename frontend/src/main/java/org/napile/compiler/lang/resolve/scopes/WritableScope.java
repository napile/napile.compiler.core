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

package org.napile.compiler.lang.resolve.scopes;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.ClassifierDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.PackageDescriptor;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import com.google.common.collect.Multimap;

/**
 * @author abreslav
 */
public interface WritableScope extends JetScope
{
	enum LockLevel
	{
		WRITING,
		BOTH,
		READING,
	}

	WritableScope changeLockLevel(LockLevel lockLevel);

	void addVariableDescriptor(@NotNull VariableDescriptor variableDescriptor);

	@Deprecated
	void addPropertyDescriptor(@NotNull VariableDescriptor propertyDescriptor);

	void addMethodDescriptor(@NotNull MethodDescriptor methodDescriptor);

	void addTypeParameterDescriptor(@NotNull TypeParameterDescriptor typeParameterDescriptor);

	void addClassifierDescriptor(@NotNull ClassifierDescriptor classDescriptor);

	void addObjectDescriptor(@NotNull ClassDescriptor objectDescriptor);

	void addClassifierAlias(@NotNull Name name, @NotNull ClassifierDescriptor classifierDescriptor);

	void addNamespaceAlias(@NotNull Name name, @NotNull PackageDescriptor packageDescriptor);

	void addFunctionAlias(@NotNull Name name, @NotNull MethodDescriptor methodDescriptor);

	void addVariableAlias(@NotNull Name name, @NotNull VariableDescriptor variableDescriptor);

	void addNamespace(@NotNull PackageDescriptor packageDescriptor);

	@Nullable
	PackageDescriptor getDeclaredNamespace(@NotNull Name name);

	@NotNull
	Multimap<Name, DeclarationDescriptor> getDeclaredDescriptorsAccessibleBySimpleName();

	void importScope(@NotNull JetScope imported);

	void setImplicitReceiver(@NotNull ReceiverDescriptor implicitReceiver);

	void importClassifierAlias(@NotNull Name importedClassifierName, @NotNull ClassifierDescriptor classifierDescriptor);

	void importNamespaceAlias(@NotNull Name aliasName, @NotNull PackageDescriptor packageDescriptor);

	void importFunctionAlias(@NotNull Name aliasName, @NotNull MethodDescriptor methodDescriptor);

	void importVariableAlias(@NotNull Name aliasName, @NotNull VariableDescriptor variableDescriptor);

	void clearImports();
}
