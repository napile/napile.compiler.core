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

package org.napile.compiler.testFramework;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.resolve.name.FqName;
import org.napile.asm.tree.members.ClassNode;
import org.napile.compiler.analyzer.AnalyzeExhaust;
import org.napile.compiler.codegen.GenerationState;
import org.napile.compiler.codegen.processors.ClassCodegen;
import org.napile.compiler.codegen.processors.ExpressionCodegenContext;
import org.napile.compiler.codegen.processors.FqNameGenerator;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.resolve.BindingTrace;

/**
 * @author VISTALL
 * @since 19:15/18.04.13
 */
public class TestGenerationState
{
	@NotNull
	private final Collection<NapileFile> files;
	@NotNull
	private final BindingTrace bindingTrace;

	private Map<FqName, ClassNode> classNodes = new LinkedHashMap<FqName, ClassNode>();

	private boolean used = false;

	public TestGenerationState(@NotNull AnalyzeExhaust exhaust, @NotNull Collection<NapileFile> files)
	{
		this.files = files;

		this.bindingTrace = exhaust.getBindingTrace();
	}

	private void markUsed()
	{
		if(used)
		{
			throw new IllegalStateException(GenerationState.class + " cannot be used more than once");
		}
		used = true;
	}

	public void compileAndGenerate() throws Exception
	{
		NapileFile file = null;
		try
		{
			markUsed();

			FqNameGenerator fqNameGenerator = new FqNameGenerator(bindingTrace);

			List<NapileClass> classes = new ArrayList<NapileClass>();
			for(NapileFile napileFile : files)
			{
				file = napileFile;

				classes.addAll(Arrays.asList(napileFile.getDeclarations()));
			}

			for(NapileClass napileClass : classes)
			{
				file = napileClass.getContainingFile();

				napileClass.accept(fqNameGenerator, null);
			}

			classNodes = new LinkedHashMap<FqName, ClassNode>(classes.size());

			ClassCodegen classCodegen = new ClassCodegen(bindingTrace);
			for(NapileClass napileClass : classes)
			{
				file = napileClass.getContainingFile();

				try
				{
					ClassNode classNode = classCodegen.gen(napileClass, ExpressionCodegenContext.empty());
					classNodes.put(classNode.name, classNode);
				}
				catch(Exception e)
				{
					//ignore all
				}
			}
		}
		catch(Exception e)
		{
			throw new Exception(file == null ? null : file.getVirtualFile().getPath(), e);
		}
	}

	public Map<FqName, ClassNode> getClassNodes()
	{
		return classNodes;
	}
}