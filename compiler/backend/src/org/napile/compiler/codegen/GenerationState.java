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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.utils.Progress;
import org.napile.asm.tree.members.ClassNode;
import org.napile.compiler.analyzer.AnalyzeExhaust;
import org.napile.compiler.codegen.processors.ClassGenerator;
import org.napile.compiler.codegen.processors.FqNameGenerator;
import org.napile.compiler.di.InjectorForJvmCodegen;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.DelegatingBindingTrace;
import org.napile.compiler.lang.resolve.name.FqName;
import com.intellij.openapi.project.Project;

public class GenerationState
{
	private final Progress progress;
	@NotNull
	private final List<NapileFile> files;
	@NotNull
	private final InjectorForJvmCodegen injector;

	private Map<FqName, ClassNode> classNodes = new LinkedHashMap<FqName, ClassNode>();

	private boolean used = false;

	public GenerationState(@NotNull Project project, Progress progress, @NotNull AnalyzeExhaust exhaust, @NotNull List<NapileFile> files)
	{
		this.progress = progress;
		this.files = files;

		final DelegatingBindingTrace trace = new DelegatingBindingTrace(exhaust.getBindingContext());

		this.injector = new InjectorForJvmCodegen(trace, this.files, project);
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

	@NotNull
	public InjectorForJvmCodegen getInjector()
	{
		return injector;
	}

	public BindingContext getBindingContext()
	{
		return injector.getBindingContext();
	}

	public void compileAndGenerate(@NotNull CompilationErrorHandler errorHandler)
	{
		markUsed();

		FqNameGenerator fqNameGenerator = new FqNameGenerator(injector.getBindingTrace());

		List<NapileClass> classes = new ArrayList<NapileClass>();
		for(NapileFile napileFile : files)
			classes.addAll(napileFile.getDeclarations());

		for(NapileClass napileClass : classes)
			napileClass.accept(fqNameGenerator, null);

		ClassGenerator classGenerator = new ClassGenerator(injector.getBindingTrace(), classNodes);

		for(NapileClass napileClass : classes)
			napileClass.accept(classGenerator, null);

		classNodes = classGenerator.getClassNodes();
	}

	public void destroy()
	{
		injector.destroy();
	}

	public Map<FqName, ClassNode> getClassNodes()
	{
		return classNodes;
	}
}
