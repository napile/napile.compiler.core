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

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ui.configuration.JdkComboBox;
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel;

/**
 * @author VISTALL
 * @since 21:22/22.09.12
 */
public class NapileRunSettingsEditor extends SettingsEditor<NapileRunConfiguration>
{
	public static class ModuleItem
	{
		public final Module module;

		public ModuleItem(Module module)
		{
			this.module = module;
		}

		@Override
		public String toString()
		{
			return module.getName();
		}
	}

	private JPanel rootPanel;
	private JdkComboBox jdkComboBox;
	private JComboBox moduleList;
	private JTextField mainClassField;
	private JTextField napileJvmField;

	private final Project project;

	public NapileRunSettingsEditor(final Project project)
	{
		this.project = project;

		ModuleManager moduleManager = ModuleManager.getInstance(project);
		for(Module m : moduleManager.getModules())
			moduleList.addItem(new ModuleItem(m));

		/*mainClassField.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				ModuleItem moduleItem = (ModuleItem) moduleList.getSelectedItem();
				if(moduleItem == null)
					return;
				NapileClassBrowser b = NapileClassBrowser.createApplicationClassBrowser(project, moduleItem.module);
				b.setField(mainClassField);
				b.actionPerformed(e);
			}
		}); */
	}

	private void createUIComponents()
	{
		final ProjectSdksModel projectJdksModel = ProjectStructureConfigurable.getInstance(project).getProjectJdksModel();
		if(!projectJdksModel.isInitialized())
			projectJdksModel.reset(project);

		jdkComboBox = new JdkComboBox(projectJdksModel);
	}
	@Override
	protected void resetEditorFrom(NapileRunConfiguration napileRunConfiguration)
	{
		if(napileRunConfiguration.getConfigurationModule().getModule() != null)
		{
			for(Module module : ModuleManager.getInstance(project).getModules())
				if(module.getName().equals(napileRunConfiguration.getConfigurationModule().getModuleName()))
				{
					for(int i = 0; i < moduleList.getItemCount(); i++)
					{
						Object item = moduleList.getItemAt(i);
						if(item instanceof ModuleItem && ((ModuleItem) item).module == module)
						{
							moduleList.setSelectedItem(item);
							break;
						}
					}
				}
		}

		napileJvmField.setText(napileRunConfiguration.napileJvm);
		mainClassField.setText(napileRunConfiguration.mainClass);
		final ProjectSdksModel projectJdksModel = ProjectStructureConfigurable.getInstance(project).getProjectJdksModel();
		Sdk sdk = projectJdksModel.findSdk(napileRunConfiguration.jdkName);
		if(sdk == null)
			jdkComboBox.setInvalidJdk(napileRunConfiguration.jdkName);
		else
			jdkComboBox.setSelectedJdk(sdk);
	}

	@Override
	protected void applyEditorTo(NapileRunConfiguration napileRunConfiguration) throws ConfigurationException
	{
		ModuleItem moduleItem = (ModuleItem) moduleList.getSelectedItem();
		if(moduleItem != null)
			napileRunConfiguration.setModule(moduleItem.module);

		napileRunConfiguration.mainClass = mainClassField.getText().trim();
		napileRunConfiguration.napileJvm = napileJvmField.getText().trim();
		Sdk jdk = jdkComboBox.getSelectedJdk();
		if(jdk != null)
			napileRunConfiguration.jdkName = jdk.getName();
	}

	@NotNull
	@Override
	protected JComponent createEditor()
	{
		return rootPanel;
	}

	@Override
	protected void disposeEditor()
	{
	}
}
