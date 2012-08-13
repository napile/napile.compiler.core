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

import org.jetbrains.jet.lang.BuiltinsScopeExtensionMode;
import org.jetbrains.jet.lang.DefaultModuleConfiguration;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.AnnotationResolver;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.BodyResolver;
import org.jetbrains.jet.lang.resolve.ControlFlowAnalyzer;
import org.jetbrains.jet.lang.resolve.DeclarationsChecker;
import org.jetbrains.jet.lang.resolve.DescriptorResolver;
import org.jetbrains.jet.lang.resolve.NamespaceFactoryImpl;
import org.jetbrains.jet.lang.resolve.QualifiedExpressionResolver;
import org.jetbrains.jet.lang.resolve.TopDownAnalysisContext;
import org.jetbrains.jet.lang.resolve.TopDownAnalysisParameters;
import org.jetbrains.jet.lang.resolve.TopDownAnalyzer;
import org.jetbrains.jet.lang.resolve.TypeResolver;
import org.jetbrains.jet.lang.resolve.calls.CallResolver;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import org.jetbrains.jet.lang.resolve.lazy.ScopeProvider;
import org.jetbrains.jet.lang.types.DependencyClassByQualifiedNameResolverDummyImpl;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingServices;
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

		generateInjectorForJavaDescriptorResolver();

		generateMacroInjector();
		generateTestInjector();
		generateInjectorForJavaSemanticServices();
		generateInjectorForJvmCodegen();
		generateInjectorForJetTypeMapper();
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
		generator.generate("compiler/frontend/src", "org.jetbrains.jet.di", "InjectorForLazyResolve");
	}

	private static void generateInjectorForTopDownAnalyzerBasic() throws IOException
	{
		DependencyInjectorGenerator generator = new DependencyInjectorGenerator(false);
		generateInjectorForTopDownAnalyzerCommon(generator);
		generator.addParameter(DefaultModuleConfiguration.class);
		generator.addParameter(BuiltinsScopeExtensionMode.class);
		generator.addField(DependencyClassByQualifiedNameResolverDummyImpl.class);
		generator.addField(NamespaceFactoryImpl.class);
		generator.generate("compiler/frontend/src", "org.jetbrains.jet.di", "InjectorForTopDownAnalyzerBasic");
	}


	private static void generateInjectorForJavaDescriptorResolver() throws IOException
	{
		DependencyInjectorGenerator generator = new DependencyInjectorGenerator(false);

		// Parameters
		generator.addPublicParameter(Project.class);
		generator.addPublicParameter(BindingTrace.class);
		generator.addPublicParameter(ModuleDescriptor.class);
		generator.addParameter(BuiltinsScopeExtensionMode.class);

		generator.generate("compiler/frontend.java/src", "org.jetbrains.jet.di", "InjectorForJavaDescriptorResolver");
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

		generator.generate("compiler/frontend/src", "org.jetbrains.jet.di", "InjectorForMacros");
	}

	private static void generateTestInjector() throws IOException
	{
		DependencyInjectorGenerator generator = new DependencyInjectorGenerator(false);

		// Fields
		generator.addPublicField(DescriptorResolver.class);
		generator.addPublicField(ExpressionTypingServices.class);
		generator.addPublicField(TypeResolver.class);
		generator.addPublicField(CallResolver.class);

		// Parameters
		generator.addPublicParameter(Project.class);

		generator.generate("compiler/tests", "org.jetbrains.jet.di", "InjectorForTests");
	}

	private static void generateInjectorForJavaSemanticServices() throws IOException
	{
		DependencyInjectorGenerator generator = new DependencyInjectorGenerator(false);

		// Fields
		generator.addField(true, BindingTrace.class, null, new GivenExpression("new org.jetbrains.jet.lang.resolve.BindingTraceContext()"));
		generator.addField(false, ModuleDescriptor.class, null, new GivenExpression("new org.jetbrains.jet.lang.descriptors.ModuleDescriptor(" + "org.jetbrains.jet.lang.resolve.name.Name.special(\"<dummy>\"))"));
		generator.addParameter(BuiltinsScopeExtensionMode.class);

		// Parameters
		generator.addPublicParameter(Project.class);

		generator.generate("compiler/frontend.java/src", "org.jetbrains.jet.di", "InjectorForJavaSemanticServices");
	}

	private static void generateInjectorForJvmCodegen() throws IOException
	{
		DependencyInjectorGenerator generator = new DependencyInjectorGenerator(false);
		generator.addParameter(BindingContext.class);
		generator.addParameter(DiType.listOf(JetFile.class));
		generator.addParameter(Project.class);
		generator.generate("compiler/backend/src", "org.jetbrains.jet.di", "InjectorForJvmCodegen");
	}

	private static void generateInjectorForJetTypeMapper() throws IOException
	{
		DependencyInjectorGenerator generator = new DependencyInjectorGenerator(false);
		generator.addParameter(BindingContext.class);
		generator.addParameter(DiType.listOf(JetFile.class));
		generator.generate("compiler/backend/src", "org.jetbrains.jet.di", "InjectorForJetTypeMapper");
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
		generator.generate("compiler/frontend/src", "org.jetbrains.jet.di", "InjectorForBodyResolve");
	}
}
