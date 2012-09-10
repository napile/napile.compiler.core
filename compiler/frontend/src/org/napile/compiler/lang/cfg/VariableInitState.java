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

package org.napile.compiler.lang.cfg;

import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author svtk
 */
public class VariableInitState
{
	public final boolean isInitialized;
	public final boolean isDeclared;

	VariableInitState(boolean isInitialized, boolean isDeclared)
	{
		this.isInitialized = isInitialized;
		this.isDeclared = isDeclared;
	}

	private static final VariableInitState VS_TT = new VariableInitState(true, true);
	private static final VariableInitState VS_TF = new VariableInitState(true, false);
	private static final VariableInitState VS_FT = new VariableInitState(false, true);
	private static final VariableInitState VS_FF = new VariableInitState(false, false);


	public static VariableInitState create(boolean isInitialized, boolean isDeclared)
	{
		if(isInitialized)
		{
			if(isDeclared)
				return VS_TT;
			return VS_TF;
		}
		if(isDeclared)
			return VS_FT;
		return VS_FF;
	}

	public static VariableInitState create(boolean isInitialized)
	{
		return create(isInitialized, false);
	}

	public static VariableInitState create(boolean isDeclaredHere, @Nullable VariableInitState mergedEdgesData)
	{
		return create(true, isDeclaredHere || (mergedEdgesData != null && mergedEdgesData.isDeclared));
	}

	public static VariableInitState create(@NotNull Set<VariableInitState> edgesData)
	{
		boolean isInitialized = true;
		boolean isDeclared = true;
		for(VariableInitState edgeData : edgesData)
		{
			if(!edgeData.isInitialized)
			{
				isInitialized = false;
			}
			if(!edgeData.isDeclared)
			{
				isDeclared = false;
			}
		}
		return create(isInitialized, isDeclared);
	}
}
