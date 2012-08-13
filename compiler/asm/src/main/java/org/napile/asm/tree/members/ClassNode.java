/***
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.napile.asm.tree.members;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.jet.lang.resolve.name.FqName;
import org.napile.asm.AnnotationVisitor;
import org.napile.asm.Attribute;
import org.napile.asm.FieldVisitor;
import org.napile.asm.MethodVisitor;
import org.napile.asm.Opcodes;
import org.napile.asm.tree.OldAnnotationNode;
import org.napile.asm.tree.InnerClassNode;
import org.napile.asm.tree.members.types.ClassTypeNode;
import org.napile.asmNew.Modifier;
import org.napile.asmNew.visitors.AsmVisitor;
import com.sun.istack.internal.NotNull;

/**
 * A node that represents a class.
 *
 * @author Eric Bruneton
 * @author VISTALL
 */
public class ClassNode implements AsmNode
{
	/**
	 * The class's access flags (see {@link org.napile.asm.Opcodes}). This
	 * field also indicates if the class is deprecated.
	 */
	@NotNull
	public Modifier[] modifiers = Modifier.EMPTY;

	/**
	 * The internal name of the class (see
	 * {@link org.napile.asm.Type#getInternalName() getInternalName}).
	 */
	public FqName name;

	/**
	 * The signature of the class. Mayt be <tt>null</tt>.
	 */
	public String signature;

	/**
	 * The internal of name of the super classes
	 */
	@NotNull
	public final List<ClassTypeNode> supers = new ArrayList<ClassTypeNode>(1);

	/**
	 * The name of the source file from which this class was compiled. May be
	 * <tt>null</tt>.
	 */
	public String sourceFile;

	/**
	 * Debug information to compute the correspondance between source and
	 * compiled elements of the class. May be <tt>null</tt>.
	 */
	public String sourceDebug;

	/**
	 * The internal name of the enclosing class of the class. May be
	 * <tt>null</tt>.
	 */
	public String outerClass;

	/**
	 * The name of the method that contains the class, or <tt>null</tt> if the
	 * class is not enclosed in a method.
	 */
	public String outerMethod;

	/**
	 * The descriptor of the method that contains the class, or <tt>null</tt>
	 * if the class is not enclosed in a method.
	 */
	public String outerMethodDesc;

	/**
	 * The runtime visible annotations of this class. This list is a list of
	 * {@link org.napile.asm.tree.OldAnnotationNode} objects. May be <tt>null</tt>.
	 *
	 * @associates OldAnnotationNode
	 * @label visible
	 */
	public List<OldAnnotationNode> visibleAnnotations;

	/**
	 * The runtime invisible annotations of this class. This list is a list of
	 * {@link org.napile.asm.tree.OldAnnotationNode} objects. May be <tt>null</tt>.
	 *
	 * @associates OldAnnotationNode
	 * @label invisible
	 */
	public List<OldAnnotationNode> invisibleAnnotations;

	/**
	 * The non standard attributes of this class. This list is a list of
	 * {@link Attribute} objects. May be <tt>null</tt>.
	 *
	 * @associates Attribute
	 */
	public List<Attribute> attrs;

	/**
	 * Informations about the inner classes of this class. This list is a list
	 * of {@link org.napile.asm.tree.InnerClassNode} objects.
	 *
	 * @associates InnerClassNode
	 */
	public final List<InnerClassNode> innerClasses = new ArrayList<InnerClassNode>(0);

	/**
	 * The fields of this class. This list is a list of {@link org.napile.asm.tree.members.FieldNode}
	 * objects.
	 *
	 * @associates FieldNode
	 */
	public final List<FieldNode> fields = new ArrayList<FieldNode>(0);

	/**
	 * The methods of this class. This list is a list of {@link org.napile.asm.tree.members.MethodNode}
	 * objects.
	 *
	 * @associates MethodNode
	 */
	public final List<MethodNode> methods = new ArrayList<MethodNode>(0);

	/**
	 * Constructs a new {@link ClassNode}. <i>Subclasses must not use this
	 * constructor</i>. Instead, they must use the {@link #ClassNode(int)}
	 * version.
	 */
	public ClassNode()
	{
		//
	}

	public void visit(@NotNull final Modifier[] access, @NotNull final String name, final String signature)
	{
		visit(access, new FqName(name), signature);
	}

	public void visit(@NotNull final Modifier[] access, @NotNull final FqName name, final String signature)
	{
		this.modifiers = access;
		this.name = name;
		this.signature = signature;
	}

	public void visitSuper(@NotNull ClassTypeNode typeNode)
	{
		supers.add(typeNode);
	}

	public void visitSource(final String file, final String debug)
	{
		sourceFile = file;
		sourceDebug = debug;
	}

	public void visitOuterClass(final String owner, final String name, final String desc)
	{
		outerClass = owner;
		outerMethod = name;
		outerMethodDesc = desc;
	}

	public AnnotationVisitor visitAnnotation(final String desc, final boolean visible)
	{
		OldAnnotationNode an = new OldAnnotationNode(desc);
		if(visible)
		{
			if(visibleAnnotations == null)
			{
				visibleAnnotations = new ArrayList<OldAnnotationNode>(1);
			}
			visibleAnnotations.add(an);
		}
		else
		{
			if(invisibleAnnotations == null)
			{
				invisibleAnnotations = new ArrayList<OldAnnotationNode>(1);
			}
			invisibleAnnotations.add(an);
		}
		return an;
	}

	public void visitAttribute(final Attribute attr)
	{
		if(attrs == null)
		{
			attrs = new ArrayList<Attribute>(1);
		}
		attrs.add(attr);
	}

	public void visitInnerClass(final String name, final String outerName, final String innerName, final int access)
	{
		InnerClassNode icn = new InnerClassNode(name, outerName, innerName, access);
		innerClasses.add(icn);
	}

	public FieldVisitor visitField(final int access, final String name, final String desc, final String signature, final Object value)
	{
		FieldNode fn = new FieldNode(access, name, desc, signature, value);
		fields.add(fn);
		return fn;
	}

	public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions)
	{
		MethodNode mn = new MethodNode(access, name, desc, signature, exceptions);
		methods.add(mn);
		return mn;
	}

	public void visitEnd()
	{
	}

	// ------------------------------------------------------------------------
	// Accept method
	// ------------------------------------------------------------------------

	/**
	 * Checks that this class node is compatible with the given ASM API version.
	 * This methods checks that this node, and all its nodes recursively, do not
	 * contain elements that were introduced in more recent versions of the ASM
	 * API than the given version.
	 *
	 * @param api an ASM API version. Must be one of {@link Opcodes#ASM4}.
	 */
	public void check(final int api)
	{
		// nothing to do
	}

	@Override
	public <T> void accept(final AsmVisitor<T> asmVisitor, T a2)
	{
		asmVisitor.visitClassNode(this, a2);
	}
}
