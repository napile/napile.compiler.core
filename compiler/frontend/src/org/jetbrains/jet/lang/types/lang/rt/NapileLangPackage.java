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

package org.jetbrains.jet.lang.types.lang.rt;

import org.jetbrains.jet.lang.resolve.name.Name;

/**
 * @author VISTALL
 * @date 20:38/04.08.12
 */
public interface NapileLangPackage
{
	TypedFqName PACKAGE = new TypedFqName("napile.lang");

	TypedFqName ANY = PACKAGE.child(Name.identifier("Any"));

	TypedFqName NUMBER = PACKAGE.child(Name.identifier("Number"));

	TypedFqName BOOL = PACKAGE.child(Name.identifier("Bool"));

	TypedFqName BYTE = PACKAGE.child(Name.identifier("Byte"));

	TypedFqName SHORT = PACKAGE.child(Name.identifier("Short"));

	TypedFqName INT = PACKAGE.child(Name.identifier("Int"));

	TypedFqName LONG = PACKAGE.child(Name.identifier("Long"));

	TypedFqName FLOAT = PACKAGE.child(Name.identifier("Float"));

	TypedFqName DOUBLE = PACKAGE.child(Name.identifier("Double"));

	TypedFqName CHAR = PACKAGE.child(Name.identifier("Char"));

	TypedFqName NULL = PACKAGE.child(Name.identifier("Null"));

	TypedFqName STRING = PACKAGE.child(Name.identifier("String"));

	TypedFqName THROWABLE = PACKAGE.child(Name.identifier("Throwable"));
}
