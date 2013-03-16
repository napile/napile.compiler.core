/*
 * Copyright 2010-2013 napile.org
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

package org.napile.compiler.analyzer;

import java.util.Collection;
import java.util.Collections;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.psi.NapileFile;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * @author VISTALL
 * @since 16:11/15.02.13
 */
public class AnalyzeContext
{
	public static final AnalyzeContext EMPTY = new AnalyzeContext(Collections.<NapileFile>emptyList(), Collections.<VirtualFile> emptyList(), Collections.<VirtualFile> emptyList());

	private final Collection<NapileFile> files;
	private final Collection<VirtualFile> bootpath;
	private final Collection<VirtualFile> classpath;

	public AnalyzeContext(@NotNull Collection<NapileFile> files, @NotNull Collection<VirtualFile> bootpath, @NotNull Collection<VirtualFile> classpath)
	{
		this.files = files;
		this.bootpath = bootpath;
		this.classpath = classpath;
	}

	@NotNull
	public Collection<NapileFile> getFiles()
	{
		return files;
	}

	@NotNull
	public Collection<VirtualFile> getClasspath()
	{
		return classpath;
	}

	@NotNull
	public Collection<VirtualFile> getBootpath()
	{
		return bootpath;
	}
}
