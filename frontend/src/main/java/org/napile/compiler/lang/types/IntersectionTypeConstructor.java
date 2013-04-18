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
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.types.impl.AbstractTypeConstructorImpl;
import com.intellij.openapi.util.text.StringUtil;

/**
 * @author abreslav
 */
public class IntersectionTypeConstructor extends AbstractTypeConstructorImpl implements TypeConstructor
{
	private final List<AnnotationDescriptor> annotations;
	private final int hashCode;

	public IntersectionTypeConstructor(@NotNull List<AnnotationDescriptor> annotations, @NotNull Collection<NapileType> typesToIntersect)
	{
		super(typesToIntersect);
		this.annotations = annotations;
		this.hashCode = typesToIntersect.hashCode();
	}

	@Override
	public List<AnnotationDescriptor> getAnnotations()
	{
		return annotations;
	}

	@Override
	public <A, R> R accept(NapileType type, TypeConstructorVisitor<A, R> visitor, A arg)
	{
		return visitor.visitType(type, this, arg);
	}

	@Override
	public String toString()
	{
		return makeDebugNameForIntersectionType(getSupertypes());
	}

	private static String makeDebugNameForIntersectionType(Collection<? extends NapileType> resultingTypes)
	{
		StringBuilder debugName = new StringBuilder("{");
		debugName.append(StringUtil.join(resultingTypes, " & "));
		debugName.append("}");
		return debugName.toString();
	}

	@Override
	public boolean equals(Object o)
	{
		if(this == o)
			return true;
		if(o == null || getClass() != o.getClass())
			return false;

		IntersectionTypeConstructor that = (IntersectionTypeConstructor) o;

		if(!getSupertypes().equals(that.getSupertypes()))
			return false;

		return true;
	}

	@Override
	public int hashCode()
	{
		return hashCode;
	}
}
