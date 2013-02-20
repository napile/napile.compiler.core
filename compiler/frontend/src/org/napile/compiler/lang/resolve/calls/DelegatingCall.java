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

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.psi.Call;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileTypeArgumentList;
import org.napile.compiler.lang.psi.NapileValueArgumentList;
import org.napile.compiler.lang.psi.ValueArgument;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.lang.psi.NapileTypeReference;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;

/**
 * @author abreslav
 */
public class DelegatingCall implements Call
{

	private final Call delegate;

	public DelegatingCall(@NotNull Call delegate)
	{
		this.delegate = delegate;
	}

	@Override
	@Nullable
	public ASTNode getCallOperationNode()
	{
		return delegate.getCallOperationNode();
	}

	@Override
	@NotNull
	public ReceiverDescriptor getExplicitReceiver()
	{
		return delegate.getExplicitReceiver();
	}

	@NotNull
	@Override
	public ReceiverDescriptor getThisObject()
	{
		return delegate.getThisObject();
	}

	@Override
	@Nullable
	public NapileExpression getCalleeExpression()
	{
		return delegate.getCalleeExpression();
	}

	@Override
	@Nullable
	public NapileValueArgumentList getValueArgumentList()
	{
		return delegate.getValueArgumentList();
	}

	@Override
	@NotNull
	public List<? extends ValueArgument> getValueArguments()
	{
		return delegate.getValueArguments();
	}

	@Override
	@NotNull
	public List<NapileExpression> getFunctionLiteralArguments()
	{
		return delegate.getFunctionLiteralArguments();
	}

	@Override
	@NotNull
	public List<? extends NapileTypeReference> getTypeArguments()
	{
		return delegate.getTypeArguments();
	}

	@Override
	@Nullable
	public NapileTypeArgumentList getTypeArgumentList()
	{
		return delegate.getTypeArgumentList();
	}

	@NotNull
	@Override
	public PsiElement getCallElement()
	{
		return delegate.getCallElement();
	}

	@NotNull
	@Override
	public CallType getCallType()
	{
		return delegate.getCallType();
	}

	public Call getDelegate()
	{
		return delegate;
	}
}
