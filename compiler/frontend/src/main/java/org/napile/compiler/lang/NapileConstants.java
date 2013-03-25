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

package org.napile.compiler.lang;

import org.napile.asm.resolve.name.Name;

/**
 * @author VISTALL
 * @since 16:52/25.03.13
 */
public interface NapileConstants
{
	Name VARIABLE_FIELD_NAME = Name.identifier("value");

	Name VARIABLE_SET_PARAMETER_NAME = Name.identifier("newValue");

	Name ANONYM_METHOD_SINGLE_PARAMETER_NAME = VARIABLE_FIELD_NAME;
}
