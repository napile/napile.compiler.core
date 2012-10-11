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

package org.napile.compiler.psi;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.LangVersion;
import org.napile.asm.io.xml.in.AsmXmlFileReader;
import org.napile.asm.io.xml.out.AsmTextTextWriter;
import org.napile.asm.tree.members.ClassNode;
import org.napile.compiler.NXmlFileType;
import org.napile.compiler.NapileLanguage;
import org.napile.compiler.lang.psi.NapileImportDirective;
import org.napile.compiler.lang.psi.NapileNamespaceHeader;
import org.napile.compiler.psi.file.NXmlFileViewProvider;
import com.intellij.ide.caches.FileContent;
import com.intellij.lang.ASTNode;
import com.intellij.lang.FileASTNode;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.impl.PsiManagerImpl;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubTree;
import com.intellij.reference.SoftReference;
import com.intellij.util.IncorrectOperationException;

/**
 * @author VISTALL
 * @date 20:39/09.10.12
 */
public class NXmlFileImpl extends NXmlElementBase implements NapileFile
{
	private final FileViewProvider fileViewProvider;

	private static final Object MIRROR_LOCK = new Object();

	private SoftReference<StubTree> stubTreeSoftRef;

	private PsiElement mirrorElement;
	private String text;

	public NXmlFileImpl(PsiManagerImpl manager, FileViewProvider viewProvider)
	{
		super(manager);

		fileViewProvider = viewProvider;
	}

	public PsiElement getMirror()
	{
		synchronized(MIRROR_LOCK)
		{
			if(mirrorElement == null)
			{
				File file = VfsUtilCore.virtualToIoFile(getVirtualFile());

				AsmXmlFileReader reader = new AsmXmlFileReader();

				try
				{
					ClassNode classNode = reader.read(new FileInputStream(file));
					AsmTextTextWriter writer = new AsmTextTextWriter();

					text = writer.write(LangVersion.CURRENT, classNode);

					PsiFile mirror = PsiFileFactory.getInstance(getProject()).createFileFromText(file.getName(), NapileLanguage.INSTANCE, text, false, false);

					final ASTNode mirrorTreeElement = SourceTreeToPsiMap.psiElementToTree(mirror);

					setMirror((TreeElement) mirrorTreeElement);
				}
				catch(IOException e)
				{
					e.printStackTrace();
				}
			}
		}

		return mirrorElement;
	}

	public static String decompile(PsiManager manager, VirtualFile file)
	{
		final FileViewProvider provider = ((PsiManagerEx) manager).getFileManager().findViewProvider(file);
		NXmlFileImpl psiFile = null;
		if(provider != null)
		{
			final PsiFile psi = provider.getPsi(provider.getBaseLanguage());
			if(psi instanceof NXmlFileImpl)
				psiFile = (NXmlFileImpl) psi;
		}

		if(psiFile == null)
			psiFile = new NXmlFileImpl((PsiManagerImpl) manager, new NXmlFileViewProvider(manager, file, true, NapileLanguage.INSTANCE));

		psiFile.getMirror();
		return psiFile.text;
	}

	@Override
	public void setMirror(@NotNull TreeElement element)
	{
		PsiElement mirrorFile = SourceTreeToPsiMap.treeElementToPsi(element);

		mirrorElement = mirrorFile;
	}

	@Nullable
	@Override
	public NapileNamespaceHeader getNamespaceHeader()
	{
		return null;
	}

	@Nullable
	@Override
	public String getPackageName()
	{
		return null;
	}

	@Nullable
	@Override
	public NapileImportDirective findImportByAlias(@NotNull String name)
	{
		return null;
	}

	@NotNull
	@Override
	public List<NapileImportDirective> getImportDirectives()
	{
		return Collections.emptyList();
	}

	@NotNull
	@Override
	public List<NapileClass> getDeclarations()
	{
		return Collections.emptyList();
	}

	@NotNull
	@Override
	public VirtualFile getVirtualFile()
	{
		return fileViewProvider.getVirtualFile();
	}

	@Override
	public boolean processChildren(PsiElementProcessor<PsiFileSystemItem> psiFileSystemItemPsiElementProcessor)
	{
		return true;
	}

	@Override
	public PsiDirectory getContainingDirectory()
	{
		VirtualFile parentFile = getVirtualFile().getParent();
		if(parentFile == null)
			return null;
		return getManager().findDirectory(parentFile);
	}

	@Override
	public PsiFile getContainingFile()
	{
		if(!isValid())
			throw new PsiInvalidElementAccessException(this);
		return this;
	}

	@Override
	public boolean isDirectory()
	{
		return false;
	}

	@Nullable
	@Override
	public PsiDirectory getParent()
	{
		return getContainingDirectory();
	}

	@Override
	public long getModificationStamp()
	{
		return fileViewProvider.getModificationStamp();
	}

	@NotNull
	@Override
	public PsiFile getOriginalFile()
	{
		return this;
	}

	@NotNull
	@Override
	public FileType getFileType()
	{
		return NXmlFileType.INSTANCE;
	}

	@NotNull
	@Override
	public PsiFile[] getPsiRoots()
	{
		return new PsiFile[]{this};
	}

	@NotNull
	@Override
	public FileViewProvider getViewProvider()
	{
		return fileViewProvider;
	}

	@Override
	public FileASTNode getNode()
	{
		return null;
	}

	@Override
	public void subtreeChanged()
	{
	}

	@Override
	public void checkSetName(String s) throws IncorrectOperationException
	{
	}

	@Override
	public PsiElement setName(@NonNls @NotNull String s) throws IncorrectOperationException
	{
		return null;
	}

	@Nullable
	@Override
	public StubTree getStubTree()
	{
		return null;
	}

	@Nullable
	@Override
	public ASTNode findTreeForStub(StubTree stubTree, StubElement<?> stubElement)
	{
		return null;
	}

	@Override
	public boolean isContentsLoaded()
	{
		return stubTreeSoftRef != null;
	}

	@Override
	public void onContentReload()
	{
	}

	@Override
	public PsiFile cacheCopy(FileContent fileContent)
	{
		return this;
	}
}
