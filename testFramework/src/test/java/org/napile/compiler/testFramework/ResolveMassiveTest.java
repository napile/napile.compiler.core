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

package org.napile.compiler.testFramework;

import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.psi.NapileReferenceExpression;
import org.napile.compiler.lang.resolve.BindingTraceKeys;
import org.napile.compiler.lang.resolve.DescriptorUtils;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import junit.framework.Test;

/**
 * @author VISTALL
 * @since 16:22/16.04.13
 */
public class ResolveMassiveTest extends AbstractMassiveTestSuite
{
	public static Test suite()
	{
		return new ResolveMassiveTest();
	}

	@Override
	public String getExpectedText(NapileFile file) throws Exception
	{
		StringBuilder builder = new StringBuilder();

		walk(file.getNode(), builder);

		return builder.toString();
	}

	private void walk(ASTNode node, StringBuilder builder)
	{
		ASTNode temp = node.getFirstChildNode();
		while(temp != null)
		{
			appendText(builder, temp);

			temp = temp.getTreeNext();
		}
	}

	private void appendText(StringBuilder builder, ASTNode temp)
	{
		PsiElement element = temp.getPsi();
		if(element instanceof NapileReferenceExpression)
		{
			final DeclarationDescriptor declarationDescriptor = analyzeExhaust.getBindingTrace().get(BindingTraceKeys.REFERENCE_TARGET, (NapileReferenceExpression) element);

			if(declarationDescriptor == null)
			{
				builder.append("<ref to=\"null\">");
			}
			else
			{
				builder.append("<ref class=\"").append(declarationDescriptor.getClass().getSimpleName()).append("\" to=\"").append(DescriptorUtils.getFQName(declarationDescriptor)).append("\">");
			}

			walk(temp, builder);

			builder.append("</ref>");
		}
		else
		{
			if(temp.getFirstChildNode() == null)
			{
				builder.append(temp.getText());
			}
			else
			{
				walk(temp, builder);
			}
		}
	}


	@Override
	public String getResultExt()
	{
		return "resolve";
	}
}
