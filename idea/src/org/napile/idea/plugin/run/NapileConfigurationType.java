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

import javax.swing.Icon;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtil;

/**
 * @author VISTALL
 * @since 19:53/22.09.12
 */
public class NapileConfigurationType implements ConfigurationType
{
	@Nullable
	public static NapileConfigurationType getInstance()
	{
		return ContainerUtil.findInstance(Extensions.getExtensions(CONFIGURATION_TYPE_EP), NapileConfigurationType.class);
	}

	private final ConfigurationFactory configurationFactory;

	public NapileConfigurationType()
	{
		configurationFactory = new ConfigurationFactory(this)
		{
			@Override
			public RunConfiguration createTemplateConfiguration(Project project)
			{
				return new NapileRunConfiguration(project, getName(), this);
			}
		};
	}

	@Override
	public String getDisplayName()
	{
		return "Application (Napile)";
	}

	@Override
	public String getConfigurationTypeDescription()
	{
		return "Napile Run Configuration";
	}

	@Override
	public Icon getIcon()
	{
		return AllIcons.RunConfigurations.Application;
	}

	@NotNull
	@Override
	public String getId()
	{
		return "#" + getClass().getName();
	}

	@Override
	public ConfigurationFactory[] getConfigurationFactories()
	{
		return new ConfigurationFactory[]{configurationFactory};
	}
}
