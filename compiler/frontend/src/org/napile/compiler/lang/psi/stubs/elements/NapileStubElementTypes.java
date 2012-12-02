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

package org.napile.compiler.lang.psi.stubs.elements;

/**
 * @author Nikolay Krasko
 */
public interface NapileStubElementTypes
{
	NapileFileElementType FILE = new NapileFileElementType();

	NapileClassElementType CLASS = new NapileClassElementType("CLASS");
	NapileMethodElementType METHOD = new NapileMethodElementType("METHOD");
	NapileMacroElementType MACRO = new NapileMacroElementType("MACRO");
	NapileVariableElementType VARIABLE = new NapileVariableElementType("PROPERTY");

	NapileParameterElementType CALL_PARAMETER_AS_VARIABLE = new NapileParameterElementType("CALL_PARAMETER_AS_VARIABLE");

	NapileParameterListElementType CALL_PARAMETER_LIST = new NapileParameterListElementType("CALL_PARAMETER_LIST");

	NapileTypeParameterElementType TYPE_PARAMETER = new NapileTypeParameterElementType("TYPE_PARAMETER");
	NapileTypeParameterListElementType TYPE_PARAMETER_LIST = new NapileTypeParameterListElementType("TYPE_PARAMETER_LIST");
}
