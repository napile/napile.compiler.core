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

import java.io.File;
import java.util.Set;
import java.util.TreeSet;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;

/**
 * @author VISTALL
 * @since 15:49/14.04.13
 */
public class AbstractTestGenerator
{
	protected void gen()  throws Exception
	{
		File file = new File("testFramework/src/test/napileRt");
		Set<String> files = new TreeSet<String>();

		collect(file, files, file.getAbsolutePath());

		final File diagnosticFile = new File("testFramework/src/test/java/org/napile/compiler/testFramework/" + getClass().getSimpleName().replace("Generator", "") + ".java");

		final String data = FileUtil.loadFile(diagnosticFile);

		StringBuilder builder = new StringBuilder();
		builder.append(data.substring(0, data.indexOf("// START GEN")));

		builder.append("// START GEN\n");
		for(String s : files)
		{
			builder.append("\tpublic void test").append(s.replace("/", "$")).append("() throws Exception\n\t{\n\t\tdoTest();\n\t}\n\n");
		}
		builder.append("}");

		FileUtil.writeToFile(diagnosticFile, builder.toString());
	}


	private void collect(File dirOrFile, Set<String> set, String root)
	{
		if(dirOrFile.isDirectory())
		{
			File[] files = dirOrFile.listFiles();
			if(files == null)
			{
				return;
			}

			for(File file : files)
			{
				collect(file, set, root);
			}
		}
		else if(FileUtilRt.getExtension(dirOrFile.getName()).equalsIgnoreCase("ns"))
		{
			final String absolutePath = dirOrFile.getAbsolutePath();

			String path = absolutePath.substring(root.length() + 1);
			path = FileUtilRt.toSystemIndependentName(path);

			set.add(FileUtil.getNameWithoutExtension(path));
		}
	}
}
