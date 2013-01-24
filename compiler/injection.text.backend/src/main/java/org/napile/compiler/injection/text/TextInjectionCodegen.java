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

package org.napile.compiler.injection.text;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.AsmConstants;
import org.napile.compiler.codegen.processors.ExpressionCodegen;
import org.napile.compiler.codegen.processors.codegen.stackValue.StackValue;
import org.napile.compiler.codegen.processors.injection.InjectionCodegen;
import org.napile.compiler.injection.text.gen.OnlyStringCheckVisitor;
import org.napile.compiler.injection.text.gen.PartToPartGenVisitor;
import com.intellij.psi.PsiElement;

/**
 * @author VISTALL
 * @date 18:47/12.11.12
 */
public class TextInjectionCodegen implements InjectionCodegen<TextCodeInjection>
{
	@NotNull
	@Override
	public StackValue gen(@NotNull PsiElement block, @NotNull StackValue data, ExpressionCodegen expressionCodegen)
	{
		OnlyStringCheckVisitor checkVisitor = new OnlyStringCheckVisitor();
		block.accept(checkVisitor);

		String str = checkVisitor.getText();
		if(str != null)
			return StackValue.constant(str, AsmConstants.STRING_TYPE);
		else
		{
			PartToPartGenVisitor genVisitor = new PartToPartGenVisitor(expressionCodegen.instructs, expressionCodegen);
			block.accept(genVisitor);
			genVisitor.genToString();
			return StackValue.onStack(AsmConstants.STRING_TYPE);
		}
	}

	@NotNull
	@Override
	public Class<TextCodeInjection> getInjectionType()
	{
		return TextCodeInjection.class;
	}
}
