/*
 * Copyright 2010-2012 napile.org
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

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.annotations.Annotated;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.resolve.name.FqName;
import org.napile.compiler.lang.rt.NapileAnnotationPackage;
import org.napile.compiler.lang.types.TypeUtils;

/**
 * @author VISTALL
 * @date 12:33/25.08.12
 */
public class AnnotationUtils
{
	public static boolean isAnnotation(@NotNull ClassDescriptor annotated)
	{
		return NapileAnnotationPackage.ANNOTATION.equals(DescriptorUtils.getFQName(annotated)) || hasAnnotation(annotated, NapileAnnotationPackage.ANNOTATION);
	}

	public static boolean hasAnnotation(@NotNull Annotated annotated, @NotNull FqName fqName)
	{
		for(AnnotationDescriptor annotationDescriptor : annotated.getAnnotations())
			if(TypeUtils.isEqualFqName(annotationDescriptor.getType(), fqName))
				return true;
		return false;
	}
}
