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

package org.napile.compiler;

import javax.swing.Icon;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * @author VISTALL
 * @date 18:02/09.10.12
 */
public class NapileXmlFileType implements FileType
{
	private static final NotNullLazyValue<Icon> ICON = new NotNullLazyValue<Icon>()
	{
		@NotNull
		@Override
		protected Icon compute()
		{
			return AllIcons.FileTypes.JavaClass;
		}
	};

	public static final FileType INSTANCE = new NapileXmlFileType();

	@NotNull
	@Override
	public String getName()
	{
		return "NXML";
	}

	@NotNull
	@Override
	public String getDescription()
	{
		return "Napile Xml Bytecode File";
	}

	@NotNull
	@Override
	public String getDefaultExtension()
	{
		return "nxml";
	}

	@Nullable
	@Override
	public Icon getIcon()
	{
		return ICON.getValue();
	}

	@Override
	public boolean isBinary()
	{
		return true;
	}

	@Override
	public boolean isReadOnly()
	{
		return false;
	}

	@Nullable
	@Override
	public String getCharset(@NotNull VirtualFile virtualFile, byte[] bytes)
	{
		return "UTF-8";
	}
}
