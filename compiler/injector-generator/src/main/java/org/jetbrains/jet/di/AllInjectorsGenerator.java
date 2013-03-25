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

package org.jetbrains.jet.di;

import java.io.IOException;

import org.napile.compiler.lang.descriptors.ModuleDescriptor;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.NamespaceFactoryImpl;
import org.napile.compiler.lang.resolve.TopDownAnalysisContext;
import org.napile.compiler.lang.resolve.TopDownAnalysisParameters;
import org.napile.compiler.lang.resolve.TopDownAnalyzer;
import org.napile.compiler.lang.resolve.processors.BodyResolver;
import org.napile.compiler.lang.resolve.processors.ControlFlowAnalyzer;
import org.napile.compiler.lang.resolve.processors.DescriptorResolver;
import org.napile.compiler.lang.resolve.processors.checkers.DeclarationsChecker;
import org.napile.compiler.lang.types.expressions.ExpressionTypingServices;
import com.intellij.openapi.project.Project;

/**
 * @author abreslav
 */
public class AllInjectorsGenerator
{

	public static void main(String[] args) throws IOException
	{
		generateInjectorForTopDownAnalyzerBasic();
	}

	private static void generateInjectorForTopDownAnalyzerBasic() throws IOException
	{
		DependencyInjectorGenerator generator = new DependencyInjectorGenerator(false);
		// Fields
		generator.addPublicField(TopDownAnalyzer.class);
		generator.addPublicField(TopDownAnalysisContext.class);
		generator.addPublicField(BodyResolver.class);
		generator.addPublicField(ControlFlowAnalyzer.class);
		generator.addPublicField(DeclarationsChecker.class);
		generator.addPublicField(DescriptorResolver.class);
		generator.addPublicField(ExpressionTypingServices.class);

		// Parameters
		generator.addPublicParameter(Project.class);
		generator.addPublicParameter(TopDownAnalysisParameters.class);
		generator.addPublicParameter(BindingTrace.class);
		generator.addParameter(ModuleDescriptor.class);

		generator.addField(NamespaceFactoryImpl.class);
		generator.generate("compiler/frontend/src/main/java", "org.napile.compiler.di", "InjectorForTopDownAnalyzerBasic");
	}
}
