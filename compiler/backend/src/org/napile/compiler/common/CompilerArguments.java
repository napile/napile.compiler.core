/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.napile.compiler.common;

import java.util.List;

import com.google.common.collect.Lists;
import com.sampullara.cli.Argument;

/**
 * @author Pavel Talanov
 */
public class CompilerArguments
{
	public List<String> freeArgs = Lists.newArrayList();

	@Argument(value = "classpath", description = "classpath to use when compiling")
	public String classpath;

	@Argument(value = "output", description = "output directory")
	public String outputDir;

	@Argument(value = "tags", description = "Demarcate each compilation message (error, warning, etc) with an open and close tag")
	public boolean tags;

	@Argument(value = "verbose", description = "Enable verbose logging output")
	public boolean verbose;

	@Argument(value = "version", description = "Display compiler version")
	public boolean version;

	@Argument(value = "help", alias = "h", description = "show help")
	public boolean help;

	public String getClasspath()
	{
		return classpath;
	}

	public void setClasspath(String classpath)
	{
		this.classpath = classpath;
	}

	public boolean isHelp()
	{
		return help;
	}

	public void setHelp(boolean help)
	{
		this.help = help;
	}

	public String getOutputDir()
	{
		return outputDir;
	}

	public void setOutputDir(String outputDir)
	{
		this.outputDir = outputDir;
	}

	public boolean isTags()
	{
		return tags;
	}

	public boolean isVersion()
	{
		return version;
	}

	public boolean isVerbose()
	{
		return verbose;
	}

	public void setTags(boolean tags)
	{
		this.tags = tags;
	}
}
