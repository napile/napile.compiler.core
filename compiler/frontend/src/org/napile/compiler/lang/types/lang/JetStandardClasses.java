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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.descriptors.*;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.resolve.name.FqNameUnsafe;
import org.napile.compiler.lang.resolve.name.Name;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.resolve.scopes.JetScopeImpl;
import org.napile.compiler.lang.resolve.scopes.RedeclarationHandler;
import org.napile.compiler.lang.resolve.scopes.WritableScope;
import org.napile.compiler.lang.resolve.scopes.WritableScopeImpl;
import org.napile.compiler.lang.resolve.scopes.receivers.ClassReceiver;
import org.napile.compiler.lang.rt.NapileLangPackage;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.JetTypeImpl;
import org.napile.compiler.lang.types.NamespaceType;
import org.napile.compiler.lang.types.TypeConstructor;
import org.napile.compiler.lang.types.TypeUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

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
		ConstructorDescriptor constructorDescriptor = new ConstructorDescriptor(nothing, Collections.<AnnotationDescriptor>emptyList());
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

	private static final ClassDescriptor ANY;
	private static final JetType ANY_TYPE;

	static
	{
		LightClassDescriptorImpl any = new LightClassDescriptorImpl(STANDARD_CLASSES_NAMESPACE, Collections.<AnnotationDescriptor>emptyList(), Modality.OPEN, Name.identifier("Any"), false);
		ConstructorDescriptor constructorDescriptor = new ConstructorDescriptor(any, Collections.<AnnotationDescriptor>emptyList());
		constructorDescriptor.initialize(Collections.<TypeParameterDescriptor>emptyList(), Collections.<ParameterDescriptor>emptyList(), Visibility.PUBLIC);
		ANY = any.initialize(false, Collections.<TypeParameterDescriptor>emptyList(), Collections.<JetType>emptySet(), JetScope.EMPTY, Collections.<ConstructorDescriptor>emptySet());
		ANY_TYPE = new JetTypeImpl(ANY.getTypeConstructor(), new JetScopeImpl()
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
				return "Scope for Any";
			}
		});
		constructorDescriptor.setReturnType(ANY_TYPE);
	}

	private static final JetType NULLABLE_ANY_TYPE = new JetTypeImpl(ANY_TYPE.getAnnotations(), ANY_TYPE.getConstructor(), true, ANY_TYPE.getArguments(), ANY_TYPE.getMemberScope());

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final JetType DEFAULT_BOUND = getNullableAnyType();

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final JetScope STUB = JetScope.EMPTY;

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final int MAX_FUNCTION_ORDER = 22;

	private static final ClassDescriptor[] FUNCTION = new ClassDescriptor[MAX_FUNCTION_ORDER + 1];
	private static final ClassDescriptor[] RECEIVER_FUNCTION = new ClassDescriptor[MAX_FUNCTION_ORDER + 1];

	private static final Set<TypeConstructor> FUNCTION_TYPE_CONSTRUCTORS = Sets.newHashSet();
	private static final Set<TypeConstructor> RECEIVER_FUNCTION_TYPE_CONSTRUCTORS = Sets.newHashSet();

	static
	{
		for(int i = 0; i <= MAX_FUNCTION_ORDER; i++)
		{
			LightClassDescriptorImpl function = new LightClassDescriptorImpl(STANDARD_CLASSES_NAMESPACE, Collections.<AnnotationDescriptor>emptyList(), Modality.ABSTRACT, Name.identifier("Function" + i), false);

			SimpleMethodDescriptorImpl invoke = new SimpleMethodDescriptorImpl(function, Collections.<AnnotationDescriptor>emptyList(), Name.identifier("invoke"), CallableMemberDescriptor.Kind.DECLARATION, false);
			WritableScope scopeForInvoke = createScopeForInvokeFunction(function, invoke);
			List<TypeParameterDescriptor> typeParameters = createTypeParameters(0, i, function);
			ConstructorDescriptor constructorDescriptorForFunction = new ConstructorDescriptor(function, Collections.<AnnotationDescriptor>emptyList());
			FUNCTION[i] = function.initialize(false, typeParameters, Collections.singleton(getAnyType()), scopeForInvoke, Collections.<ConstructorDescriptor>emptySet());
			FUNCTION_TYPE_CONSTRUCTORS.add(FUNCTION[i].getTypeConstructor());
			FunctionDescriptorUtil.initializeFromFunctionType(invoke, function.getDefaultType(), new ClassReceiver(FUNCTION[i]), Modality.ABSTRACT, Visibility.PUBLIC);

			constructorDescriptorForFunction.initialize(function.getTypeConstructor().getParameters(), Collections.<ParameterDescriptor>emptyList(), Visibility.PUBLIC);
			constructorDescriptorForFunction.setReturnType(function.getDefaultType());

			LightClassDescriptorImpl receiverFunction = new LightClassDescriptorImpl(STANDARD_CLASSES_NAMESPACE, Collections.<AnnotationDescriptor>emptyList(), Modality.ABSTRACT, Name.identifier("ExtensionFunction" + i), false);
			SimpleMethodDescriptorImpl invokeWithReceiver = new SimpleMethodDescriptorImpl(receiverFunction, Collections.<AnnotationDescriptor>emptyList(), Name.identifier("invoke"), CallableMemberDescriptor.Kind.DECLARATION, false);
			WritableScope scopeForInvokeWithReceiver = createScopeForInvokeFunction(receiverFunction, invokeWithReceiver);
			List<TypeParameterDescriptor> parameters = createTypeParameters(1, i, receiverFunction);
			parameters.add(0, TypeParameterDescriptorImpl.createWithDefaultBound(receiverFunction, Collections.<AnnotationDescriptor>emptyList(), false, Name.identifier("T"), 0));

			ConstructorDescriptor constructorDescriptorForReceiverFunction = new ConstructorDescriptor(function, Collections.<AnnotationDescriptor>emptyList());
			RECEIVER_FUNCTION[i] = receiverFunction.initialize(false, parameters, Collections.singleton(getAnyType()), scopeForInvokeWithReceiver, Collections.<ConstructorDescriptor>emptySet());
			RECEIVER_FUNCTION_TYPE_CONSTRUCTORS.add(RECEIVER_FUNCTION[i].getTypeConstructor());
			FunctionDescriptorUtil.initializeFromFunctionType(invokeWithReceiver, receiverFunction.getDefaultType(), new ClassReceiver(RECEIVER_FUNCTION[i]), Modality.ABSTRACT, Visibility.PUBLIC);

			constructorDescriptorForReceiverFunction.initialize(receiverFunction.getTypeConstructor().getParameters(), Collections.<ParameterDescriptor>emptyList(), Visibility.PUBLIC);
			constructorDescriptorForReceiverFunction.setReturnType(receiverFunction.getDefaultType());
		}
	}

	private static WritableScope createScopeForInvokeFunction(LightClassDescriptorImpl function, SimpleMethodDescriptorImpl invoke)
	{
		WritableScope scopeForInvoke = new WritableScopeImpl(STUB, function, RedeclarationHandler.THROW_EXCEPTION, "Scope for function type");
		scopeForInvoke.addFunctionDescriptor(invoke);
		scopeForInvoke.changeLockLevel(WritableScope.LockLevel.READING);
		return scopeForInvoke;
	}

	private static List<TypeParameterDescriptor> createTypeParameters(int baseIndex, int parameterCount, LightClassDescriptorImpl function)
	{
		List<TypeParameterDescriptor> parameters = new ArrayList<TypeParameterDescriptor>();
		for(int j = 0; j < parameterCount; j++)
		{
			parameters.add(TypeParameterDescriptorImpl.createWithDefaultBound(function, Collections.<AnnotationDescriptor>emptyList(), false, Name.identifier("P" + (j + 1)), baseIndex + j));
		}
		parameters.add(TypeParameterDescriptorImpl.createWithDefaultBound(function, Collections.<AnnotationDescriptor>emptyList(), false, Name.identifier("R"), baseIndex + parameterCount));
		return parameters;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final Name UNIT_ALIAS = Name.identifier("Unit");
	private static final ClassDescriptor UNIT;
	private static final JetType UNIT_TYPE;

	static
	{
		LightClassDescriptorImpl any = new LightClassDescriptorImpl(STANDARD_CLASSES_NAMESPACE, Collections.<AnnotationDescriptor>emptyList(), Modality.OPEN, UNIT_ALIAS, false);
		ConstructorDescriptor constructorDescriptor = new ConstructorDescriptor(any, Collections.<AnnotationDescriptor>emptyList());
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
		constructorDescriptor.setReturnType(ANY_TYPE);

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
	public static JetType getDefaultBound()
	{
		return DEFAULT_BOUND;
	}

	@NotNull
	public static ClassDescriptor getAny()
	{
		return ANY;
	}

	@NotNull
	public static JetType getAnyType()
	{
		return ANY_TYPE;
	}

	public static boolean isAny(JetType type)
	{
		return TypeUtils.isEqualFqName(type, NapileLangPackage.ANY);
	}

	public static JetType getNullableAnyType()
	{
		return NULLABLE_ANY_TYPE;
	}

	@NotNull
	public static ClassDescriptor getNothing()
	{
		return NOTHING_CLASS;
	}

	@NotNull
	public static ClassDescriptor getFunction(int parameterCount)
	{
		return FUNCTION[parameterCount];
	}

	@NotNull
	public static ClassDescriptor getReceiverFunction(int parameterCount)
	{
		return RECEIVER_FUNCTION[parameterCount];
	}

	public static JetType getUnitType()
	{
		return UNIT_TYPE;
	}

	public static JetType getNothingType()
	{
		return NOTHING_TYPE;
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

	private static List<JetType> toTypes(List<ParameterDescriptor> labeledEntries)
	{
		List<JetType> result = new ArrayList<JetType>();
		for(ParameterDescriptor entry : labeledEntries)
		{
			result.add(entry.getType());
		}
		return result;
	}

	// TODO : labeled version?
	public static JetType getFunctionType(List<AnnotationDescriptor> annotations, @Nullable JetType receiverType, @NotNull List<JetType> parameterTypes, @NotNull JetType returnType)
	{
		List<JetType> arguments = new ArrayList<JetType>();
		if(receiverType != null)
		{
			arguments.add(receiverType);
		}
		for(JetType parameterType : parameterTypes)
		{
			arguments.add(parameterType);
		}
		arguments.add(returnType);
		int size = parameterTypes.size();
		ClassDescriptor classDescriptor = receiverType == null ? FUNCTION[size] : RECEIVER_FUNCTION[size];
		TypeConstructor constructor = classDescriptor.getTypeConstructor();
		return new JetTypeImpl(annotations, constructor, false, arguments, classDescriptor.getMemberScope(arguments));
	}

	public static boolean isFunctionType(@NotNull JetType type)
	{
		return FUNCTION_TYPE_CONSTRUCTORS.contains(type.getConstructor()) || isReceiverFunctionType(type);
	}

	public static boolean isReceiverFunctionType(@NotNull JetType type)
	{
		return RECEIVER_FUNCTION_TYPE_CONSTRUCTORS.contains(type.getConstructor());
	}

	@Nullable
	public static JetType getReceiverType(@NotNull JetType type)
	{
		assert isFunctionType(type) : type;
		if(RECEIVER_FUNCTION_TYPE_CONSTRUCTORS.contains(type.getConstructor()))
		{
			return type.getArguments().get(0);
		}
		return null;
	}

	@NotNull
	public static List<ParameterDescriptor> getValueParameters(@NotNull MethodDescriptor methodDescriptor, @NotNull JetType type)
	{
		assert isFunctionType(type);
		List<ParameterDescriptor> valueParameters = Lists.newArrayList();
		List<JetType> parameterTypes = getParameterTypeProjectionsFromFunctionType(type);
		for(int i = 0; i < parameterTypes.size(); i++)
		{
			JetType parameterType = parameterTypes.get(i);
			PropertyParameterDescriptorImpl valueParameterDescriptor = new PropertyParameterDescriptorImpl(methodDescriptor, i, Collections.<AnnotationDescriptor>emptyList(), Name.identifier("p" + (i + 1)), PropertyKind.VAL, parameterType, false, null);
			valueParameters.add(valueParameterDescriptor);
		}
		return valueParameters;
	}

	@NotNull
	public static List<JetType> getParameterTypeProjectionsFromFunctionType(@NotNull JetType type)
	{
		assert isFunctionType(type);
		List<JetType> arguments = type.getArguments();
		int first = RECEIVER_FUNCTION_TYPE_CONSTRUCTORS.contains(type.getConstructor()) ? 1 : 0;
		int last = arguments.size() - 2;
		List<JetType> parameterTypes = Lists.newArrayList();
		for(int i = first; i <= last; i++)
		{
			parameterTypes.add(arguments.get(i));
		}
		return parameterTypes;
	}

	@NotNull
	public static JetType getReturnTypeFromFunctionType(@NotNull JetType type)
	{
		assert isFunctionType(type);
		List<JetType> arguments = type.getArguments();
		return arguments.get(arguments.size() - 1);
	}

	@NotNull
	public static Collection<DeclarationDescriptor> getAllStandardClasses()
	{
		return STANDARD_CLASSES.getAllDescriptors();
	}

	public static boolean isNotAny(@NotNull DeclarationDescriptor superClassDescriptor)
	{
		return !superClassDescriptor.equals(getAny());
	}
}
