/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.core.internal.index.util;

import com.google.common.collect.MapMaker;
import com.google.dart.compiler.resolver.ClassElement;
import com.google.dart.compiler.resolver.ConstructorElement;
import com.google.dart.compiler.resolver.EnclosingElement;
import com.google.dart.compiler.resolver.FieldElement;
import com.google.dart.compiler.resolver.LibraryElement;
import com.google.dart.compiler.resolver.MethodElement;
import com.google.dart.compiler.type.InterfaceType;
import com.google.dart.compiler.util.apache.StringUtils;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.index.Element;
import com.google.dart.tools.core.index.Resource;
import com.google.dart.tools.core.model.CompilationUnitElement;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.utilities.bindings.BindingUtils;

import java.util.Map;

/**
 * The class <code>ElementFactory</code> defines utility methods used to create {@link Element
 * elements}.
 */
public final class ElementFactory {
  /**
   * The element id used for library elements.
   */
  public static final String LIBRARY_ELEMENT_ID = "#library";
  private static final Map<com.google.dart.compiler.resolver.Element, Element> compilerToModelElement = new MapMaker().weakKeys().softValues().makeMap();
  private static final Map<com.google.dart.compiler.resolver.FieldElement, Element[]> compilerToModelFieldElement = new MapMaker().weakKeys().softValues().makeMap();

  /**
   * Compose the element id of the given parent element and the name of a child element into an
   * element id appropriate for the child element.
   * 
   * @param parentElement the element representing the parent of the child element
   * @param childName the unescaped name of a child element
   * @return the element id appropriate for the child element
   */
  public static String composeElementId(Element parentElement, String childName) {
    StringBuilder builder = new StringBuilder();
    if (parentElement != null) {
      builder.append(parentElement.getElementId()); // This has already been escaped.
      builder.append(ResourceFactory.SEPARATOR_CHAR);
    }
    ResourceFactory.escape(builder, childName);
    return builder.toString();
  }

  /**
   * Compose the name of a child element into an element id appropriate for the child element.
   * 
   * @param childName the unescaped name of a child element
   * @return the element id appropriate for the child element
   */
  public static String composeElementId(String childName) {
    return composeElementId(null, childName);
  }

  /**
   * Return an element representing the given type.
   * 
   * @param element the type element to be represented
   * @return an element representing the given type
   * @throws DartModelException if a resource could not be created to represent the compilation unit
   *           containing the type
   */
  public static Element getElement(ClassElement element) throws DartModelException {
    if (element.isDynamic()) {
      return null;
    }
    Element result = compilerToModelElement.get(element);
    if (result == null) {
      result = getElement0(element);
      if (result != null) {
        compilerToModelElement.put(element, result);
      }
    }
    return result;
  }

  /**
   * Return an element representing the given element.
   * 
   * @param element the element to be represented
   * @return an element representing the given element
   * @throws DartModelException if a resource could not be created to represent the compilation unit
   *           containing the element
   */
  public static Element getElement(com.google.dart.compiler.resolver.Element element)
      throws DartModelException {
    if (element instanceof ClassElement) {
      return getElement((ClassElement) element);
    } else if (element instanceof FieldElement) {
      return getElement((FieldElement) element, false, false);
    } else if (element instanceof LibraryElement) {
      return getElement((LibraryElement) element);
    } else if (element instanceof MethodElement) {
      return getElement((MethodElement) element);
    } else {
      DartCore.logInformation("Could not getElement for " + element.getClass().getName());
      return null;
    }
  }

  /**
   * Return an element representing the given field.
   * 
   * @param element the field element to be represented
   * @param allowGetter <code>true</code> if a getter is allowed to be returned
   * @param allowSetter <code>true</code> if a setter is allowed to be returned
   * @return an element representing the given field
   * @throws DartModelException if a resource could not be created to represent the compilation unit
   *           containing the field
   */
  public static Element getElement(FieldElement element, boolean allowGetter, boolean allowSetter)
      throws DartModelException {
    int index = (allowGetter ? 2 : 0) + (allowSetter ? 1 : 0);
    // prepare array for getter/setter
    Element[] resultArray = compilerToModelFieldElement.get(element);
    if (resultArray == null) {
      resultArray = new Element[4];
      compilerToModelFieldElement.put(element, resultArray);
    }
    // prepare single array element
    Element result = resultArray[index];
    if (result == null) {
      result = getElement0(element, allowGetter, allowSetter);
      resultArray[index] = result;
    }
    // done
    return result;
  }

  /**
   * Return an element representing the given type.
   * 
   * @param node the node representing the declaration of the type
   * @return an element representing the given type
   * @throws DartModelException if a resource could not be created to represent the compilation unit
   *           containing the type
   */
  public static Element getElement(InterfaceType type) throws DartModelException {
    return getElement(type.getElement());
  }

