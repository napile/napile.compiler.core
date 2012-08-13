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

package org.jetbrains.jet.plugin.completion;

import org.jetbrains.annotations.NotNull;
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.util.Consumer;

/**
 * @author Nikolay Krasko
 */
public class JetClassCompletionContributor extends CompletionContributor
{
	public JetClassCompletionContributor()
	{
		// Should be removed in new idea
		//extend(CompletionType.CLASS_NAME, PlatformPatterns.psiElement(),
		//       new CompletionProvider<CompletionParameters>() {
		//           @Override
		//           protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context,
		//                                         final @NotNull CompletionResultSet result) {
		//               final CompletionResultSet jetResult = JetCompletionSorting.addJetSorting(parameters, result);
		//
		//               final PsiElement position = parameters.getPosition();
		//               if (!(position.getContainingFile() instanceof JetFile)) {
		//                   return;
		//               }
		//
		//               final PsiReference ref = position.getContainingFile().findReferenceAt(parameters.getOffset());
		//               if (ref instanceof JetSimpleNameReference) {
		//                   addClasses(parameters, result, new Consumer<LookupElement>() {
		//                       @Override
		//                       public void consume(LookupElement lookupElement) {
		//                           jetResult.addElement(lookupElement);
		//                       }
		//                   });
		//               }
		//
		//               result.stopHere();
		//           }
		//       });
	}

	/**
	 * Jet classes will be added as java completions for unification
	 */
	static void addClasses(@NotNull final CompletionParameters parameters, @NotNull final CompletionResultSet result, @NotNull final Consumer<LookupElement> consumer)
	{


	}
}
