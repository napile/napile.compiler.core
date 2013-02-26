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

/*
 * @author max
 */
package org.napile.idea.plugin.codeInsight.toolWindow.resolvewindow;

import static org.napile.compiler.lang.resolve.BindingContext.EXPRESSION_TYPE;
import static org.napile.compiler.lang.resolve.BindingContext.REFERENCE_TARGET;
import static org.napile.compiler.lang.resolve.BindingContext.RESOLVED_CALL;
import static org.napile.compiler.lang.resolve.calls.ResolutionDebugInfo.BOUNDS_FOR_KNOWNS;
import static org.napile.compiler.lang.resolve.calls.ResolutionDebugInfo.BOUNDS_FOR_UNKNOWNS;
import static org.napile.compiler.lang.resolve.calls.ResolutionDebugInfo.Data;
import static org.napile.compiler.lang.resolve.calls.ResolutionDebugInfo.ERRORS;
import static org.napile.compiler.lang.resolve.calls.ResolutionDebugInfo.LOG;
import static org.napile.compiler.lang.resolve.calls.ResolutionDebugInfo.NO_DEBUG_INFO;
import static org.napile.compiler.lang.resolve.calls.ResolutionDebugInfo.RESOLUTION_DEBUG_INFO;
import static org.napile.compiler.lang.resolve.calls.ResolutionDebugInfo.RESULT;
import static org.napile.compiler.lang.resolve.calls.ResolutionDebugInfo.TASKS;

import java.awt.BorderLayout;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.descriptors.CallParameterDescriptor;
import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.psi.NapileElement;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.psi.NapileReferenceExpression;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.calls.ResolutionDebugInfo;
import org.napile.compiler.lang.resolve.calls.ResolutionTask;
import org.napile.compiler.lang.resolve.calls.ResolvedCall;
import org.napile.compiler.lang.resolve.calls.ResolvedCallWithTrace;
import org.napile.compiler.lang.resolve.calls.ResolvedValueArgument;
import org.napile.compiler.lang.resolve.calls.inference.BoundsOwner;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.util.slicedmap.ReadOnlySlice;
import org.napile.compiler.util.slicedmap.WritableSlice;
import org.napile.idea.plugin.codeInsight.toolWindow.EditorLocation;
import org.napile.idea.plugin.module.ModuleAnalyzerUtil;
import org.napile.idea.plugin.util.LongRunningReadTask;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.Alarm;

/*
 * @author abreslav
 */
public class ResolveToolwindow extends JPanel implements Disposable
{
	public static final String BAR = "\n\n===\n\n";

	private static final int UPDATE_DELAY = 1000;
	private static final String DEFAULT_TEXT = "/*\n" +
			"Information about symbols resolved by\n" +
			"Napile compiler.\n" +
			"No Napile source file is opened.\n" +
			"*/";

	public class UpdateToolWindowTask extends LongRunningReadTask<EditorLocation, String>
	{
		@Nullable
		@Override
		protected EditorLocation prepareRequestInfo()
		{
			EditorLocation location = EditorLocation.fromEditor(FileEditorManager.getInstance(myProject).getSelectedTextEditor(), myProject);
			if(location.getEditor() == null || location.getFile() == null)
			{
				return null;
			}

			return location;
		}

		@NotNull
		@Override
		protected EditorLocation cloneRequestInfo(@NotNull EditorLocation location)
		{
			EditorLocation newLocation = super.cloneRequestInfo(location);
			assert location.equals(newLocation) : "cloneRequestInfo should generate same location object";
			return newLocation;
		}

		@Override
		protected void hideResultOnInvalidLocation()
		{
			setText(DEFAULT_TEXT);
		}

