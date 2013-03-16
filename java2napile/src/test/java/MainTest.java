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

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.napile.java2napile.converter.Java2NapileConvertAction;
import org.napile.java2napile.converter.Java2NapileOutIdea;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * @author VISTALL
 * @since 13:04/04.01.13
 */
public class MainTest extends Assert
{
	@Test
	public void test()
	{
		Java2NapileOutIdea converter = new Java2NapileOutIdea("java2napile/src/testdata");
		converter.convert(new Java2NapileConvertAction()
		{
			@Override
			public void storeData(VirtualFile file, StringBuilder builder)
			{
				try
				{
					String template = "Template not found " + file.getPath();
					VirtualFile napileFile = file.getParent().findChild(file.getNameWithoutExtension() + ".ns");
					if(napileFile != null)
						template = FileUtil.loadFile(new File(napileFile.getPath()));
					assertEquals(template, builder.toString());
				}
				catch(IOException e)
				{
					throw new RuntimeException(e);
				}
			}
		});
	}
}
