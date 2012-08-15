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

package org.napile.asmNew;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author VISTALL
 * @date 23:08/13.08.12
 */
public enum Modifier
{
	// access
	COVERED(ModifierType.ACCESS),
	HERITABLE(ModifierType.ACCESS),
	LOCAL(ModifierType.ACCESS),

	FINAL(ModifierType.EXTENDING),
	ABSTRACT(ModifierType.EXTENDING),
	NATIVE(ModifierType.EXTENDING),

	STATIC(ModifierType.ETC);

	public static final Modifier[] EMPTY = new Modifier[0];

	private final ModifierType modifierType;

	private Modifier(ModifierType modifierType)
	{
		this.modifierType = modifierType;
	}

	public ModifierType getModifierType()
	{
		return modifierType;
	}

	public static Modifier[] list(Modifier... modifiers)
	{
		Set<Modifier> set = new TreeSet<Modifier>(new Comparator<Modifier>()
		{
			@Override
			public int compare(Modifier o1, Modifier o2)
			{
				if(o1.getModifierType() == o2.getModifierType())
					if(o1.getModifierType() != ModifierType.ETC)
						return 0;
				return -1;
			}
		});

		set.addAll(Arrays.asList(modifiers));

		if(set.size() != modifiers.length)
			throw new IllegalArgumentException();

		return set.toArray(new Modifier[set.size()]);
	}
}
