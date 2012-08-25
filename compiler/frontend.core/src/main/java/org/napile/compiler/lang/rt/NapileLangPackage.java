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

package org.napile.compiler.lang.rt;

import org.napile.compiler.lang.resolve.name.FqName;
import org.napile.compiler.lang.resolve.name.Name;

/**
 * @author VISTALL
 * @date 20:38/04.08.12
 */
public interface NapileLangPackage
{
	FqName PACKAGE = new FqName("napile.lang");

	FqName ANY = PACKAGE.child(Name.identifier("Any"));

	FqName NUMBER = PACKAGE.child(Name.identifier("Number"));

	FqName BOOL = PACKAGE.child(Name.identifier("Bool"));

	FqName BYTE = PACKAGE.child(Name.identifier("Byte"));

	FqName SHORT = PACKAGE.child(Name.identifier("Short"));

	FqName INT = PACKAGE.child(Name.identifier("Int"));

	FqName LONG = PACKAGE.child(Name.identifier("Long"));

	FqName FLOAT = PACKAGE.child(Name.identifier("Float"));

	FqName DOUBLE = PACKAGE.child(Name.identifier("Double"));

	FqName CHAR = PACKAGE.child(Name.identifier("Char"));

	FqName NULL = PACKAGE.child(Name.identifier("Null"));

	FqName STRING = PACKAGE.child(Name.identifier("String"));

	FqName THROWABLE = PACKAGE.child(Name.identifier("Throwable"));
}
