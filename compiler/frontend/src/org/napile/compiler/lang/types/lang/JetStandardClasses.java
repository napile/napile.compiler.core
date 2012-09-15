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

package org.napile.compiler.lang.types.lang;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.AbstractCollection;
import java.util.Collections;
import java.util.Iterator;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.*;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.resolve.name.FqNameUnsafe;
import org.napile.compiler.lang.resolve.name.Name;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.resolve.scopes.JetScopeImpl;
import org.napile.compiler.lang.resolve.scopes.RedeclarationHandler;
import org.napile.compiler.lang.resolve.scopes.WritableScope;
import org.napile.compiler.lang.resolve.scopes.WritableScopeImpl;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.NamespaceType;
import org.napile.compiler.lang.types.impl.JetTypeImpl;

/**
 * @author abreslav
 */
@Deprecated
public class JetStandardClasses
{

	private JetStandardClasses()
	{
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final ModuleDescriptor FAKE_STANDARD_CLASSES_MODULE = new ModuleDescriptor(Name.special("<builtin>"));

	private static final NamespaceDescriptorImpl STANDARD_CLASSES_FAKE_ROOT_NS = new NamespaceDescriptorImpl(FAKE_STANDARD_CLASSES_MODULE, Collections.<AnnotationDescriptor>emptyList(), FqNameUnsafe.ROOT_NAME);

	static
	{
		FAKE_STANDARD_CLASSES_MODULE.setRootNamespace(STANDARD_CLASSES_FAKE_ROOT_NS);
	}

	public static NamespaceDescriptorImpl STANDARD_CLASSES_NAMESPACE = new NamespaceDescriptorImpl(STANDARD_CLASSES_FAKE_ROOT_NS, Collections.<AnnotationDescriptor>emptyList(), Name.identifier("idea"));

	private static final ClassDescriptor NOTHING_CLASS;
	private static final JetType NOTHING_TYPE;

	static
	{
		LightClassDescriptorImpl nothing = new LightClassDescriptorImpl(STANDARD_CLASSES_NAMESPACE, Collections.<AnnotationDescriptor>emptyList(), Modality.FINAL, Name.identifier("Nothing"), false);
		ConstructorDescriptor constructorDescriptor = new ConstructorDescriptor(nothing, Collections.<AnnotationDescriptor>emptyList(), false);
		constructorDescriptor.initialize(Collections.<TypeParameterDescriptor>emptyList(), Collections.<ParameterDescriptor>emptyList(), Visibility.LOCAL);
		NOTHING_CLASS = nothing.initialize(true, Collections.<TypeParameterDescriptor>emptyList(), new AbstractCollection<JetType>()
		{
			@Override
			public boolean contains(Object o)
			{
				return o instanceof JetType;
			}

			@Override
			public Iterator<JetType> iterator()
			{
				throw new UnsupportedOperationException("Don't enumerate supertypes of Nothing");
			}

			@Override
			public int size()
			{
				throw new UnsupportedOperationException("Supertypes of Nothing do not constitute a valid collection");
			}
		}, JetScope.EMPTY, Collections.<ConstructorDescriptor>emptySet());
		NOTHING_TYPE = new JetTypeImpl(getNothing());
		constructorDescriptor.setReturnType(NOTHING_TYPE);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final JetScope STUB = JetScope.EMPTY;


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final Name UNIT_ALIAS = Name.identifier("Unit");
	private static final ClassDescriptor UNIT;
	private static final JetType UNIT_TYPE;

	static
	{
		LightClassDescriptorImpl any = new LightClassDescriptorImpl(STANDARD_CLASSES_NAMESPACE, Collections.<AnnotationDescriptor>emptyList(), Modality.OPEN, UNIT_ALIAS, false);
		ConstructorDescriptor constructorDescriptor = new ConstructorDescriptor(any, Collections.<AnnotationDescriptor>emptyList(), false);
		constructorDescriptor.initialize(Collections.<TypeParameterDescriptor>emptyList(), Collections.<ParameterDescriptor>emptyList(), Visibility.PUBLIC);
		UNIT = any.initialize(false, Collections.<TypeParameterDescriptor>emptyList(), Collections.<JetType>emptySet(), JetScope.EMPTY, Collections.<ConstructorDescriptor>emptySet());
		UNIT_TYPE = new JetTypeImpl(UNIT.getTypeConstructor(), new JetScopeImpl()
		{
			@NotNull
			@Override
			public DeclarationDescriptor getContainingDeclaration()
			{
				return STANDARD_CLASSES_NAMESPACE;
			}

			@Override
			public String toString()
			{
				return "Scope for Unit";
			}
		});
		constructorDescriptor.setReturnType(UNIT_TYPE);

	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@NotNull
	public static final JetScope STANDARD_CLASSES;

	static
	{
		WritableScope writableScope = new WritableScopeImpl(JetScope.EMPTY, STANDARD_CLASSES_NAMESPACE, RedeclarationHandler.DO_NOTHING, "JetStandardClasses.STANDARD_CLASSES");
		writableScope.changeLockLevel(WritableScope.LockLevel.BOTH);

		STANDARD_CLASSES = writableScope;
		writableScope.addClassifierAlias(UNIT_ALIAS, UNIT);

		Field[] declaredFields = JetStandardClasses.class.getDeclaredFields();
		for(Field field : declaredFields)
		{
			if((field.getModifiers() & Modifier.STATIC) == 0)
			{
				continue;
			}
			Class<?> type = field.getType();
			if(type == ClassDescriptor.class)
			{
				try
				{
					ClassDescriptor descriptor = (ClassDescriptor) field.get(null);
					writableScope.addClassifierDescriptor(descriptor);
				}
				catch(IllegalAccessException e)
				{
					throw new IllegalStateException(e);
				}
			}
			else if(type.isArray() && type.getComponentType() == ClassDescriptor.class)
			{
				try
				{
					ClassDescriptor[] array = (ClassDescriptor[]) field.get(null);
					for(ClassDescriptor descriptor : array)
					{
						writableScope.addClassifierDescriptor(descriptor);
					}
				}
				catch(IllegalAccessException e)
				{
					throw new IllegalStateException(e);
				}
			}
		}
		STANDARD_CLASSES_NAMESPACE.initialize(writableScope);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@NotNull
	public static ClassDescriptor getNothing()
	{
		return NOTHING_CLASS;
	}


	public static JetType getUnitType()
	{
		return UNIT_TYPE;
	}


	public static boolean isNothing(@NotNull JetType type)
	{
		return isNothingOrNullableNothing(type) && !type.isNullable();
	}

	public static boolean isNothingOrNullableNothing(@NotNull JetType type)
	{
		return !(type instanceof NamespaceType) && type.getConstructor() == NOTHING_CLASS.getTypeConstructor();
	}

	public static boolean isUnit(@NotNull JetType type)
	{
		return !(type instanceof NamespaceType) && type.getConstructor() == UNIT_TYPE.getConstructor();
	}

}
