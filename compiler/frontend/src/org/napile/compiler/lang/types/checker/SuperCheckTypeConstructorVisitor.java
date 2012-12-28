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
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.MethodTypeConstructor;
import org.napile.compiler.lang.types.MultiTypeConstructor;
import org.napile.compiler.lang.types.MultiTypeEntry;
import org.napile.compiler.lang.types.SelfTypeConstructor;
import org.napile.compiler.lang.types.TypeConstructor;
import org.napile.compiler.lang.types.TypeConstructorVisitor;

/**
 * @author VISTALL
 * @date 11:40/28.12.12
 */
public class SuperCheckTypeConstructorVisitor extends TypeConstructorVisitor<JetType, Boolean>
{
	private final TypeCheckingProcedure typeCheckingProcedure;
	private final TypingConstraints constraints;

	public SuperCheckTypeConstructorVisitor(TypeCheckingProcedure typeCheckingProcedure, TypingConstraints typingConstraints)
	{
		this.typeCheckingProcedure = typeCheckingProcedure;
		this.constraints = typingConstraints;
	}

	@Override
	public Boolean visitType(JetType subTemp, TypeConstructor t, JetType superType)
	{
		return superType.accept(new TypeConstructorVisitor<JetType, Boolean>()
		{
			@Override
			public Boolean visitSelfType(JetType superType, SelfTypeConstructor superConstructor, JetType subType)
			{
				ClassifierDescriptor subTypeClass = subType.getConstructor().getDeclarationDescriptor();
				//FIXME [VISTALL] correct?
				if(!(subTypeClass instanceof ClassDescriptor) || ((ClassDescriptor) subTypeClass).getModality() != Modality.FINAL)
					return false;

				return typeCheckingProcedure.equalTypes(superType.getConstructor().getDeclarationDescriptor().getDefaultType(), subType);
			}

			@Override
			public Boolean visitType(JetType superType, TypeConstructor superConstructor, JetType subType)
			{
				@Nullable JetType closestSupertype = TypeCheckingProcedure.findCorrespondingSupertype(subType, superType);
				if(closestSupertype == null)
					return constraints.noCorrespondingSupertype(subType, superType); // if this returns true, there still isn't any supertype to continue with

				return checkSubtypeForTheSameConstructor(closestSupertype, superType);
			}
		}, subTemp);
	}

	@Override
	public Boolean visitSelfType(JetType subType, SelfTypeConstructor subConstructor, JetType superType)
	{
		return superType.accept(new TypeConstructorVisitor<JetType, Boolean>()
		{
			@Override
			public Boolean visitMethodType(JetType type, MethodTypeConstructor t, JetType arg)
			{
				return false; // this cant be cast to method type
			}

			@Override
			public Boolean visitMultiType(JetType type, MultiTypeConstructor t, JetType arg)
			{
				return false; // this cant be cast to multi type
			}

			@Override
			public Boolean visitType(JetType superType, TypeConstructor superTypeConstructor, JetType subConstructor)
			{
				JetType subUnwrapType = subConstructor.getConstructor().getDeclarationDescriptor().getDefaultType();

				return typeCheckingProcedure.isSubtypeOf(subUnwrapType, superType);
			}

			@Override
			public Boolean visitSelfType(JetType superType, SelfTypeConstructor superTypeConstructor, JetType subConstructor)
			{
				JetType subUnwrapType = subConstructor.getConstructor().getDeclarationDescriptor().getDefaultType();
				JetType superUnwrapType = superTypeConstructor.getDeclarationDescriptor().getDefaultType();

				return typeCheckingProcedure.isSubtypeOf(subUnwrapType, superUnwrapType);
			}
		}, subType);
	}

	@Override
	public Boolean visitMethodType(JetType subType, MethodTypeConstructor subConstructor, JetType superType)
	{
		return superType.accept(new TypeConstructorVisitor<JetType, Boolean>()
		{
			@Override
			public Boolean visitSelfType(JetType type, SelfTypeConstructor t, JetType arg)
			{
				return false;
			}

			@Override
			public Boolean visitMultiType(JetType type, MultiTypeConstructor t, JetType arg)
			{
				return false;
			}

			@Override
			public Boolean visitMethodType(JetType superType, MethodTypeConstructor superTypeConstructor, JetType subType)
			{
				MethodTypeConstructor multiTypeConstructor = (MethodTypeConstructor) subType.getConstructor();
				if(multiTypeConstructor.getParameterTypes().size() != superTypeConstructor.getParameterTypes().size())
					return false;

				Iterator<Map.Entry<Name, JetType>> subIterator = multiTypeConstructor.getParameterTypes().entrySet().iterator();
				Iterator<Map.Entry<Name, JetType>> superIterator = superTypeConstructor.getParameterTypes().entrySet().iterator();
				while(subIterator.hasNext())
				{
					Map.Entry<Name, JetType> subEntry = subIterator.next();
					Map.Entry<Name, JetType> superEntry = superIterator.next();
					if(!typeCheckingProcedure.isSubtypeOf(subEntry.getValue(), superEntry.getValue()))
						return false;
				}
				return typeCheckingProcedure.isSubtypeOf(multiTypeConstructor.getReturnType(), superTypeConstructor.getReturnType());
			}

			@Override
			public Boolean visitType(JetType superType, TypeConstructor superTypeConstructor, JetType subType)
			{
				MethodTypeConstructor multiTypeConstructor = (MethodTypeConstructor) subType.getConstructor();
				JetType multiType = multiTypeConstructor.getSupertypes().isEmpty() ? null : multiTypeConstructor.getSupertypes().iterator().next();

				return multiType != null && typeCheckingProcedure.isSubtypeOf(multiType, superType);
			}
		}, subType);
	}

	@Override
	public Boolean visitMultiType(JetType subType, MultiTypeConstructor subConstructor, JetType superType)
	{
		return superType.accept(new TypeConstructorVisitor<JetType, Boolean>()
		{
			@Override
			public Boolean visitSelfType(JetType type, SelfTypeConstructor t, JetType arg)
			{
				return false;
			}

			@Override
			public Boolean visitMethodType(JetType type, MethodTypeConstructor t, JetType arg)
			{
				return false;
			}

			@Override
			public Boolean visitMultiType(JetType superType, MultiTypeConstructor superTypeConstructor, JetType subType)
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
			public Boolean visitType(JetType superType, TypeConstructor superTypeConstructor, JetType subType)
			{
				MultiTypeConstructor multiTypeConstructor = (MultiTypeConstructor) subType.getConstructor();
				JetType multiType = multiTypeConstructor.getSupertypes().isEmpty() ? null : multiTypeConstructor.getSupertypes().iterator().next();

				return multiType != null && typeCheckingProcedure.isSubtypeOf(multiType, superType);
			}
		}, subType);
	}

	private boolean checkSubtypeForTheSameConstructor(@NotNull JetType subtype, @NotNull JetType supertype)
	{
		TypeConstructor constructor = subtype.getConstructor();
		assert constructor.equals(supertype.getConstructor()) : constructor + " is not " + supertype.getConstructor();

		List<JetType> subArguments = subtype.getArguments();
		List<JetType> superArguments = supertype.getArguments();
		List<TypeParameterDescriptor> parameters = constructor.getParameters();
		for(int i = 0; i < parameters.size(); i++)
		{
			JetType subArgument = subArguments.get(i);

			JetType superArgument = superArguments.get(i);

			if(superArgument.isNullable() && !subArgument.isNullable())
				return false;
			if(!typeCheckingProcedure.isSubtypeOf(subArgument, superArgument))
				return false;
		}
		return true;
	}
}
