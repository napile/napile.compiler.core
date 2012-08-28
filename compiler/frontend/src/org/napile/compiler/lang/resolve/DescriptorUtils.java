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

package org.napile.compiler.lang.resolve;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.resolve.name.FqName;
import org.napile.compiler.lang.resolve.name.FqNameUnsafe;
import org.napile.compiler.lang.resolve.name.Name;
import org.napile.compiler.lang.descriptors.*;
import org.napile.compiler.lang.psi.NapileElement;
import org.napile.compiler.lang.psi.NapileMethod;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.lang.types.ErrorUtils;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.TypeConstructor;
import org.napile.compiler.lang.types.TypeSubstitution;
import org.napile.compiler.lang.types.TypeSubstitutor;
import org.napile.compiler.lang.types.TypeUtils;
import org.napile.compiler.lang.types.lang.JetStandardClasses;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * @author abreslav
 */
public class DescriptorUtils
{

	@NotNull
	public static <D extends CallableDescriptor> D substituteBounds(@NotNull D functionDescriptor)
	{
		final List<TypeParameterDescriptor> typeParameters = functionDescriptor.getTypeParameters();
		if(typeParameters.isEmpty())
			return functionDescriptor;
		final Map<TypeConstructor, TypeParameterDescriptor> typeConstructors = Maps.newHashMap();
		for(TypeParameterDescriptor typeParameter : typeParameters)
		{
			typeConstructors.put(typeParameter.getTypeConstructor(), typeParameter);
		}
		//noinspection unchecked
		return (D) functionDescriptor.substitute(new TypeSubstitutor(TypeSubstitution.EMPTY)
		{
			@Override
			public boolean inRange(@NotNull TypeConstructor typeConstructor)
			{
				return typeConstructors.containsKey(typeConstructor);
			}

			@Override
			public boolean isEmpty()
			{
				return typeParameters.isEmpty();
			}

			@NotNull
			@Override
			public TypeSubstitution getSubstitution()
			{
				throw new UnsupportedOperationException();
			}

			@NotNull
			@Override
			public JetType safeSubstitute(@NotNull JetType type)
			{
				JetType substituted = substitute(type);
				if(substituted == null)
				{
					return ErrorUtils.createErrorType("Substitution failed");
				}
				return substituted;
			}

			@Nullable
			@Override
			public JetType substitute(@NotNull JetType type)
			{
				return super.substitute(type);
			}
		});
	}

	public static Modality convertModality(Modality modality, boolean makeNonAbstract)
	{
		if(makeNonAbstract && modality == Modality.ABSTRACT)
			return Modality.OPEN;
		return modality;
	}

	@NotNull
	public static ReceiverDescriptor getExpectedThisObjectIfNeeded(@NotNull DeclarationDescriptor containingDeclaration)
	{
		if(containingDeclaration instanceof ClassDescriptor)
		{
			ClassDescriptor classDescriptor = (ClassDescriptor) containingDeclaration;
			return classDescriptor.getImplicitReceiver();
		}

		return ReceiverDescriptor.NO_RECEIVER;
	}

	/**
	 * The primary case for local extensions is the following:
	 * <p/>
	 * I had a locally declared extension function or a local variable of function type called foo
	 * And I called it on my x
	 * Now, someone added function foo() to the class of x
	 * My code should not change
	 * <p/>
	 * thus
	 * <p/>
	 * local extension prevail over members (and members prevail over all non-local extensions)
	 */
	public static boolean isLocal(DeclarationDescriptor containerOfTheCurrentLocality, DeclarationDescriptor candidate)
	{
		if(candidate instanceof ValueParameterDescriptor)
		{
			return true;
		}
		DeclarationDescriptor parent = candidate.getContainingDeclaration();
		if(!(parent instanceof FunctionDescriptor))
		{
			return false;
		}
		FunctionDescriptor functionDescriptor = (FunctionDescriptor) parent;
		DeclarationDescriptor current = containerOfTheCurrentLocality;
		while(current != null)
		{
			if(current == functionDescriptor)
			{
				return true;
			}
			current = current.getContainingDeclaration();
		}
		return false;
	}

