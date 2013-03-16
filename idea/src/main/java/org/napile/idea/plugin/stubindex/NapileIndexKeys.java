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

package org.napile.idea.plugin.stubindex;

import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileNamedMethodOrMacro;
import org.napile.compiler.lang.psi.NapileVariable;
import com.intellij.psi.stubs.StubIndexKey;

/**
 * @author Nikolay Krasko
 */
public interface NapileIndexKeys
{
	StubIndexKey<String, NapileClass> FQN_KEY = StubIndexKey.createIndexKey("napile.fqn");

	StubIndexKey<String, NapileClass> CLASSES_SHORT_NAME_KEY = StubIndexKey.createIndexKey("napile.classes.short.name");

	StubIndexKey<String, NapileNamedMethodOrMacro> METHODS_SHORT_NAME_KEY = StubIndexKey.createIndexKey("napile.methods.short.name");

	StubIndexKey<String, NapileNamedMethodOrMacro> MACROS_SHORT_NAME_KEY = StubIndexKey.createIndexKey("napile.macros.short.name");

	StubIndexKey<String, NapileVariable> VARIABLES_SHORT_NAME_KEY = StubIndexKey.createIndexKey("napile.variables.short.name");
}

