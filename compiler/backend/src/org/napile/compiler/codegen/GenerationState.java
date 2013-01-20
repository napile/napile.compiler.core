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

/*
 * @author max
 */
package org.napile.compiler.codegen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.resolve.name.FqName;
import org.napile.asm.tree.members.ClassNode;
import org.napile.compiler.analyzer.AnalyzeExhaust;
import org.napile.compiler.codegen.processors.ClassCodegen;
import org.napile.compiler.codegen.processors.ExpressionCodegenContext;
import org.napile.compiler.codegen.processors.FqNameGenerator;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.DelegatingBindingTrace;
import com.intellij.openapi.project.Project;

public class GenerationState
{
	private final Progress progress;
	@NotNull
	private final List<NapileFile> files;
	@NotNull
	private final BindingTrace bindingTrace;

	private Map<FqName, ClassNode> classNodes = new LinkedHashMap<FqName, ClassNode>();

	private boolean used = false;

	public GenerationState(@NotNull Project project, Progress progress, @NotNull AnalyzeExhaust exhaust, @NotNull List<NapileFile> files)
	{
		this.progress = progress;
		this.files = files;

		this.bindingTrace = new DelegatingBindingTrace(exhaust.getBindingContext());
	}

	private void markUsed()
	{
		if(used)
		{
			throw new IllegalStateException(GenerationState.class + " cannot be used more than once");
		}
		used = true;
	}

	public Progress getProgress()
	{
		return progress;
	}

	public void compileAndGenerate(@NotNull CompilationErrorHandler errorHandler)
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

				ClassNode classNode = classCodegen.gen(napileClass, ExpressionCodegenContext.empty());
				classNodes.put(classNode.name, classNode);
			}
		}
		catch(Exception e)
		{
			errorHandler.reportException(e, file == null ? null : file.getVirtualFile().getUrl());
		}
	}

	public Map<FqName, ClassNode> getClassNodes()
	{
		return classNodes;
	}
}
