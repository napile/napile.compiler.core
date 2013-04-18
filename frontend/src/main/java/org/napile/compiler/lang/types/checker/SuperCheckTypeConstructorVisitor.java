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

package org.napile.compiler.lang.types.checker;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.ClassifierDescriptor;
import org.napile.compiler.lang.descriptors.Modality;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.resolve.DescriptorUtils;
import org.napile.compiler.lang.types.NapileType;
import org.napile.compiler.lang.types.MethodTypeConstructor;
import org.napile.compiler.lang.types.MultiTypeConstructor;
import org.napile.compiler.lang.types.MultiTypeEntry;
import org.napile.compiler.lang.types.SelfTypeConstructor;
import org.napile.compiler.lang.types.TypeConstructor;
import org.napile.compiler.lang.types.TypeConstructorVisitor;

/**
 * @author VISTALL
 * @since 11:40/28.12.12
 */
public class SuperCheckTypeConstructorVisitor extends TypeConstructorVisitor<NapileType, Boolean>
{
	private final TypeCheckingProcedure typeCheckingProcedure;
	private final TypingConstraints constraints;

	public SuperCheckTypeConstructorVisitor(TypeCheckingProcedure typeCheckingProcedure, TypingConstraints typingConstraints)
	{
		this.typeCheckingProcedure = typeCheckingProcedure;
		this.constraints = typingConstraints;
	}

	@Override
	public Boolean visitType(NapileType subTemp, TypeConstructor t, NapileType superType)
	{
		return superType.accept(new TypeConstructorVisitor<NapileType, Boolean>()
		{
			@Override
			public Boolean visitSelfType(NapileType superType, SelfTypeConstructor superConstructor, NapileType subType)
			{
				ClassifierDescriptor subTypeClass = subType.getConstructor().getDeclarationDescriptor();
				//FIXME [VISTALL] correct?
				if(!(subTypeClass instanceof ClassDescriptor) || ((ClassDescriptor) subTypeClass).getModality() != Modality.FINAL)
					return false;

				return typeCheckingProcedure.equalTypes(superType.getConstructor().getDeclarationDescriptor().getDefaultType(), subType);
			}

			@Override
			public Boolean visitType(NapileType superType, TypeConstructor superConstructor, NapileType subType)
			{
				@Nullable NapileType closestSupertype = TypeCheckingProcedure.findCorrespondingSupertype(subType, superType);
				if(closestSupertype == null)
					return constraints.noCorrespondingSupertype(subType, superType); // if this returns true, there still isn't any supertype to continue with

				return checkSubtypeForTheSameConstructor(closestSupertype, superType);
			}
		}, subTemp);
	}

	@Override
	public Boolean visitSelfType(NapileType subType, SelfTypeConstructor subConstructor, NapileType superType)
	{
		return superType.accept(new TypeConstructorVisitor<NapileType, Boolean>()
		{
			@Override
			public Boolean visitMethodType(NapileType type, MethodTypeConstructor t, NapileType arg)
			{
				return false; // this cant be cast to method type
			}

			@Override
			public Boolean visitMultiType(NapileType type, MultiTypeConstructor t, NapileType arg)
			{
				return false; // this cant be cast to multi type
			}

			@Override
			public Boolean visitType(NapileType superType, TypeConstructor superTypeConstructor, NapileType subConstructor)
			{
				NapileType subUnwrapType = subConstructor.getConstructor().getDeclarationDescriptor().getDefaultType();

				return typeCheckingProcedure.isSubtypeOf(subUnwrapType, superType);
			}

			@Override
			public Boolean visitSelfType(NapileType superType, SelfTypeConstructor superTypeConstructor, NapileType subConstructor)
			{
				ClassDescriptor subDesc = (ClassDescriptor) subConstructor.getConstructor().getDeclarationDescriptor();
				ClassDescriptor supeDesc = superTypeConstructor.getDeclarationDescriptor();

				return DescriptorUtils.isSubclass(subDesc, supeDesc);
			}
		}, subType);
	}

