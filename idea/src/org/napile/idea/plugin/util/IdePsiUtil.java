package org.napile.idea.plugin.util;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.lib.NapileAnnotationPackage;
import org.napile.compiler.analyzer.AnalyzeExhaust;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.MutableClassDescriptor;
import org.napile.compiler.lang.psi.NapileClassLike;
import org.napile.compiler.lang.psi.NapileDeclaration;
import org.napile.compiler.lang.resolve.AnnotationUtils;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.util.RunUtil;
import org.napile.idea.plugin.project.WholeProjectAnalyzerFacade;

/**
 * @author VISTALL
 * @date 14:23/08.10.12
 */
public class IdePsiUtil extends RunUtil
{
	public static boolean isDeprecated(@NotNull NapileDeclaration declaration)
	{
		AnalyzeExhaust analyzeExhaust = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile(declaration.getContainingFile());
		DeclarationDescriptor descriptor = analyzeExhaust.getBindingContext().get(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration);
		if(descriptor == null)
			return false;
		return AnnotationUtils.hasAnnotation(descriptor, NapileAnnotationPackage.DEPRECATED);
	}

	public static boolean hasClassPoint(@NotNull NapileClassLike classLike)
	{
		AnalyzeExhaust analyzeExhaust = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile(classLike.getContainingFile());
		MutableClassDescriptor descriptor = (MutableClassDescriptor) analyzeExhaust.getBindingContext().get(BindingContext.DECLARATION_TO_DESCRIPTOR, classLike);
		if(descriptor == null)
			return false;

		for(MethodDescriptor methodDescriptor : descriptor.getMethods())
			if(isRunPoint(methodDescriptor))
				return true;
		return false;
	}
}
