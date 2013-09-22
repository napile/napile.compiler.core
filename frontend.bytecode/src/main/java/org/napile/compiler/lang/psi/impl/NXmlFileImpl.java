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

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.io.xml.in.AsmXmlFileReader;
import org.napile.asm.resolve.name.FqName;
import org.napile.asm.tree.members.ClassNode;
import org.napile.compiler.NXmlFileType;
import org.napile.compiler.NapileFileType;
import org.napile.compiler.lang.NapileLanguage;
import org.napile.compiler.lang.psi.NXmlElementBase;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.psi.NapileImportDirective;
import org.napile.compiler.lang.psi.NapilePackage;
import org.napile.compiler.lang.psi.NapileVisitor;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import org.napile.compiler.lang.psi.impl.file.NXmlFileViewProvider;
import org.napile.compiler.lang.psi.stubs.NapilePsiFileStub;
import org.napile.compiler.util.NodeToStringBuilder;
import com.intellij.ide.caches.FileContent;
import com.intellij.lang.ASTNode;
import com.intellij.lang.FileASTNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.psi.PsiManager;
import com.intellij.psi.StubBasedPsiElement;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubTree;
import com.intellij.psi.stubs.StubTreeLoader;
import com.intellij.util.IncorrectOperationException;

/**
 * @author VISTALL
 * @since 20:39/09.10.12
 * <p/>
 * Stub & Mirror system was 'copied' from IDEA CE
 */
public class NXmlFileImpl extends NXmlElementBase implements NapileFile, StubBasedPsiElement<NapilePsiFileStub>
{
	private static final Logger LOGGER = Logger.getInstance(NXmlElementBase.class);

	private final FileViewProvider fileViewProvider;

	private static final Object MIRROR_LOCK = new Object();

	private SoftReference<StubTree> stubTreeSoftRef;
	private final Object stubLock = new Object();

	private NapileFile mirrorElement;
	private String text;

	private NXmlPackageImpl packageImpl;

	public NXmlFileImpl(FileViewProvider viewProvider)
	{
		fileViewProvider = viewProvider;
	}

	@Override
	public NapileFile getMirror()
	{
		synchronized(MIRROR_LOCK)
		{
			if(mirrorElement == null)
			{
				AsmXmlFileReader reader = new AsmXmlFileReader();

				try
				{
					ClassNode classNode = reader.read(getVirtualFile().getInputStream());

					text = NodeToStringBuilder.convertClass(classNode);

					PsiFile mirror = PsiFileFactory.getInstance(getProject()).createFileFromText(getVirtualFile().getName().replace(NXmlFileType.INSTANCE.getDefaultExtension(), NapileFileType.INSTANCE.getDefaultExtension()), NapileLanguage.INSTANCE, text, false, false);

					final ASTNode mirrorTreeElement = SourceTreeToPsiMap.psiElementToTree(mirror);

					setMirror((TreeElement) mirrorTreeElement);

					mirrorElement = (NapileFile) mirror;
				}
				catch(IOException e)
				{
					e.printStackTrace();
				}
			}
		}

		return mirrorElement;
	}

	@Override
	public PsiManager getManager()
	{
		return getViewProvider().getManager();
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
			psiFile = new NXmlFileImpl(new NXmlFileViewProvider(manager, file));

		psiFile.getMirror();
		return psiFile.text;
	}

	@Override
	public void setMirror(@NotNull TreeElement element)
	{
		PsiElement mirrorElement = SourceTreeToPsiMap.treeToPsiNotNull(element);
		if(!(mirrorElement instanceof NapileFile))
		{
			throw new InvalidMirrorException("Unexpected mirror file: " + mirrorElement);
		}

		final NapileFile mirrorFile = (NapileFile) mirrorElement;

		packageImpl = new NXmlPackageImpl(this, mirrorFile.getPackage());

		setMirrors(getDeclarations(), mirrorFile.getDeclarations());
	}

	@NotNull
	@Override
	public PsiElement[] getChildren()
	{
		return getStub().getClasses();
	}

	@NotNull
	@Override
	public NapilePackage getPackage()
	{
		getMirror();

		return packageImpl;
	}

	@NotNull
	@Override
	public FqName getPackageFqName()
	{
		NapilePsiFileStub stub = (NapilePsiFileStub) getStub();
		return stub.getFqName();
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
		return getStub().getClasses();
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
	public NapileFile getContainingFile()
	{
		if(!isValid())
			throw new PsiInvalidElementAccessException(this);
		return this;
	}

	@Override
	@NotNull
	public String getName()
	{
		return getVirtualFile().getName();
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
	public IStubElementType getElementType()
	{
		return null;
	}

	@NotNull
	@Override
	public NapilePsiFileStub getStub()
	{
		return (NapilePsiFileStub) getStubTree().getRoot();
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

			StubTree emptyTree = new StubTree(new NapilePsiFileStub(this, FqName.ROOT, true));
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
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitNapileFile(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitNapileFile(this, data);
	}

	/*@Override
	public PsiFile getDecompiledPsiFile()
	{
		return getMirror();
	} */
}