		@Nullable
		@Override
		protected String processRequest(@NotNull EditorLocation editorLocation)
		{
			final NapileFile psiFile = editorLocation.getFile();

			BindingContext bindingContext = ModuleAnalyzerUtil.analyze(psiFile).getBindingContext();


			PsiElement elementAtOffset;
			final int startOffset = editorLocation.startOffset;
			final int endOffset = editorLocation.endOffset;

			if(startOffset == endOffset)
			{
				elementAtOffset = PsiUtilCore.getElementAtOffset(psiFile, startOffset);
			}
			else
			{
				PsiElement start = PsiUtilCore.getElementAtOffset(psiFile, startOffset);
				PsiElement end = PsiUtilCore.getElementAtOffset(psiFile, endOffset - 1);
				elementAtOffset = PsiTreeUtil.findCommonParent(start, end);
			}

			PsiElement currentElement = elementAtOffset;


			PsiElement elementWithDebugInfo = findData(bindingContext, currentElement, RESOLUTION_DEBUG_INFO);
			if(elementWithDebugInfo != null)
			{
				return renderDebugInfo(elementWithDebugInfo, bindingContext.get(RESOLUTION_DEBUG_INFO, elementWithDebugInfo), null);
			}
			else
			{
				PsiElement elementWithResolvedCall = findData(bindingContext, currentElement, (WritableSlice) RESOLVED_CALL);
				if(elementWithResolvedCall instanceof NapileElement)
				{
					return renderDebugInfo(elementWithResolvedCall, null, bindingContext.get(RESOLVED_CALL, (NapileElement) elementWithResolvedCall));
				}
			}

			NapileExpression parentExpression = (elementAtOffset instanceof NapileExpression) ? (NapileExpression) elementAtOffset : PsiTreeUtil.getParentOfType(elementAtOffset, NapileExpression.class);
			if(parentExpression != null)
			{
				JetType type = bindingContext.get(EXPRESSION_TYPE, parentExpression);
				String text = parentExpression + "|" + parentExpression.getText() + "| : " + type;
				if(parentExpression instanceof NapileReferenceExpression)
				{
					NapileReferenceExpression referenceExpression = (NapileReferenceExpression) parentExpression;
					DeclarationDescriptor target = bindingContext.get(REFERENCE_TARGET, referenceExpression);
					text += "\nReference target: \n" + target;
				}
				return text;
			}

			return DEFAULT_TEXT;
		}

		private String printStackTraceToString(Throwable e)
		{
			StringWriter out = new StringWriter(1024);
			e.printStackTrace(new PrintWriter(out));
			return out.toString().replace("\r", "");
		}

		@Override
		protected void onResultReady(@NotNull EditorLocation requestInfo, final String resultText)
		{
			Editor editor = requestInfo.getEditor();
			assert editor != null;

			if(resultText == null)
			{
				return;
			}

			setText(resultText);
		}
	}

	private final Editor myEditor;
	private final Alarm myUpdateAlarm;

	private final Project myProject;
	private UpdateToolWindowTask currentTask;

	public ResolveToolwindow(Project project)
	{
		super(new BorderLayout());
		myProject = project;
		myEditor = EditorFactory.getInstance().createEditor(EditorFactory.getInstance().createDocument(""), project, PlainTextFileType.INSTANCE, true);
		add(myEditor.getComponent());

		myUpdateAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, this);
		myUpdateAlarm.addRequest(new Runnable()
		{
			@Override
			public void run()
			{
				myUpdateAlarm.addRequest(this, UPDATE_DELAY);
				UpdateToolWindowTask task = new UpdateToolWindowTask();
				task.init();

				if(task.shouldStart(currentTask))
				{
					currentTask = task;
					currentTask.run();
				}
			}
		}, UPDATE_DELAY);

