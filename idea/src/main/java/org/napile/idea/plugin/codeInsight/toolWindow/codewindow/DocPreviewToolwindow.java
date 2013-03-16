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

package org.napile.idea.plugin.codeInsight.toolWindow.codewindow;

import java.awt.BorderLayout;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.text.html.HTMLEditorKit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.doc.lang.psi.NapileDoc;
import org.napile.idea.plugin.codeInsight.toolWindow.EditorLocation;
import org.napile.idea.plugin.util.LongRunningReadTask;
import org.napile.idea.plugin.util.NapileDocUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Alarm;

/**
 * @author VISTALL
 * @since 10:18/31.01.13
 */
public class DocPreviewToolwindow extends JPanel implements Disposable
{
	private static final int UPDATE_DELAY = 1000;
	private static final String DEFAULT_TEXT = "No doc";

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
			NapileFile napileFile = editorLocation.getFile();
			PsiElement start = napileFile.findElementAt(editorLocation.startOffset);

			if(start == null)
				return DEFAULT_TEXT;
			else
			{
				NapileDoc element = PsiTreeUtil.getParentOfType(start, NapileDoc.class);
				if(element  != null)
					return NapileDocUtil.render(element);
			}
			return DEFAULT_TEXT;
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

	private final JEditorPane editorPane;
	private final Alarm myUpdateAlarm;
	private final Project myProject;
	private UpdateToolWindowTask currentTask;

	public DocPreviewToolwindow(Project project)
	{
		super(new BorderLayout());
		myProject = project;

		editorPane = new JEditorPane();
		editorPane.setEditorKit(new HTMLEditorKit());
		editorPane.setEditable(false);

		add(editorPane);

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

	private void setText(final String text)
	{
		new WriteCommandAction(myProject)
		{
			@Override
			protected void run(Result result) throws Throwable
			{
				editorPane.setText(text);
			}
		}.execute();
	}

	@Override
	public void dispose()
	{

	}
}
