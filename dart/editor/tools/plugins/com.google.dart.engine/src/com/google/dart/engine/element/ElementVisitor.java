/*
 * Copyright (c) 2013, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.engine.element;

/**
 * The interface {@code ElementVisitor} defines the behavior of objects that can be used to visit an
 * element structure.
 * 
 * @coverage dart.engine.element
 */
public interface ElementVisitor<R> {
  public R visitClassElement(ClassElement element);

  public R visitCompilationUnitElement(CompilationUnitElement element);

  public R visitConstructorElement(ConstructorElement element);

  public R visitEmbeddedHtmlScriptElement(EmbeddedHtmlScriptElement element);

  public R visitExportElement(ExportElement element);

  public R visitExternalHtmlScriptElement(ExternalHtmlScriptElement element);

  public R visitFieldElement(FieldElement element);

  public R visitFieldFormalParameterElement(FieldFormalParameterElement element);

  public R visitFunctionElement(FunctionElement element);

  public R visitFunctionTypeAliasElement(FunctionTypeAliasElement element);

  public R visitHtmlElement(HtmlElement element);

  public R visitImportElement(ImportElement element);

  public R visitLabelElement(LabelElement element);

  public R visitLibraryElement(LibraryElement element);

  public R visitLocalVariableElement(LocalVariableElement element);

  public R visitMethodElement(MethodElement element);

  public R visitMultiplyDefinedElement(MultiplyDefinedElement element);

  public R visitParameterElement(ParameterElement element);

  public R visitPrefixElement(PrefixElement element);

  public R visitPropertyAccessorElement(PropertyAccessorElement element);

  public R visitTopLevelVariableElement(TopLevelVariableElement element);

  public R visitTypeVariableElement(TypeVariableElement element);
}
