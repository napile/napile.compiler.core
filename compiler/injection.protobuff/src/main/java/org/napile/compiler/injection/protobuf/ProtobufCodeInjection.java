/*
 * Copyright 2010-2012 napile.org
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

package org.napile.compiler.injection.protobuf;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.lib.NapileLangPackage;
import org.napile.compiler.injection.CodeInjection;
import org.napile.compiler.injection.protobuf.lexer.ProtobufLexer;
import org.napile.compiler.injection.protobuf.lexer.ProtobufNodeTokens;
import org.napile.compiler.injection.protobuf.psi.ProtobufMessageBlock;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.TypeUtils;
import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;

/**
 * @author VISTALL
 * @date 11:04/12.10.12
 */
public class ProtobufCodeInjection extends CodeInjection
{
	@Override
	public void parse(@NotNull PsiBuilder builder)
	{
		PsiBuilder.Marker marker = builder.mark();
		builder.advanceLexer();
		marker.done(ProtobufNodeTokens.MESSAGE_BLOCK);
	}

	@NotNull
	@Override
	public String getName()
	{
		return "protobuf";
	}

	@NotNull
	@Override
	public JetType getReturnType(@Nullable JetType expectType, @NotNull BindingTrace bindingTrace, @NotNull JetScope jetScope)
	{
		return TypeUtils.getTypeOfClassOrErrorType(jetScope, NapileLangPackage.ANY);
	}

	@NotNull
	@Override
	public Lexer createLexer(Project project)
	{
		return new ProtobufLexer();
	}

	@NotNull
	@Override
	public PsiElement createElement(ASTNode astNode)
	{
		return new ProtobufMessageBlock(astNode);
	}
}
