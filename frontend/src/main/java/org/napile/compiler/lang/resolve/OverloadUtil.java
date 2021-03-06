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

import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptorImpl;

/**
 * @author Stepan Koltsov
 */
public class OverloadUtil
{

	/**
	 * Does not check names.
	 */
	public static OverloadCompatibilityInfo isOverloadable(CallableDescriptor a, CallableDescriptor b)
	{
		int abc = braceCount(a);
		int bbc = braceCount(b);

		if(abc != bbc)
		{
			return OverloadCompatibilityInfo.success();
		}

		OverridingUtil.OverrideCompatibilityInfo overrideCompatibilityInfo = OverridingUtil.isOverridableByImpl(a, b, false);
		switch(overrideCompatibilityInfo.getResult())
		{
			case OVERRIDABLE:
			case CONFLICT:
				return OverloadCompatibilityInfo.someError();
			case INCOMPATIBLE:
				return OverloadCompatibilityInfo.success();
			default:
				throw new IllegalStateException();
		}
	}

	private static int braceCount(CallableDescriptor a)
	{
		if(a instanceof VariableDescriptorImpl)
			return 0;
		else if(a instanceof MethodDescriptor)
			return 1;
		else
			throw new IllegalStateException(a.toString());
	}

	public static class OverloadCompatibilityInfo
	{

		private static final OverloadCompatibilityInfo SUCCESS = new OverloadCompatibilityInfo(true, "SUCCESS");

		public static OverloadCompatibilityInfo success()
		{
			return SUCCESS;
		}

		public static OverloadCompatibilityInfo someError()
		{
			return new OverloadCompatibilityInfo(false, "XXX");
		}


		////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		private final boolean isSuccess;
		private final String message;

		public OverloadCompatibilityInfo(boolean success, String message)
		{
			isSuccess = success;
			this.message = message;
		}

		public boolean isSuccess()
		{
			return isSuccess;
		}

		public String getMessage()
		{
			return message;
		}
	}
}
