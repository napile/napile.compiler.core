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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.analyzer.AnalyzeExhaust;
import org.napile.compiler.lang.diagnostics.AbstractDiagnosticFactory;
import org.napile.compiler.lang.diagnostics.Diagnostic;
import org.napile.compiler.lang.diagnostics.Errors;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.psi.NapileReferenceExpression;
import org.napile.compiler.lang.resolve.BindingTrace;
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
import com.intellij.codeInsight.daemon.impl.quickfix.QuickFixAction;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.problems.ProblemImpl;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.problems.Problem;
import com.intellij.problems.WolfTheProblemSolver;
import com.intellij.psi.MultiRangeReference;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.util.containers.MultiMap;

/**
 * @author VISTALL
 * @since 19:21/26.02.13
 */
public class NapileHighlightPass extends TextEditorHighlightingPass
{
	public static final Set<? extends AbstractDiagnosticFactory> WARNINGS_LIKE_UNUSED = ImmutableSet.<AbstractDiagnosticFactory>builder().add(Errors.UNUSED_VARIABLE, Errors.UNUSED_PARAMETER, Errors.ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE).build();
	public static final Set<? extends AbstractDiagnosticFactory> UNRESOLVED_REFERENCES = ImmutableSet.<AbstractDiagnosticFactory>builder().add(Errors.NAMED_PARAMETER_NOT_FOUND, Errors.UNRESOLVED_REFERENCE, Errors.EXPECTED_METHOD_NOT_FOUND).build();
	public static final Set<? extends AbstractDiagnosticFactory> REDECLARATION = ImmutableSet.<AbstractDiagnosticFactory>builder().add(Errors.REDECLARATION, Errors.NAME_SHADOWING).build();

	private final NapileFile file;
	private final Editor editor;

	private MultiMap<PsiFile, HighlightInfo> infos;
	private MultiMap<HighlightInfo, IntentionAction> quickFixes;

	protected NapileHighlightPass(@NotNull NapileFile file, @NotNull Editor editor)
	{
		super(file.getProject(), editor.getDocument());

		this.file = file;
		this.editor = editor;
	}

	@Override
	public void doCollectInformation(@NotNull ProgressIndicator progress)
	{
		final AnalyzeExhaust analyze = ModuleAnalyzerUtil.lastAnalyze(file);
		final BindingTrace bindingTrace = analyze.getBindingTrace();

		quickFixes = new MultiMap<HighlightInfo, IntentionAction>();
		infos = new MultiMap<PsiFile, HighlightInfo>();

		convertDiagnostic(bindingTrace);

		Collection<HighlightInfo> selfInfos = infos.getModifiable(file);

		for(PostHighlightVisitor visitor : new PostHighlightVisitor[]
		{
				new LabelsHighlightingVisitor(bindingTrace, selfInfos),
				new MethodsHighlightingVisitor(bindingTrace, selfInfos),
				new VariablesHighlightingVisitor(bindingTrace, selfInfos),
				new TypeKindHighlightingVisitor(bindingTrace, selfInfos),
				new SoftKeywordPostHighlightVisitor(bindingTrace, selfInfos),
				new InjectionHighlightingVisitor(bindingTrace, selfInfos)
		})
		{
			file.accept(visitor);
		}
	}

	private void convertDiagnostic(BindingTrace bindingContext)
	{
		for(Diagnostic diagnostic : bindingContext.getDiagnostics())
		{
			if(!diagnostic.isValid())
				continue;

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

						putHighlightAndActions(builder, diagnostic);
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

								putHighlightAndActions(builder, diagnostic);
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

								putHighlightAndActions(builder, diagnostic);
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

						putHighlightAndActions(builder, diagnostic);
					}
					break;
				case WARNING:
					for(TextRange textRange : textRanges)
					{
						final HighlightInfo.Builder builder = HighlightInfo.newHighlightInfo(WARNINGS_LIKE_UNUSED.contains(diagnostic.getFactory()) ? HighlightInfoType.UNUSED_SYMBOL : HighlightInfoType.WARNING);
						builder.range(textRange);
						builder.description(JetPsiChecker.getDefaultMessage(diagnostic));
						builder.escapedToolTip(JetPsiChecker.getTooltipMessage(diagnostic));

						putHighlightAndActions(builder, diagnostic);
					}
					break;
			}
		}
	}

	private void putHighlightAndActions(HighlightInfo.Builder builder, Diagnostic diagnostic)
	{
		final HighlightInfo e = builder.create();
		if(e != null)
		{
			infos.putValue(diagnostic.getPsiFile(), e);

			NapileQuickFixProviderEP.collectionQuickActions(editor, diagnostic, e, quickFixes);
		}
	}

	@Override
	public void doApplyInformationToEditor()
	{
		if(infos != null)
		{
			for(Map.Entry<PsiFile, Collection<HighlightInfo>> entry : infos.entrySet())
			{
				final PsiFile key = entry.getKey();
				final Collection<HighlightInfo> value = entry.getValue();

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

		if(quickFixes != null)
		{

			for(Map.Entry<HighlightInfo, Collection<IntentionAction>> entry : quickFixes.entrySet())
			{
				for(IntentionAction intentionAction : entry.getValue())
				{
					QuickFixAction.registerQuickFixAction(entry.getKey(), intentionAction);
				}
			}
		}
	}
}
