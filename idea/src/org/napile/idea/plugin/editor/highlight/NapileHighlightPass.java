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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.analyzer.AnalyzeExhaust;
import org.napile.compiler.lang.diagnostics.AbstractDiagnosticFactory;
import org.napile.compiler.lang.diagnostics.Diagnostic;
import org.napile.compiler.lang.diagnostics.Errors;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.psi.NapileReferenceExpression;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.idea.plugin.highlighter.JetPsiChecker;
import org.napile.idea.plugin.module.ModuleAnalyzerUtil;
import com.google.common.collect.ImmutableSet;
import com.intellij.codeHighlighting.TextEditorHighlightingPass;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.HighlightInfoType;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.MultiRangeReference;
import com.intellij.psi.PsiReference;

/**
 * @author VISTALL
 * @date 19:21/26.02.13
 */
public class NapileHighlightPass extends TextEditorHighlightingPass
{
	public static final Set<? extends AbstractDiagnosticFactory> WARNINGS_LIKE_UNUSED = ImmutableSet.<AbstractDiagnosticFactory>builder().add(Errors.UNUSED_VARIABLE, Errors.UNUSED_PARAMETER, Errors.ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE).build();
	public static final Set<? extends AbstractDiagnosticFactory> UNRESOLVED_REFERENCES = ImmutableSet.<AbstractDiagnosticFactory>builder().add(Errors.NAMED_PARAMETER_NOT_FOUND, Errors.UNRESOLVED_REFERENCE).build();
	public static final Set<? extends AbstractDiagnosticFactory> REDECLARATION = ImmutableSet.<AbstractDiagnosticFactory>builder().add(Errors.REDECLARATION, Errors.NAME_SHADOWING).build();

	private final NapileFile file;

	protected NapileHighlightPass(@NotNull NapileFile file, @Nullable Document document)
	{
		super(file.getProject(), document);

		this.file = file;
	}

	@Override
	public void doCollectInformation(@NotNull ProgressIndicator progress)
	{
	}

	@Override
	public void doApplyInformationToEditor()
	{
	}

	@Nullable
	@Override
	public List<HighlightInfo> getInfos()
	{
		List<HighlightInfo> infos = new ArrayList<HighlightInfo>();

		final AnalyzeExhaust analyze = ModuleAnalyzerUtil.analyze(file);
		final BindingContext bindingContext = analyze.getBindingContext();

		for(Diagnostic diagnostic :  bindingContext.getDiagnostics())
		{
			if(!diagnostic.isValid())
				continue;

			final List<TextRange> textRanges = diagnostic.getTextRanges();

			switch(diagnostic.getSeverity())
			{
				case INFO:
					break;
				case ERROR:
					if(UNRESOLVED_REFERENCES.contains(diagnostic.getFactory()))
					{
						NapileReferenceExpression referenceExpression = (NapileReferenceExpression) diagnostic.getPsiElement();
						PsiReference reference = referenceExpression.getReference();
						if(reference instanceof MultiRangeReference)
						{
							MultiRangeReference mrr = (MultiRangeReference) reference;
							for(TextRange range : mrr.getRanges())
							{
								final HighlightInfo.Builder builder = HighlightInfo.newHighlightInfo(HighlightInfoType.UNUSED_SYMBOL);
								builder.range(range.shiftRight(referenceExpression.getTextOffset()));
								builder.description(JetPsiChecker.getDefaultMessage(diagnostic));
								builder.unescapedToolTip(JetPsiChecker.getTooltipMessage(diagnostic));

								infos.add(builder.create());
							}
						}
						else
						{
							for(TextRange textRange : textRanges)
							{
								final HighlightInfo.Builder builder = HighlightInfo.newHighlightInfo(HighlightInfoType.UNUSED_SYMBOL);
								builder.range(textRange);
								builder.description(JetPsiChecker.getDefaultMessage(diagnostic));
								builder.unescapedToolTip(JetPsiChecker.getTooltipMessage(diagnostic));

								infos.add(builder.create());
							}
						}

						continue;
					}

					if(REDECLARATION.contains(diagnostic.getFactory()))
					{
						//registerQuickFix(markRedeclaration(redeclarations, diagnostic, holder), diagnostic);
						continue;
					}

					// Generic annotation
					for(TextRange textRange : textRanges)
					{
						final HighlightInfo.Builder builder = HighlightInfo.newHighlightInfo(diagnostic.getFactory() == Errors.INVISIBLE_REFERENCE ? HighlightInfoType.UNUSED_SYMBOL : HighlightInfoType.WARNING);
						builder.range(textRange);
						builder.description(JetPsiChecker.getDefaultMessage(diagnostic));
						builder.unescapedToolTip(JetPsiChecker.getTooltipMessage(diagnostic));

						infos.add(builder.create());
					}
					break;
				case WARNING:
					for(TextRange textRange : textRanges)
					{
						final HighlightInfo.Builder builder = HighlightInfo.newHighlightInfo(WARNINGS_LIKE_UNUSED.contains(diagnostic.getFactory()) ? HighlightInfoType.UNUSED_SYMBOL : HighlightInfoType.WARNING);
						builder.range(textRange);
						builder.description(JetPsiChecker.getDefaultMessage(diagnostic));
						builder.unescapedToolTip(JetPsiChecker.getTooltipMessage(diagnostic));

						infos.add(builder.create());
					}
					break;
			}
		}

		return infos;
	}
}
