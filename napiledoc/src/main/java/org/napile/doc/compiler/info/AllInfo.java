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

package org.napile.doc.compiler.info;

import java.util.Map;
import java.util.TreeMap;

import org.napile.compiler.analyzer.AnalyzeExhaust;
import org.napile.compiler.common.NapileCoreEnvironment;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileFile;

/**
 * @author VISTALL
 * @since 14:57/01.02.13
 */
public class AllInfo
{
	public final Map<String, PackageInfo> packages = new TreeMap<String, PackageInfo>();

	public AllInfo(NapileCoreEnvironment environment, AnalyzeExhaust analyzeExhaust)
	{
		for(NapileFile napileFile : environment.getSourceFiles())
		{
			String packageName = napileFile.getPackageFqName();
			if(packageName == null)  //FIXME [VISTALL] default package?
				continue;

			PackageInfo packageInfo = packages.get(packageName);
			if(packageInfo == null)
				packages.put(packageName, packageInfo = new PackageInfo(analyzeExhaust.getBindingTrace(), napileFile));

			for(NapileClass napileClass : napileFile.getDeclarations())
			{
				ClassInfo classInfo = new ClassInfo(analyzeExhaust.getBindingTrace(), napileClass, packageName);
				packageInfo.getClasses().put(classInfo.getName(), classInfo);
			}
		}
	}
}
