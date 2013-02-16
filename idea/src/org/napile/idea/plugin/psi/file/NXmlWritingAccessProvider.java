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

package org.napile.idea.plugin.psi.file;

import java.util.Collection;
import java.util.Collections;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.NXmlFileType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.WritingAccessProvider;

/**
 * @author VISTALL
 * @date 18:42/16.02.13
 */
public class NXmlWritingAccessProvider extends WritingAccessProvider
{
	@NotNull
	@Override
	public Collection<VirtualFile> requestWriting(VirtualFile... files)
	{
		return Collections.emptyList();
	}

	@Override
	public boolean isPotentiallyWritable(@NotNull VirtualFile file)
	{
		return file.getFileType() != NXmlFileType.INSTANCE;
	}
}
