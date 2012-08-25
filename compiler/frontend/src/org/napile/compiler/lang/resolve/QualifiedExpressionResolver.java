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

import static org.napile.compiler.lang.diagnostics.Errors.CANNOT_BE_IMPORTED;
import static org.napile.compiler.lang.diagnostics.Errors.CANNOT_IMPORT_FROM_ELEMENT;
import static org.napile.compiler.lang.diagnostics.Errors.INVISIBLE_REFERENCE;
import static org.napile.compiler.lang.diagnostics.Errors.UNRESOLVED_REFERENCE;
import static org.napile.compiler.lang.diagnostics.Errors.UNSUPPORTED;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.resolve.name.Name;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.ClassKind;
import org.napile.compiler.lang.descriptors.ClassifierDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptorWithVisibility;
import org.napile.compiler.lang.descriptors.NamespaceDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.descriptors.Visibilities;
import org.napile.compiler.lang.diagnostics.Errors;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileImportDirective;
import org.napile.compiler.lang.psi.NapilePsiUtil;
import org.napile.compiler.lang.psi.NapileQualifiedExpression;
import org.napile.compiler.lang.psi.NapileSimpleNameExpression;
import org.napile.compiler.lang.psi.NapileUserType;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author svtk
 */
public class QualifiedExpressionResolver
{

	@NotNull
	public Collection<? extends DeclarationDescriptor> analyseImportReference(@NotNull NapileImportDirective importDirective, @NotNull JetScope scope, @NotNull BindingTrace trace)
	{

		return processImportReference(importDirective, scope, scope, Importer.DO_NOTHING, trace, false);
	}

	@NotNull
	public Collection<? extends DeclarationDescriptor> processImportReference(@NotNull NapileImportDirective importDirective, @NotNull JetScope scope, @NotNull JetScope scopeToCheckVisibility, @NotNull Importer importer, @NotNull BindingTrace trace, boolean onlyClasses)
	{

		if(importDirective.isAbsoluteInRootNamespace())
		{
			trace.report(UNSUPPORTED.on(importDirective, "TypeHierarchyResolver")); // TODO
			return Collections.emptyList();
		}
		NapileExpression importedReference = importDirective.getImportedReference();
		if(importedReference == null)
		{
			return Collections.emptyList();
		}

		Collection<? extends DeclarationDescriptor> descriptors;
		if(importedReference instanceof NapileQualifiedExpression)
		{
			//store result only when we find all descriptors, not only classes on the second phase
			descriptors = lookupDescriptorsForQualifiedExpression((NapileQualifiedExpression) importedReference, scope, scopeToCheckVisibility, trace, onlyClasses, !onlyClasses);
		}
		else
		{
			assert importedReference instanceof NapileSimpleNameExpression;
			descriptors = lookupDescriptorsForSimpleNameReference((NapileSimpleNameExpression) importedReference, scope, scopeToCheckVisibility, trace, onlyClasses, true, !onlyClasses);
		}

		NapileSimpleNameExpression referenceExpression = NapilePsiUtil.getLastReference(importedReference);
		if(importDirective.isAllUnder())
		{
			if(referenceExpression == null || !canImportMembersFrom(descriptors, referenceExpression, trace, onlyClasses))
			{
				return Collections.emptyList();
			}

			for(DeclarationDescriptor descriptor : descriptors)
			{
				importer.addAllUnderImport(descriptor);
			}
			return Collections.emptyList();
		}

		Name aliasName = NapilePsiUtil.getAliasName(importDirective);
		if(aliasName == null)
		{
			return Collections.emptyList();
		}

		for(DeclarationDescriptor descriptor : descriptors)
		{
			importer.addAliasImport(descriptor, aliasName);
		}

		return descriptors;
	}

