package org.napile.compiler.bytecode;/*
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.intellij.openapi.util.io.FileUtil;

/**
 * @author VISTALL
 * @date 18:05/15.02.13
 */
public class Generator
{
	public static void main(String[] args) throws Exception
	{
		List<File> files = new ArrayList<File>();

		File dir = new File("compiler/frontend.bytecode/rsc/test/java");

		collectData(files, dir);


		StringBuilder builder = new StringBuilder();

		builder.append("package org.napile.compiler.bytecode;\n\n");
		builder.append("public class ConvertTest extends ConvertTestCase\n{\n");
		for(File file : files)
		{
			String relativePath = FileUtil.getRelativePath(dir, file);
			if(relativePath == null)
				continue;

			String methodName = relativePath.replace("\\", "_").replace(".nxml", "");

			builder.append("\tpublic void test$").append(methodName).append("() {}\n");
		}
		builder.append("}");

		FileUtil.writeToFile(new File("compiler/frontend.bytecode/src/test/java/org/napile/compiler/bytecode/ConvertTest.java"), builder.toString());
	}

	private static void collectData(List<File> nodes, File file)
	{
		if(file.isDirectory())
		{
			for(File d : file.listFiles())
			{
				collectData(nodes, d);
			}
		}
		else if(file.getName().endsWith(".nxml"))
		{
			nodes.add(file);
		}
	}
}
