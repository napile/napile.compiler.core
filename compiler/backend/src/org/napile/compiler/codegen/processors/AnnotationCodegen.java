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

package org.napile.compiler.codegen.processors;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.tree.members.AnnotableNode;
import org.napile.asm.tree.members.AnnotationNode;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.compiler.codegen.processors.codegen.CallTransformer;
import org.napile.compiler.codegen.processors.codegen.CallableMethod;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.annotations.Annotated;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.calls.ResolvedCall;

/**
 * @author VISTALL
 * @date 15:03/14.10.12
 */
public class AnnotationCodegen
{
	public static void convert(@NotNull BindingTrace bindingTrace, @NotNull Annotated annotated, @NotNull AnnotableNode<?> annotableNode)
	{
		for(AnnotationDescriptor a : annotated.getAnnotations())
			annotableNode.annotations.add(convert(bindingTrace, a));
	}

	@NotNull
	public static AnnotationNode convert(@NotNull BindingTrace bindingTrace, @NotNull AnnotationDescriptor annotationDescriptor)
	{
		assert annotationDescriptor.getResolvedCall() == null;

		ResolvedCall<ConstructorDescriptor> resolvedCall = annotationDescriptor.getResolvedCall();

		ConstructorDescriptor constructorDescriptor = resolvedCall.getResultingDescriptor();

		CallableMethod callableMethod = CallTransformer.transformToCallable(resolvedCall, false);

		TypeNode type = TypeTransformer.toAsmType(constructorDescriptor.getReturnType());

		ExpressionGenerator gen = new ExpressionGenerator(bindingTrace, type);

		gen.pushMethodArguments(resolvedCall, callableMethod.getValueParameterTypes());

		gen.getInstructs().newObject(type, callableMethod.getValueParameterTypes());
		//gen.getInstructs().returnVal();

		AnnotationNode node = new AnnotationNode();
		node.putInstructions(gen.getInstructs());
		return node;
	}
}