	private boolean canImportMembersFrom(@NotNull Collection<? extends DeclarationDescriptor> descriptors, @NotNull NapileSimpleNameExpression reference, @NotNull BindingTrace trace, boolean onlyClasses)
	{

		if(onlyClasses)
		{
			return true;
		}
		if(descriptors.size() == 1)
		{
			return canImportMembersFrom(descriptors.iterator().next(), reference, trace, onlyClasses);
		}
		TemporaryBindingTrace temporaryTrace = TemporaryBindingTrace.create(trace);
		boolean canImport = false;
		for(DeclarationDescriptor descriptor : descriptors)
		{
			canImport |= canImportMembersFrom(descriptor, reference, temporaryTrace, onlyClasses);
		}
		if(!canImport)
		{
			temporaryTrace.commit();
		}
		return canImport;
	}

	private boolean canImportMembersFrom(@NotNull DeclarationDescriptor descriptor, @NotNull NapileSimpleNameExpression reference, @NotNull BindingTrace trace, boolean onlyClasses)
	{
		assert !onlyClasses;
		if(descriptor instanceof NamespaceDescriptor)
		{
			return true;
		}
		if(descriptor instanceof ClassDescriptor && ((ClassDescriptor) descriptor).getKind() != ClassKind.OBJECT)
		{
			return true;
		}
		trace.report(CANNOT_IMPORT_FROM_ELEMENT.on(reference, descriptor));
		return false;
	}

	@NotNull
	public Collection<? extends DeclarationDescriptor> lookupDescriptorsForUserType(@NotNull NapileUserType userType, @NotNull JetScope outerScope, @NotNull BindingTrace trace)
	{

		if(userType.isAbsoluteInRootNamespace())
		{
			trace.report(Errors.UNSUPPORTED.on(userType, "package"));
			return Collections.emptyList();
		}
		NapileSimpleNameExpression referenceExpression = userType.getReferenceExpression();
		if(referenceExpression == null)
		{
			return Collections.emptyList();
		}
		NapileUserType qualifier = userType.getQualifier();
		if(qualifier == null)
		{
			return lookupDescriptorsForSimpleNameReference(referenceExpression, outerScope, outerScope, trace, true, false, true);
		}
		Collection<? extends DeclarationDescriptor> declarationDescriptors = lookupDescriptorsForUserType(qualifier, outerScope, trace);
		return lookupSelectorDescriptors(referenceExpression, declarationDescriptors, trace, outerScope, true, true);
	}

	@NotNull
	public Collection<? extends DeclarationDescriptor> lookupDescriptorsForQualifiedExpression(@NotNull NapileQualifiedExpression importedReference, @NotNull JetScope outerScope, @NotNull JetScope scopeToCheckVisibility, @NotNull BindingTrace trace, boolean onlyClasses, boolean storeResult)
	{

		NapileExpression receiverExpression = importedReference.getReceiverExpression();
		Collection<? extends DeclarationDescriptor> declarationDescriptors;
		if(receiverExpression instanceof NapileQualifiedExpression)
		{
			declarationDescriptors = lookupDescriptorsForQualifiedExpression((NapileQualifiedExpression) receiverExpression, outerScope, scopeToCheckVisibility, trace, onlyClasses, storeResult);
		}
		else
		{
			assert receiverExpression instanceof NapileSimpleNameExpression;
			declarationDescriptors = lookupDescriptorsForSimpleNameReference((NapileSimpleNameExpression) receiverExpression, outerScope, scopeToCheckVisibility, trace, onlyClasses, true, storeResult);
		}

		NapileExpression selectorExpression = importedReference.getSelectorExpression();
		if(!(selectorExpression instanceof NapileSimpleNameExpression))
		{
			return Collections.emptyList();
		}

		NapileSimpleNameExpression selector = (NapileSimpleNameExpression) selectorExpression;
		NapileSimpleNameExpression lastReference = NapilePsiUtil.getLastReference(receiverExpression);
		if(lastReference == null || !canImportMembersFrom(declarationDescriptors, lastReference, trace, onlyClasses))
		{
			return Collections.emptyList();
		}

		return lookupSelectorDescriptors(selector, declarationDescriptors, trace, scopeToCheckVisibility, onlyClasses, storeResult);
	}

