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

import java.util.ArrayList;
import java.util.List;

import org.napile.compiler.lang.diagnostics.Diagnostic;
import com.intellij.openapi.util.TextRange;

/**
 * @author VISTALL
 * @since 15:14/16.04.13
 */
public class DiagnosticTextBuilder
{
	private static class IndexInfo
	{
		private int offset;

		private String text;
	}

	private final List<Diagnostic> diagnostics;

	private StringBuilder builder;

	private String text;

	private final List<IndexInfo> indexes = new ArrayList<IndexInfo>();

	public DiagnosticTextBuilder(List<Diagnostic> d, String t)
	{
		diagnostics = d;
		text = t;
	}

	public String getText()
	{
		build();

		return builder.toString();
	}

	private void build()
	{

		for(Diagnostic diagnostic : diagnostics)
		{
			for(TextRange textRange : diagnostic.getTextRanges())
			{
				initIndex(textRange.getStartOffset(), "<" + diagnostic.getFactory().getName()+ ">");
				initIndex(textRange.getEndOffset(), "</" + diagnostic.getFactory().getName() + ">");
			}
		}

		builder = new StringBuilder();

		for(int i = 0; i < text.length(); i++)
		{
			for(IndexInfo indexInfo : indexes)
			{
				if(indexInfo.offset == i)
				{
					builder.append(indexInfo.text);
				}
			}

			builder.append(text.charAt(i));
		}
	}

	private IndexInfo initIndex(int offset, String text)
	{
		IndexInfo indexInfo = new IndexInfo();
		indexInfo.offset = offset;
		indexInfo.text = text;

		indexes.add(indexInfo);
		return indexInfo;
	}
}
