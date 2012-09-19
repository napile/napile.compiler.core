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

package org.napile.compiler.lang.descriptors;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.MethodTypeConstructor;

/**
 * @author abreslav
 */
public abstract class VariableDescriptorImpl extends DeclarationDescriptorNonRootImpl implements VariableDescriptor
{
	private MethodDescriptor methodDescriptor;
	private JetType outType;
	protected final boolean isStatic;

	public VariableDescriptorImpl(@NotNull DeclarationDescriptor containingDeclaration, @NotNull List<AnnotationDescriptor> annotations, @NotNull Name name, @Nullable JetType outType, boolean isStatic)
	{
		super(containingDeclaration, annotations, name);

		if(outType != null)
			setOutType(outType);
		this.isStatic = isStatic;
	}

	protected VariableDescriptorImpl(@NotNull DeclarationDescriptor containingDeclaration, @NotNull List<AnnotationDescriptor> annotations, @NotNull Name name, boolean isStatic)
	{
		this(containingDeclaration, annotations, name, null, isStatic);
	}

	@Override
	public boolean isStatic()
	{
		return isStatic;
	}

	@NotNull
	@Override
	public JetType getType()
	{
		return outType;
	}

	public void setOutType(@NotNull JetType type)
	{
		assert this.outType == null;
		outType = type;
		methodDescriptor = type.getConstructor() instanceof MethodTypeConstructor ? FunctionDescriptorUtil.createDescriptorFromType(getName(), type, getContainingDeclaration()) : null;
	}

	@Override
	@NotNull
	public VariableDescriptor getOriginal()
	{
		return (VariableDescriptor) super.getOriginal();
	}

	@NotNull
	@Override
	public List<ParameterDescriptor> getValueParameters()
	{
		return Collections.emptyList();
	}

	@NotNull
	@Override
	public Set<? extends CallableDescriptor> getOverriddenDescriptors()
	{
		return Collections.emptySet();
	}

	@NotNull
	@Override
	public List<TypeParameterDescriptor> getTypeParameters()
	{
		return Collections.emptyList();
	}

	@NotNull
	@Override
	public ReceiverDescriptor getReceiverParameter()
	{
		return ReceiverDescriptor.NO_RECEIVER;
	}

	@NotNull
	@Override
	public ReceiverDescriptor getExpectedThisObject()
	{
		return ReceiverDescriptor.NO_RECEIVER;
	}

	@NotNull
	@Override
	public JetType getReturnType()
	{
		return getType();
	}

	@Nullable
	@Override
	public MethodDescriptor getCallableDescriptor()
	{
		return methodDescriptor;
	}
}
