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

package org.jetbrains.jet.plugin.highlighter;

import static org.jetbrains.jet.lang.diagnostics.Errors.ABSTRACT_MEMBER_NOT_IMPLEMENTED;
import static org.jetbrains.jet.lang.diagnostics.Errors.ASSIGN_OPERATOR_AMBIGUITY;
import static org.jetbrains.jet.lang.diagnostics.Errors.CONFLICTING_OVERLOADS;
import static org.jetbrains.jet.lang.diagnostics.Errors.ITERATOR_AMBIGUITY;
import static org.jetbrains.jet.lang.diagnostics.Errors.MANY_IMPL_MEMBER_NOT_IMPLEMENTED;
import static org.jetbrains.jet.lang.diagnostics.Errors.NONE_APPLICABLE;
import static org.jetbrains.jet.lang.diagnostics.Errors.OVERLOAD_RESOLUTION_AMBIGUITY;
import static org.jetbrains.jet.lang.diagnostics.Errors.RESULT_TYPE_MISMATCH;
import static org.jetbrains.jet.lang.diagnostics.Errors.RETURN_TYPE_MISMATCH_ON_OVERRIDE;
import static org.jetbrains.jet.lang.diagnostics.Errors.TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS;
import static org.jetbrains.jet.lang.diagnostics.Errors.TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH;
import static org.jetbrains.jet.lang.diagnostics.Errors.TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER;
import static org.jetbrains.jet.lang.diagnostics.Errors.TYPE_INFERENCE_TYPE_CONSTRUCTOR_MISMATCH;
import static org.jetbrains.jet.lang.diagnostics.Errors.TYPE_INFERENCE_UPPER_BOUND_VIOLATED;
import static org.jetbrains.jet.lang.diagnostics.Errors.TYPE_MISMATCH;
import static org.jetbrains.jet.lang.diagnostics.Errors.TYPE_MISMATCH_IN_FOR_LOOP;
import static org.jetbrains.jet.lang.diagnostics.Errors.UPPER_BOUND_VIOLATED;
import static org.jetbrains.jet.lang.diagnostics.Errors.VAR_OVERRIDDEN_BY_VAL;
import static org.jetbrains.jet.lang.diagnostics.Errors.WRONG_GETTER_RETURN_TYPE;
import static org.jetbrains.jet.lang.diagnostics.Errors.WRONG_SETTER_PARAMETER_TYPE;
import static org.jetbrains.jet.lang.diagnostics.rendering.Renderers.RENDER_CLASS_OR_OBJECT;
import static org.jetbrains.jet.lang.diagnostics.rendering.Renderers.TO_STRING;
import static org.jetbrains.jet.lang.diagnostics.rendering.TabledDescriptorRenderer.TextElementType;
import static org.jetbrains.jet.plugin.highlighter.HtmlTabledDescriptorRenderer.tableForTypes;
import static org.jetbrains.jet.plugin.highlighter.IdeRenderers.HTML_AMBIGUOUS_CALLS;
import static org.jetbrains.jet.plugin.highlighter.IdeRenderers.HTML_RENDER_TYPE;
import static org.jetbrains.jet.plugin.highlighter.IdeRenderers.HTML_TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS_RENDERER;
import static org.jetbrains.jet.plugin.highlighter.IdeRenderers.HTML_TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER_RENDERER;
import static org.jetbrains.jet.plugin.highlighter.IdeRenderers.HTML_TYPE_INFERENCE_TYPE_CONSTRUCTOR_MISMATCH_RENDERER;
import static org.jetbrains.jet.plugin.highlighter.IdeRenderers.HTML_TYPE_INFERENCE_UPPER_BOUND_VIOLATED_RENDERER;
import static org.jetbrains.jet.plugin.highlighter.IdeRenderers.NoneApplicableCallsRenderer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.rendering.DefaultErrorMessages;
import org.jetbrains.jet.lang.diagnostics.rendering.DiagnosticFactoryToRendererMap;
import org.jetbrains.jet.lang.diagnostics.rendering.DiagnosticRenderer;
import org.jetbrains.jet.lang.diagnostics.rendering.DispatchingDiagnosticRenderer;
import org.jetbrains.jet.lang.diagnostics.rendering.Renderer;
import org.jetbrains.jet.resolve.DescriptorRenderer;


/**
 * @author Evgeny Gerashchenko
 * @see DefaultErrorMessages
 * @since 4/13/12
 */
public class IdeErrorMessages
{
	public static final DiagnosticFactoryToRendererMap MAP = new DiagnosticFactoryToRendererMap();
	public static final DiagnosticRenderer<Diagnostic> RENDERER = new DispatchingDiagnosticRenderer(MAP, DefaultErrorMessages.MAP);

