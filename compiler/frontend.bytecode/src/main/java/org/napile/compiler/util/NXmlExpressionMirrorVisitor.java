/*
 * Copyright 2010-2013 napile.org
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

package org.napile.compiler.util;

import org.napile.compiler.lang.psi.NXmlElementBase;
import org.napile.compiler.lang.psi.NapileAnonymMethodExpression;
import org.napile.compiler.lang.psi.NapileBlockExpression;
import org.napile.compiler.lang.psi.NapileConstantExpression;
import org.napile.compiler.lang.psi.NapileSimpleNameExpression;
import org.napile.compiler.lang.psi.NapileVisitor;
import org.napile.compiler.lang.psi.impl.NXmlAnonymMethodExpressionImpl;
import org.napile.compiler.lang.psi.impl.NXmlBlockExpressionImpl;
import org.napile.compiler.lang.psi.impl.NXmlConstantExpressionImpl;
import org.napile.compiler.lang.psi.impl.NXmlSimpleNameExpressionImpl;

/**
 * @author VISTALL
 * @date 13:24/19.02.13
 */
public class NXmlExpressionMirrorVisitor extends NapileVisitor<NXmlElementBase, NXmlElementBase>
{
	@Override
	public NXmlElementBase visitBlockExpression(NapileBlockExpression expression, NXmlElementBase parent)
	{
		return new NXmlBlockExpressionImpl(parent);
	}

	@Override
	public NXmlElementBase visitConstantExpression(NapileConstantExpression expression, NXmlElementBase parent)
	{
		return new NXmlConstantExpressionImpl(parent);
	}

	@Override
	public NXmlElementBase visitSimpleNameExpression(NapileSimpleNameExpression expression, NXmlElementBase parent)
	{
		return new NXmlSimpleNameExpressionImpl(parent, null);
	}

	@Override
	public NXmlElementBase visitAnonymMethodExpression(NapileAnonymMethodExpression expression, NXmlElementBase parent)
	{
		return new NXmlAnonymMethodExpressionImpl(parent);
	}
}