		setText(DEFAULT_TEXT);
	}


	@Nullable
	private static <D> PsiElement findData(BindingContext bindingContext, PsiElement currentElement, ReadOnlySlice<PsiElement, D> slice)
	{
		while(currentElement != null && !(currentElement instanceof PsiFile))
		{
			if(currentElement instanceof NapileElement)
			{
				NapileElement atOffset = (NapileElement) currentElement;
				D data = bindingContext.get(slice, atOffset);
				if(data != null && data != NO_DEBUG_INFO)
				{
					return currentElement;
				}
			}
			currentElement = currentElement.getParent();
		}
		return null;
	}

	private static String renderDebugInfo(PsiElement currentElement, @Nullable ResolutionDebugInfo.Data debugInfo, @Nullable ResolvedCall<? extends CallableDescriptor> call)
	{
		StringBuilder result = new StringBuilder();

		if(debugInfo != null)
		{
			List<? extends ResolutionTask<? extends CallableDescriptor, ?>> resolutionTasks = debugInfo.get(TASKS);
			for(ResolutionTask<? extends CallableDescriptor, ?> resolutionTask : resolutionTasks)
			{
				for(ResolvedCallWithTrace<? extends CallableDescriptor> resolvedCall : resolutionTask.getResolvedCalls())
				{
					renderResolutionLogForCall(debugInfo, resolvedCall, result);
				}
			}

			call = debugInfo.get(RESULT);
		}

		if(call != null)
		{
			renderCall(result, call);
		}
		else
		{
			result.append("Resolved call is null\n");
		}
		result.append(currentElement).append(": ").append(currentElement.getText());
		return result.toString();
	}

	private static void renderResolutionLogForCall(Data debugInfo, ResolvedCallWithTrace<? extends CallableDescriptor> resolvedCall, StringBuilder result)
	{
		result.append("Trying to call ").append(resolvedCall.getCandidateDescriptor()).append("\n");
		StringBuilder errors = debugInfo.getByKey(ERRORS, resolvedCall);
		if(errors != null)
		{
			result.append("Errors: \n").append(errors).append(BAR);
		}

		StringBuilder log = debugInfo.getByKey(LOG, resolvedCall);
		if(log != null)
		{
			result.append("Log: \n").append(log).append(BAR);
		}

		Map<JetType, BoundsOwner> knowns = debugInfo.getByKey(BOUNDS_FOR_KNOWNS, resolvedCall);
		renderMap(knowns, result);
		Map<TypeParameterDescriptor, BoundsOwner> unknowns = debugInfo.getByKey(BOUNDS_FOR_UNKNOWNS, resolvedCall);
		renderMap(unknowns, result);

		result.append(BAR);
	}

	private static <K> void renderMap(Map<K, BoundsOwner> map, StringBuilder builder)
	{
		if(map == null)
			return;

		for(Map.Entry<K, BoundsOwner> entry : map.entrySet())
		{
			K key = entry.getKey();
			BoundsOwner typeValue = entry.getValue();
			builder.append("Bounds for ").append(key).append("\n");
			for(BoundsOwner bound : typeValue.getLowerBounds())
			{
				builder.append("    >: ").append(bound).append("\n");
			}
			for(BoundsOwner bound : typeValue.getUpperBounds())
			{
				builder.append("    <: ").append(bound).append("\n");
			}
		}
	}

	private static String renderCall(StringBuilder builder, ResolvedCall<? extends CallableDescriptor> resolvedCall)
	{

		CallableDescriptor resultingDescriptor = resolvedCall.getResultingDescriptor();
		ReceiverDescriptor thisObject = resolvedCall.getThisObject();
		Map<TypeParameterDescriptor, JetType> typeArguments = resolvedCall.getTypeArguments();
		Map<CallParameterDescriptor, ResolvedValueArgument> valueArguments = resolvedCall.getValueArguments();

		builder.append(resultingDescriptor.getName());
		renderTypeArguments(typeArguments, builder);

		if(resultingDescriptor instanceof MethodDescriptor)
		{
			renderValueArguments(valueArguments, builder);
		}

		builder.append(" : ").append(resultingDescriptor.getReturnType());

		builder.append("\n");
		builder.append("\n");

		CallableDescriptor candidateDescriptor = resolvedCall.getCandidateDescriptor();
		builder.append("Candidate: \n").append(candidateDescriptor).append("\n");
		if(resultingDescriptor != candidateDescriptor)
		{
			builder.append("Result: \n").append(resultingDescriptor).append("\n");
		}

		builder.append("This object: \n").append(thisObject).append("\n");
		builder.append("Type args: \n").append(typeArguments).append("\n");
		builder.append("Value args: \n").append(valueArguments).append("\n");

		return builder.toString();
	}

	private static void renderValueArguments(Map<CallParameterDescriptor, ResolvedValueArgument> valueArguments, StringBuilder builder)
	{
		ResolvedValueArgument[] args = new ResolvedValueArgument[valueArguments.size()];
		for(Map.Entry<CallParameterDescriptor, ResolvedValueArgument> entry : valueArguments.entrySet())
		{
			CallParameterDescriptor key = entry.getKey();
			ResolvedValueArgument value = entry.getValue();

			args[key.getIndex()] = value;
		}
		builder.append("(");
		for(int i = 0, argsLength = args.length; i < argsLength; i++)
		{
			ResolvedValueArgument arg = args[i];
			builder.append(arg);
			if(i != argsLength - 1)
			{
				builder.append(", ");
			}
		}
		builder.append(")");
	}

	private static void renderTypeArguments(Map<TypeParameterDescriptor, JetType> typeArguments, StringBuilder builder)
	{
		JetType[] args = new JetType[typeArguments.size()];
		for(Map.Entry<TypeParameterDescriptor, JetType> entry : typeArguments.entrySet())
		{
			TypeParameterDescriptor key = entry.getKey();
			JetType value = entry.getValue();
			args[key.getIndex()] = value;
		}
		builder.append("<");
		for(int i = 0, argsLength = args.length; i < argsLength; i++)
		{
			JetType type = args[i];
			builder.append(type);
			if(i != argsLength - 1)
			{
				builder.append(", ");
			}
		}
		builder.append(">");
	}

	private static void renderReceiver(ReceiverDescriptor receiverArgument, ReceiverDescriptor thisObject, StringBuilder builder)
	{
		if(receiverArgument.exists())
		{
			builder.append("/").append(receiverArgument);
		}

		if(thisObject.exists())
		{
			builder.append("/this=").append(thisObject);
		}

		if(thisObject.exists() || receiverArgument.exists())
		{
			builder.append("/.");
		}
	}

	private void setText(final String text)
	{
		new WriteCommandAction(myProject)
		{
			@Override
			protected void run(Result result) throws Throwable
			{
				myEditor.getDocument().setText(text);
			}
		}.execute();
	}

	@Override
	public void dispose()
	{
		EditorFactory.getInstance().releaseEditor(myEditor);
	}
}
