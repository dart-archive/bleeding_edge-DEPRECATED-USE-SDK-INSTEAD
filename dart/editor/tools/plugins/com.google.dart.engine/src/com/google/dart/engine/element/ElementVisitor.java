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
 * The interface {@code ElementVisitor} defines the behavior of objects that can be used to visit an
 * element structure.
 * 
 * @coverage dart.engine.element
 */
public interface ElementVisitor<R> {
  public R visitAngularComponentElement(AngularComponentElement element);

  public R visitAngularControllerElement(AngularControllerElement element);

  public R visitAngularDirectiveElement(AngularDecoratorElement element);

  public R visitAngularFormatterElement(AngularFormatterElement element);

  public R visitAngularPropertyElement(AngularPropertyElement element);

  public R visitAngularScopePropertyElement(AngularScopePropertyElement element);

  public R visitAngularSelectorElement(AngularSelectorElement element);

  public R visitAngularViewElement(AngularViewElement element);

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

  public R visitPolymerAttributeElement(PolymerAttributeElement element);

  public R visitPolymerTagDartElement(PolymerTagDartElement element);

  public R visitPolymerTagHtmlElement(PolymerTagHtmlElement element);

  public R visitPrefixElement(PrefixElement element);

  public R visitPropertyAccessorElement(PropertyAccessorElement element);

  public R visitTopLevelVariableElement(TopLevelVariableElement element);

  public R visitTypeParameterElement(TypeParameterElement element);
}
