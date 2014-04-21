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
package com.google.dart.engine.element.visitor;

import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.ElementVisitor;
import com.google.dart.engine.element.EmbeddedHtmlScriptElement;
import com.google.dart.engine.element.ExportElement;
import com.google.dart.engine.element.ExternalHtmlScriptElement;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.FieldFormalParameterElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.FunctionTypeAliasElement;
import com.google.dart.engine.element.HtmlElement;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LabelElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.LocalVariableElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.MultiplyDefinedElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PrefixElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.TopLevelVariableElement;
import com.google.dart.engine.element.TypeParameterElement;
import com.google.dart.engine.element.angular.AngularComponentElement;
import com.google.dart.engine.element.angular.AngularControllerElement;
import com.google.dart.engine.element.angular.AngularDecoratorElement;
import com.google.dart.engine.element.angular.AngularFormatterElement;
import com.google.dart.engine.element.angular.AngularPropertyElement;
import com.google.dart.engine.element.angular.AngularScopePropertyElement;
import com.google.dart.engine.element.angular.AngularSelectorElement;
import com.google.dart.engine.element.angular.AngularViewElement;
import com.google.dart.engine.element.polymer.PolymerAttributeElement;
import com.google.dart.engine.element.polymer.PolymerTagDartElement;
import com.google.dart.engine.element.polymer.PolymerTagHtmlElement;

/**
 * Instances of the class {@code SimpleElementVisitor} implement an element visitor that will do
 * nothing when visiting an element. It is intended to be a superclass for classes that use the
 * visitor pattern primarily as a dispatch mechanism (and hence don't need to recursively visit a
 * whole structure) and that only need to visit a small number of element types.
 * 
 * @coverage dart.engine.element
 */
public class SimpleElementVisitor<R> implements ElementVisitor<R> {
  @Override
  public R visitAngularComponentElement(AngularComponentElement element) {
    return null;
  }

  @Override
  public R visitAngularControllerElement(AngularControllerElement element) {
    return null;
  }

  @Override
  public R visitAngularDirectiveElement(AngularDecoratorElement element) {
    return null;
  }

  @Override
  public R visitAngularFormatterElement(AngularFormatterElement element) {
    return null;
  }

  @Override
  public R visitAngularPropertyElement(AngularPropertyElement element) {
    return null;
  }

  @Override
  public R visitAngularScopePropertyElement(AngularScopePropertyElement element) {
    return null;
  }

  @Override
  public R visitAngularSelectorElement(AngularSelectorElement element) {
    return null;
  }

  @Override
  public R visitAngularViewElement(AngularViewElement element) {
    return null;
  }

  @Override
  public R visitClassElement(ClassElement element) {
    return null;
  }

  @Override
  public R visitCompilationUnitElement(CompilationUnitElement element) {
    return null;
  }

  @Override
  public R visitConstructorElement(ConstructorElement element) {
    return null;
  }

  @Override
  public R visitEmbeddedHtmlScriptElement(EmbeddedHtmlScriptElement element) {
    return null;
  }

  @Override
  public R visitExportElement(ExportElement element) {
    return null;
  }

  @Override
  public R visitExternalHtmlScriptElement(ExternalHtmlScriptElement element) {
    return null;
  }

  @Override
  public R visitFieldElement(FieldElement element) {
    return null;
  }

  @Override
  public R visitFieldFormalParameterElement(FieldFormalParameterElement element) {
    return null;
  }

  @Override
  public R visitFunctionElement(FunctionElement element) {
    return null;
  }

  @Override
  public R visitFunctionTypeAliasElement(FunctionTypeAliasElement element) {
    return null;
  }

  @Override
  public R visitHtmlElement(HtmlElement element) {
    return null;
  }

  @Override
  public R visitImportElement(ImportElement element) {
    return null;
  }

  @Override
  public R visitLabelElement(LabelElement element) {
    return null;
  }

  @Override
  public R visitLibraryElement(LibraryElement element) {
    return null;
  }

  @Override
  public R visitLocalVariableElement(LocalVariableElement element) {
    return null;
  }

  @Override
  public R visitMethodElement(MethodElement element) {
    return null;
  }

  @Override
  public R visitMultiplyDefinedElement(MultiplyDefinedElement element) {
    return null;
  }

  @Override
  public R visitParameterElement(ParameterElement element) {
    return null;
  }

  @Override
  public R visitPolymerAttributeElement(PolymerAttributeElement element) {
    return null;
  }

  @Override
  public R visitPolymerTagDartElement(PolymerTagDartElement element) {
    return null;
  }

  @Override
  public R visitPolymerTagHtmlElement(PolymerTagHtmlElement element) {
    return null;
  }

  @Override
  public R visitPrefixElement(PrefixElement element) {
    return null;
  }

  @Override
  public R visitPropertyAccessorElement(PropertyAccessorElement element) {
    return null;
  }

  @Override
  public R visitTopLevelVariableElement(TopLevelVariableElement element) {
    return null;
  }

  @Override
  public R visitTypeParameterElement(TypeParameterElement element) {
    return null;
  }
}
