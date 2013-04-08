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

package org.napile.doc.compiler.generator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.napile.doc.compiler.Arguments;
import org.napile.doc.compiler.Main;
import org.napile.doc.compiler.info.AllInfo;
import com.intellij.openapi.util.io.FileUtil;

/**
 * @author VISTALL
 * @since 12:42/01.02.13
 */
public abstract class TemplateDocGenerator implements DocGenerator
{
	protected File outDir;
	protected Map<String, Object> map;

	public abstract void generateImpl(AllInfo allInfo, Map<String, Object> map);

	@Override
	public void generate(AllInfo allInfo, File outDir)
	{
		this.outDir = outDir;

		newArguments();

		generateImpl(allInfo, map);
	}

	protected void newArguments()
	{
		this.map = new HashMap<String, Object>();
		map.put("URL", Arguments.URL);
		map.put("name", Arguments.NAME);
		map.put("version", Arguments.VERSION);
		map.put("copyright", Arguments.COPYRIGHT);
	}

	public void processToFile(String template, String toFile)
	{
		VelocityContext context = new VelocityContext();
		for(Map.Entry<String, Object> entry : map.entrySet())
			context.put(entry.getKey(), entry.getValue());

		InputStream stream = Main.class.getResourceAsStream("/org/napile/doc/compiler/" + template + ".ft");

		try
		{
			File toFileInstance = new File(outDir, toFile);
			if(!toFileInstance.exists())
			{
				FileUtil.createParentDirs(toFileInstance);

				toFileInstance.createNewFile();
			}

			FileWriter writer = new FileWriter(toFileInstance);

			Velocity.evaluate(context, writer, toFile, new InputStreamReader(stream));

			writer.close();
		}
		catch(IOException e)
		{
			throw new RuntimeException(e);
		}
	}
}
