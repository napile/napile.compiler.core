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

package org.napile.idea.plugin.util;

import org.napile.compiler.lang.psi.NapileNamedDeclaration;
import org.napile.doc.lang.psi.NapileDoc;
import org.napile.doc.lang.psi.NapileDocLine;
import org.pegdown.PegDownProcessor;

/**
 * @author VISTALL
 * @since 16:00/08.02.13
 */
//FIXME [VISTALL] move to frontend.doc
public class NapileDocUtil
{
	private static final PegDownProcessor PROCESSOR = new PegDownProcessor();

	public static String render(NapileNamedDeclaration declaration)
	{
		NapileDoc doc = declaration.getDocComment();
		if(doc != null)
			return render(doc);
		else
			return "";
	}

	public static String render(NapileDoc doc)
	{
		StringBuilder builder = new StringBuilder();
		for(NapileDocLine line : doc.getLines())
		{
			builder.append(line.getText()).append("\n\r");
		}
		return PROCESSOR.markdownToHtml(builder.toString());
	}
}
