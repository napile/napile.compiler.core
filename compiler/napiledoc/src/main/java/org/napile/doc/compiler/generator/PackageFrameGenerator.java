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
import java.util.Map;

import org.napile.doc.compiler.info.AllInfo;
import org.napile.doc.compiler.info.PackageInfo;

/**
 * @author VISTALL
 * @date 14:17/01.02.13
 */
public class PackageFrameGenerator extends TemplateDocGenerator
{
	@Override
	public void generate(AllInfo allInfo, File outDir)
	{
		this.outDir = outDir;

		for(Map.Entry<String, PackageInfo> entry : allInfo.packages.entrySet())
		{
			String packageName = entry.getKey();

			PackageInfo value = entry.getValue();

			newArguments();

			map.put("package", value);
			map.put("next", MapUtil.getNextElement(allInfo.packages, value));
			map.put("prev", MapUtil.getPrevElement(allInfo.packages, value));

			processToFile("package-frame.htm", "api/" + packageName.replace(".", "/") + "/package-frame.htm");

			processToFile("package-summary.htm", "api/" + packageName.replace(".", "/") + "/package-summary.htm");
		}
	}

	@Override
	public void generateImpl(AllInfo allInfo, Map<String, Object> map)
	{
		//
	}
}
