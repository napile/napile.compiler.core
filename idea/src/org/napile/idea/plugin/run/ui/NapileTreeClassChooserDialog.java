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

package org.napile.idea.plugin.run.ui;

import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.psi.NapileClassLike;
import org.napile.idea.plugin.caches.JetShortNamesCache;
import org.napile.idea.plugin.projectView.NapileClassTreeNode;
import org.napile.idea.plugin.psi.filter.NapileClassFilterWithScope;
import com.intellij.ide.util.AbstractTreeClassChooserDialog;
import com.intellij.ide.util.TreeChooser;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.containers.ContainerUtil;

/**
 * @author VISTALL
 * @since 21:42/22.09.12
 */
public class NapileTreeClassChooserDialog extends AbstractTreeClassChooserDialog<NapileClassLike> implements TreeChooser<NapileClassLike>
{
	public NapileTreeClassChooserDialog(String title, Project project, NapileClassFilterWithScope filter)
	{
		super(title, project, filter.getScope(), NapileClassLike.class, filter, null);
	}

	@Override
	@Nullable
	protected NapileClassLike getSelectedFromTreeUserObject(DefaultMutableTreeNode node)
	{
		Object userObject = node.getUserObject();
		if(!(userObject instanceof NapileClassTreeNode))
			return null;
		NapileClassTreeNode descriptor = (NapileClassTreeNode) userObject;
		return descriptor.getValue();
	}

	@NotNull
	@Override
	protected List<NapileClassLike> getClassesByName(final String name, final boolean checkBoxState, final String pattern, final GlobalSearchScope searchScope)
	{
		final JetShortNamesCache cache = JetShortNamesCache.getInstance(getProject());
		NapileClassLike[] classes = cache.getClassesByName(name, checkBoxState ? searchScope : GlobalSearchScope.projectScope(getProject()).intersectWith(searchScope));
		return ContainerUtil.newArrayList(classes);
	}
}