  /**
   * Return an element representing the given library.
   * 
   * @param element the library element to be represented
   * @return an element representing the given library
   * @throws DartModelException if a resource could not be created to represent the library
   */
  public static Element getElement(LibraryElement element) {
    String libraryId = element.getLibraryUnit().getSource().getUri().toString();
    return new Element(
        new Resource(ResourceFactory.composeResourceId(libraryId, libraryId)),
        LIBRARY_ELEMENT_ID);
  }

  /**
   * Return an element representing the given method.
   * 
   * @param element the element representing the method
   * @return an element representing the given method
   * @throws DartModelException if a resource could not be created to represent the compilation unit
   *           containing the method
   */
  public static Element getElement(MethodElement element) throws DartModelException {
    Element result = compilerToModelElement.get(element);
    if (result == null) {
      result = getElement0(element);
      if (result != null) {
        compilerToModelElement.put(element, result);
      }
    }
    return result;
  }

  public static Element getParameterElement(MethodElement method, String parameterName)
      throws DartModelException {
    Element methodElement = getElement(method);
    return new Element(methodElement.getResource(), composeElementId(methodElement, parameterName));
  }

  private static Element getElement0(ClassElement element) throws DartModelException {
    Element result;
    LibraryElement libraryElement = getLibraryElement(element);
    CompilationUnitElement dartType = BindingUtils.getDartElement(
        BindingUtils.getDartElement(libraryElement),
        element);
    if (dartType == null) {
      return null;
    }
    result = new Element(ResourceFactory.getResource(dartType), composeElementId(element.getName()));
    return result;
  }

  private static Element getElement0(FieldElement element, boolean allowGetter, boolean allowSetter)
      throws DartModelException {
    CompilationUnitElement field = BindingUtils.getDartElement(
        BindingUtils.getDartElement(BindingUtils.getLibrary(element)),
        element,
        allowGetter,
        allowSetter);
    if (field == null) {
      DartCore.logInformation("Could not getElement for field " + pathTo(element));
      return null;
    }
    EnclosingElement parentElement = element.getEnclosingElement();
    if (parentElement instanceof LibraryElement) {
      return new Element(ResourceFactory.getResource(field), composeElementId(element.getName()));
    }
    return new Element(ResourceFactory.getResource(field), composeElementId(
        getElement(parentElement),
        element.getName()));
  }

  private static Element getElement0(MethodElement element) throws DartModelException {
    DartLibrary library = BindingUtils.getDartElement(BindingUtils.getLibrary(element));
    if (library == null) {
      DartCore.logInformation("Could not getElement for method " + pathTo(element));
      return null;
    }
    CompilationUnitElement method = BindingUtils.getDartElement(library, element);
    if (method == null) {
      DartCore.logInformation("Could not getElement for method " + pathTo(element));
      return null;
    }
    String methodName = element.getName();
    if (element instanceof ConstructorElement) {
      String typeName = element.getEnclosingElement().getName();
      methodName = StringUtils.isEmpty(methodName) ? typeName : typeName + "." + methodName;
    }
    EnclosingElement parentElement = element.getEnclosingElement();
    if (parentElement instanceof LibraryElement) {
      return new Element(ResourceFactory.getResource(method), composeElementId(methodName));
    }
    return new Element(ResourceFactory.getResource(method), composeElementId(
        getElement(parentElement),
        methodName));
  }

  /**
   * Return the library element for the library that contains the given element, or
   * <code>null</code> if the given element is not contained in a library.
   * 
   * @param element the element whose enclosing library is to be returned
   * @return the library element for the library that contains the given element
   */
  private static LibraryElement getLibraryElement(com.google.dart.compiler.resolver.Element element) {
    com.google.dart.compiler.resolver.Element parentElement = element.getEnclosingElement();
    while (parentElement != null) {
      if (parentElement instanceof LibraryElement) {
        return (LibraryElement) parentElement;
      }
    }
    return null;
  }

  /**
   * Create a path to the given element. This is not a valid id and should only be used for
   * debugging purposes.
   * 
   * @param element the element whose path is to be returned
   * @return the path to the element
   */
  private static String pathTo(com.google.dart.compiler.resolver.Element element) {
    if (element.isDynamic()) {
      return "dynamic";
    }
    StringBuilder builder = new StringBuilder();
    pathTo(builder, element);
    return builder.toString();
  }

  /**
   * Append a path to the given element to the given builder. This is not a valid id and should only
   * be used for debugging purposes.
   * 
   * @param builder the builder to which the path is to be appended
   * @param element the element whose path is to be appended to the builder
   */
  private static void pathTo(StringBuilder builder,
      com.google.dart.compiler.resolver.Element element) {
    com.google.dart.compiler.resolver.Element parent = element.getEnclosingElement();
    if (parent == null) {
      builder.append(element.getName());
    } else if (parent == element) {
      DartCore.logInformation("The element " + element.getName() + " is it's own parent");
      builder.append(element.getName());
    } else {
      pathTo(builder, parent);
      builder.append("/");
      builder.append(element.getName());
    }
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private ElementFactory() {
    super();
  }
}
