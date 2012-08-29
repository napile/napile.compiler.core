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

import static org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor.NO_RECEIVER;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.resolve.OverridingUtil;
import org.napile.compiler.lang.resolve.name.Name;
import org.napile.compiler.lang.resolve.scopes.receivers.ExtensionReceiver;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.lang.resolve.scopes.receivers.TransientReceiver;
import org.napile.compiler.lang.types.DescriptorSubstitutor;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.TypeSubstitutor;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author abreslav
 */
public abstract class MethodDescriptorImpl extends DeclarationDescriptorNonRootImpl implements MethodDescriptor
{

	protected List<TypeParameterDescriptor> typeParameters;
	protected List<ValueParameterDescriptor> unsubstitutedValueParameters;
	protected JetType unsubstitutedReturnType;
	private ReceiverDescriptor receiverParameter;
	protected ReceiverDescriptor expectedThisObject;

	protected Modality modality;
	protected Visibility visibility;
	private final boolean isStatic;
	protected final Set<MethodDescriptor> overriddenMethods = Sets.newLinkedHashSet(); // LinkedHashSet is essential here
	private final MethodDescriptor original;
	private final Kind kind;

	protected MethodDescriptorImpl(@NotNull DeclarationDescriptor containingDeclaration, @NotNull List<AnnotationDescriptor> annotations, @NotNull Name name, Kind kind, boolean isStatic)
	{
		super(containingDeclaration, annotations, name);
		this.original = this;
		this.kind = kind;
		this.isStatic = isStatic;
	}

	protected MethodDescriptorImpl(@NotNull DeclarationDescriptor containingDeclaration, @NotNull MethodDescriptor original, @NotNull List<AnnotationDescriptor> annotations, @NotNull Name name, Kind kind, boolean isStatic)
	{
		super(containingDeclaration, annotations, name);
		this.original = original;
		this.kind = kind;
		this.isStatic = isStatic;
	}

	public MethodDescriptorImpl initialize(@Nullable JetType receiverParameterType, @NotNull ReceiverDescriptor expectedThisObject, @NotNull List<? extends TypeParameterDescriptor> typeParameters, @NotNull List<ValueParameterDescriptor> unsubstitutedValueParameters, @Nullable JetType unsubstitutedReturnType, @Nullable Modality modality, @NotNull Visibility visibility)
	{
		this.typeParameters = Lists.newArrayList(typeParameters);
		this.unsubstitutedValueParameters = unsubstitutedValueParameters;
		this.unsubstitutedReturnType = unsubstitutedReturnType;
		this.modality = modality;
		this.visibility = visibility;
		this.receiverParameter = receiverParameterType == null ? NO_RECEIVER : new ExtensionReceiver(this, receiverParameterType);
		this.expectedThisObject = expectedThisObject;

		for(int i = 0; i < typeParameters.size(); ++i)
		{
			TypeParameterDescriptor typeParameterDescriptor = typeParameters.get(i);
			if(typeParameterDescriptor.getIndex() != i)
			{
				throw new IllegalStateException(typeParameterDescriptor + " index is " + typeParameterDescriptor.getIndex() + " but position is " + i);
			}
		}

		for(int i = 0; i < unsubstitutedValueParameters.size(); ++i)
		{
			// TODO fill me
			int firstValueParameterOffset = 0; // receiverParameter.exists() ? 1 : 0;
			ValueParameterDescriptor valueParameterDescriptor = unsubstitutedValueParameters.get(i);
			if(valueParameterDescriptor.getIndex() != i + firstValueParameterOffset)
			{
				throw new IllegalStateException(valueParameterDescriptor + "index is " + valueParameterDescriptor.getIndex() + " but position is " + i);
			}
		}

		return this;
	}

	public void setVisibility(@NotNull Visibility visibility)
	{
		this.visibility = visibility;
	}

	public void setReturnType(@NotNull JetType unsubstitutedReturnType)
	{
		if(this.unsubstitutedReturnType != null)
		{
			// TODO: uncomment and fix tests
			//throw new IllegalStateException("returnType already set");
		}
		this.unsubstitutedReturnType = unsubstitutedReturnType;
	}

