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
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.compiler.CompilerPaths;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * @author VISTALL
 * @date 11:52/08.01.13
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

		if(configuration.findSdk() == null)
			throw new ExecutionException("Cant find java.exe");

		JavaParameters parameters = new JavaParameters();

		parameters.setJdk(configuration.findSdk());
		parameters.setMainClass("org.napile.vm.Main");
		parameters.getVMParametersList().add("-classpath");
		parameters.getVMParametersList().add(configuration.napileJvm);

		VirtualFile virtualFile = CompilerPaths.getModuleOutputDirectory(configuration.getConfigurationModule().getModule(), false);
		if(virtualFile == null)
			throw new ExecutionException("Cant find module output");

		parameters.getProgramParametersList().add("-cp");
		parameters.getProgramParametersList().add(virtualFile.getPath());
		parameters.getProgramParametersList().add(configuration.mainClass);

		return parameters.createOSProcessHandler();
	}
}
