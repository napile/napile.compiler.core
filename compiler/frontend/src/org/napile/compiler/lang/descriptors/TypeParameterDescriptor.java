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

import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.TypeConstructor;
import org.napile.compiler.lang.types.TypeSubstitutor;

/**
 * @author abreslav
 */
public interface TypeParameterDescriptor extends ClassifierDescriptor
{
	//TODO [VISTALL] move it to getSupertypes()
	@NotNull
	@Deprecated
	Set<JetType> getUpperBounds();

	@NotNull
	JetType getUpperBoundsAsType();

	@NotNull
	@Override
	TypeConstructor getTypeConstructor();

	@NotNull
	@Override
	@Deprecated
		// Use the static method TypeParameterDescriptor.substitute()
	TypeParameterDescriptor substitute(TypeSubstitutor substitutor);

	int getIndex();
}
