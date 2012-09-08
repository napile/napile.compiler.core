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

import java.util.Collection;
import java.util.Collections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.descriptors.*;
import org.napile.compiler.lang.descriptors.SimpleMethodDescriptor;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.diagnostics.Diagnostic;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.lang.resolve.calls.OverloadResolutionResults;
import org.napile.compiler.lang.resolve.calls.ResolvedCall;
import org.napile.compiler.lang.resolve.calls.autocasts.DataFlowInfo;
import org.napile.compiler.lang.resolve.constants.CompileTimeConstant;
import org.napile.compiler.lang.resolve.name.FqName;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.types.DeferredType;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.util.Box;
import org.napile.compiler.util.slicedmap.BasicWritableSlice;
import org.napile.compiler.util.slicedmap.ReadOnlySlice;
import org.napile.compiler.util.slicedmap.SlicedMap;
import org.napile.compiler.util.slicedmap.Slices;
import org.napile.compiler.util.slicedmap.WritableSlice;
import org.napile.compiler.util.slicedmap.RewritePolicy;
import com.intellij.psi.PsiElement;

/**
 * @author abreslav
 */
public interface BindingContext
{
	BindingContext EMPTY = new BindingContext()
	{
		@Override
		public Collection<Diagnostic> getDiagnostics()
		{
			return Collections.emptyList();
		}

		@Override
		public <K, V> V get(ReadOnlySlice<K, V> slice, K key)
		{
			return null;
		}

		@NotNull
		@Override
		public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice)
		{
			return Collections.emptyList();
		}
	};

	WritableSlice<NapileAnnotationEntry, AnnotationDescriptor> ANNOTATION = Slices.createSimpleSlice();

	WritableSlice<NapileExpression, CompileTimeConstant<?>> COMPILE_TIME_VALUE = Slices.createSimpleSlice();
	WritableSlice<NapileTypeReference, JetType> TYPE = Slices.createSimpleSlice();
	WritableSlice<NapileExpression, JetType> EXPRESSION_TYPE = new BasicWritableSlice<NapileExpression, JetType>(RewritePolicy.DO_NOTHING);
	WritableSlice<NapileExpression, DataFlowInfo> EXPRESSION_DATA_FLOW_INFO = new BasicWritableSlice<NapileExpression, DataFlowInfo>(RewritePolicy.DO_NOTHING);

	WritableSlice<NapileReferenceExpression, DeclarationDescriptor> REFERENCE_TARGET = new BasicWritableSlice<NapileReferenceExpression, DeclarationDescriptor>(RewritePolicy.DO_NOTHING);
	WritableSlice<NapileElement, ResolvedCall<? extends CallableDescriptor>> RESOLVED_CALL = new BasicWritableSlice<NapileElement, ResolvedCall<? extends CallableDescriptor>>(RewritePolicy.DO_NOTHING);

	WritableSlice<NapileReferenceExpression, Collection<? extends DeclarationDescriptor>> AMBIGUOUS_REFERENCE_TARGET = new BasicWritableSlice<NapileReferenceExpression, Collection<? extends DeclarationDescriptor>>(RewritePolicy.DO_NOTHING);

	WritableSlice<CallKey, OverloadResolutionResults<MethodDescriptor>> RESOLUTION_RESULTS_FOR_FUNCTION = Slices.createSimpleSlice();
	WritableSlice<CallKey, OverloadResolutionResults<VariableDescriptor>> RESOLUTION_RESULTS_FOR_PROPERTY = Slices.createSimpleSlice();
	WritableSlice<NapileExpression, DelegatingBindingTrace> TRACE_DELTAS_CACHE = Slices.createSimpleSlice();

	WritableSlice<NapileExpression, MethodDescriptor> LOOP_RANGE_ITERATOR = Slices.createSimpleSlice();
	WritableSlice<NapileExpression, CallableDescriptor> LOOP_RANGE_HAS_NEXT = Slices.createSimpleSlice();
	WritableSlice<NapileExpression, MethodDescriptor> LOOP_RANGE_NEXT = Slices.createSimpleSlice();

	WritableSlice<NapileExpression, ResolvedCall<MethodDescriptor>> INDEXED_LVALUE_GET = Slices.createSimpleSlice();
	WritableSlice<NapileExpression, ResolvedCall<MethodDescriptor>> INDEXED_LVALUE_SET = Slices.createSimpleSlice();

	WritableSlice<NapileExpression, JetType> AUTOCAST = Slices.createSimpleSlice();

	/**
	 * A scope where type of expression has been resolved
	 */
	WritableSlice<NapileTypeReference, JetScope> TYPE_RESOLUTION_SCOPE = Slices.createSimpleSlice();
	WritableSlice<NapileExpression, JetScope> RESOLUTION_SCOPE = Slices.createSimpleSlice();

	/**
	 * Collected during analyze, used in IDE in auto-cast completion
	 */
	WritableSlice<NapileExpression, DataFlowInfo> NON_DEFAULT_EXPRESSION_DATA_FLOW = Slices.createSimpleSlice();

	WritableSlice<NapileExpression, Boolean> VARIABLE_REASSIGNMENT = Slices.createSimpleSetSlice();
	WritableSlice<ParameterDescriptor, Boolean> AUTO_CREATED_IT = Slices.createSimpleSetSlice();
	WritableSlice<NapileExpression, DeclarationDescriptor> VARIABLE_ASSIGNMENT = Slices.createSimpleSlice();
	WritableSlice<NapileExpression, DataFlowInfo> DATAFLOW_INFO_AFTER_CONDITION = Slices.createSimpleSlice();

	/**
	 * Has type of current expression has been already resolved
	 */
	WritableSlice<NapileExpression, Boolean> PROCESSED = Slices.createSimpleSetSlice();
	WritableSlice<NapileElement, Boolean> STATEMENT = Slices.createRemovableSetSlice();

	WritableSlice<VariableDescriptor, Boolean> CAPTURED_IN_CLOSURE = Slices.createSimpleSetSlice();

	//    enum DeferredTypeKey {DEFERRED_TYPE_KEY}
	//    WritableSlice<DeferredTypeKey, Collection<DeferredType>> DEFERRED_TYPES = Slices.createSimpleSlice();

	WritableSlice<Box<DeferredType>, Boolean> DEFERRED_TYPE = Slices.createCollectiveSetSlice();

	WritableSlice<PropertyDescriptor, Boolean> BACKING_FIELD_REQUIRED = new Slices.SetSlice<PropertyDescriptor>(RewritePolicy.DO_NOTHING)
	{
		@Override
		public Boolean computeValue(SlicedMap map, PropertyDescriptor propertyDescriptor, Boolean backingFieldRequired, boolean valueNotFound)
		{
			if(propertyDescriptor.getKind() != CallableMemberDescriptor.Kind.DECLARATION)
			{
				return false;
			}
			backingFieldRequired = valueNotFound ? false : backingFieldRequired;
			assert backingFieldRequired != null;
			// TODO: user BindingContextAccessors
			PsiElement declarationPsiElement = map.get(BindingContextUtils.DESCRIPTOR_TO_DECLARATION, propertyDescriptor);
			if(declarationPsiElement instanceof NapilePropertyParameter)
			{
				NapilePropertyParameter jetParameter = (NapilePropertyParameter) declarationPsiElement;
				return jetParameter.getVarNode() != null || backingFieldRequired; // this part is unused because we do not allow access to constructor parameters in member bodies
			}
			if(propertyDescriptor.getModality() == Modality.ABSTRACT)
				return false;
			PropertyGetterDescriptor getter = propertyDescriptor.getGetter();
			PropertySetterDescriptor setter = propertyDescriptor.getSetter();
			if(getter == null)
			{
				return true;
			}
			else if(propertyDescriptor.getPropertyKind() == PropertyKind.VAR && setter == null)
			{
				return true;
			}
			else if(setter != null && !setter.hasBody() && setter.getModality() != Modality.ABSTRACT)
			{
				return true;
			}
			else if(!getter.hasBody() && getter.getModality() != Modality.ABSTRACT)
			{
				return true;
			}
			return backingFieldRequired;
		}
	};
	WritableSlice<PropertyDescriptor, Boolean> IS_INITIALIZED = Slices.createSimpleSetSlice();

	WritableSlice<NapileFunctionLiteralExpression, Boolean> BLOCK = new Slices.SetSlice<NapileFunctionLiteralExpression>(RewritePolicy.DO_NOTHING)
	{
		@Override
		public Boolean computeValue(SlicedMap map, NapileFunctionLiteralExpression expression, Boolean isBlock, boolean valueNotFound)
		{
			isBlock = valueNotFound ? false : isBlock;
			assert isBlock != null;
			return isBlock && !expression.getFunctionLiteral().hasParameterSpecification();
		}
	};

	WritableSlice<PsiElement, NamespaceDescriptor> NAMESPACE = Slices.<PsiElement, NamespaceDescriptor>sliceBuilder().setOpposite((WritableSlice) BindingContextUtils.DESCRIPTOR_TO_DECLARATION).build();
	WritableSlice<PsiElement, ClassDescriptor> CLASS = Slices.<PsiElement, ClassDescriptor>sliceBuilder().setOpposite((WritableSlice) BindingContextUtils.DESCRIPTOR_TO_DECLARATION).build();
	WritableSlice<NapileTypeParameter, TypeParameterDescriptor> TYPE_PARAMETER = Slices.<NapileTypeParameter, TypeParameterDescriptor>sliceBuilder().setOpposite((WritableSlice) BindingContextUtils.DESCRIPTOR_TO_DECLARATION).build();
	/**
	 * @see BindingContextUtils#recordFunctionDeclarationToDescriptor(BindingTrace, PsiElement, org.napile.compiler.lang.descriptors.SimpleMethodDescriptor)}
	 */
	WritableSlice<PsiElement, SimpleMethodDescriptor> FUNCTION = Slices.<PsiElement, SimpleMethodDescriptor>sliceBuilder().setOpposite((WritableSlice) BindingContextUtils.DESCRIPTOR_TO_DECLARATION).build();
	WritableSlice<PsiElement, ConstructorDescriptor> CONSTRUCTOR = Slices.<PsiElement, ConstructorDescriptor>sliceBuilder().setOpposite((WritableSlice) BindingContextUtils.DESCRIPTOR_TO_DECLARATION).build();
	WritableSlice<PsiElement, VariableDescriptor> VARIABLE = Slices.<PsiElement, VariableDescriptor>sliceBuilder().setOpposite((WritableSlice) BindingContextUtils.DESCRIPTOR_TO_DECLARATION).build();
	WritableSlice<NapilePropertyParameter, VariableDescriptor> VALUE_PARAMETER = Slices.<NapilePropertyParameter, VariableDescriptor>sliceBuilder().setOpposite((WritableSlice) BindingContextUtils.DESCRIPTOR_TO_DECLARATION).build();
	WritableSlice<NapilePropertyAccessor, PropertyAccessorDescriptor> PROPERTY_ACCESSOR = Slices.<NapilePropertyAccessor, PropertyAccessorDescriptor>sliceBuilder().setOpposite((WritableSlice) BindingContextUtils.DESCRIPTOR_TO_DECLARATION).build();

	// normalize value to getOriginal(value)
	WritableSlice<NapileObjectDeclarationName, PropertyDescriptor> OBJECT_DECLARATION = Slices.<NapileObjectDeclarationName, PropertyDescriptor>sliceBuilder().setOpposite((WritableSlice) BindingContextUtils.DESCRIPTOR_TO_DECLARATION).build();

	WritableSlice[] DECLARATIONS_TO_DESCRIPTORS = new WritableSlice[]{
			NAMESPACE,
			CLASS,
			TYPE_PARAMETER,
			FUNCTION,
			CONSTRUCTOR,
			VARIABLE,
			VALUE_PARAMETER,
			PROPERTY_ACCESSOR,
			OBJECT_DECLARATION
	};

	ReadOnlySlice<PsiElement, DeclarationDescriptor> DECLARATION_TO_DESCRIPTOR = Slices.<PsiElement, DeclarationDescriptor>sliceBuilder().setFurtherLookupSlices(DECLARATIONS_TO_DESCRIPTORS).build();

	WritableSlice<NapileReferenceExpression, PsiElement> LABEL_TARGET = Slices.<NapileReferenceExpression, PsiElement>sliceBuilder().build();
	WritableSlice<NapilePropertyParameter, PropertyDescriptor> VALUE_PARAMETER_AS_PROPERTY = Slices.<NapilePropertyParameter, PropertyDescriptor>sliceBuilder().build();

	WritableSlice<FqName, ClassDescriptor> FQNAME_TO_CLASS_DESCRIPTOR = new BasicWritableSlice<FqName, ClassDescriptor>(RewritePolicy.DO_NOTHING, true);
	WritableSlice<FqName, NamespaceDescriptor> FQNAME_TO_NAMESPACE_DESCRIPTOR = new BasicWritableSlice<FqName, NamespaceDescriptor>(RewritePolicy.DO_NOTHING);
	WritableSlice<NapileFile, NamespaceDescriptor> FILE_TO_NAMESPACE = Slices.createSimpleSlice();
	WritableSlice<NamespaceDescriptor, Collection<NapileFile>> NAMESPACE_TO_FILES = Slices.createSimpleSlice();

	/**
	 * Each namespace found in src must be registered here.
	 */
	WritableSlice<NamespaceDescriptor, Boolean> NAMESPACE_IS_SRC = Slices.createSimpleSlice();

	WritableSlice<ClassDescriptor, Boolean> INCOMPLETE_HIERARCHY = Slices.createCollectiveSetSlice();

	@SuppressWarnings("UnusedDeclaration")
	@Deprecated
	// This field is needed only for the side effects of its initializer
			Void _static_initializer = BasicWritableSlice.initSliceDebugNames(BindingContext.class);

	Collection<Diagnostic> getDiagnostics();

	@Nullable
	<K, V> V get(ReadOnlySlice<K, V> slice, K key);

	// slice.isCollective() must be true
	@NotNull
	<K, V> Collection<K> getKeys(WritableSlice<K, V> slice);
}
