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

package org.napile.idea.plugin.editor.highlight;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.psi.NapileFile;
import com.intellij.codeHighlighting.Pass;
import com.intellij.codeHighlighting.TextEditorHighlightingPass;
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory;
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;

/**
 * @author VISTALL
 * @since 19:22/26.02.13
 */
public class NapileHighlightPassFactory extends AbstractProjectComponent implements TextEditorHighlightingPassFactory
{
	public NapileHighlightPassFactory(Project project, TextEditorHighlightingPassRegistrar registrar)
	{
		super(project);

		registrar.registerTextEditorHighlightingPass(this, TextEditorHighlightingPassRegistrar.Anchor.AFTER, Pass.UPDATE_FOLDING, false, false);
	}

	@Nullable
	@Override
	public TextEditorHighlightingPass createHighlightingPass(@NotNull PsiFile file, @NotNull Editor editor)
	{
		if(!(file instanceof NapileFile))
			return null;

		return new NapileHighlightPass((NapileFile) file, editor.getDocument());
	}
}
