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

package org.napile.idea.plugin.actions;

import java.io.IOException;

import org.napile.compiler.NapileFileType;
import org.napile.java2napile.converter.Java2NapileConvertAction;
import org.napile.java2napile.converter.Java2NapileConverter;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;

/**
 * @author VISTALL
 * @since 18:56/04.01.13
 */
public class ConvertJavaToNapileAction extends AnAction
{
	@Override
	public void actionPerformed(AnActionEvent e)
	{
		VirtualFile[] virtualFiles = VcsUtil.getVirtualFiles(e);

		Java2NapileConverter converter = new Java2NapileConverter(e.getProject());

		for(VirtualFile virtualFile : virtualFiles)
		{
			converter.convert(virtualFile, new Java2NapileConvertAction()
			{
				@Override
				public void storeData(final VirtualFile file, final StringBuilder builder)
				{
					ApplicationManager.getApplication().runWriteAction(new Runnable()
					{
						@Override
						public void run()
						{
							try
							{
								VirtualFile newFile = file.getParent().findOrCreateChildData(null, file.getNameWithoutExtension() + "." + NapileFileType.INSTANCE.getDefaultExtension());
								newFile.setBinaryContent(builder.toString().getBytes());
							}
							catch(IOException e1)
							{
								e1.printStackTrace();
							}
						}
					});
				}
			});
		}
	}
}
