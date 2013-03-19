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
import org.napile.idea.plugin.module.ModuleAnalyzerUtil;
import com.intellij.codeHighlighting.TextEditorHighlightingPass;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.progress.ProgressIndicator;

/**
 * @author VISTALL
 * @since 16:55/19.03.13
 */
public class NapileAnalyzeHighlightPass extends TextEditorHighlightingPass
{
	private final NapileFile file;


	protected NapileAnalyzeHighlightPass(@NotNull NapileFile file, @Nullable Document document)
	{
		super(file.getProject(), document);

		this.file = file;
	}

	@Override
	public void doCollectInformation(@NotNull ProgressIndicator progress)
	{
		ModuleAnalyzerUtil.analyze(file);
	}

	@Override
	public void doApplyInformationToEditor()
	{
	}
}
