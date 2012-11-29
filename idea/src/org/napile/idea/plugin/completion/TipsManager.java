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

package org.napile.idea.plugin.completion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.NamespaceDescriptor;
import org.napile.compiler.lang.psi.NapilePackageImpl;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileImportDirective;
import org.napile.compiler.lang.psi.NapileSimpleNameExpression;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.calls.autocasts.AutoCastServiceImpl;
import org.napile.compiler.lang.resolve.calls.autocasts.DataFlowInfo;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.resolve.scopes.JetScopeUtils;
import org.napile.compiler.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.NamespaceType;
import org.napile.compiler.lang.types.expressions.ExpressionTypingUtils;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Nikolay Krasko, Alefas
 */
public final class TipsManager
{

	private TipsManager()
	{
	}

	@NotNull
	public static Collection<DeclarationDescriptor> getReferenceVariants(NapileSimpleNameExpression expression, BindingContext context)
	{
		NapileExpression receiverExpression = expression.getReceiverExpression();
		if(receiverExpression != null)
		{
			// Process as call expression
			final JetScope resolutionScope = context.get(BindingContext.RESOLUTION_SCOPE, expression);
			final JetType expressionType = context.get(BindingContext.EXPRESSION_TYPE, receiverExpression);

			if(expressionType != null && resolutionScope != null)
			{
				if(!(expressionType instanceof NamespaceType))
				{
					ExpressionReceiver receiverDescriptor = new ExpressionReceiver(receiverExpression, expressionType);
					Set<DeclarationDescriptor> descriptors = new HashSet<DeclarationDescriptor>();

					DataFlowInfo info = context.get(BindingContext.NON_DEFAULT_EXPRESSION_DATA_FLOW, expression);
					if(info == null)
					{
						info = DataFlowInfo.EMPTY;
					}

					AutoCastServiceImpl autoCastService = new AutoCastServiceImpl(info, context);
					List<ReceiverDescriptor> variantsForExplicitReceiver = autoCastService.getVariantsForReceiver(receiverDescriptor);

					for(ReceiverDescriptor descriptor : variantsForExplicitReceiver)
					{
						descriptors.addAll(includeExternalCallableExtensions(excludePrivateDescriptors(descriptor.getType().getMemberScope().getAllDescriptors()), resolutionScope, descriptor));
					}

					return descriptors;
				}

				return includeExternalCallableExtensions(excludePrivateDescriptors(expressionType.getMemberScope().getAllDescriptors()), resolutionScope, new ExpressionReceiver(receiverExpression, expressionType));
			}
			return Collections.emptyList();
		}
		else
		{
			return getVariantsNoReceiver(expression, context);
		}
	}

	public static Collection<DeclarationDescriptor> getVariantsNoReceiver(NapileExpression expression, BindingContext context)
	{
		JetScope resolutionScope = context.get(BindingContext.RESOLUTION_SCOPE, expression);
		if(resolutionScope != null)
		{
			if(expression.getParent() instanceof NapileImportDirective || expression.getParent() instanceof NapilePackageImpl)
			{
				return excludeNonPackageDescriptors(resolutionScope.getAllDescriptors());
			}
			else
			{
				Collection<DeclarationDescriptor> descriptorsSet = Sets.newHashSet();

				ArrayList<ReceiverDescriptor> result = new ArrayList<ReceiverDescriptor>();
				resolutionScope.getImplicitReceiversHierarchy(result);

				for(ReceiverDescriptor receiverDescriptor : result)
				{
					JetType receiverType = receiverDescriptor.getType();
					descriptorsSet.addAll(receiverType.getMemberScope().getAllDescriptors());
				}

				descriptorsSet.addAll(resolutionScope.getAllDescriptors());
				return excludeNotCallableExtensions(excludePrivateDescriptors(descriptorsSet), resolutionScope);
			}
		}
		return Collections.emptyList();
	}

	@NotNull
	public static Collection<DeclarationDescriptor> getReferenceVariants(NapilePackageImpl expression, BindingContext context)
	{
		JetScope resolutionScope = context.get(BindingContext.RESOLUTION_SCOPE, expression);
		if(resolutionScope != null)
		{
			return excludeNonPackageDescriptors(resolutionScope.getAllDescriptors());
		}

		return Collections.emptyList();
	}

	public static Collection<DeclarationDescriptor> excludePrivateDescriptors(@NotNull Collection<DeclarationDescriptor> descriptors)
	{

		return Collections2.filter(descriptors, new Predicate<DeclarationDescriptor>()
		{
			@Override
			public boolean apply(@Nullable DeclarationDescriptor descriptor)
			{
				if(descriptor == null)
				{
					return false;
				}

				return true;
			}
		});
	}

	public static Collection<DeclarationDescriptor> excludeNotCallableExtensions(@NotNull Collection<? extends DeclarationDescriptor> descriptors, @NotNull final JetScope scope)
	{
		final Set<DeclarationDescriptor> descriptorsSet = Sets.newHashSet(descriptors);

		final ArrayList<ReceiverDescriptor> result = new ArrayList<ReceiverDescriptor>();
		scope.getImplicitReceiversHierarchy(result);

		descriptorsSet.removeAll(Collections2.filter(JetScopeUtils.getAllExtensions(scope), new Predicate<CallableDescriptor>()
		{
			@Override
			public boolean apply(CallableDescriptor callableDescriptor)
			{
				return false;
			}
		}));

		return Lists.newArrayList(descriptorsSet);
	}

	private static Collection<DeclarationDescriptor> excludeNonPackageDescriptors(@NotNull Collection<DeclarationDescriptor> descriptors)
	{
		return Collections2.filter(descriptors, new Predicate<DeclarationDescriptor>()
		{
			@Override
			public boolean apply(DeclarationDescriptor declarationDescriptor)
			{
				return declarationDescriptor instanceof NamespaceDescriptor;
			}
		});
	}

	private static Set<DeclarationDescriptor> includeExternalCallableExtensions(@NotNull Collection<DeclarationDescriptor> descriptors, @NotNull final JetScope externalScope, @NotNull final ReceiverDescriptor receiverDescriptor)
	{
		// It's impossible to add extension function for namespace
		JetType receiverType = receiverDescriptor.getType();
		if(receiverType instanceof NamespaceType)
		{
			return new HashSet<DeclarationDescriptor>(descriptors);
		}

		Set<DeclarationDescriptor> descriptorsSet = Sets.newHashSet(descriptors);

		descriptorsSet.addAll(Collections2.filter(JetScopeUtils.getAllExtensions(externalScope), new Predicate<CallableDescriptor>()
		{
			@Override
			public boolean apply(CallableDescriptor callableDescriptor)
			{
				return ExpressionTypingUtils.checkIsExtensionCallable(receiverDescriptor, callableDescriptor);
			}
		}));

		return descriptorsSet;
	}
}
