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

import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.resolve.DescriptorUtils;
import com.google.common.collect.Maps;

/**
 * @author svtk
 */
public class Visibilities
{

	private Visibilities()
	{
	}

	public static boolean isVisible(DeclarationDescriptorWithVisibility what, DeclarationDescriptor from)
	{
		DeclarationDescriptorWithVisibility parent = what;
		while(parent != null)
		{
			if(parent.getVisibility() == Visibility.LOCAL2)
			{
				return true;
			}
			if(!parent.getVisibility().isVisible(parent, from))
			{
				return false;
			}
			parent = DescriptorUtils.getParentOfType(parent, DeclarationDescriptorWithVisibility.class);
		}
		return true;
	}

	private static final Map<Visibility, Integer> ORDERED_VISIBILITIES = Maps.newHashMap();

	static
	{
		ORDERED_VISIBILITIES.put(Visibility.LOCAL, 0);
		ORDERED_VISIBILITIES.put(Visibility.COVERED, 1);
		ORDERED_VISIBILITIES.put(Visibility.HERITABLE, 1);
		ORDERED_VISIBILITIES.put(Visibility.PUBLIC, 2);
	}

	/*package*/
	static Integer compareLocal(@NotNull Visibility first, @NotNull Visibility second)
	{
		if(first == second)
			return 0;
		Integer firstIndex = ORDERED_VISIBILITIES.get(first);
		Integer secondIndex = ORDERED_VISIBILITIES.get(second);
		if(firstIndex == null || secondIndex == null || firstIndex.equals(secondIndex))
		{
			return null;
		}
		return firstIndex - secondIndex;
	}

	@Nullable
	public static Integer compare(@NotNull Visibility first, @NotNull Visibility second)
	{
		Integer result = compareLocal(first, second);
		if(result != null)
		{
			return result;
		}
		Integer oppositeResult = compareLocal(second, first);
		if(oppositeResult != null)
		{
			return -oppositeResult;
		}
		return null;
	}
}
