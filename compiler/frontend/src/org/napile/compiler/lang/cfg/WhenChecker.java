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

package org.napile.compiler.lang.cfg;

import java.util.Collection;
import java.util.Collections;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.ClassKind;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileExpressionPattern;
import org.napile.compiler.lang.psi.NapileWhenCondition;
import org.napile.compiler.lang.psi.NapileWhenConditionWithExpression;
import org.napile.compiler.lang.psi.NapileWhenEntry;
import org.napile.compiler.lang.psi.NapileWhenExpression;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.types.JetType;

/**
 * @author svtk
 */
public class WhenChecker
{
	public static boolean isWhenExhaustive(@NotNull NapileWhenExpression expression, @NotNull BindingTrace trace)
	{
		NapileExpression subjectExpression = expression.getSubjectExpression();
		if(subjectExpression == null)
			return false;
		JetType type = trace.get(BindingContext.EXPRESSION_TYPE, subjectExpression);
		if(type == null)
			return false;
		DeclarationDescriptor declarationDescriptor = type.getConstructor().getDeclarationDescriptor();
		if(!(declarationDescriptor instanceof ClassDescriptor))
			return false;
		ClassDescriptor classDescriptor = (ClassDescriptor) declarationDescriptor;
		if(classDescriptor.getKind() != ClassKind.ENUM_CLASS || classDescriptor.getModality().isOverridable())
			return false;

		JetScope memberScope = classDescriptor.getMemberScope(Collections.<JetType>emptyList());
		Collection<ClassDescriptor> objectDescriptors = memberScope.getObjectDescriptors();
		boolean isExhaust = true;
		boolean notEmpty = false;
		for(ClassDescriptor descriptor : objectDescriptors)
		{
			if(descriptor.getKind() == ClassKind.ENUM_ENTRY)
			{
				notEmpty = true;
				if(!containsEnumEntryCase(expression, descriptor, trace))
				{
					isExhaust = false;
				}
			}
		}
		return isExhaust && notEmpty;
	}

	private static boolean containsEnumEntryCase(@NotNull NapileWhenExpression whenExpression, @NotNull ClassDescriptor enumEntry, @NotNull BindingTrace trace)
	{
		assert enumEntry.getKind() == ClassKind.ENUM_ENTRY;
		for(NapileWhenEntry whenEntry : whenExpression.getEntries())
		{
			for(NapileWhenCondition condition : whenEntry.getConditions())
			{
				if(condition instanceof NapileWhenConditionWithExpression)
				{
					NapileExpressionPattern pattern = ((NapileWhenConditionWithExpression) condition).getPattern();
					if(pattern == null)
						continue;
					NapileExpression patternExpression = pattern.getExpression();
					JetType type = trace.get(BindingContext.EXPRESSION_TYPE, patternExpression);
					if(type == null)
						continue;
					if(type.getConstructor().getDeclarationDescriptor() == enumEntry)
					{
						return true;
					}
				}
			}
		}
		return false;
	}
}
