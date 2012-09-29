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

package org.napile.compiler.codegen.processors.codegen;

import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntIterator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.napile.asm.tree.members.types.TypeNode;
import org.napile.compiler.codegen.processors.Triple;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import com.google.common.collect.Lists;

/**
 * @author max
 */
public class FrameMap
{
	private final TObjectIntHashMap<DeclarationDescriptor> myVarIndex = new TObjectIntHashMap<DeclarationDescriptor>();
	private final TObjectIntHashMap<DeclarationDescriptor> myVarSizes = new TObjectIntHashMap<DeclarationDescriptor>();
	private int myMaxIndex = 0;

	public int enter(DeclarationDescriptor descriptor, TypeNode type)
	{
		int index = myMaxIndex;
		myVarIndex.put(descriptor, index);
		myMaxIndex += 1;
		myVarSizes.put(descriptor, 1);
		return index;
	}

	public int leave(DeclarationDescriptor descriptor)
	{
		int size = myVarSizes.get(descriptor);
		myMaxIndex -= size;
		myVarSizes.remove(descriptor);
		int oldIndex = myVarIndex.remove(descriptor);
		if(oldIndex != myMaxIndex)
		{
			throw new IllegalStateException("descriptor can be left only if it is last");
		}
		return oldIndex;
	}

	public int enterTemp(TypeNode type)
	{
		int result = myMaxIndex;
		myMaxIndex += 1;
		return result;
	}

	public void leaveTemp(TypeNode type)
	{
		myMaxIndex -= 1;
	}

	public int getIndex(DeclarationDescriptor descriptor)
	{
		return myVarIndex.contains(descriptor) ? myVarIndex.get(descriptor) : -1;
	}

	public Mark mark()
	{
		return new Mark(myMaxIndex);
	}

	public class Mark
	{
		private final int myIndex;

		public Mark(int index)
		{
			myIndex = index;
		}

		public void dropTo()
		{
			List<DeclarationDescriptor> descriptorsToDrop = new ArrayList<DeclarationDescriptor>();
			TObjectIntIterator<DeclarationDescriptor> iterator = myVarIndex.iterator();
			while(iterator.hasNext())
			{
				iterator.advance();
				if(iterator.value() >= myIndex)
				{
					descriptorsToDrop.add(iterator.key());
				}
			}
			for(DeclarationDescriptor declarationDescriptor : descriptorsToDrop)
			{
				myVarIndex.remove(declarationDescriptor);
				myVarSizes.remove(declarationDescriptor);
			}
			myMaxIndex = myIndex;
		}
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		if(myVarIndex.size() != myVarSizes.size())
		{
			return "inconsistent";
		}

		List<Triple<DeclarationDescriptor, Integer, Integer>> descriptors = Lists.newArrayList();

		for(Object descriptor0 : myVarIndex.keys())
		{
			DeclarationDescriptor descriptor = (DeclarationDescriptor) descriptor0;
			int varIndex = myVarIndex.get(descriptor);
			int varSize = myVarSizes.get(descriptor);
			descriptors.add(new Triple<DeclarationDescriptor, Integer, Integer>(descriptor, varIndex, varSize));
		}

		Collections.sort(descriptors, new Comparator<Triple<DeclarationDescriptor, Integer, Integer>>()
		{
			@Override
			public int compare(Triple<DeclarationDescriptor, Integer, Integer> left, Triple<DeclarationDescriptor, Integer, Integer> right)
			{
				return left.b - right.b;
			}
		});

		sb.append("size=").append(myMaxIndex);

		boolean first = true;
		for(Triple<DeclarationDescriptor, Integer, Integer> t : descriptors)
		{
			if(!first)
			{
				sb.append(", ");
			}
			first = false;
			sb.append(t.a).append(",i=").append(t.b).append(",s=").append(t.c);
		}

		return sb.toString();
	}
}
