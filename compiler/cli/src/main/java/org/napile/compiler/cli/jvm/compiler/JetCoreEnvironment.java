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

package org.napile.compiler.cli.jvm.compiler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.NapileFileType;
import org.napile.compiler.NapileXmlFileType;
import org.napile.compiler.cli.jvm.JVMConfigurationKeys;
import org.napile.compiler.config.CommonConfigurationKeys;
import org.napile.compiler.config.CompilerConfiguration;
import org.napile.compiler.lang.parsing.JetParserDefinition;
import org.napile.compiler.lang.resolve.JetFilesProvider;
import org.napile.compiler.psi.NapileFile;
import com.intellij.core.CoreApplicationEnvironment;
import com.intellij.core.CoreJavaFileManager;
import com.intellij.mock.MockApplication;
import com.intellij.mock.MockProject;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.file.impl.JavaFileManager;

/**
 * @author yole
 */
public class JetCoreEnvironment
{

	private final CoreApplicationEnvironment applicationEnvironment;
	private final JetCoreProjectEnvironment projectEnvironment;
	private final List<NapileFile> sourceFiles = new ArrayList<NapileFile>();

	private final CompilerConfiguration configuration;

	private boolean initialized = false;

	public JetCoreEnvironment(Disposable parentDisposable, @NotNull CompilerConfiguration configuration)
	{
		this.configuration = configuration.copy();
		this.configuration.setReadOnly(true);

		this.applicationEnvironment = new CoreApplicationEnvironment(parentDisposable);
		applicationEnvironment.registerFileType(NapileFileType.INSTANCE, NapileFileType.INSTANCE.getDefaultExtension());
		applicationEnvironment.registerFileType(NapileXmlFileType.INSTANCE, NapileXmlFileType.INSTANCE.getDefaultExtension());
		applicationEnvironment.registerParserDefinition(new JetParserDefinition());

		projectEnvironment = new JetCoreProjectEnvironment(parentDisposable, applicationEnvironment);

		MockProject project = projectEnvironment.getProject();
		project.registerService(JetFilesProvider.class, new CliJetFilesProvider(this));
		project.registerService(CoreJavaFileManager.class, (CoreJavaFileManager) ServiceManager.getService(project, JavaFileManager.class));

		for(File path : configuration.getList(JVMConfigurationKeys.CLASSPATH_KEY))
			addToClasspath(path);

		for(String path : configuration.getList(CommonConfigurationKeys.SOURCE_ROOTS_KEY))
			addSources(path);

		initialized = true;
	}

	public CompilerConfiguration getConfiguration()
	{
		return configuration;
	}

	@NotNull
	public MockApplication getApplication()
	{
		return applicationEnvironment.getApplication();
	}

	@NotNull
	public Project getProject()
	{
		return projectEnvironment.getProject();
	}

	private void addSources(File file)
	{
		if(file.isDirectory())
		{
			File[] files = file.listFiles();
			if(files != null)
			{
				for(File child : files)
				{
					addSources(child);
				}
			}
		}
		else
		{
			VirtualFile fileByPath = applicationEnvironment.getLocalFileSystem().findFileByPath(file.getAbsolutePath());
			if(fileByPath != null)
			{
				PsiFile psiFile = PsiManager.getInstance(getProject()).findFile(fileByPath);
				if(psiFile instanceof NapileFile)
				{
					sourceFiles.add((NapileFile) psiFile);
				}
			}
		}
	}

	private void addSources(String path)
	{
		if(path == null)
		{
			return;
		}

		VirtualFile vFile = applicationEnvironment.getLocalFileSystem().findFileByPath(path);
		if(vFile == null)
		{
			throw new CompileEnvironmentException("File/directory not found: " + path);
		}
		if(!vFile.isDirectory() && vFile.getFileType() != NapileFileType.INSTANCE)
		{
			throw new CompileEnvironmentException("Not a Kotlin file: " + path);
		}

		addSources(new File(path));
	}

	public void addToClasspath(File path)
	{
		if(initialized)
		{
			throw new IllegalStateException("Cannot add class path when JetCoreEnvironment is already initialized");
		}
		if(path.isFile())
		{
			projectEnvironment.addJarToClassPath(path);
		}
		else
		{
			final VirtualFile root = applicationEnvironment.getLocalFileSystem().findFileByPath(path.getAbsolutePath());
			if(root == null)
			{
				throw new IllegalArgumentException("trying to add non-existing file to classpath: " + path);
			}
			projectEnvironment.addSourcesToClasspath(root);
		}
	}

	public List<NapileFile> getSourceFiles()
	{
		return sourceFiles;
	}
}
