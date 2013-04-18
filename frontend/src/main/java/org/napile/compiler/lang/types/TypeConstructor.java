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

package org.napile.compiler.lang.types;

import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.ReadOnly;
import org.napile.compiler.lang.descriptors.ClassifierDescriptor;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.descriptors.annotations.Annotated;

/**
 * @author abreslav
 */
public interface TypeConstructor extends Annotated
{
	@NotNull
	@ReadOnly
	List<TypeParameterDescriptor> getParameters();

	@NotNull
	@ReadOnly
	Collection<? extends NapileType> getSupertypes();

	boolean isSealed();

	@Nullable
	ClassifierDescriptor getDeclarationDescriptor();

	<A, R> R accept(NapileType type, TypeConstructorVisitor<A, R> visitor, A arg);
}
