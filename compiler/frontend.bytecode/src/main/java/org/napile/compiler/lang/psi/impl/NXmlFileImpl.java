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

package org.napile.compiler.lang.psi.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;
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
import org.napile.compiler.NapileFileType;
import org.napile.compiler.lang.NapileLanguage;
import org.napile.compiler.lang.psi.NXmlElementBase;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.psi.NapileImportDirective;
import org.napile.compiler.lang.psi.NapilePackageImpl;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import org.napile.compiler.lang.psi.impl.file.NXmlFileViewProvider;
import org.napile.compiler.lang.psi.stubs.NapilePsiFileStub;
import com.intellij.ide.caches.FileContent;
import com.intellij.lang.ASTNode;
import com.intellij.lang.FileASTNode;
import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.PsiElementBase;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.impl.PsiManagerImpl;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubTree;
import com.intellij.psi.stubs.StubTreeLoader;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.io.StringRef;

/**
 * @author VISTALL
 * @date 20:39/09.10.12
 * <p/>
 * Stub & Mirror system was 'copied' from IDEA CE
 */
public class NXmlFileImpl extends PsiElementBase implements NapileFile
{
	private static final Logger LOGGER = Logger.getInstance(NXmlElementBase.class);

	private final FileViewProvider fileViewProvider;

	private static final Object MIRROR_LOCK = new Object();

	private SoftReference<StubTree> stubTreeSoftRef;
	private final Object stubLock = new Object();

	private NapileFile mirrorElement;
	private String text;

	public NXmlFileImpl(PsiManagerImpl manager, FileViewProvider viewProvider)
	{
		//super(manager);

		fileViewProvider = viewProvider;
	}

	public NapileFile getMirror()
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

					PsiFile mirror = PsiFileFactory.getInstance(getProject()).createFileFromText(file.getName().replace(NXmlFileType.INSTANCE.getDefaultExtension(), NapileFileType.INSTANCE.getDefaultExtension()), NapileLanguage.INSTANCE, text, false, false);

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
			psiFile = new NXmlFileImpl((PsiManagerImpl) manager, new NXmlFileViewProvider(manager, file));

		psiFile.getMirror();
		return psiFile.text;
	}

	public void setMirror(@NotNull TreeElement element)
	{
		NapileFile mirrorFile = (NapileFile) SourceTreeToPsiMap.treeElementToPsi(element);

		assert mirrorFile != null;

		mirrorElement = mirrorFile;
	}

	@NotNull
	@Override
	public Language getLanguage()
	{
		return NapileLanguage.INSTANCE;
	}

	@Override
	public PsiManager getManager()
	{
		return getViewProvider().getManager();
	}

	@NotNull
	@Override
	public PsiElement[] getChildren()
	{
		return getMirror().getChildren();
	}

	@NotNull
	@Override
	public NapilePackageImpl getNamespaceHeader()
	{
		return getMirror().getNamespaceHeader();
	}

	@Nullable
	@Override
	public String getPackageName()
	{
		return getMirror().getPackageName();
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

	@Override
	public boolean isCompiled()
	{
		return true;
	}

	@NotNull
	@Override
	public NapileClass[] getDeclarations()
	{
		return getMirror().getDeclarations();
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
	public TextRange getTextRange()
	{
		return null;
	}

	@Override
	public int getStartOffsetInParent()
	{
		return 0;
	}

	@Override
	public int getTextLength()
	{
		return 0;
	}

	@Nullable
	@Override
	public PsiElement findElementAt(int offset)
	{
		return null;
	}

	@Override
	public int getTextOffset()
	{
		return 0;
	}

	@Override
	public String getText()
	{
		return null;
	}

	@NotNull
	@Override
	public char[] textToCharArray()
	{
		return new char[0];
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

	@Override
	@NotNull
	public StubTree getStubTree()
	{
		ApplicationManager.getApplication().assertReadAccessAllowed();

		final StubTree derefd = derefStub();
		if(derefd != null)
			return derefd;

		StubTree stubHolder = (StubTree) StubTreeLoader.getInstance().readOrBuild(getProject(), getVirtualFile(), this);
		if(stubHolder == null)
		{
			// Must be corrupted classfile
			LOGGER.info("Class file is corrupted: " + getVirtualFile().getPresentableUrl());

			StubTree emptyTree = new StubTree(new NapilePsiFileStub(this, StringRef.fromString("unknown"), true));
			setStubTree(emptyTree);
			resetMirror();
			return emptyTree;
		}

		synchronized(stubLock)
		{
			final StubTree derefdOnLock = derefStub();
			if(derefdOnLock != null)
				return derefdOnLock;

			setStubTree(stubHolder);
		}

		resetMirror();
		return stubHolder;
	}

	private void resetMirror()
	{
		synchronized(MIRROR_LOCK)
		{
			mirrorElement = null;
		}
	}

	private void setStubTree(StubTree tree)
	{
		synchronized(stubLock)
		{
			stubTreeSoftRef = new SoftReference<StubTree>(tree);
			((NapilePsiFileStub) tree.getRoot()).setPsi(this);
		}
	}

	@Nullable
	private StubTree derefStub()
	{
		synchronized(stubLock)
		{
			return stubTreeSoftRef != null ? stubTreeSoftRef.get() : null;
		}
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
		SoftReference<StubTree> stub = stubTreeSoftRef;
		StubTree stubHolder = stub == null ? null : stub.get();
		if(stubHolder != null)
			((StubBase<?>) stubHolder.getRoot()).setPsi(null);
		stubTreeSoftRef = null;

		ApplicationManager.getApplication().assertWriteAccessAllowed();

		synchronized(MIRROR_LOCK)
		{
			mirrorElement = null;
		}
	}

	@Override
	public PsiFile cacheCopy(FileContent fileContent)
	{
		return this;
	}

	@Override
	public void accept(@NotNull PsiElementVisitor visitor)
	{
		if(visitor instanceof NapileVisitorVoid)
			((NapileVisitorVoid) visitor).visitNapileFile(this);
		else
			visitor.visitFile(this);
	}

	@Override
	public PsiElement copy()
	{
		return this;
	}

	@Override
	public PsiElement add(@NotNull PsiElement element) throws IncorrectOperationException
	{
		return null;
	}

	@Override
	public PsiElement addBefore(@NotNull PsiElement element, @Nullable PsiElement anchor) throws IncorrectOperationException
	{
		return null;
	}

	@Override
	public PsiElement addAfter(@NotNull PsiElement element, @Nullable PsiElement anchor) throws IncorrectOperationException
	{
		return null;
	}

	@Override
	public void checkAdd(@NotNull PsiElement element) throws IncorrectOperationException
	{
	}

	@Override
	public void delete() throws IncorrectOperationException
	{
	}

	@Override
	public void checkDelete() throws IncorrectOperationException
	{
	}

	@Override
	public PsiElement replace(@NotNull PsiElement newElement) throws IncorrectOperationException
	{
		return null;
	}
}
