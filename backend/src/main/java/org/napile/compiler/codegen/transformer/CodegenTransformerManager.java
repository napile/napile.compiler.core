package org.napile.compiler.codegen.transformer;

import java.util.ArrayList;
import java.util.List;

import org.napile.compiler.codegen.processors.codegen.stackValue.StackValue;
import org.napile.compiler.codegen.transformer.impl.CharConstantCodegenTransformer;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.resolve.BindingTrace;

/**
 * @author VISTALL
 * @since 19:04/12.05.13
 */
public class CodegenTransformerManager implements CodegenTransformer
{
	public static final CodegenTransformerManager INSTANCE = new CodegenTransformerManager();

	private final List<CodegenTransformer> transformers = new ArrayList<CodegenTransformer>();

	private CodegenTransformerManager()
	{
		transformers.add(new CharConstantCodegenTransformer());
	}

	@Override
	public StackValue doTransform(BindingTrace bindingTrace, NapileExpression expression)
	{
		for(CodegenTransformer transformer : transformers)
		{
			final StackValue stackValue = transformer.doTransform(bindingTrace, expression);
			if(stackValue != null)
			{
				return stackValue;
			}
		}
		return null;
	}
}