	static
	{
		MAP.put(TYPE_MISMATCH, "<html>Type mismatch.<table><tr><td>Required:</td><td>{0}</td></tr><tr><td>Found:</td><td>{1}</td></tr></table></html>", HTML_RENDER_TYPE, HTML_RENDER_TYPE);

		MAP.put(ASSIGN_OPERATOR_AMBIGUITY, "<html>Assignment operators ambiguity. All these functions match.<ul>{0}</ul></table></html>", HTML_AMBIGUOUS_CALLS);

		MAP.put(TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS, "<html>Type inference failed: {0}</html>", HTML_TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS_RENDERER);
		MAP.put(TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER, "<html>Type inference failed: {0}</html>", HTML_TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER_RENDERER);
		MAP.put(TYPE_INFERENCE_TYPE_CONSTRUCTOR_MISMATCH, "<html>Type inference failed: {0}</html>", HTML_TYPE_INFERENCE_TYPE_CONSTRUCTOR_MISMATCH_RENDERER);
		MAP.put(TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH, tableForTypes("Type inference failed. Expected type mismatch: ", "found: ", TextElementType.ERROR, "required: ", TextElementType.STRONG), HTML_RENDER_TYPE, HTML_RENDER_TYPE);
		MAP.put(TYPE_INFERENCE_UPPER_BOUND_VIOLATED, "<html>{0}</html>", HTML_TYPE_INFERENCE_UPPER_BOUND_VIOLATED_RENDERER);

		MAP.put(WRONG_SETTER_PARAMETER_TYPE, "<html>Setter parameter type must be equal to the type of the property." +
				"<table><tr><td>Expected:</td><td>{0}</td></tr>" +
				"<tr><td>Found:</td><td>{1}</td></tr></table></html>", HTML_RENDER_TYPE, HTML_RENDER_TYPE);
		MAP.put(WRONG_GETTER_RETURN_TYPE, "<html>Getter return type must be equal to the type of the property." +
				"<table><tr><td>Expected:</td><td>{0}</td></tr>" +
				"<tr><td>Found:</td><td>{1}</td></tr></table></html>", HTML_RENDER_TYPE, HTML_RENDER_TYPE);

		MAP.put(ITERATOR_AMBIGUITY, "<html>Method ''iterator()'' is ambiguous for this expression.<ul>{0}</ul></html>", HTML_AMBIGUOUS_CALLS);

		MAP.put(UPPER_BOUND_VIOLATED, "<html>Type argument is not within its bounds." +
				"<table><tr><td>Expected:</td><td>{0}</td></tr>" +
				"<tr><td>Found:</td><td>{1}</td></tr></table></html>", HTML_RENDER_TYPE, HTML_RENDER_TYPE);

		MAP.put(TYPE_MISMATCH_IN_FOR_LOOP, "<html>Loop parameter type mismatch." +
				"<table><tr><td>Iterated values:</td><td>{0}</td></tr>" +
				"<tr><td>Parameter:</td><td>{1}</td></tr></table></html>", HTML_RENDER_TYPE, HTML_RENDER_TYPE);

		MAP.put(RETURN_TYPE_MISMATCH_ON_OVERRIDE, "<html>Return type is ''{0}'', which is not a subtype of overridden<br/>" + "{1}</html>", new Renderer<CallableMemberDescriptor>()
		{
			@NotNull
			@Override
			public String render(@NotNull CallableMemberDescriptor object)
			{
				return DescriptorRenderer.HTML.renderType(object.getReturnType());
			}
		}, DescriptorRenderer.HTML);

		MAP.put(VAR_OVERRIDDEN_BY_VAL, "<html>Val-property cannot override var-property<br />" + "{1}</html>", DescriptorRenderer.HTML, DescriptorRenderer.HTML);

		MAP.put(ABSTRACT_MEMBER_NOT_IMPLEMENTED, "<html>{0} must be declared abstract or implement abstract member<br/>" + "{1}</html>", RENDER_CLASS_OR_OBJECT, DescriptorRenderer.HTML);

		MAP.put(MANY_IMPL_MEMBER_NOT_IMPLEMENTED, "<html>{0} must override {1}<br />because it inherits many implementations of it</html>", RENDER_CLASS_OR_OBJECT, DescriptorRenderer.HTML);
		MAP.put(CONFLICTING_OVERLOADS, "<html>{1}<br />is already defined in ''{0}''</html>", DescriptorRenderer.HTML, TO_STRING);

		MAP.put(RESULT_TYPE_MISMATCH, "<html>Method return type mismatch." +
				"<table><tr><td>Expected:</td><td>{1}</td></tr>" +
				"<tr><td>Found:</td><td>{2}</td></tr></table></html>", TO_STRING, HTML_RENDER_TYPE, HTML_RENDER_TYPE);

		MAP.put(OVERLOAD_RESOLUTION_AMBIGUITY, "<html>Overload resolution ambiguity. All these functions match. <ul>{0}</ul></html>", HTML_AMBIGUOUS_CALLS);
		MAP.put(NONE_APPLICABLE, "<html>None of the following functions can be called with the arguments supplied. <ul>{0}</ul></html>", new NoneApplicableCallsRenderer());

		MAP.setImmutable();
	}

	private IdeErrorMessages()
	{
	}
}
