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

package org.napile.compiler.lang.resolve.processors.checkers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.ClassifierDescriptor;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.Modality;
import org.napile.compiler.lang.descriptors.MutableClassDescriptor;
import org.napile.compiler.lang.descriptors.SimpleMethodDescriptor;
import org.napile.compiler.lang.diagnostics.Errors;
import org.napile.compiler.lang.psi.NapileAnonymClass;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileConstructor;
import org.napile.compiler.lang.psi.NapileNamedMethodOrMacro;
import org.napile.compiler.lang.psi.NapileTypeParameter;
import org.napile.compiler.lang.psi.NapileTypeReference;
import org.napile.compiler.lang.psi.NapileDelegationToSuperCall;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.BodiesResolveContext;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.SelfTypeConstructor;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * @author svtk
 */
public class DeclarationsChecker
{
	@NotNull
	private BindingTrace trace;

	@Inject
	public void setTrace(@NotNull BindingTrace trace)
	{
		this.trace = trace;
	}

	public void process(@NotNull BodiesResolveContext bodiesResolveContext)
	{
		for(Map.Entry<NapileClass, MutableClassDescriptor> entry : bodiesResolveContext.getClasses().entrySet())
		{
			NapileClass aClass = entry.getKey();
			MutableClassDescriptor classDescriptor = entry.getValue();

			if(!bodiesResolveContext.completeAnalysisNeeded(aClass))
				continue;

			checkSuperListForFinalClasses(aClass.getSuperTypes());
			checkSuperListForDuplicates(aClass.getSuperTypes());

			if(classDescriptor.isTraited())
				checkSuperListForClassesWithConstructors(aClass.getSuperTypes());

			for(NapileTypeParameter typeParameter : aClass.getTypeParameters())
				checkSuperListForDuplicates(typeParameter.getSuperTypes());
		}

		for(Map.Entry<NapileNamedMethodOrMacro, SimpleMethodDescriptor> entry : bodiesResolveContext.getMethods().entrySet())
		{
			NapileNamedMethodOrMacro method = entry.getKey();

			if(!bodiesResolveContext.completeAnalysisNeeded(method))
				continue;

			for(NapileTypeParameter typeParameter : method.getTypeParameters())
				checkSuperListForDuplicates(typeParameter.getSuperTypes());
		}

		for(Map.Entry<NapileAnonymClass, MutableClassDescriptor> entry : bodiesResolveContext.getAnonymous().entrySet())
		{
			NapileAnonymClass anonymClass = entry.getKey();

			if(!bodiesResolveContext.completeAnalysisNeeded(anonymClass))
				continue;

			checkSuperListForFinalClasses(anonymClass.getSuperTypes());
			checkSuperListForDuplicates(anonymClass.getSuperTypes());
		}

		for(Map.Entry<NapileConstructor, ConstructorDescriptor> entry : bodiesResolveContext.getConstructors().entrySet())
		{
			NapileConstructor constructor = entry.getKey();

			if(!bodiesResolveContext.completeAnalysisNeeded(constructor))
				continue;

			checkConstructor(constructor);
		}
	}

	private void checkSuperListForFinalClasses(@NotNull List<? extends NapileTypeReference> typeReferences)
	{
		for(NapileTypeReference typeReference : typeReferences)
		{
			JetType jetType = trace.get(BindingContext.TYPE, typeReference);
			if(jetType == null)
				continue;

			ClassifierDescriptor classifierDescriptor = jetType.getConstructor().getDeclarationDescriptor();
			if(classifierDescriptor instanceof ClassDescriptor)
			{
				if(((ClassDescriptor) classifierDescriptor).getModality() == Modality.FINAL)
					trace.report(Errors.FINAL_SUPERTYPE.on(typeReference));
			}
			else
				trace.report(Errors.INVALID_SUPER_CALL.on(typeReference));
		}
	}

