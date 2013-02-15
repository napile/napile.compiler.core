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
import java.io.FileReader;
import java.io.IOException;

import org.napile.asm.io.xml.in.AsmXmlFileReader;
import org.napile.asm.tree.members.ClassNode;
import org.napile.compiler.util.NodeToStringConverter;
import com.intellij.openapi.util.io.FileUtil;
import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * @author VISTALL
 * @date 18:06/15.02.13
 */
public abstract class ConvertTestCase extends TestCase
{
	private boolean createResults = false;

	@Override
	protected void runTest() throws java.lang.Throwable
	{
		AsmXmlFileReader fileReader = new AsmXmlFileReader();

		File file = new File("compiler/frontend.bytecode/rsc/test/java/" + getName().substring(5, getName().length()).replace("_", "/") + ".nxml");

		ClassNode classNode = fileReader.read(new FileReader(file));

		String currentResult = NodeToStringConverter.convertClass(classNode);

		String result = "not found";
		try
		{
			result = FileUtil.loadFile(new File(file.getAbsolutePath() + ".ns.result"));
		}
		catch(IOException e)
		{
			//
		}

		if(createResults && !currentResult.equals(result))
		{
			FileUtil.writeToFile( new File(file.getAbsolutePath() + ".ns.result"), currentResult);
		}

		Assert.assertEquals(result, currentResult);
	}
}