	@NotNull
	public static FqNameUnsafe getFQName(@NotNull DeclarationDescriptor descriptor)
	{
		DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();

		if(descriptor instanceof ModuleDescriptor || containingDeclaration instanceof ModuleDescriptor)
		{
			return FqName.ROOT.toUnsafe();
		}

		if(containingDeclaration == null)
		{
			if(descriptor instanceof NamespaceDescriptor)
			{
				// TODO: namespace must always have parent
				if(descriptor.getName().equals(Name.identifier("idea")))
				{
					return FqNameUnsafe.topLevel(Name.identifier("idea"));
				}
				if(descriptor.getName().equals(Name.special("<java_root>")))
				{
					return FqName.ROOT.toUnsafe();
				}
			}
			throw new IllegalStateException("descriptor is not module descriptor and has null containingDeclaration: " + containingDeclaration);
		}

		return getFQName(containingDeclaration).child(descriptor.getName());
	}

	public static boolean isTopLevelFunction(@NotNull SimpleFunctionDescriptor functionDescriptor)
	{
		return functionDescriptor.getContainingDeclaration() instanceof NamespaceDescriptor;
	}

	@Nullable
	public static <D extends DeclarationDescriptor> D getParentOfType(@Nullable DeclarationDescriptor descriptor, @NotNull Class<D> aClass)
	{
		return getParentOfType(descriptor, aClass, true);
	}

	@Nullable
	public static <D extends DeclarationDescriptor> D getParentOfType(@Nullable DeclarationDescriptor descriptor, @NotNull Class<D> aClass, boolean strict)
	{
		if(descriptor == null)
			return null;
		if(strict)
		{
			descriptor = descriptor.getContainingDeclaration();
		}
		while(descriptor != null)
		{
			if(aClass.isInstance(descriptor))
			{
				//noinspection unchecked
				return (D) descriptor;
			}
			descriptor = descriptor.getContainingDeclaration();
		}
		return null;
	}

	public static boolean isAncestor(@Nullable DeclarationDescriptor ancestor, @NotNull DeclarationDescriptor declarationDescriptor, boolean strict)
	{
		if(ancestor == null)
			return false;
		DeclarationDescriptor descriptor = strict ? declarationDescriptor.getContainingDeclaration() : declarationDescriptor;
		while(descriptor != null)
		{
			if(ancestor == descriptor)
				return true;
			descriptor = descriptor.getContainingDeclaration();
		}
		return false;
	}

	@Nullable
	public static VariableDescriptor filterNonExtensionProperty(Collection<VariableDescriptor> variables)
	{
		for(VariableDescriptor variable : variables)
		{
			if(!variable.getReceiverParameter().exists())
			{
				return variable;
			}
		}
		return null;
	}

	@NotNull
	public static JetType getFunctionExpectedReturnType(@NotNull FunctionDescriptor descriptor, @NotNull NapileElement function)
	{
		JetType expectedType;
		if(function instanceof NapileMethod)
		{
			if(((NapileMethod) function).getReturnTypeRef() != null || ((NapileMethod) function).hasBlockBody())
			{
				expectedType = descriptor.getReturnType();
			}
			else
			{
				expectedType = TypeUtils.NO_EXPECTED_TYPE;
			}
		}
		else
		{
			expectedType = descriptor.getReturnType();
		}
		return expectedType != null ? expectedType : TypeUtils.NO_EXPECTED_TYPE;
	}

	public static boolean isObject(@NotNull ClassifierDescriptor classifier)
	{
		if(classifier instanceof ClassDescriptor)
		{
			ClassDescriptor clazz = (ClassDescriptor) classifier;
			return clazz.getKind() == ClassKind.OBJECT || clazz.getKind() == ClassKind.ENUM_ENTRY;
		}
		else if(classifier instanceof TypeParameterDescriptor)
		{
			return false;
		}
		else
		{
			throw new IllegalStateException("unknown classifier: " + classifier);
		}
	}

