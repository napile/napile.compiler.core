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

package org.napile.idea.plugin.module.type;

import java.util.ArrayList;
import java.util.List;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleConfigurationEditor;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.ClasspathEditor;
import com.intellij.openapi.roots.ui.configuration.ContentEntriesEditor;
import com.intellij.openapi.roots.ui.configuration.ModuleConfigurationEditorProvider;
import com.intellij.openapi.roots.ui.configuration.ModuleConfigurationState;

/**
 * @author VISTALL
 * @since 15:28/11.01.13
 */
public class DefaultModuleEditorsProvider implements ModuleConfigurationEditorProvider
{
	@Override
	public ModuleConfigurationEditor[] createEditors(ModuleConfigurationState state)
	{
		ModifiableRootModel rootModel = state.getRootModel();
		Module module = rootModel.getModule();
		if(ModuleType.get(module) != NapileModuleType.getInstance())
			return ModuleConfigurationEditor.EMPTY;

		String moduleName = module.getName();
		List<ModuleConfigurationEditor> editors = new ArrayList<ModuleConfigurationEditor>();
		editors.add(new ContentEntriesEditor(moduleName, state));
		editors.add(new ClasspathEditor(state));
		return editors.toArray(new ModuleConfigurationEditor[editors.size()]);
	}
}
