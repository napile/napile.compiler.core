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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.napile.idea.plugin.editor.highlight.postHighlight.InjectionHighlightingVisitor;
import org.napile.idea.plugin.editor.highlight.postHighlight.LabelsHighlightingVisitor;
import org.napile.idea.plugin.editor.highlight.postHighlight.MethodsHighlightingVisitor;
import org.napile.idea.plugin.editor.highlight.postHighlight.PostHighlightVisitor;
import org.napile.idea.plugin.editor.highlight.postHighlight.SoftKeywordPostHighlightVisitor;
import org.napile.idea.plugin.editor.highlight.postHighlight.TypeKindHighlightingVisitor;
import org.napile.idea.plugin.editor.highlight.postHighlight.VariablesHighlightingVisitor;
import org.napile.idea.plugin.highlighter.JetPsiChecker;
import org.napile.idea.plugin.module.ModuleAnalyzerUtil;
import com.google.common.collect.ImmutableSet;
import com.intellij.codeHighlighting.TextEditorHighlightingPass;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.HighlightInfoType;
import com.intellij.codeInsight.daemon.impl.UpdateHighlightersUtil;
import com.intellij.codeInsight.problems.ProblemImpl;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.problems.Problem;
import com.intellij.problems.WolfTheProblemSolver;
import com.intellij.psi.MultiRangeReference;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;

/**
 * @author VISTALL
 * @since 19:21/26.02.13
 */
public class NapileHighlightPass extends TextEditorHighlightingPass
{
	public static final Set<? extends AbstractDiagnosticFactory> WARNINGS_LIKE_UNUSED = ImmutableSet.<AbstractDiagnosticFactory>builder().add(Errors.UNUSED_VARIABLE, Errors.UNUSED_PARAMETER, Errors.ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE).build();
	public static final Set<? extends AbstractDiagnosticFactory> UNRESOLVED_REFERENCES = ImmutableSet.<AbstractDiagnosticFactory>builder().add(Errors.NAMED_PARAMETER_NOT_FOUND, Errors.UNRESOLVED_REFERENCE).build();
	public static final Set<? extends AbstractDiagnosticFactory> REDECLARATION = ImmutableSet.<AbstractDiagnosticFactory>builder().add(Errors.REDECLARATION, Errors.NAME_SHADOWING).build();

	private final NapileFile file;
	private Map<PsiFile, List<HighlightInfo>> infos;

	protected NapileHighlightPass(@NotNull NapileFile file, @Nullable Document document)
	{
		super(file.getProject(), document);

		this.file = file;
	}

	@Override
	public void doCollectInformation(@NotNull ProgressIndicator progress)
	{
		final AnalyzeExhaust analyze = ModuleAnalyzerUtil.lastAnalyze(file);
		final BindingContext bindingContext = analyze.getBindingContext();

		infos = convertDiagnostic(bindingContext);

		List<HighlightInfo> selfInfos = infos.get(file);
		if(selfInfos == null)
			infos.put(file, selfInfos = new ArrayList<HighlightInfo>());

		for(PostHighlightVisitor visitor : new PostHighlightVisitor[]
		{
				new LabelsHighlightingVisitor(bindingContext, selfInfos),
				new MethodsHighlightingVisitor(bindingContext, selfInfos),
				new VariablesHighlightingVisitor(bindingContext, selfInfos),
				new TypeKindHighlightingVisitor(bindingContext, selfInfos),
				new SoftKeywordPostHighlightVisitor(bindingContext, selfInfos),
				new InjectionHighlightingVisitor(bindingContext, selfInfos)
		})
		{
			file.accept(visitor);
		}
	}

