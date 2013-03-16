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

package org.napile.idea.plugin.run;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.idea.plugin.caches.NapileClassResolver;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.compiler.CompilerPaths;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;

/**
 * @author VISTALL
 * @since 11:52/08.01.13
 */
public class NapileRunningState extends CommandLineState
{
	protected NapileRunningState(ExecutionEnvironment environment)
	{
		super(environment);
	}

	@NotNull
	@Override
	protected OSProcessHandler startProcess() throws ExecutionException
	{
		NapileRunConfiguration configuration = (NapileRunConfiguration) getEnvironment().getRunProfile();

		final Sdk sdk = configuration.findSdk();
		if(sdk == null)
		{
			throw new ExecutionException("Cant find java.exe");
		}

		JavaParameters parameters = new JavaParameters();

		parameters.setJdk(sdk);
		parameters.setMainClass("org.napile.vm.Main");
		parameters.getVMParametersList().add("-classpath");
		parameters.getVMParametersList().add(configuration.napileJvm);

		final Module module = configuration.getConfigurationModule().getModule();
		NapileClass[] classesByName = NapileClassResolver.getInstance(configuration.getProject()).getClassesByFqName(configuration.mainClass, GlobalSearchScope.moduleWithDependenciesScope(module));
		if(classesByName.length != 1)
		{
			throw new ExecutionException("Wrong class name");
		}

		ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);

		VirtualFile outpath = CompilerPaths.getModuleOutputDirectory(configuration.getConfigurationModule().getModule(),  moduleRootManager.getFileIndex().isInTestSourceContent(classesByName[0].getContainingFile().getVirtualFile()));
		if(outpath == null)
		{
			throw new ExecutionException("Cant find module output");
		}

		NapileClasspathCollector classpathCollector = new NapileClasspathCollector(configuration.getConfigurationModule().getModule());

		parameters.getProgramParametersList().add("-cp");
		parameters.getProgramParametersList().add(classpathCollector.getClasspath() + outpath.getPath());
		parameters.getProgramParametersList().add(configuration.mainClass);

		return parameters.createOSProcessHandler();
	}
}
