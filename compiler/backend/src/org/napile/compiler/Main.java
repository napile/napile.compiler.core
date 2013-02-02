/*
 * Copyright 2010-2012 napile.org
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

package org.napile.compiler;

import static org.napile.compiler.common.ExitCode.INTERNAL_ERROR;
import static org.napile.compiler.common.ExitCode.OK;

import java.io.PrintStream;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.common.CompileEnvironmentException;
import org.napile.compiler.common.CompilerProcessor;
import org.napile.compiler.common.ExitCode;

/**
 * @author VISTALL
 * @date 19:22/11.10.12
 */
//TODO [VISTALL] move to napilec module
public class Main
{
	public static final String VERSION = "0.1";

	public static void main(String... args)
	{
		doMain(args);
	}

	public static void doMain(@NotNull String[] args)
	{
		ExitCode exitCode = doMainNoExit(System.out, args);
		if(exitCode != OK)
			System.exit(exitCode.getCode());
	}

	@NotNull
	public static ExitCode doMainNoExit(@NotNull PrintStream printStream, @NotNull String[] args)
	{
		try
		{
			ExitCode rc = new CompilerProcessor().exec(printStream, args);
			if(rc != OK)
				System.err.println("exec() finished with " + rc + " return code");

			if(Boolean.parseBoolean(System.getProperty("kotlin.print.cmd.args")))
			{
				System.out.println("Command line arguments:");
				for(String arg : args)
					System.out.println(arg);
			}
			return rc;
		}
		catch(CompileEnvironmentException e)
		{
			System.err.println(e.getMessage());
			return INTERNAL_ERROR;
		}
	}
}
