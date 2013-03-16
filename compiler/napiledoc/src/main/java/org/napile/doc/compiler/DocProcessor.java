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

package org.napile.doc.compiler;

import java.io.File;

import org.napile.compiler.analyzer.AnalyzeExhaust;
import org.napile.compiler.common.AnalyzeProcessor;
import org.napile.compiler.common.CompilerConfigurationKeys;
import org.napile.compiler.common.JetCoreEnvironment;
import org.napile.compiler.common.messages.MessageCollector;
import org.napile.compiler.config.CompilerConfiguration;
import org.napile.doc.compiler.generator.AllClassesFrameGenerator;
import org.napile.doc.compiler.generator.ClassSummaryGenerator;
import org.napile.doc.compiler.generator.DocGenerator;
import org.napile.doc.compiler.generator.IndexGenerator;
import org.napile.doc.compiler.generator.OverviewFrameGenerator;
import org.napile.doc.compiler.generator.OverviewSummaryGenerator;
import org.napile.doc.compiler.generator.PackageFrameGenerator;
import org.napile.doc.compiler.generator.StylesheetGenerator;
import org.napile.doc.compiler.info.AllInfo;
import com.intellij.openapi.util.Disposer;

/**
 * @author VISTALL
 * @since 12:32/01.02.13
 */
public class DocProcessor
{
	private static final DocGenerator[] GENERATORS = new DocGenerator[]
	{
		new StylesheetGenerator(),
		new IndexGenerator(),
		new AllClassesFrameGenerator(),
		new OverviewSummaryGenerator(),
		new OverviewFrameGenerator(),
		new PackageFrameGenerator(),
		new ClassSummaryGenerator()
	};

	public static void generate(File dir, File outDir)
	{
		CompilerConfiguration configuration = new CompilerConfiguration();

		configuration.put(CompilerConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.PLAIN_TEXT_TO_SYSTEM_ERR);

		JetCoreEnvironment environment = new JetCoreEnvironment(Disposer.newDisposable(), configuration);
		environment.addSources(dir);

		AnalyzeExhaust analyzeExhaust = AnalyzeProcessor.analyze(environment);
		if(analyzeExhaust == null)
		{
			System.out.println("fail");
			return;
		}

		AllInfo allInfo = new AllInfo(environment, analyzeExhaust);

		for(DocGenerator docGenerator : GENERATORS)
			docGenerator.generate(allInfo, outDir);
	}
}
