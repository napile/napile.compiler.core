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

package org.napile.compiler.common;

import java.io.File;
import java.util.List;

import org.napile.compiler.common.messages.MessageCollector;
import org.napile.compiler.config.CompilerConfigurationKey;

/**
 * @author Evgeny Gerashchenko
 * @since 7/23/12
 */
public interface CompilerConfigurationKeys
{
	CompilerConfigurationKey<MessageCollector> MESSAGE_COLLECTOR_KEY = CompilerConfigurationKey.create("message collector");

	CompilerConfigurationKey<List<String>> SOURCE_ROOTS_KEY = CompilerConfigurationKey.create("source roots");

	CompilerConfigurationKey<List<File>> CLASSPATH_KEY = CompilerConfigurationKey.create("classpath");
}
