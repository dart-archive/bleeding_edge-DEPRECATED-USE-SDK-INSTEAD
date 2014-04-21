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
 * Instances of the class {@code RecursiveElementVisitor} implement an element visitor that will
 * recursively visit all of the element in an element model. For example, using an instance of this
 * class to visit a {@link CompilationUnitElement} will also cause all of the types in the
 * compilation unit to be visited.
 * <p>
 * Subclasses that override a visit method must either invoke the overridden visit method or must
 * explicitly ask the visited element to visit its children. Failure to do so will cause the
 * children of the visited element to not be visited.
 * 
 * @coverage dart.engine.element
 */
public class RecursiveElementVisitor<R> implements ElementVisitor<R> {
  @Override
  public R visitAngularComponentElement(AngularComponentElement element) {
    element.visitChildren(this);
    return null;
  }

  @Override
  public R visitAngularControllerElement(AngularControllerElement element) {
    element.visitChildren(this);
    return null;
  }

  @Override
  public R visitAngularDirectiveElement(AngularDecoratorElement element) {
    element.visitChildren(this);
    return null;
  }

  @Override
  public R visitAngularFormatterElement(AngularFormatterElement element) {
    element.visitChildren(this);
    return null;
  }

  @Override
  public R visitAngularPropertyElement(AngularPropertyElement element) {
    element.visitChildren(this);
    return null;
  }

  @Override
  public R visitAngularScopePropertyElement(AngularScopePropertyElement element) {
    element.visitChildren(this);
    return null;
  }

  @Override
  public R visitAngularSelectorElement(AngularSelectorElement element) {
    element.visitChildren(this);
    return null;
  }

  @Override
  public R visitAngularViewElement(AngularViewElement element) {
    element.visitChildren(this);
    return null;
  }

  @Override
  public R visitClassElement(ClassElement element) {
    element.visitChildren(this);
    return null;
  }

  @Override
  public R visitCompilationUnitElement(CompilationUnitElement element) {
    element.visitChildren(this);
    return null;
  }

  @Override
  public R visitConstructorElement(ConstructorElement element) {
    element.visitChildren(this);
    return null;
  }

  @Override
  public R visitEmbeddedHtmlScriptElement(EmbeddedHtmlScriptElement element) {
    element.visitChildren(this);
    return null;
  }

  @Override
  public R visitExportElement(ExportElement element) {
    element.visitChildren(this);
    return null;
  }

  @Override
  public R visitExternalHtmlScriptElement(ExternalHtmlScriptElement element) {
    element.visitChildren(this);
    return null;
  }

  @Override
  public R visitFieldElement(FieldElement element) {
    element.visitChildren(this);
    return null;
  }

  @Override
  public R visitFieldFormalParameterElement(FieldFormalParameterElement element) {
    element.visitChildren(this);
    return null;
  }

  @Override
  public R visitFunctionElement(FunctionElement element) {
    element.visitChildren(this);
    return null;
  }

  @Override
  public R visitFunctionTypeAliasElement(FunctionTypeAliasElement element) {
    element.visitChildren(this);
    return null;
  }

  @Override
  public R visitHtmlElement(HtmlElement element) {
    element.visitChildren(this);
    return null;
  }

  @Override
  public R visitImportElement(ImportElement element) {
    element.visitChildren(this);
    return null;
  }

  @Override
  public R visitLabelElement(LabelElement element) {
    element.visitChildren(this);
    return null;
  }

  @Override
  public R visitLibraryElement(LibraryElement element) {
    element.visitChildren(this);
    return null;
  }

  @Override
  public R visitLocalVariableElement(LocalVariableElement element) {
    element.visitChildren(this);
    return null;
  }

  @Override
  public R visitMethodElement(MethodElement element) {
    element.visitChildren(this);
    return null;
  }

  @Override
  public R visitMultiplyDefinedElement(MultiplyDefinedElement element) {
    element.visitChildren(this);
    return null;
  }

  @Override
  public R visitParameterElement(ParameterElement element) {
    element.visitChildren(this);
    return null;
  }

  @Override
  public R visitPolymerAttributeElement(PolymerAttributeElement element) {
    element.visitChildren(this);
    return null;
  }

  @Override
  public R visitPolymerTagDartElement(PolymerTagDartElement element) {
    element.visitChildren(this);
    return null;
  }

  @Override
  public R visitPolymerTagHtmlElement(PolymerTagHtmlElement element) {
    element.visitChildren(this);
    return null;
  }

  @Override
  public R visitPrefixElement(PrefixElement element) {
    element.visitChildren(this);
    return null;
  }

  @Override
  public R visitPropertyAccessorElement(PropertyAccessorElement element) {
    element.visitChildren(this);
    return null;
  }

  @Override
  public R visitTopLevelVariableElement(TopLevelVariableElement element) {
    element.visitChildren(this);
    return null;
  }

  @Override
  public R visitTypeParameterElement(TypeParameterElement element) {
    element.visitChildren(this);
    return null;
  }
}
