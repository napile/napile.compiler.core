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

package org.napile.compiler.lang.descriptors;

import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.resolve.name.FqNameUnsafe;
import org.napile.compiler.lang.resolve.DescriptorUtils;
import org.napile.compiler.lexer.NapileTokens;
import org.napile.compiler.lexer.NapileKeywordToken;
import com.google.common.collect.Sets;

/**
 * @author svtk
 */
public enum  Visibility
{
	LOCAL(NapileTokens.LOCAL_KEYWORD, false)
	{
		@Override
		protected boolean isVisible(@NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from)
		{
			DeclarationDescriptor parent = what;
			while(parent != null)
			{
				parent = parent.getContainingDeclaration();
				if((parent instanceof ClassDescriptor && !DescriptorUtils.isClassObject(parent)) || parent instanceof NamespaceDescriptor)
				{
					break;
				}
			}
			DeclarationDescriptor fromParent = from;
			while(fromParent != null)
			{
				if(parent == fromParent)
				{
					return true;
				}
				fromParent = fromParent.getContainingDeclaration();
			}
			return false;
		}
	},

	COVERED(NapileTokens.COVERED_KEYWORD, true)
	{
		@Override
		protected boolean isVisible(@NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from)
		{
			ClassDescriptor classDescriptor = DescriptorUtils.getParentOfType(what, ClassDescriptor.class);
			if(classDescriptor == null)
				return false;

			ClassDescriptor fromClass = DescriptorUtils.getParentOfType(from, ClassDescriptor.class, false);
			if(fromClass == null)
				return false;

			FqNameUnsafe p1 = DescriptorUtils.getFQName(classDescriptor).parent();
			FqNameUnsafe p2 = DescriptorUtils.getFQName(fromClass).parent();
			if(p1.isRoot() && p2.isRoot())
				return true;

			return p2.getFqName().startsWith(p1.getFqName());
		}
	},

	HERITABLE(NapileTokens.HERITABLE_KEYWORD, true)
	{
		@Override
		protected boolean isVisible(@NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from)
		{
			ClassDescriptor classDescriptor = DescriptorUtils.getParentOfType(what, ClassDescriptor.class);
			if(classDescriptor == null)
				return false;

			ClassDescriptor fromClass = DescriptorUtils.getParentOfType(from, ClassDescriptor.class, false);
			if(fromClass == null)
				return false;
			if(DescriptorUtils.isSubclass(fromClass, classDescriptor))
			{
				return true;
			}
			return isVisible(what, fromClass.getContainingDeclaration());
		}
	},

	PUBLIC(null, true)
	{
		@Override
		protected boolean isVisible(@NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from)
		{
			return true;
		}
	},

	@Deprecated
	LOCAL2(null, false)
	{
		@Override
		protected boolean isVisible(@NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from)
		{
			throw new IllegalStateException(); //This method shouldn't be invoked for LOCAL visibility
		}
	},

	@Deprecated
	INHERITED(null, false)
	{
		@Override
		protected boolean isVisible(@NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from)
		{
			throw new IllegalStateException("Visibility is unknown yet"); //This method shouldn't be invoked for INHERITED visibility
		}
	},

	/* Visibility for fake override invisible members (they are created for better error reporting) */
	INVISIBLE_FAKE(null, false)
	{
		@Override
		protected boolean isVisible(@NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from)
		{
			return false;
		}
	};

	public static final Set<Visibility> INTERNAL_VISIBILITIES = Sets.newHashSet(LOCAL, LOCAL2);
	private final boolean isPublicAPI;
	private final NapileKeywordToken keyword;

	Visibility(@Nullable NapileKeywordToken keyword, boolean isPublicAPI)
	{
		this.isPublicAPI = isPublicAPI;
		this.keyword = keyword;
	}

	public boolean isPublicAPI()
	{
		return isPublicAPI;
	}

	// if it call it always not null
	@NotNull
	public NapileKeywordToken getKeyword()
	{
		return keyword;
	}

	@Override
	public String toString()
	{
		return keyword == null ? "" : keyword.toString();
	}

	protected abstract boolean isVisible(@NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from);
}
