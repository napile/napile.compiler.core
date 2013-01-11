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

package org.napile.idea.plugin.highlighter;

import static org.napile.compiler.lang.resolve.BindingContext.AMBIGUOUS_REFERENCE_TARGET;
import static org.napile.compiler.lang.resolve.BindingContext.EXPRESSION_TYPE;
import static org.napile.compiler.lang.resolve.BindingContext.LABEL_TARGET;
import static org.napile.compiler.lang.resolve.BindingContext.REFERENCE_TARGET;
import static org.napile.compiler.lang.lexer.NapileTokens.*;

import java.util.Collection;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.lexer.NapileNodes;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.diagnostics.Diagnostic;
import org.napile.compiler.lang.diagnostics.UnresolvedReferenceDiagnosticFactory;
import org.napile.compiler.lang.psi.NapileReferenceExpression;
import org.napile.compiler.lang.psi.NapileSimpleNameExpression;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.types.ErrorUtils;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.compiler.lang.psi.NapileElement;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.idea.plugin.module.Analyzer;
import com.google.common.collect.Sets;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;

/**
 * Quick showing possible problems with idea internals in IDEA with a tooltips
 *
 * @author abreslav
 */
public class DebugInfoAnnotator implements Annotator
{

	public static final TokenSet EXCLUDED = TokenSet.create(COLON, AS_KEYWORD, AS_SAFE, IS_KEYWORD, NOT_IS, OROR, ANDAND, EQ, ELVIS, EXCLEXCL);

	public static boolean isDebugInfoEnabled()
	{
		return ApplicationManager.getApplication().isInternal();
	}

	@Override
	public void annotate(@NotNull PsiElement element, @NotNull final AnnotationHolder holder)
	{
		if(!isDebugInfoEnabled() || !JetPsiChecker.isErrorReportingEnabled())
		{
			return;
		}
		if(!ProjectFileIndex.SERVICE.getInstance(element.getProject()).isInContent(element.getContainingFile().getVirtualFile()))
		{
			return;
		}

		if(element instanceof NapileFile)
		{
			NapileFile file = (NapileFile) element;
			try
			{
				final BindingContext bindingContext = Analyzer.analyzeAll(file).getBindingContext();

				final Set<NapileReferenceExpression> unresolvedReferences = Sets.newHashSet();
				for(Diagnostic diagnostic : bindingContext.getDiagnostics())
				{
					if(diagnostic.getFactory() instanceof UnresolvedReferenceDiagnosticFactory)
					{
						unresolvedReferences.add((NapileReferenceExpression) diagnostic.getPsiElement());
					}
				}

				file.acceptChildren(new NapileVisitorVoid()
				{

					@Override
					public void visitReferenceExpression(NapileReferenceExpression expression)
					{
						if(expression instanceof NapileSimpleNameExpression)
						{
							NapileSimpleNameExpression nameExpression = (NapileSimpleNameExpression) expression;
							IElementType elementType = expression.getNode().getElementType();
							if(elementType == NapileNodes.OPERATION_REFERENCE)
							{
								IElementType referencedNameElementType = nameExpression.getReferencedNameElementType();
								if(EXCLUDED.contains(referencedNameElementType))
								{
									return;
								}
							}
							else if(nameExpression.getReferencedNameElementType() == NapileTokens.THIS_KEYWORD)
							{
								return;
							}
						}

						String target = null;
						DeclarationDescriptor declarationDescriptor = bindingContext.get(REFERENCE_TARGET, expression);
						if(declarationDescriptor != null)
						{
							target = declarationDescriptor.toString();
						}
						else
						{
							PsiElement labelTarget = bindingContext.get(LABEL_TARGET, expression);
							if(labelTarget != null)
							{
								target = labelTarget.getText();
							}
							else
							{
								Collection<? extends DeclarationDescriptor> declarationDescriptors = bindingContext.get(AMBIGUOUS_REFERENCE_TARGET, expression);
								if(declarationDescriptors != null)
								{
									target = "[" + declarationDescriptors.size() + " descriptors]";
								}
							}
						}
						boolean resolved = target != null;
						boolean unresolved = unresolvedReferences.contains(expression);
						JetType expressionType = bindingContext.get(EXPRESSION_TYPE, expression);
						if(declarationDescriptor != null &&
								!ApplicationManager.getApplication().isUnitTestMode() &&
								(ErrorUtils.isError(declarationDescriptor) || ErrorUtils.containsErrorType(expressionType)))
						{
							holder.createErrorAnnotation(expression, "[DEBUG] Resolved to error element").setTextAttributes(NapileHighlightingColors.RESOLVED_TO_ERROR);
						}
						if(resolved && unresolved)
						{
							holder.createErrorAnnotation(expression, "[DEBUG] Reference marked as unresolved is actually resolved to " + target).setTextAttributes(NapileHighlightingColors.DEBUG_INFO);
						}
						else if(!resolved && !unresolved)
						{
							holder.createErrorAnnotation(expression, "[DEBUG] Reference is not resolved to anything, but is not marked unresolved").setTextAttributes(NapileHighlightingColors.DEBUG_INFO);
						}
					}

					@Override
					public void visitJetElement(NapileElement element)
					{
						element.acceptChildren(this);
					}
				});
			}
			catch(ProcessCanceledException e)
			{
				throw e;
			}
			catch(Throwable e)
			{
				// TODO
				holder.createErrorAnnotation(element, e.getClass().getCanonicalName() + ": " + e.getMessage());
				e.printStackTrace();
			}
		}
	}
}