	@NotNull
	private Collection<? extends DeclarationDescriptor> lookupSelectorDescriptors(@NotNull NapileSimpleNameExpression selector, @NotNull Collection<? extends DeclarationDescriptor> declarationDescriptors, @NotNull BindingTrace trace, @NotNull JetScope scopeToCheckVisibility, boolean onlyClasses, boolean storeResult)
	{

		Set<SuccessfulLookupResult> results = Sets.newHashSet();
		for(DeclarationDescriptor declarationDescriptor : declarationDescriptors)
		{
			if(declarationDescriptor instanceof NamespaceDescriptor)
			{
				addResult(results, lookupSimpleNameReference(selector, ((NamespaceDescriptor) declarationDescriptor).getMemberScope(), onlyClasses, true));
			}
			if(declarationDescriptor instanceof ClassDescriptor)
			{
				addResult(results, lookupSimpleNameReference(selector, getAppropriateScope((ClassDescriptor) declarationDescriptor, onlyClasses), onlyClasses, false));
				ClassDescriptor classObjectDescriptor = ((ClassDescriptor) declarationDescriptor).getClassObjectDescriptor();
				if(classObjectDescriptor != null)
				{
					addResult(results, lookupSimpleNameReference(selector, getAppropriateScope(classObjectDescriptor, onlyClasses), onlyClasses, false));
				}
			}
		}
		return filterAndStoreResolutionResult(results, selector, trace, scopeToCheckVisibility, onlyClasses, storeResult);
	}

	@NotNull
	private JetScope getAppropriateScope(@NotNull ClassDescriptor classDescriptor, boolean onlyClasses)
	{
		return onlyClasses ? classDescriptor.getUnsubstitutedInnerClassesScope() : classDescriptor.getDefaultType().getMemberScope();
	}

	private void addResult(@NotNull Set<SuccessfulLookupResult> results, @NotNull LookupResult result)
	{
		if(result == LookupResult.EMPTY)
			return;
		results.add((SuccessfulLookupResult) result);
	}


	@NotNull
	public Collection<? extends DeclarationDescriptor> lookupDescriptorsForSimpleNameReference(@NotNull NapileSimpleNameExpression referenceExpression, @NotNull JetScope outerScope, @NotNull JetScope scopeToCheckVisibility, @NotNull BindingTrace trace, boolean onlyClasses, boolean namespaceLevel, boolean storeResult)
	{

		LookupResult lookupResult = lookupSimpleNameReference(referenceExpression, outerScope, onlyClasses, namespaceLevel);
		if(lookupResult == LookupResult.EMPTY)
			return Collections.emptyList();
		return filterAndStoreResolutionResult(Collections.singletonList((SuccessfulLookupResult) lookupResult), referenceExpression, trace, scopeToCheckVisibility, onlyClasses, storeResult);
	}

	@NotNull
	private LookupResult lookupSimpleNameReference(@NotNull NapileSimpleNameExpression referenceExpression, @NotNull JetScope outerScope, boolean onlyClasses, boolean namespaceLevel)
	{

		Name referencedName = referenceExpression.getReferencedNameAsName();
		if(referencedName == null)
		{
			//to store a scope where we tried to resolve this reference
			return new SuccessfulLookupResult(Collections.<DeclarationDescriptor>emptyList(), outerScope, namespaceLevel);
		}

		Set<DeclarationDescriptor> descriptors = Sets.newHashSet();
		NamespaceDescriptor namespaceDescriptor = outerScope.getNamespace(referencedName);
		if(namespaceDescriptor != null)
		{
			descriptors.add(namespaceDescriptor);
		}

		ClassifierDescriptor classifierDescriptor = outerScope.getClassifier(referencedName);
		if(classifierDescriptor != null)
		{
			descriptors.add(classifierDescriptor);
		}

		if(onlyClasses)
		{
			ClassDescriptor objectDescriptor = outerScope.getObjectDescriptor(referencedName);
			if(objectDescriptor != null)
			{
				descriptors.add(objectDescriptor);
			}
		}
		else
		{
			descriptors.addAll(outerScope.getFunctions(referencedName));
			descriptors.addAll(outerScope.getProperties(referencedName));

			VariableDescriptor localVariable = outerScope.getLocalVariable(referencedName);
			if(localVariable != null)
			{
				descriptors.add(localVariable);
			}
		}
		return new SuccessfulLookupResult(descriptors, outerScope, namespaceLevel);
	}

