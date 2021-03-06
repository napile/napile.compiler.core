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

package org.napile.compiler.codegen.processors;

import org.napile.asm.resolve.name.FqName;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.psi.NapileDeclaration;
import org.napile.compiler.util.slicedmap.BasicWritableSlice;
import org.napile.compiler.util.slicedmap.Slices;
import org.napile.compiler.util.slicedmap.WritableSlice;

/**
 * @author VISTALL
 * @since 20:15/03.09.12
 */
public interface BindingTraceKeys2
{
	WritableSlice<NapileDeclaration, FqName> DECLARATION_TO_FQ_NAME = Slices.createSimpleSlice();

	WritableSlice<DeclarationDescriptor, FqName> DESCRIPTOR_TO_FQ_NAME = Slices.createSimpleSlice();

	@SuppressWarnings("UnusedDeclaration")
	@Deprecated
	// This field is needed only for the side effects of its initializer
	Void _static_initializer = BasicWritableSlice.initSliceDebugNames(BindingTraceKeys2.class);
}
