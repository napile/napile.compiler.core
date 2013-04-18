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

package org.napile.compiler.lang.resolve.calls;

import gnu.trove.THashSet;
import gnu.trove.TObjectHashingStrategy;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.lib.NapileLangPackage;
import org.napile.compiler.lang.descriptors.CallParameterDescriptor;
import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.resolve.DescriptorUtils;
import org.napile.compiler.lang.resolve.OverridingUtil;
import org.napile.compiler.lang.types.NapileType;
import org.napile.compiler.lang.types.TypeUtils;
import org.napile.compiler.lang.types.checker.NapileTypeChecker;

/**
 * @author abreslav
 */
public class OverloadingConflictResolver
{

	@Nullable
	public <D extends CallableDescriptor> ResolvedCallWithTrace<D> findMaximallySpecific(Set<ResolvedCallWithTrace<D>> candidates, boolean discriminateGenericDescriptors)
	{
		// Different autocasts may lead to the same candidate descriptor wrapped into different ResolvedCallImpl objects
		Set<ResolvedCallWithTrace<D>> maximallySpecific = new THashSet<ResolvedCallWithTrace<D>>(new TObjectHashingStrategy<ResolvedCallWithTrace<D>>()
		{
			@Override
			public boolean equals(ResolvedCallWithTrace<D> o1, ResolvedCallWithTrace<D> o2)
			{
				return o1 == null ? o2 == null : o1.getResultingDescriptor().equals(o2.getResultingDescriptor());
			}

			@Override
			public int computeHashCode(ResolvedCallWithTrace<D> object)
			{
				return object == null ? 0 : object.getResultingDescriptor().hashCode();
			}
		});
		meLoop:
		for(ResolvedCallWithTrace<D> candidateCall : candidates)
		{
			D me = candidateCall.getResultingDescriptor();
			for(ResolvedCallWithTrace<D> otherCall : candidates)
			{
				D other = otherCall.getResultingDescriptor();
				if(other == me)
					continue;
				if(!moreSpecific(me, other, discriminateGenericDescriptors) || moreSpecific(other, me, discriminateGenericDescriptors))
				{
					continue meLoop;
				}
			}
			maximallySpecific.add(candidateCall);
		}
		if(maximallySpecific.size() == 1)
		{
			ResolvedCallWithTrace<D> result = maximallySpecific.iterator().next();
			result.getTrace().commit();
			return result;
		}
		return null;
	}