	public static boolean isSubclass(@NotNull ClassDescriptor subClass, @NotNull ClassDescriptor superClass)
	{
		return isSubtypeOfClass(subClass.getDefaultType(), superClass.getOriginal());
	}

	private static boolean isSubtypeOfClass(@NotNull JetType type, @NotNull DeclarationDescriptor superClass)
	{
		DeclarationDescriptor descriptor = type.getConstructor().getDeclarationDescriptor();
		if(descriptor != null && superClass == descriptor.getOriginal())
		{
			return true;
		}
		for(JetType superType : type.getConstructor().getSupertypes())
		{
			if(isSubtypeOfClass(superType, superClass))
			{
				return true;
			}
		}
		return false;
	}

	public static void addSuperTypes(JetType type, Set<JetType> set)
	{
		set.add(type);

		for(JetType jetType : type.getConstructor().getSupertypes())
		{
			addSuperTypes(jetType, set);
		}
	}

	public static boolean isTopLevelNamespace(@NotNull NamespaceDescriptor namespaceDescriptor)
	{
		return namespaceDescriptor.getContainingDeclaration() instanceof NamespaceDescriptor && namespaceDescriptor.getContainingDeclaration().getContainingDeclaration() instanceof ModuleDescriptor;
	}

	public static boolean isRootNamespace(@NotNull NamespaceDescriptor namespaceDescriptor)
	{
		return namespaceDescriptor.getContainingDeclaration() instanceof ModuleDescriptor;
	}

	@NotNull
	public static List<DeclarationDescriptor> getPathWithoutRootNsAndModule(@NotNull DeclarationDescriptor descriptor)
	{
		List<DeclarationDescriptor> path = Lists.newArrayList();
		DeclarationDescriptor current = descriptor;
		while(true)
		{
			if(current instanceof NamespaceDescriptor && isRootNamespace((NamespaceDescriptor) current))
			{
				return Lists.reverse(path);
			}
			path.add(current);
			current = current.getContainingDeclaration();
		}
	}

	public static boolean isClassObject(@NotNull DeclarationDescriptor descriptor)
	{
		return false;
	}

	@NotNull
	public static List<ClassDescriptor> getSuperclassDescriptors(@NotNull ClassDescriptor classDescriptor)
	{
		Collection<? extends JetType> superclassTypes = classDescriptor.getTypeConstructor().getSupertypes();
		List<ClassDescriptor> superClassDescriptors = new ArrayList<ClassDescriptor>();
		for(JetType type : superclassTypes)
		{
			ClassDescriptor result = getClassDescriptorForType(type);
			if(JetStandardClasses.isNotAny(result))
			{
				superClassDescriptors.add(result);
			}
		}
		return superClassDescriptors;
	}

	@NotNull
	public static ClassDescriptor getClassDescriptorForType(@NotNull JetType type)
	{
		DeclarationDescriptor superClassDescriptor = type.getConstructor().getDeclarationDescriptor();
		assert superClassDescriptor instanceof ClassDescriptor : "Superclass descriptor of a type should be of type ClassDescriptor";
		return (ClassDescriptor) superClassDescriptor;
	}

	public static boolean inStaticContext(@NotNull DeclarationDescriptor descriptor)
	{
		DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
		if(containingDeclaration instanceof NamespaceDescriptor)
		{
			return true;
		}
		if(containingDeclaration instanceof ClassDescriptor)
		{
			ClassDescriptor classDescriptor = (ClassDescriptor) containingDeclaration;

			if(classDescriptor.getKind() == ClassKind.OBJECT)
			{
				return inStaticContext(classDescriptor.getContainingDeclaration());
			}
		}
		return false;
	}
}
