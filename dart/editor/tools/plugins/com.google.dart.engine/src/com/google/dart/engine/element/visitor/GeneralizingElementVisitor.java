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
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementVisitor;
import com.google.dart.engine.element.EmbeddedHtmlScriptElement;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.ExportElement;
import com.google.dart.engine.element.ExternalHtmlScriptElement;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.FieldFormalParameterElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.FunctionTypeAliasElement;
import com.google.dart.engine.element.HtmlElement;
import com.google.dart.engine.element.HtmlScriptElement;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LabelElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.LocalElement;
import com.google.dart.engine.element.LocalVariableElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.MultiplyDefinedElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PrefixElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.PropertyInducingElement;
import com.google.dart.engine.element.ToolkitObjectElement;
import com.google.dart.engine.element.TopLevelVariableElement;
import com.google.dart.engine.element.TypeParameterElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.element.angular.AngularComponentElement;
import com.google.dart.engine.element.angular.AngularControllerElement;
import com.google.dart.engine.element.angular.AngularDecoratorElement;
import com.google.dart.engine.element.angular.AngularElement;
import com.google.dart.engine.element.angular.AngularFormatterElement;
import com.google.dart.engine.element.angular.AngularHasSelectorElement;
import com.google.dart.engine.element.angular.AngularPropertyElement;
import com.google.dart.engine.element.angular.AngularScopePropertyElement;
import com.google.dart.engine.element.angular.AngularSelectorElement;
import com.google.dart.engine.element.angular.AngularViewElement;
import com.google.dart.engine.element.polymer.PolymerAttributeElement;
import com.google.dart.engine.element.polymer.PolymerElement;
import com.google.dart.engine.element.polymer.PolymerTagDartElement;
import com.google.dart.engine.element.polymer.PolymerTagHtmlElement;

/**
 * Instances of the class {@code GeneralizingElementVisitor} implement an element visitor that will
 * recursively visit all of the elements in an element model (like instances of the class
 * {@link RecursiveElementVisitor}). In addition, when an element of a specific type is visited not
 * only will the visit method for that specific type of element be invoked, but additional methods
 * for the supertypes of that element will also be invoked. For example, using an instance of this
 * class to visit a {@link MethodElement} will cause the method
 * {@link #visitMethodElement(MethodElement)} to be invoked but will also cause the methods
 * {@link #visitExecutableElement(ExecutableElement)} and {@link #visitElement(Element)} to be
 * subsequently invoked. This allows visitors to be written that visit all executable elements
 * without needing to override the visit method for each of the specific subclasses of
 * {@link ExecutableElement}.
 * <p>
 * Note, however, that unlike many visitors, element visitors visit objects based on the interfaces
 * implemented by those elements. Because interfaces form a graph structure rather than a tree
 * structure the way classes do, and because it is generally undesirable for an object to be visited
 * more than once, this class flattens the interface graph into a pseudo-tree. In particular, this
 * class treats elements as if the element types were structured in the following way:
 * <p>
 * 
 * <pre>
 * Element
 *   ClassElement
 *   CompilationUnitElement
 *   ExecutableElement
 *      ConstructorElement
 *      LocalElement
 *         FunctionElement
 *      MethodElement
 *      PropertyAccessorElement
 *   ExportElement
 *   HtmlElement
 *   ImportElement
 *   LabelElement
 *   LibraryElement
 *   MultiplyDefinedElement
 *   PrefixElement
 *   TypeAliasElement
 *   TypeParameterElement
 *   UndefinedElement
 *   VariableElement
 *      PropertyInducingElement
 *         FieldElement
 *         TopLevelVariableElement
 *      LocalElement
 *         LocalVariableElement
 *         ParameterElement
 *            FieldFormalParameterElement
 * </pre>
 * <p>
 * Subclasses that override a visit method must either invoke the overridden visit method or
 * explicitly invoke the more general visit method. Failure to do so will cause the visit methods
 * for superclasses of the element to not be invoked and will cause the children of the visited node
 * to not be visited.
 * 
 * @coverage dart.engine.element
 */
public class GeneralizingElementVisitor<R> implements ElementVisitor<R> {
  @Override
  public R visitAngularComponentElement(AngularComponentElement element) {
    return visitAngularHasSelectorElement(element);
  }

  @Override
  public R visitAngularControllerElement(AngularControllerElement element) {
    return visitAngularHasSelectorElement(element);
  }

  @Override
  public R visitAngularDirectiveElement(AngularDecoratorElement element) {
    return visitAngularHasSelectorElement(element);
  }

  public R visitAngularElement(AngularElement element) {
    return visitToolkitObjectElement(element);
  }

