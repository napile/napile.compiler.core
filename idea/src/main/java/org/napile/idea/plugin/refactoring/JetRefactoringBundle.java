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

package org.napile.idea.plugin.refactoring;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;
import com.intellij.AbstractBundle;

/**
 * User: Alefas
 * Date: 25.01.12
 */
public class JetRefactoringBundle extends AbstractBundle
{
	private static final JetRefactoringBundle INSTANCE = new JetRefactoringBundle();

	@NonNls
	private static final String BUNDLE = "org.napile.idea.plugin.refactoring.JetRefactoringBundle";

	private JetRefactoringBundle()
	{
		super(BUNDLE);
	}

	public static String message(@NonNls @PropertyKey(resourceBundle = BUNDLE) String key, Object... params)
	{
		return INSTANCE.getMessage(key, params);
	}
}
