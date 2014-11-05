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
package com.google.dart.engine.internal.scope;

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.error.CompileTimeErrorCode;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.general.StringUtilities;

import java.util.HashMap;

/**
 * The abstract class {@code Scope} defines the behavior common to name scopes used by the resolver
 * to determine which names are visible at any given point in the code.
 * 
 * @coverage dart.engine.resolver
 */
public abstract class Scope {
  /**
   * The prefix used to mark an identifier as being private to its library.
   */
  public static final int PRIVATE_NAME_PREFIX = '_';

  /**
   * The suffix added to the declared name of a setter when looking up the setter. Used to
   * disambiguate between a getter and a setter that have the same name.
   */
  public static final String SETTER_SUFFIX = "="; //$NON-NLS-1$

  /**
   * The name used to look up the method used to implement the unary minus operator. Used to
   * disambiguate between the unary and binary operators.
   */
  public static final String UNARY_MINUS = "unary-"; //$NON-NLS-1$

  /**
   * Return {@code true} if the given name is a library-private name.
   * 
   * @param name the name being tested
   * @return {@code true} if the given name is a library-private name
   */
  public static boolean isPrivateName(String name) {
    return name != null && StringUtilities.startsWithChar(name, PRIVATE_NAME_PREFIX);
  }

  /**
   * A table mapping names that are defined in this scope to the element representing the thing
   * declared with that name.
   */
  private HashMap<String, Element> definedNames = new HashMap<String, Element>();

  /**
   * A flag indicating whether there are any names defined in this scope.
   */
  private boolean hasName = false;

  /**
   * Initialize a newly created scope to be empty.
   */
  public Scope() {
    super();
  }

  /**
   * Add the given element to this scope. If there is already an element with the given name defined
   * in this scope, then an error will be generated and the original element will continue to be
   * mapped to the name. If there is an element with the given name in an enclosing scope, then a
   * warning will be generated but the given element will hide the inherited element.
   * 
   * @param element the element to be added to this scope
   */
  public void define(Element element) {
    String name = getName(element);
    if (name != null && !name.isEmpty()) {
      if (definedNames.containsKey(name)) {
        getErrorListener().onError(getErrorForDuplicate(definedNames.get(name), element));
      } else {
        definedNames.put(name, element);
        hasName = true;
      }
    }
  }

  /**
   * Return the scope in which this scope is lexically enclosed.
   * 
   * @return the scope in which this scope is lexically enclosed
   */
  public Scope getEnclosingScope() {
    return null;
  }

  /**
   * Return the element with which the given identifier is associated, or {@code null} if the name
   * is not defined within this scope.
   * 
   * @param identifier the identifier associated with the element to be returned
   * @param referencingLibrary the library that contains the reference to the name, used to
   *          implement library-level privacy
   * @return the element with which the given identifier is associated
   */
  public Element lookup(Identifier identifier, LibraryElement referencingLibrary) {
    return internalLookup(identifier, identifier.getName(), referencingLibrary);
  }

  /**
   * Add the given element to this scope without checking for duplication or hiding.
   * 
   * @param name the name of the element to be added
   * @param element the element to be added to this scope
   */
  protected void defineNameWithoutChecking(String name, Element element) {
    definedNames.put(name, element);
    hasName = true;
  }

  /**
   * Add the given element to this scope without checking for duplication or hiding.
   * 
   * @param element the element to be added to this scope
   */
  protected void defineWithoutChecking(Element element) {
    definedNames.put(getName(element), element);
    hasName = true;
  }

  /**
   * Return the error code to be used when reporting that a name being defined locally conflicts
   * with another element of the same name in the local scope.
   * 
   * @param existing the first element to be declared with the conflicting name
   * @param duplicate another element declared with the conflicting name
   * @return the error code used to report duplicate names within a scope
   */
  protected AnalysisError getErrorForDuplicate(Element existing, Element duplicate) {
    // TODO(brianwilkerson) Customize the error message based on the types of elements that share
    // the same name.
    // TODO(jwren) There are 4 error codes for duplicate, but only 1 is being generated.
    Source source = duplicate.getSource();
    return new AnalysisError(
        source,
        duplicate.getNameOffset(),
        duplicate.getDisplayName().length(),
        CompileTimeErrorCode.DUPLICATE_DEFINITION,
        existing.getDisplayName());
  }

  /**
   * Return the listener that is to be informed when an error is encountered.
   * 
   * @return the listener that is to be informed when an error is encountered
   */
  protected abstract AnalysisErrorListener getErrorListener();

  /**
   * Return the source that contains the given identifier, or the source associated with this scope
   * if the source containing the identifier could not be determined.
   * 
   * @param identifier the identifier whose source is to be returned
   * @return the source that contains the given identifier
   */
  protected Source getSource(AstNode node) {
    CompilationUnit unit = node.getAncestor(CompilationUnit.class);
    if (unit != null) {
      CompilationUnitElement unitElement = unit.getElement();
      if (unitElement != null) {
        return unitElement.getSource();
      }
    }
    return null;
  }

  /**
   * Return the element with which the given name is associated, or {@code null} if the name is not
   * defined within this scope.
   * 
   * @param identifier the identifier node to lookup element for, used to report correct kind of a
   *          problem and associate problem with
   * @param name the name associated with the element to be returned
   * @param referencingLibrary the library that contains the reference to the name, used to
   *          implement library-level privacy
   * @return the element with which the given name is associated
   */
  protected abstract Element internalLookup(Identifier identifier, String name,
      LibraryElement referencingLibrary);

  /**
   * Return the element with which the given name is associated, or {@code null} if the name is not
   * defined within this scope. This method only returns elements that are directly defined within
   * this scope, not elements that are defined in an enclosing scope.
   * 
   * @param name the name associated with the element to be returned
   * @param referencingLibrary the library that contains the reference to the name, used to
   *          implement library-level privacy
   * @return the element with which the given name is associated
   */
  protected Element localLookup(String name, LibraryElement referencingLibrary) {
    if (hasName) {
      return definedNames.get(name);
    }
    return null;
  }

  /**
   * Return the name that will be used to look up the given element.
   * 
   * @param element the element whose look-up name is to be returned
   * @return the name that will be used to look up the given element
   */
  private String getName(Element element) {
    if (element instanceof MethodElement) {
      MethodElement method = (MethodElement) element;
      if (method.getName().equals("-") && method.getParameters().length == 0) {
        return UNARY_MINUS;
      }
    }
    return element.getName();
  }
}
