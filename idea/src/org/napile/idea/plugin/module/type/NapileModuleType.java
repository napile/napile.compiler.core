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

import javax.swing.Icon;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleTypeManager;

/**
 * @author VISTALL
 * @date 15:18/11.01.13
 */
public class NapileModuleType extends ModuleType<NapileModuleBuilder>
{
	public static final String ID = "NAPILE_MODULE";

	public static NapileModuleType getInstance()
	{
		return (NapileModuleType) ModuleTypeManager.getInstance().findByID(ID);
	}

	public NapileModuleType()
	{
		super(ID);
	}

	@Override
	public NapileModuleBuilder createModuleBuilder()
	{
		return new NapileModuleBuilder();
	}

	@Override
	public String getName()
	{
		return "Napile Module";
	}

	@Override
	public String getDescription()
	{
		return getName(); //TODO [VISTALL]
	}

	@Override
	public Icon getBigIcon()
	{
		return AllIcons.Modules.Types.JavaModule;
	}

	@Override
	public Icon getNodeIcon(@Deprecated boolean isOpened)
	{
		return AllIcons.Nodes.Module;
	}
}
