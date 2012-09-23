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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.jetbrains.annotations.NotNull;
import org.napile.idea.plugin.run.ui.NapileClassBrowser;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.JdkComboBox;
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;

/**
 * @author VISTALL
 * @date 21:22/22.09.12
 */
public class NapileRunSettingsEditor extends SettingsEditor<NapileRunConfiguration>
{
	private JPanel rootPanel;
	private TextFieldWithBrowseButton mainClassField;
	private JdkComboBox jdkComboBox;

	private Project project;

	public NapileRunSettingsEditor(final Project project)
	{
		this.project = project;
		mainClassField.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				NapileClassBrowser b = NapileClassBrowser.createApplicationClassBrowser(project, null);
				b.setField(mainClassField);
				b.actionPerformed(e);
			}
		});
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
	}

	@Override
	protected void applyEditorTo(NapileRunConfiguration napileRunConfiguration) throws ConfigurationException
	{
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
