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

package org.napile.idea.plugin.run;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationInfoProvider;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.JDOMExternalizable;

/**
 * @author VISTALL
 * @date 20:30/22.09.12
 */
public class NapileRunConfiguration extends RunConfigurationBase
{
	public NapileRunConfiguration(Project project, ConfigurationFactory factory)
	{
		super(project, factory, "Local");
	}

	@Override
	public SettingsEditor<? extends RunConfiguration> getConfigurationEditor()
	{
		return new NapileRunSettingsEditor(getProject());
	}

	@Nullable
	@Override
	public JDOMExternalizable createRunnerSettings(ConfigurationInfoProvider configurationInfoProvider)
	{
		return null;
	}

	@Nullable
	@Override
	public SettingsEditor<JDOMExternalizable> getRunnerSettingsEditor(ProgramRunner programRunner)
	{
		return null;
	}

	@Nullable
	@Override
	public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment executionEnvironment) throws ExecutionException
	{
		return null;
	}

	@Override
	public void checkConfiguration() throws RuntimeConfigurationException
	{
	}
}