	private void checkSuperListForDuplicates(@NotNull List<? extends NapileTypeReference> typeReferences)
	{
		List<JetType> list = new ArrayList<JetType>(typeReferences.size());
		for(NapileTypeReference typeReference : typeReferences)
		{
			JetType jetType = trace.get(BindingContext.TYPE, typeReference);
			if(jetType == null)
				continue;

			if(list.contains(jetType))
				trace.report(Errors.SUPERTYPE_APPEARS_TWICE.on(typeReference));
			else
				list.add(jetType);
		}
	}

	private void checkSuperListForClassesWithConstructors(@NotNull List<? extends NapileTypeReference> typeReferences)
	{
		for(NapileTypeReference typeReference : typeReferences)
		{
			JetType jetType = trace.safeGet(BindingContext.TYPE, typeReference);

			ClassifierDescriptor classifierDescriptor = jetType.getConstructor().getDeclarationDescriptor();
			if(classifierDescriptor instanceof ClassDescriptor && !classifierDescriptor.getConstructors().isEmpty())
				trace.report(Errors.TRAITED_CLASS_CANT_EXTEND_CLASS_WITH_CONSTRUCTORS.on(typeReference));
		}
	}

	private void checkConstructor(NapileConstructor constructor)
	{
		if(constructor instanceof PsiCompiledElement)
			return;

		NapileClass parent = PsiTreeUtil.getParentOfType(constructor, NapileClass.class);

		assert parent != null;

		Map<NapileTypeReference, JetType> classSpecifiers = makeTypeListBySuperList(parent.getSuperTypes());
		Map<NapileTypeReference, JetType> constructorSpecifiers = makeTypeList(constructor.getDelegationSpecifiers());

		boolean thisCall = false;
		for(Map.Entry<NapileTypeReference, JetType> constructorEntry : constructorSpecifiers.entrySet())
		{
			if(constructorEntry.getValue().getConstructor() instanceof SelfTypeConstructor)
			{
				thisCall = true;
				continue;
			}

			if(thisCall)
				trace.report(Errors.MANY_CALLS_TO_THIS.on(constructorEntry.getKey()));
			else if(!classSpecifiers.values().contains(constructorEntry.getValue()))
				trace.report(Errors.INVALID_SUPER_CALL.on(constructorEntry.getKey()));
		}

		if(!thisCall)
			for(Map.Entry<NapileTypeReference, JetType> classEntry : classSpecifiers.entrySet())
			{
				if(!constructorSpecifiers.values().contains(classEntry.getValue()))
					trace.report(Errors.MISSED_SUPER_CALL.on(constructor.getNameIdentifier(), classEntry.getValue()));
			}
	}

	@NotNull
	private Map<NapileTypeReference, JetType> makeTypeListBySuperList(@NotNull List<? extends NapileTypeReference> list)
	{
		Map<NapileTypeReference, JetType> types = new LinkedHashMap<NapileTypeReference, JetType>(list.size());
		for(NapileTypeReference typeReference : list)
		{
			JetType type = trace.safeGet(BindingContext.TYPE, typeReference);
			ClassifierDescriptor constructorDescriptor = type.getConstructor().getDeclarationDescriptor();
			// traited class don't have constructors
			if(constructorDescriptor instanceof ClassDescriptor && ((ClassDescriptor) constructorDescriptor).isTraited())
				continue;
			types.put(typeReference, type);
		}

		return types;
	}

	@NotNull
	private Map<NapileTypeReference, JetType> makeTypeList(@NotNull List<NapileDelegationToSuperCall> list)
	{
		Map<NapileTypeReference, JetType> types = new LinkedHashMap<NapileTypeReference, JetType>(list.size());
		for(NapileDelegationToSuperCall delegationSpecifier : list)
		{
			JetType type = trace.get(BindingContext.TYPE, delegationSpecifier.getTypeReference());
			types.put(delegationSpecifier.getTypeReference(), type);
		}

		return types;
	}
}