	@NotNull
	private Collection<? extends DeclarationDescriptor> filterAndStoreResolutionResult(@NotNull Collection<SuccessfulLookupResult> lookupResults, @NotNull NapileSimpleNameExpression referenceExpression, @NotNull BindingTrace trace, @NotNull JetScope scopeToCheckVisibility, boolean onlyClasses, boolean storeResult)
	{

		if(lookupResults.isEmpty())
		{
			return Collections.emptyList();
		}
		Collection<DeclarationDescriptor> descriptors = Sets.newLinkedHashSet();
		for(SuccessfulLookupResult lookupResult : lookupResults)
		{
			descriptors.addAll(lookupResult.descriptors);
		}

		Collection<JetScope> possibleResolutionScopes = Lists.newArrayList();
		for(SuccessfulLookupResult lookupResult : lookupResults)
		{
			if(!lookupResult.descriptors.isEmpty())
			{
				possibleResolutionScopes.add(lookupResult.resolutionScope);
			}
		}
		if(possibleResolutionScopes.isEmpty())
		{
			for(SuccessfulLookupResult lookupResult : lookupResults)
			{
				possibleResolutionScopes.add(lookupResult.resolutionScope);
			}
		}

		Collection<DeclarationDescriptor> filteredDescriptors;
		if(onlyClasses)
		{
			filteredDescriptors = Collections2.filter(descriptors, new Predicate<DeclarationDescriptor>()
			{
				@Override
				public boolean apply(@Nullable DeclarationDescriptor descriptor)
				{
					return descriptor instanceof ClassifierDescriptor || descriptor instanceof NamespaceDescriptor;
				}
			});
		}
		else
		{
			filteredDescriptors = Sets.newLinkedHashSet();
			//functions and properties can be imported if lookupResult.namespaceLevel == true
			for(SuccessfulLookupResult lookupResult : lookupResults)
			{
				if(lookupResult.namespaceLevel)
				{
					filteredDescriptors.addAll(lookupResult.descriptors);
					continue;
				}
				filteredDescriptors.addAll(Collections2.filter(lookupResult.descriptors, new Predicate<DeclarationDescriptor>()
				{
					@Override
					public boolean apply(@Nullable DeclarationDescriptor descriptor)
					{
						boolean isStatic = descriptor instanceof DeclarationDescriptorWithVisibility && ((DeclarationDescriptorWithVisibility) descriptor).isStatic();
						return descriptor instanceof NamespaceDescriptor || isStatic;
					}
				}));
			}
		}
		if(storeResult)
		{
			storeResolutionResult(descriptors, filteredDescriptors, referenceExpression, possibleResolutionScopes, trace, scopeToCheckVisibility);
		}
		return filteredDescriptors;
	}

	private void storeResolutionResult(@NotNull Collection<? extends DeclarationDescriptor> descriptors, @NotNull Collection<? extends DeclarationDescriptor> canBeImportedDescriptors, @NotNull NapileSimpleNameExpression referenceExpression, @NotNull Collection<JetScope> possibleResolutionScopes, @NotNull BindingTrace trace, @NotNull JetScope scopeToCheckVisibility)
	{

		assert canBeImportedDescriptors.size() <= descriptors.size();
		assert !possibleResolutionScopes.isEmpty();
		//todo completion here needs all possible resolution scopes, if there are many
		JetScope resolutionScope = possibleResolutionScopes.iterator().next();

		// A special case - will fill all trace information
		if(resolveClassNamespaceAmbiguity(canBeImportedDescriptors, referenceExpression, resolutionScope, trace, scopeToCheckVisibility))
		{
			return;
		}

		// Simple case of no descriptors
		if(descriptors.isEmpty())
		{
			trace.record(BindingContext.RESOLUTION_SCOPE, referenceExpression, resolutionScope);
			trace.report(UNRESOLVED_REFERENCE.on(referenceExpression));
			return;
		}

		// Decide if expression has resolved reference
		DeclarationDescriptor descriptor = null;
		if(descriptors.size() == 1)
		{
			descriptor = descriptors.iterator().next();
			assert canBeImportedDescriptors.size() <= 1;
		}
		else if(canBeImportedDescriptors.size() == 1)
		{
			descriptor = canBeImportedDescriptors.iterator().next();
		}
		if(descriptor != null)
		{
			trace.record(BindingContext.REFERENCE_TARGET, referenceExpression, descriptors.iterator().next());
			trace.record(BindingContext.RESOLUTION_SCOPE, referenceExpression, resolutionScope);

			if(descriptor instanceof DeclarationDescriptorWithVisibility)
			{
				checkVisibility((DeclarationDescriptorWithVisibility) descriptor, trace, referenceExpression, scopeToCheckVisibility);
			}
		}

		// Check for more information and additional errors
		if(canBeImportedDescriptors.isEmpty())
		{
			assert descriptors.size() >= 1;
			trace.report(CANNOT_BE_IMPORTED.on(referenceExpression, descriptors.iterator().next()));
			return;
		}
		if(canBeImportedDescriptors.size() > 1)
		{
			trace.record(BindingContext.AMBIGUOUS_REFERENCE_TARGET, referenceExpression, descriptors);
		}
	}