	private static Map<PsiFile, List<HighlightInfo>> convertDiagnostic(BindingContext bindingContext)
	{
		Map<PsiFile, List<HighlightInfo>> data = new HashMap<PsiFile, List<HighlightInfo>>();
		for(Diagnostic diagnostic : bindingContext.getDiagnostics())
		{
			if(!diagnostic.isValid())
				continue;

			List<HighlightInfo> infos = data.get(diagnostic.getPsiFile());
			if(infos == null)
				data.put(diagnostic.getPsiFile(), infos = new ArrayList<HighlightInfo>());


			final List<TextRange> textRanges = diagnostic.getTextRanges();

			switch(diagnostic.getSeverity())
			{
				case INFO:
					// Generic annotation
					for(TextRange textRange : textRanges)
					{
						final HighlightInfo.Builder builder = HighlightInfo.newHighlightInfo(HighlightInfoType.INFORMATION);
						builder.range(textRange);
						builder.description(JetPsiChecker.getDefaultMessage(diagnostic));
						builder.escapedToolTip(JetPsiChecker.getTooltipMessage(diagnostic));
						if(diagnostic.getFactory() == Errors.VALID_STRING_ESCAPE)
						{
							builder.textAttributes(NapileHighlightingColors.VALID_STRING_ESCAPE);
						}

						final HighlightInfo e = builder.create();
						if(e != null)
						{
							NapileQuickFixProviderEP.callRegisterFor(diagnostic, e);
							infos.add(e);
						}
					}
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
								final HighlightInfo.Builder builder = HighlightInfo.newHighlightInfo(HighlightInfoType.ERROR);
								builder.range(range.shiftRight(referenceExpression.getTextOffset()));
								builder.description(JetPsiChecker.getDefaultMessage(diagnostic));
								builder.escapedToolTip(JetPsiChecker.getTooltipMessage(diagnostic));
								builder.textAttributes(CodeInsightColors.WRONG_REFERENCES_ATTRIBUTES);

								final HighlightInfo e = builder.create();
								if(e != null)
								{
									NapileQuickFixProviderEP.callRegisterFor(diagnostic, e);
									infos.add(e);
								}
							}
						}
						else
						{
							for(TextRange textRange : textRanges)
							{
								final HighlightInfo.Builder builder = HighlightInfo.newHighlightInfo(HighlightInfoType.ERROR);
								builder.range(textRange);
								builder.description(JetPsiChecker.getDefaultMessage(diagnostic));
								builder.escapedToolTip(JetPsiChecker.getTooltipMessage(diagnostic));
								builder.textAttributes(CodeInsightColors.WRONG_REFERENCES_ATTRIBUTES);

								final HighlightInfo e = builder.create();
								if(e != null)
								{
									NapileQuickFixProviderEP.callRegisterFor(diagnostic, e);
									infos.add(e);
								}
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
						final HighlightInfo.Builder builder = HighlightInfo.newHighlightInfo(HighlightInfoType.ERROR);
						builder.range(textRange);
						builder.description(JetPsiChecker.getDefaultMessage(diagnostic));
						builder.escapedToolTip(JetPsiChecker.getTooltipMessage(diagnostic));
						if(diagnostic.getFactory() == Errors.INVALID_STRING_ESCAPE)
						{
							builder.textAttributes(NapileHighlightingColors.INVALID_STRING_ESCAPE);
						}

						final HighlightInfo e = builder.create();
						if(e != null)
						{
							NapileQuickFixProviderEP.callRegisterFor(diagnostic, e);
							infos.add(e);
						}
					}
					break;
				case WARNING:
					for(TextRange textRange : textRanges)
					{
						final HighlightInfo.Builder builder = HighlightInfo.newHighlightInfo(WARNINGS_LIKE_UNUSED.contains(diagnostic.getFactory()) ? HighlightInfoType.UNUSED_SYMBOL : HighlightInfoType.WARNING);
						builder.range(textRange);
						builder.description(JetPsiChecker.getDefaultMessage(diagnostic));
						builder.escapedToolTip(JetPsiChecker.getTooltipMessage(diagnostic));

						final HighlightInfo e = builder.create();
						if(e != null)
						{
							NapileQuickFixProviderEP.callRegisterFor(diagnostic, e);
							infos.add(e);
						}
					}
					break;
			}
		}

		return data;
	}

	@Override
	public void doApplyInformationToEditor()
	{
		if(infos == null)
			return;

		for(Map.Entry<PsiFile, List<HighlightInfo>> entry : infos.entrySet())
		{
			final PsiFile key = entry.getKey();
			final List<HighlightInfo> value = entry.getValue();

			if(key == file)
			{
				assert myDocument != null;

				UpdateHighlightersUtil.setHighlightersToEditor(myProject, myDocument, 0, file.getTextLength(), value, getColorsScheme(), getId());
			}
			else
			{
				final VirtualFile virtualFile = key.getVirtualFile();

				assert virtualFile != null;

				List<Problem> problems = new ArrayList<Problem>(value.size());
				for(HighlightInfo highlightInfo : value)
				{
					if(highlightInfo.getSeverity() == HighlightSeverity.ERROR)
					{
						problems.add(new ProblemImpl(virtualFile, highlightInfo, false));
					}
				}
				WolfTheProblemSolver.getInstance(myProject).weHaveGotProblems(virtualFile, problems);
			}
		}
	}
}
