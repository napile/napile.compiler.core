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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.napile.doc.compiler.info.AllInfo;
import org.napile.doc.compiler.info.ClassInfo;
import org.napile.doc.compiler.info.PackageInfo;

/**
 * @author VISTALL
 * @date 13:23/01.02.13
 */
public class AllClassesFrameGenerator extends TemplateDocGenerator
{
	@Override
	public void generateImpl(AllInfo allInfo, Map<String, Object> map)
	{
		List<ClassInfo> list = new ArrayList<ClassInfo>();

		for(PackageInfo packageInfo : allInfo.packages.values())
			list.addAll(packageInfo.getClasses().values()) ;

		Collections.sort(list);

		map.put("list", list);

		processToFile("allclasses-frame.htm", "summary/allclasses-frame.htm");
	}
}
