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

package org.napile.compiler.lang.psi;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.NapileNodeTypes;
import com.intellij.lang.ASTNode;

/**
 * @author max
 */
// TODO: Remove when all implementations of NapileTypeParameterListOwner get stubs
@Deprecated
abstract class NapileTypeParameterListOwnerNotStubbed extends NapileNamedDeclarationNotStubbed implements NapileTypeParameterListOwner
{
	public NapileTypeParameterListOwnerNotStubbed(@NotNull ASTNode node)
	{
		super(node);
	}

	@Nullable
	NapileTypeParameterList getTypeParameterList()
	{
		return (NapileTypeParameterList) findChildByType(NapileNodeTypes.TYPE_PARAMETER_LIST);
	}

	@Nullable
	NapileTypeConstraintList getTypeConstraintList()
	{
		return (NapileTypeConstraintList) findChildByType(NapileNodeTypes.TYPE_CONSTRAINT_LIST);
	}

	@Override
	@NotNull
	public List<NapileTypeConstraint> getTypeConstraints()
	{
		NapileTypeConstraintList typeConstraintList = getTypeConstraintList();
		if(typeConstraintList == null)
		{
			return Collections.emptyList();
		}
		return typeConstraintList.getConstraints();
	}

	@Override
	@NotNull
	public List<NapileTypeParameter> getTypeParameters()
	{
		NapileTypeParameterList list = getTypeParameterList();
		if(list == null)
			return Collections.emptyList();

		return list.getParameters();
	}
}
