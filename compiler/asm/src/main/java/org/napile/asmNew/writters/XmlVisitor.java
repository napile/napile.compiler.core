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

package org.napile.asmNew.writters;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.napile.asm.tree.members.AnnotationNode;
import org.napile.asm.tree.members.ClassNode;
import org.napile.asm.tree.members.types.ClassTypeNode;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.asmNew.LangVersion;
import org.napile.asmNew.Modifier;

/**
 * @author VISTALL
 * @date 22:38/13.08.12
 */
public class XmlVisitor implements AsmWriter<Element>
{
	private Document document = DocumentHelper.createDocument();

	private final LangVersion langVersion;

	public XmlVisitor()
	{
		this(LangVersion.CURRENT);
	}

	public XmlVisitor(LangVersion version)
	{
		langVersion = version;
	}

	@Override
	public void visitClassNode(ClassNode classNode, Element a2)
	{
		Element element = document.addElement("class");
		element.addAttribute("version", String.valueOf(langVersion.ordinal()));
		element.addAttribute("name", classNode.name.getFqName());

		addModifiers(element, classNode.modifiers);

		if(!classNode.supers.isEmpty())
		{
			Element temp = element.addElement("supers");
			for(ClassTypeNode fqName : classNode.supers)
				fqName.accept(this, temp);
		}
	}

	@Override
	public void visitClassTypeNode(ClassTypeNode classTypeNode, Element a2)
	{
		final Element temp = a2.addElement("class_type");
		temp.addAttribute("name", classTypeNode.getClassName().getFqName());
		temp.addAttribute("nullable", String.valueOf(classTypeNode.isNullable()));

		if(!classTypeNode.getAnnotations().isEmpty())
		{
			Element temp2 = temp.addElement("annotations");
			for(AnnotationNode annotationNode : classTypeNode.getAnnotations())
				annotationNode.accept(this, temp2);
		}

		if(!classTypeNode.getTypeParameters().isEmpty())
		{
			Element temp2 = temp.addElement("type_parameters");
			for(TypeNode typeNode : classTypeNode.getTypeParameters())
				typeNode.accept(this, temp2);
		}
	}

	@Override
	public void visitAnnotationNode(AnnotationNode annotationNode, Element o)
	{
		final Element temp = o.addElement("annotation");
		temp.addAttribute("name", annotationNode.getName().getFqName());
	}

	@Override
	public void write(OutputStream stream)
	{
		OutputFormat format = OutputFormat.createPrettyPrint();
		format.setIndent("\t");
		try
		{
			XMLWriter writer = new XMLWriter(stream, format);
			writer.write(document);
			writer.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void write(Writer writer0)
	{
		OutputFormat format = OutputFormat.createPrettyPrint();
		format.setIndent("\t");
		try
		{
			XMLWriter writer = new XMLWriter(writer0, format);
			writer.write(document);
			writer.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	private static void addModifiers(Element parent, Modifier[] modifiers)
	{
		Element tag = parent.addElement("modifiers");
		for(Modifier modifier : modifiers)
			tag.addElement(modifier.name().toLowerCase());
	}
}