	/**
	 * Let < mean "more specific"
	 * Subtype < supertype
	 * Double < Float
	 * Int < Long
	 * Int < Short < Byte
	 */
	private <Descriptor extends CallableDescriptor> boolean moreSpecific(Descriptor f, Descriptor g, boolean discriminateGenericDescriptors)
	{
		if(discriminateGenericDescriptors && !isGeneric(f) && isGeneric(g))
			return true;
		if(OverridingUtil.overrides(f, g))
			return true;
		if(OverridingUtil.overrides(g, f))
			return false;

		List<CallParameterDescriptor> fParams = f.getValueParameters();
		List<CallParameterDescriptor> gParams = g.getValueParameters();

		int fSize = fParams.size();
		int gSize = gParams.size();

		/*boolean fIsVararg = isVariableArity(fParams);
		boolean gIsVararg = isVariableArity(gParams);

		if(!fIsVararg && gIsVararg)
			return true;
		if(fIsVararg && !gIsVararg)
			return false;

		if(!fIsVararg && !gIsVararg)
		{
			if(fSize != gSize)
				return false;

			for(int i = 0; i < fSize; i++)
			{
				CallParameterDescriptor fParam = fParams.get(i);
				CallParameterDescriptor gParam = gParams.get(i);

				JetType fParamType = fParam.getType();
				JetType gParamType = gParam.getType();

				if(!typeMoreSpecific(fParamType, gParamType))
				{
					return false;
				}
			}
		}

		if(fIsVararg && gIsVararg)
		{
			// Check matching parameters
			int minSize = Math.min(fSize, gSize);
			for(int i = 0; i < minSize - 1; i++)
			{
				CallParameterDescriptor fParam = fParams.get(i);
				CallParameterDescriptor gParam = gParams.get(i);

				JetType fParamType = fParam.getType();
				JetType gParamType = gParam.getType();

				if(!typeMoreSpecific(fParamType, gParamType))
				{
					return false;
				}
			}

			// Check the non-matching parameters of one function against the vararg parameter of the other funciton
			// Example:
			//   f(a : A, vararg vf : T)
			//   g(vararg vg : T)
			// here we check that typeof(a) < typeof(vg) and elementTypeOf(vf) < elementTypeOf(vg)
			if(fSize < gSize)
			{
				CallParameterDescriptor fParam = fParams.get(fSize - 1);
				JetType fParamType = fParam.getVarargElementType();
				assert fParamType != null : "fIsVararg guarantees this";
				for(int i = fSize - 1; i < gSize; i++)
				{
					CallParameterDescriptor gParam = gParams.get(i);
					if(!typeMoreSpecific(fParamType, gParam.getType()))
					{
						return false;
					}
				}
			}
			else
			{
				CallParameterDescriptor gParam = gParams.get(gSize - 1);
				JetType gParamType = gParam.getVarargElementType();
				assert gParamType != null : "gIsVararg guarantees this";
				for(int i = gSize - 1; i < fSize; i++)
				{
					CallParameterDescriptor fParam = fParams.get(i);
					if(!typeMoreSpecific(fParam.getType(), gParamType))
					{
						return false;
					}
				}
			}
		}   */

		if(discriminateGenericDescriptors && isGeneric(f))
		{
			if(!isGeneric(g))
			{
				return false;
			}

			// g is generic, too

			return moreSpecific(DescriptorUtils.substituteBounds(f), DescriptorUtils.substituteBounds(g), false);
		}

		return true;
	}

	private boolean isGeneric(CallableDescriptor f)
	{
		return !f.getOriginal().getTypeParameters().isEmpty();
	}

	private boolean typeMoreSpecific(@NotNull NapileType specific, @NotNull NapileType general)
	{
		return NapileTypeChecker.INSTANCE.isSubtypeOf(specific, general) || numericTypeMoreSpecific(specific, general);
	}

	private boolean numericTypeMoreSpecific(@NotNull NapileType specific, @NotNull NapileType general)
	{
		NapileType _double = TypeUtils.getTypeOfClassOrErrorType(specific.getMemberScope(), NapileLangPackage.DOUBLE, false);
		NapileType _float = TypeUtils.getTypeOfClassOrErrorType(specific.getMemberScope(), NapileLangPackage.FLOAT, false);
		NapileType _long = TypeUtils.getTypeOfClassOrErrorType(specific.getMemberScope(), NapileLangPackage.LONG, false);
		NapileType _int = TypeUtils.getTypeOfClassOrErrorType(specific.getMemberScope(), NapileLangPackage.INT, false);
		NapileType _byte = TypeUtils.getTypeOfClassOrErrorType(specific.getMemberScope(), NapileLangPackage.BYTE, false);
		NapileType _short = TypeUtils.getTypeOfClassOrErrorType(specific.getMemberScope(), NapileLangPackage.SHORT, false);

		if(TypeUtils.equalTypes(specific, _double) && TypeUtils.equalTypes(general, _float))
			return true;
		if(TypeUtils.equalTypes(specific, _int))
		{
			if(TypeUtils.equalTypes(general, _long))
				return true;
			if(TypeUtils.equalTypes(general, _byte))
				return true;
			if(TypeUtils.equalTypes(general, _short))
				return true;
		}
		if(TypeUtils.equalTypes(specific, _short) && TypeUtils.equalTypes(general, _byte))
			return true;
		return false;
	}
}
