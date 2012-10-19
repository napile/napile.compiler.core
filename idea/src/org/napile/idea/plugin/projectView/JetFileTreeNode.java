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

package org.napile.idea.plugin.projectView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileFile;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.PsiFileNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;

/**
 * User: Alefas
 * Date: 15.02.12
 */
public class JetFileTreeNode extends PsiFileNode
{
	public JetFileTreeNode(Project project, NapileFile value, ViewSettings viewSettings)
	{
		super(project, value, viewSettings);
	}

	@Override
	public Collection<AbstractTreeNode> getChildrenImpl()
	{
		NapileFile file = (NapileFile) getValue();

		if(file == null || !getSettings().isShowMembers())
			return Collections.emptyList();

		List<NapileClass> classes = file.getDeclarations();
		List<AbstractTreeNode> result = new ArrayList<AbstractTreeNode>(classes.size());
		for(NapileClass declaration : file.getDeclarations())
			result.add(new NapileClassTreeNode(file.getProject(), declaration, getSettings()));
		return result;
	}
}