	@NotNull
	@Override
	public ReceiverDescriptor getReceiverParameter()
	{
		return receiverParameter;
	}

	@NotNull
	@Override
	public ReceiverDescriptor getExpectedThisObject()
	{
		return expectedThisObject;
	}

	@NotNull
	@Override
	public Set<? extends MethodDescriptor> getOverriddenDescriptors()
	{
		return overriddenMethods;
	}

	@NotNull
	@Override
	public Modality getModality()
	{
		return modality;
	}

	@NotNull
	@Override
	public Visibility getVisibility()
	{
		return visibility;
	}

	@Override
	public boolean isStatic()
	{
		return isStatic;
	}

	@Override
	public void addOverriddenDescriptor(@NotNull CallableMemberDescriptor overriddenFunction)
	{
		overriddenMethods.add((MethodDescriptor) overriddenFunction);
	}

	@Override
	@NotNull
	public List<TypeParameterDescriptor> getTypeParameters()
	{
		return typeParameters;
	}

	@Override
	@NotNull
	public List<ValueParameterDescriptor> getValueParameters()
	{
		return unsubstitutedValueParameters;
	}

	@Override
	public JetType getReturnType()
	{
		return unsubstitutedReturnType;
	}

	@NotNull
	@Override
	public MethodDescriptor getOriginal()
	{
		return original == this ? this : original.getOriginal();
	}

	@Override
	public Kind getKind()
	{
		return kind;
	}

	@Override
	public final MethodDescriptor substitute(TypeSubstitutor originalSubstitutor)
	{
		if(originalSubstitutor.isEmpty())
		{
			return this;
		}
		return doSubstitute(originalSubstitutor, getContainingDeclaration(), modality, visibility, true, true, getKind());
	}

	protected MethodDescriptor doSubstitute(TypeSubstitutor originalSubstitutor, DeclarationDescriptor newOwner, Modality newModality, Visibility newVisibility, boolean preserveOriginal, boolean copyOverrides, Kind kind)
	{
		MethodDescriptorImpl substitutedDescriptor = createSubstitutedCopy(newOwner, preserveOriginal, kind);

		List<TypeParameterDescriptor> substitutedTypeParameters = Lists.newArrayList();
		TypeSubstitutor substitutor = DescriptorSubstitutor.substituteTypeParameters(getTypeParameters(), originalSubstitutor, substitutedDescriptor, substitutedTypeParameters);

		JetType substitutedReceiverParameterType = null;
		if(receiverParameter.exists())
		{
			substitutedReceiverParameterType = substitutor.substitute(getReceiverParameter().getType());
			if(substitutedReceiverParameterType == null)
			{
				return null;
			}
		}

		ReceiverDescriptor substitutedExpectedThis = NO_RECEIVER;
		if(expectedThisObject.exists())
		{
			JetType substitutedType = substitutor.substitute(expectedThisObject.getType());
			if(substitutedType == null)
			{
				return null;
			}
			substitutedExpectedThis = new TransientReceiver(substitutedType);
		}

		List<ValueParameterDescriptor> substitutedValueParameters = FunctionDescriptorUtil.getSubstitutedValueParameters(substitutedDescriptor, this, substitutor);
		if(substitutedValueParameters == null)
		{
			return null;
		}

		JetType substitutedReturnType = FunctionDescriptorUtil.getSubstitutedReturnType(this, substitutor);
		if(substitutedReturnType == null)
		{
			return null;
		}

		substitutedDescriptor.initialize(substitutedReceiverParameterType, substitutedExpectedThis, substitutedTypeParameters, substitutedValueParameters, substitutedReturnType, newModality, newVisibility);
		if(copyOverrides)
		{
			for(MethodDescriptor overriddenMethod : overriddenMethods)
			{
				OverridingUtil.bindOverride(substitutedDescriptor, overriddenMethod.substitute(substitutor));
			}
		}
		return substitutedDescriptor;
	}

	protected abstract MethodDescriptorImpl createSubstitutedCopy(DeclarationDescriptor newOwner, boolean preserveOriginal, Kind kind);

	@Override
	public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data)
	{
		return visitor.visitFunctionDescriptor(this, data);
	}
}