	@Override
	public Boolean visitMethodType(NapileType subType, MethodTypeConstructor subConstructor, NapileType superType)
	{
		return superType.accept(new TypeConstructorVisitor<NapileType, Boolean>()
		{
			@Override
			public Boolean visitSelfType(NapileType type, SelfTypeConstructor t, NapileType arg)
			{
				return false;
			}

			@Override
			public Boolean visitMultiType(NapileType type, MultiTypeConstructor t, NapileType arg)
			{
				return false;
			}

			@Override
			public Boolean visitMethodType(NapileType superType, MethodTypeConstructor superTypeConstructor, NapileType subType)
			{
				MethodTypeConstructor multiTypeConstructor = (MethodTypeConstructor) subType.getConstructor();
				if(multiTypeConstructor.getParameterTypes().size() != superTypeConstructor.getParameterTypes().size())
					return false;

				Iterator<Map.Entry<Name, NapileType>> subIterator = multiTypeConstructor.getParameterTypes().entrySet().iterator();
				Iterator<Map.Entry<Name, NapileType>> superIterator = superTypeConstructor.getParameterTypes().entrySet().iterator();
				while(subIterator.hasNext())
				{
					Map.Entry<Name, NapileType> subEntry = subIterator.next();
					Map.Entry<Name, NapileType> superEntry = superIterator.next();
					if(!typeCheckingProcedure.isSubtypeOf(subEntry.getValue(), superEntry.getValue()))
						return false;
				}
				return typeCheckingProcedure.isSubtypeOf(multiTypeConstructor.getReturnType(), superTypeConstructor.getReturnType());
			}

			@Override
			public Boolean visitType(NapileType superType, TypeConstructor superTypeConstructor, NapileType subType)
			{
				MethodTypeConstructor multiTypeConstructor = (MethodTypeConstructor) subType.getConstructor();
				NapileType multiType = multiTypeConstructor.getSupertypes().isEmpty() ? null : multiTypeConstructor.getSupertypes().iterator().next();

				return multiType != null && typeCheckingProcedure.isSubtypeOf(multiType, superType);
			}
		}, subType);
	}

	@Override
	public Boolean visitMultiType(NapileType subType, MultiTypeConstructor subConstructor, NapileType superType)
	{
		return superType.accept(new TypeConstructorVisitor<NapileType, Boolean>()
		{
			@Override
			public Boolean visitSelfType(NapileType type, SelfTypeConstructor t, NapileType arg)
			{
				return false;
			}

			@Override
			public Boolean visitMethodType(NapileType type, MethodTypeConstructor t, NapileType arg)
			{
				return false;
			}

			@Override
			public Boolean visitMultiType(NapileType superType, MultiTypeConstructor superTypeConstructor, NapileType subType)
			{
				MultiTypeConstructor multiTypeConstructor = (MultiTypeConstructor) subType.getConstructor();
				if(superTypeConstructor.getEntries().size() != multiTypeConstructor.getEntries().size())
					return false;

				Iterator<MultiTypeEntry> subEntryIterator = multiTypeConstructor.getEntries().iterator();
				Iterator<MultiTypeEntry> superEntryIterator = superTypeConstructor.getEntries().iterator();
				while(subEntryIterator.hasNext())
				{
					MultiTypeEntry subEntry = subEntryIterator.next();
					MultiTypeEntry superEntry = superEntryIterator.next();

					if(subEntry.mutable != null && subEntry.mutable != superEntry.mutable)
						return false;

					if(!typeCheckingProcedure.isSubtypeOf(subEntry.type, superEntry.type))
						return false;
				}
				return true;
			}

			@Override
			public Boolean visitType(NapileType superType, TypeConstructor superTypeConstructor, NapileType subType)
			{
				MultiTypeConstructor multiTypeConstructor = (MultiTypeConstructor) subType.getConstructor();
				NapileType multiType = multiTypeConstructor.getSupertypes().isEmpty() ? null : multiTypeConstructor.getSupertypes().iterator().next();

				return multiType != null && typeCheckingProcedure.isSubtypeOf(multiType, superType);
			}
		}, subType);
	}

	private boolean checkSubtypeForTheSameConstructor(@NotNull NapileType subtype, @NotNull NapileType supertype)
	{
		TypeConstructor constructor = subtype.getConstructor();

		List<NapileType> subArguments = subtype.getArguments();
		List<NapileType> superArguments = supertype.getArguments();
		if(subArguments.size() != superArguments.size())
			return false;

		if(subArguments.isEmpty())
			return true;

		List<TypeParameterDescriptor> parameters = constructor.getParameters();
		for(int i = 0; i < parameters.size(); i++)
		{
			NapileType subArgument = subArguments.get(i);

			NapileType superArgument = superArguments.get(i);

			//if(superArgument.isNullable() && !subArgument.isNullable())
			//	return false;
			if(!typeCheckingProcedure.isSubtypeOf(subArgument, superArgument))
				return false;
		}
		return true;
	}
}
