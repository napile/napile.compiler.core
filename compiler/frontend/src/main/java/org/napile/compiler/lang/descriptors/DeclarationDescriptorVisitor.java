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

package org.napile.compiler.lang.descriptors;

/**
 * @author Stepan Koltsov
 */
public interface DeclarationDescriptorVisitor<R, D>
{
	R visitNamespaceDescriptor(PackageDescriptor descriptor, D data);

	R visitVariableDescriptor(VariableDescriptor descriptor, D data);

	R visitFunctionDescriptor(MethodDescriptor descriptor, D data);

	R visitTypeParameterDescriptor(TypeParameterDescriptor descriptor, D data);

	R visitClassDescriptor(ClassDescriptor descriptor, D data);

	R visitModuleDeclaration(ModuleDescriptor descriptor, D data);

	R visitConstructorDescriptor(ConstructorDescriptor constructorDescriptor, D data);

	R visitLocalVariableDescriptor(LocalVariableDescriptor descriptor, D data);

	R visitPropertyDescriptor(VariableDescriptorImpl descriptor, D data);

	R visitCallParameterAsVariableDescriptor(CallParameterDescriptor descriptor, D data);

	R visitCallParameterAsReferenceDescriptor(CallParameterAsReferenceDescriptorImpl descriptor, D data);
}