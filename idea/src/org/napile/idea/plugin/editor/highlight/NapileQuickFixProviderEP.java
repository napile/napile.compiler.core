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

package org.napile.idea.plugin.editor.highlight;

import org.napile.compiler.lang.diagnostics.Diagnostic;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.util.xmlb.annotations.Attribute;

/**
 * @author VISTALL
 * @since 22:31/26.02.13
 */
public class NapileQuickFixProviderEP
{
	public static final ExtensionPointName<NapileQuickFixProviderEP> EP_NAME = ExtensionPointName.create("org.napile.idea.lang.quickFix");

	@Attribute("diagnosticName")
	public String diagnosticName;

	@Attribute("implementationClass")
	public String implementationClass;

	private NapileQuickFixProvider implementationInstance;

	public NapileQuickFixProvider getImplementationInstance()
	{
		if(implementationInstance == null)
		{
			try
			{
				final Class<?> aClass = Class.forName(implementationClass);
				implementationInstance = (NapileQuickFixProvider) aClass.newInstance();
			}
			catch(Exception e)
			{
				throw new RuntimeException(e);
			}
		}

		return implementationInstance;
	}

	public static void callRegisterFor(Diagnostic diagnostic, HighlightInfo highlightInfo)
	{
		for(NapileQuickFixProviderEP ep : EP_NAME.getExtensions())
		{
			if(!ep.diagnosticName.equals(diagnostic.getFactory().getName()))
			{
				continue;
			}

			ep.getImplementationInstance().registerQuickFix(diagnostic, highlightInfo);
		}
	}
}
