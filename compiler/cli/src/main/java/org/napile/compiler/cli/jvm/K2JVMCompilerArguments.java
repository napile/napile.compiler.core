/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.napile.compiler.cli.jvm;

import org.napile.compiler.cli.common.CompilerArguments;
import com.sampullara.cli.Argument;

/**
 * Command line arguments for the {@link K2JVMCompiler}
 */
public class K2JVMCompilerArguments extends CompilerArguments
{
	@Argument(value = "classpath", description = "classpath to use when compiling")
	public String classpath;

	@Argument(value = "builtins", description = "compile builtin classes (internal)")
	public boolean builtins;

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

	@Override
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

	@Override
	public boolean isTags()
	{
		return tags;
	}

	@Override
	public boolean isVersion()
	{
		return version;
	}

	@Override
	public boolean isVerbose()
	{
		return verbose;
	}

	public void setTags(boolean tags)
	{
		this.tags = tags;
	}
}
