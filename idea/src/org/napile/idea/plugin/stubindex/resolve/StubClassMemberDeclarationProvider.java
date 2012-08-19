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

package org.napile.idea.plugin.stubindex.resolve;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.psi.NapileNamedFunction;
import org.napile.compiler.lang.psi.NapileProperty;
import org.napile.compiler.lang.resolve.lazy.ClassMemberDeclarationProvider;
import org.napile.compiler.lang.resolve.lazy.data.NapileClassLikeInfo;
import org.jetbrains.jet.lang.resolve.name.Name;

/**
 * @author Nikolay Krasko
 */
public class StubClassMemberDeclarationProvider extends AbstractStubDeclarationProvider implements ClassMemberDeclarationProvider
{
	@NotNull
	@Override
	public NapileClassLikeInfo getOwnerInfo()
	{
		// TODO:
		return null;
	}

	@NotNull
	@Override
	public Collection<NapileNamedFunction> getFunctionDeclarations(@NotNull Name name)
	{
		// TODO:
		return null;
	}

	@NotNull
	@Override
	public Collection<NapileProperty> getPropertyDeclarations(@NotNull Name name)
	{
		// TODO:
		return null;
	}
}
