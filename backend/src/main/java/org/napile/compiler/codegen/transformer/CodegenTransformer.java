package org.napile.compiler.codegen.transformer;

import org.jetbrains.annotations.Nullable;
import org.napile.compiler.codegen.processors.codegen.stackValue.StackValue;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.resolve.BindingTrace;

/**
 * @author VISTALL
 * @since 19:03/12.05.13
 */
public interface CodegenTransformer
{
	@Nullable
	StackValue doTransform(BindingTrace bindingTrace, NapileExpression expression);
}