	/**
	 * This method tries to resolve descriptors ambiguity between class descriptor and namespace descriptor for the same class.
	 * It's ok choose class for expression reference resolution.
	 *
	 * @return <code>true</code> if method has successfully resolved ambiguity
	 */
	private boolean resolveClassNamespaceAmbiguity(@NotNull Collection<? extends DeclarationDescriptor> filteredDescriptors, @NotNull NapileSimpleNameExpression referenceExpression, @NotNull JetScope resolutionScope, @NotNull BindingTrace trace, @NotNull JetScope scopeToCheckVisibility)
	{

		if(filteredDescriptors.size() == 2)
		{
			NamespaceDescriptor namespaceDescriptor = null;
			ClassDescriptor classDescriptor = null;

			for(DeclarationDescriptor filteredDescriptor : filteredDescriptors)
			{
				if(filteredDescriptor instanceof NamespaceDescriptor)
				{
					namespaceDescriptor = (NamespaceDescriptor) filteredDescriptor;
				}
				else if(filteredDescriptor instanceof ClassDescriptor)
				{
					classDescriptor = (ClassDescriptor) filteredDescriptor;
				}
			}

			if(namespaceDescriptor != null && classDescriptor != null)
			{
				if(DescriptorUtils.getFQName(namespaceDescriptor).equalsTo(DescriptorUtils.getFQName(classDescriptor)))
				{
					trace.record(BindingContext.REFERENCE_TARGET, referenceExpression, classDescriptor);
					trace.record(BindingContext.RESOLUTION_SCOPE, referenceExpression, resolutionScope);
					checkVisibility(classDescriptor, trace, referenceExpression, scopeToCheckVisibility);
					return true;
				}
			}
		}

		return false;
	}

	private void checkVisibility(@NotNull DeclarationDescriptorWithVisibility descriptor, @NotNull BindingTrace trace, @NotNull NapileSimpleNameExpression referenceExpression, @NotNull JetScope scopeToCheckVisibility)
	{

		if(!Visibilities.isVisible(descriptor, scopeToCheckVisibility.getContainingDeclaration()))
		{
			trace.report(INVISIBLE_REFERENCE.on(referenceExpression, descriptor, descriptor.getContainingDeclaration()));
		}
	}

	private interface LookupResult
	{
		LookupResult EMPTY = new LookupResult()
		{
		};
	}

	private static class SuccessfulLookupResult implements LookupResult
	{
		final Collection<? extends DeclarationDescriptor> descriptors;
		final JetScope resolutionScope;
		final boolean namespaceLevel;

		private SuccessfulLookupResult(Collection<? extends DeclarationDescriptor> descriptors, JetScope resolutionScope, boolean namespaceLevel)
		{
			this.descriptors = descriptors;
			this.resolutionScope = resolutionScope;
			this.namespaceLevel = namespaceLevel;
		}
	}
}
