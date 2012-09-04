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
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.NamespaceFactoryImpl;
import org.napile.compiler.lang.resolve.TopDownAnalysisContext;
import org.napile.compiler.lang.resolve.TopDownAnalysisParameters;
import org.napile.compiler.lang.resolve.TopDownAnalyzer;
import org.napile.compiler.lang.resolve.lazy.ResolveSession;
import org.napile.compiler.lang.resolve.lazy.ScopeProvider;
import org.napile.compiler.lang.resolve.processors.AnnotationResolver;
import org.napile.compiler.lang.resolve.processors.BodyResolver;
import org.napile.compiler.lang.resolve.processors.ControlFlowAnalyzer;
import org.napile.compiler.lang.resolve.processors.DeclarationsChecker;
import org.napile.compiler.lang.resolve.processors.DescriptorResolver;
import org.napile.compiler.lang.resolve.processors.QualifiedExpressionResolver;
import org.napile.compiler.lang.resolve.processors.TypeResolver;
import org.napile.compiler.lang.types.expressions.ExpressionTypingServices;
import com.intellij.openapi.project.Project;

/**
 * @author abreslav
 */
// NOTE: After making changes, you need to re-generate the injectors.
//       To do that, you can run either this class, or /build.xml/generateInjectors task
public class AllInjectorsGenerator
{

	public static void main(String[] args) throws IOException
	{
		generateInjectorForTopDownAnalyzerBasic();

		generateMacroInjector();

		generateInjectorForJvmCodegen();
		generateInjectorForLazyResolve();
		generateInjectorForBodyResolve();
	}

	private static void generateInjectorForLazyResolve() throws IOException
	{
		DependencyInjectorGenerator generator = new DependencyInjectorGenerator(false);
		generator.addParameter(Project.class);
		generator.addParameter(ResolveSession.class);
		generator.addParameter(BindingTrace.class);
		generator.addPublicField(DescriptorResolver.class);
		generator.addPublicField(ExpressionTypingServices.class);
		generator.addPublicField(TypeResolver.class);
		generator.addPublicField(ScopeProvider.class);
		generator.addPublicField(AnnotationResolver.class);
		generator.addPublicField(QualifiedExpressionResolver.class);
		generator.generate("compiler/frontend/src", "org.napile.compiler.di", "InjectorForLazyResolve");
	}

	private static void generateInjectorForTopDownAnalyzerBasic() throws IOException
	{
		DependencyInjectorGenerator generator = new DependencyInjectorGenerator(false);
		generateInjectorForTopDownAnalyzerCommon(generator);

		generator.addField(NamespaceFactoryImpl.class);
		generator.generate("compiler/frontend/src", "org.napile.compiler.di", "InjectorForTopDownAnalyzerBasic");
	}

	private static void generateInjectorForTopDownAnalyzerCommon(DependencyInjectorGenerator generator)
	{
		// Fields
		generator.addPublicField(TopDownAnalyzer.class);
		generator.addPublicField(TopDownAnalysisContext.class);
		generator.addPublicField(BodyResolver.class);
		generator.addPublicField(ControlFlowAnalyzer.class);
		generator.addPublicField(DeclarationsChecker.class);
		generator.addPublicField(DescriptorResolver.class);

		// Parameters
		generator.addPublicParameter(Project.class);
		generator.addPublicParameter(TopDownAnalysisParameters.class);
		generator.addPublicParameter(BindingTrace.class);
		generator.addParameter(ModuleDescriptor.class);
	}

	private static void generateMacroInjector() throws IOException
	{
		DependencyInjectorGenerator generator = new DependencyInjectorGenerator(false);

		// Fields
		generator.addPublicField(ExpressionTypingServices.class);

		// Parameters
		generator.addPublicParameter(Project.class);

		generator.generate("compiler/frontend/src", "org.napile.compiler.di", "InjectorForMacros");
	}


	private static void generateInjectorForJvmCodegen() throws IOException
	{
		DependencyInjectorGenerator generator = new DependencyInjectorGenerator(false);
		generator.addParameter(BindingTrace.class);
		generator.addParameter(DiType.listOf(NapileFile.class));
		generator.addParameter(Project.class);
		generator.addField(false, BindingContext.class, "bindingContext", new GivenExpression("bindingTrace.getBindingContext()"));
		generator.addPublicParameter(BindingTrace.class);
		generator.generate("compiler/backend/src", "org.napile.compiler.di", "InjectorForJvmCodegen");
	}

	private static void generateInjectorForBodyResolve() throws IOException
	{
		DependencyInjectorGenerator generator = new DependencyInjectorGenerator(false);
		// Fields
		generator.addPublicField(BodyResolver.class);

		// Parameters
		generator.addPublicParameter(Project.class);
		generator.addPublicParameter(TopDownAnalysisParameters.class);
		generator.addPublicParameter(BindingTrace.class);
		generator.generate("compiler/frontend/src", "org.napile.compiler.di", "InjectorForBodyResolve");
	}
}
