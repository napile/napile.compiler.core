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

import org.napile.compiler.lang.psi.Call;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.calls.autocasts.DataFlowInfo;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.types.JetType;

/**
 * @author abreslav
 */
public abstract class ResolutionContext
{
	/*package*/ final BindingTrace trace;
	/*package*/ final JetScope scope;
	/*package*/ final Call call;
	/*package*/ final JetType expectedType;
	/*package*/ final DataFlowInfo dataFlowInfo;

	protected ResolutionContext(BindingTrace trace, JetScope scope, Call call, JetType expectedType, DataFlowInfo dataFlowInfo)
	{
		this.trace = trace;
		this.scope = scope;
		this.call = call;
		this.expectedType = expectedType;
		this.dataFlowInfo = dataFlowInfo;
	}

	public BasicResolutionContext toBasic()
	{
		return BasicResolutionContext.create(trace, scope, call, expectedType, dataFlowInfo);
	}
}
