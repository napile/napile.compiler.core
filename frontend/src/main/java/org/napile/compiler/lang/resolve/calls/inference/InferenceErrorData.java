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

package org.napile.compiler.lang.resolve.calls.inference;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.types.NapileType;
import org.napile.compiler.lang.types.TypeUtils;

/**
 * @author svtk
 */
public class InferenceErrorData
{
	public final CallableDescriptor descriptor;
	public final ConstraintSystem constraintSystem;
	public final NapileType expectedType;
	public final List<NapileType> valueArgumentsTypes;

	private InferenceErrorData(@NotNull CallableDescriptor descriptor, @NotNull ConstraintSystem constraintSystem, @Nullable List<NapileType> valueArgumentsTypes, @Nullable NapileType expectedType)
	{
		this.descriptor = descriptor;
		this.constraintSystem = constraintSystem;
		this.valueArgumentsTypes = valueArgumentsTypes;
		this.expectedType = expectedType;
	}

	public static InferenceErrorData create(@NotNull CallableDescriptor descriptor, @NotNull ConstraintSystem constraintSystem, @NotNull List<NapileType> valueArgumentsTypes, @Nullable NapileType expectedType)
	{
		return new InferenceErrorData(descriptor, constraintSystem, valueArgumentsTypes, expectedType != TypeUtils.NO_EXPECTED_TYPE ? expectedType : null);
	}

	public static InferenceErrorData create(@NotNull CallableDescriptor descriptor, @NotNull ConstraintSystem constraintSystem)
	{
		return new InferenceErrorData(descriptor, constraintSystem, null, null);
	}
}
