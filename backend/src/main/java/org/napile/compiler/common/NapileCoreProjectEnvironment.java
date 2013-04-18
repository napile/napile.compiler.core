/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package org.napile.compiler.common;

import java.io.File;

import org.jetbrains.annotations.NotNull;
import com.intellij.core.CoreApplicationEnvironment;
import com.intellij.core.CorePackageIndex;
import com.intellij.core.CoreProjectEnvironment;
import com.intellij.mock.MockFileIndexFacade;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.roots.PackageIndex;
import com.intellij.openapi.vfs.VirtualFile;

public class NapileCoreProjectEnvironment extends CoreProjectEnvironment
{
	private final PackageIndex myPackageIndex;

	public NapileCoreProjectEnvironment(Disposable parentDisposable, CoreApplicationEnvironment applicationEnvironment)
	{
		super(parentDisposable, applicationEnvironment);

		myPackageIndex = new CorePackageIndex();
		myProject.registerService(PackageIndex.class, myPackageIndex);
	}

	public void addJarToClassPath(File path)
	{
		assert path.isFile();

		final VirtualFile root = getEnvironment().getJarFileSystem().findFileByPath(path + "!/");
		if(root == null)
			throw new IllegalArgumentException("trying to add non-existing file to classpath: " + path);

		addSourcesToClasspath(root);
	}

	public void addSourcesToClasspath(@NotNull VirtualFile root)
	{
		assert root.isDirectory();

		((CorePackageIndex) myPackageIndex).addToClasspath(root);
		((MockFileIndexFacade) myFileIndexFacade).addLibraryRoot(root);
	}
}
