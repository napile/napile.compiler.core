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

package org.napile.idea.plugin.internal.codewindow;

import java.awt.BorderLayout;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.text.html.HTMLEditorKit;

import org.napile.compiler.lang.psi.NapileFile;
import org.napile.doc.lang.psi.NapileDoc;
import org.napile.idea.plugin.internal.EditorLocation;
import org.napile.idea.plugin.util.NapileDocUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Alarm;

/**
 * @author VISTALL
 * @date 10:18/31.01.13
 */
public class DocPreviewToolwindow extends JPanel implements Disposable
{
	private static final int UPDATE_DELAY = 500;
	private static final String DEFAULT_TEXT = "No doc";

	private JEditorPane editorPane;
	private final Alarm myUpdateAlarm;
	private EditorLocation myCurrentLocation;
	private final Project myProject;

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
				EditorLocation location = EditorLocation.fromEditor(FileEditorManager.getInstance(myProject).getSelectedTextEditor());
				if(!Comparing.equal(location, myCurrentLocation))
				{
					render(location, myCurrentLocation);
					myCurrentLocation = location;
				}
			}
		}, UPDATE_DELAY);
	}

	private void render(EditorLocation location, EditorLocation oldLocation)
	{
		Editor editor = location.editor;

		if(editor == null)
		{
			setText(DEFAULT_TEXT);
		}
		else
		{
			VirtualFile vFile = ((EditorEx) editor).getVirtualFile();
			if(vFile == null)
			{
				setText(DEFAULT_TEXT);
				return;
			}

			PsiFile psiFile = PsiManager.getInstance(myProject).findFile(vFile);
			if(!(psiFile instanceof NapileFile))
			{
				setText(DEFAULT_TEXT);
				return;
			}


			{
				PsiElement start = psiFile.findElementAt(location.startOffset);

				if(start == null)
					setText(DEFAULT_TEXT);
				else
				{
					NapileDoc element = PsiTreeUtil.getParentOfType(start, NapileDoc.class);
					if(element  != null)
						setText(NapileDocUtil.render(element));
				}
			}
		}
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