  @Override
  public R visitAngularFormatterElement(AngularFormatterElement element) {
    return visitAngularElement(element);
  }

  public R visitAngularHasSelectorElement(AngularHasSelectorElement element) {
    return visitAngularElement(element);
  }

  @Override
  public R visitAngularPropertyElement(AngularPropertyElement element) {
    return visitAngularElement(element);
  }

  @Override
  public R visitAngularScopePropertyElement(AngularScopePropertyElement element) {
    return visitAngularElement(element);
  }

  @Override
  public R visitAngularSelectorElement(AngularSelectorElement element) {
    return visitAngularElement(element);
  }

  @Override
  public R visitAngularViewElement(AngularViewElement element) {
    return visitAngularElement(element);
  }

  @Override
  public R visitClassElement(ClassElement element) {
    return visitElement(element);
  }

  @Override
  public R visitCompilationUnitElement(CompilationUnitElement element) {
    return visitElement(element);
  }

  @Override
  public R visitConstructorElement(ConstructorElement element) {
    return visitExecutableElement(element);
  }

  public R visitElement(Element element) {
    element.visitChildren(this);
    return null;
  }

  @Override
  public R visitEmbeddedHtmlScriptElement(EmbeddedHtmlScriptElement element) {
    return visitHtmlScriptElement(element);
  }

  public R visitExecutableElement(ExecutableElement element) {
    return visitElement(element);
  }

  @Override
  public R visitExportElement(ExportElement element) {
    return visitElement(element);
  }

  @Override
  public R visitExternalHtmlScriptElement(ExternalHtmlScriptElement element) {
    return visitHtmlScriptElement(element);
  }

  @Override
  public R visitFieldElement(FieldElement element) {
    return visitPropertyInducingElement(element);
  }

  @Override
  public R visitFieldFormalParameterElement(FieldFormalParameterElement element) {
    return visitParameterElement(element);
  }

  @Override
  public R visitFunctionElement(FunctionElement element) {
    return visitLocalElement(element);
  }

  @Override
  public R visitFunctionTypeAliasElement(FunctionTypeAliasElement element) {
    return visitElement(element);
  }

  @Override
  public R visitHtmlElement(HtmlElement element) {
    return visitElement(element);
  }

  public R visitHtmlScriptElement(HtmlScriptElement element) {
    return visitElement(element);
  }

  @Override
  public R visitImportElement(ImportElement element) {
    return visitElement(element);
  }

  @Override
  public R visitLabelElement(LabelElement element) {
    return visitElement(element);
  }

  @Override
  public R visitLibraryElement(LibraryElement element) {
    return visitElement(element);
  }

  public R visitLocalElement(LocalElement element) {
    if (element instanceof LocalVariableElement) {
      return visitVariableElement((LocalVariableElement) element);
    } else if (element instanceof ParameterElement) {
      return visitVariableElement((ParameterElement) element);
    } else if (element instanceof FunctionElement) {
      return visitExecutableElement((FunctionElement) element);
    }
    return null;
  }

  @Override
  public R visitLocalVariableElement(LocalVariableElement element) {
    return visitLocalElement(element);
  }

  @Override
  public R visitMethodElement(MethodElement element) {
    return visitExecutableElement(element);
  }

  @Override
  public R visitMultiplyDefinedElement(MultiplyDefinedElement element) {
    return visitElement(element);
  }

  @Override
  public R visitParameterElement(ParameterElement element) {
    return visitLocalElement(element);
  }

  @Override
  public R visitPolymerAttributeElement(PolymerAttributeElement element) {
    return visitPolymerElement(element);
  }

  public R visitPolymerElement(PolymerElement element) {
    return visitToolkitObjectElement(element);
  }

  @Override
  public R visitPolymerTagDartElement(PolymerTagDartElement element) {
    return visitPolymerElement(element);
  }

  @Override
  public R visitPolymerTagHtmlElement(PolymerTagHtmlElement element) {
    return visitPolymerElement(element);
  }

  @Override
  public R visitPrefixElement(PrefixElement element) {
    return visitElement(element);
  }

  @Override
  public R visitPropertyAccessorElement(PropertyAccessorElement element) {
    return visitExecutableElement(element);
  }

  public R visitPropertyInducingElement(PropertyInducingElement element) {
    return visitVariableElement(element);
  }

  public R visitToolkitObjectElement(ToolkitObjectElement element) {
    return visitElement(element);
  }

  @Override
  public R visitTopLevelVariableElement(TopLevelVariableElement element) {
    return visitPropertyInducingElement(element);
  }

  @Override
  public R visitTypeParameterElement(TypeParameterElement element) {
    return visitElement(element);
  }

  public R visitVariableElement(VariableElement element) {
    return visitElement(element);
  }
}
