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

package org.napile.idea.plugin.psi.file;

import java.io.ByteArrayInputStream;

import org.jetbrains.annotations.Nullable;
import org.napile.asm.io.xml.in.AsmXmlFileReader;
import org.napile.asm.tree.members.ClassNode;
import org.napile.compiler.NXmlFileType;
import org.napile.compiler.lang.psi.stubs.NapilePsiFileStub;
import org.napile.compiler.lang.psi.stubs.elements.NapileFileElementType;
import org.napile.compiler.util.NodeToStubBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.stubs.BinaryFileStubBuilder;
import com.intellij.psi.stubs.Stub;
import com.intellij.util.io.StringRef;

/**
 * @author VISTALL
 * @since 14:35/15.10.12
 */
public class NXmlFileStubBuilder implements BinaryFileStubBuilder
{
	private static final Logger LOGGER = Logger.getInstance(NXmlFileStubBuilder.class);

	private final NodeToStubBuilder builder = new NodeToStubBuilder();

	@Override
	public boolean acceptsFile(VirtualFile file)
	{
		return file.getFileType() == NXmlFileType.INSTANCE;
	}

	@Nullable
	@Override
	public Stub buildStubTree(VirtualFile virtualFile, byte[] content, Project project)
	{
		AsmXmlFileReader reader = new AsmXmlFileReader();

		ClassNode classNode;
		try
		{
			classNode = reader.read(new ByteArrayInputStream(content));
		}
		catch(Throwable e)
		{
			// show error only in internal mode
			if(ApplicationManager.getApplication().isInternal())
			{
				LOGGER.error(e);
			}
			classNode = null;
		}

		if(classNode == null)
		{
			return null;
		}

		NapilePsiFileStub psiFileStub = null;
		try
		{
			psiFileStub = new NapilePsiFileStub(null, StringRef.fromString(classNode.name.parent().getFqName()), true);

			classNode.accept(builder, psiFileStub);
		}
		catch(Exception e)
		{
			LOGGER.error(e);
		}

		return psiFileStub;
	}

	@Override
	public int getStubVersion()
	{
		return NapileFileElementType.STUB_VERSION + 1;
	}
}
