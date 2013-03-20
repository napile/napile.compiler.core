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

import java.util.Collection;

import org.napile.compiler.lang.diagnostics.Diagnostic;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.util.containers.MultiMap;
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

	private static MultiMap<String, NapileQuickFixProvider> QUICK_FIX_PROVIDER_CACHE;

	public NapileQuickFixProvider createInstance()
	{

		try
		{
			final Class<?> aClass = Class.forName(implementationClass);
			return (NapileQuickFixProvider) aClass.newInstance();
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	public static void collectionQuickActions(Editor editor,Diagnostic diagnostic, HighlightInfo highlightInfo, MultiMap<HighlightInfo, IntentionAction> map)
	{
		if(QUICK_FIX_PROVIDER_CACHE == null)
		{
			QUICK_FIX_PROVIDER_CACHE = initCache();
		}

		final Collection<NapileQuickFixProvider> providers = QUICK_FIX_PROVIDER_CACHE.get(diagnostic.getFactory().getName());
		if(providers.isEmpty())
		{
			return;
		}

		for(NapileQuickFixProvider ep : providers)
		{
			final IntentionAction quickFix = ep.createQuickFix(diagnostic, editor, highlightInfo);
			if(quickFix != null)
			{
				map.putValue(highlightInfo, quickFix);
			}
		}
	}

	private static MultiMap<String, NapileQuickFixProvider> initCache()
	{
		MultiMap<String, NapileQuickFixProvider> map = new MultiMap<String, NapileQuickFixProvider>();

		for(NapileQuickFixProviderEP ep : EP_NAME.getExtensions())
		{
			map.putValue(ep.diagnosticName, ep.createInstance());
		}

		return map;
	}
}
