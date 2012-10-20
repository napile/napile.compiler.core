/*
 * Copyright 2010-2012 napile.org
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

package org.napile.compiler.lang.psi;

import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @date 7:55/20.10.12
 */
public interface NapileUserType extends NapileTypeElement
{
	boolean isAbsoluteInRootNamespace();

	@Nullable
	NapileTypeArgumentList getTypeArgumentList();

	@Nullable
	@IfNotParsed
	NapileSimpleNameExpression getReferenceExpression();

	@Nullable
	NapileUserType getQualifier();

	@Nullable
	String getReferencedName();
}
