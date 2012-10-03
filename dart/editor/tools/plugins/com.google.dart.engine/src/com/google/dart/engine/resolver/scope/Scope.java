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
package com.google.dart.engine.resolver.scope;

import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.resolver.ResolverErrorCode;
import com.google.dart.engine.source.Source;

import java.util.HashMap;

/**
 * The abstract class {@code Scope} defines the behavior common to name scopes used by the resolver
 * to determine which names are visible at any given point in the code.
 */
public abstract class Scope {
  /**
   * Return {@code true} if the given name is a library-private name.
   * 
   * @param name the name being tested
   * @return {@code true} if the given name is a library-private name
   */
  public static boolean isPrivateName(String name) {
    return name != null && name.startsWith("_");
  }

  /**
   * A table mapping names that are defined in this scope to the element representing the thing
   * declared with that name.
   */
  private HashMap<String, Element> definedNames;

  /**
   * Initialize a newly created scope.
   */
  public Scope() {
    this.definedNames = new HashMap<String, Element>();
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
    String name = element.getName();
    if (definedNames.containsKey(name)) {
      getErrorListener().onError(new AnalysisError(getSource(), getErrorCodeForDuplicate(), name));
    } else {
      Element overriddenElement = lookup(name, getDefiningLibrary());
      if (overriddenElement != null) {
        ErrorCode errorCode = getErrorCodeForHiding();
        if (errorCode != null) {
          getErrorListener().onError(new AnalysisError(getSource(), errorCode, name));
        }
      }
      definedNames.put(name, element);
    }
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
  public Element lookup(SimpleIdentifier identifier, LibraryElement referencingLibrary) {
    return lookup(identifier.getName(), referencingLibrary);
  }

  /**
   * Return the element with which the given name is associated, or {@code null} if the name is not
   * defined within this scope.
   * 
   * @param name the name associated with the element to be returned
   * @param referencingLibrary the library that contains the reference to the name, used to
   *          implement library-level privacy
   * @return the element with which the given name is associated
   */
  public abstract Element lookup(String name, LibraryElement referencingLibrary);

  /**
   * Return the element representing the library in which this scope is enclosed.
   * 
   * @return the element representing the library in which this scope is enclosed
   */
  protected abstract LibraryElement getDefiningLibrary();

  /**
   * Return the error code to be used when reporting that a name being defined locally conflicts
   * with another element of the same name in the local scope.
   * 
   * @return the error code used to report duplicate names within a scope
   */
  protected ErrorCode getErrorCodeForDuplicate() {
    // TODO(brianwilkerson) Make this an abstract method
    return ResolverErrorCode.DUPLICATE_MEMBER_ERROR;
  }

  /**
   * Return the error code to be used when reporting that a name being defined locally hides a name
   * defined in an outer scope.
   * 
   * @return the error code used to report name hiding
   */
  protected ErrorCode getErrorCodeForHiding() {
    // TODO(brianwilkerson) Make this an abstract method
    return ResolverErrorCode.DUPLICATE_MEMBER_WARNING;
  }

  /**
   * Return the listener that is to be informed when an error is encountered.
   * 
   * @return the listener that is to be informed when an error is encountered
   */
  protected abstract AnalysisErrorListener getErrorListener();

  /**
   * Return the source object representing the compilation unit with which errors related to this
   * scope should be associated.
   * 
   * @return the source object with which errors should be associated
   */
  protected Source getSource() {
    return getDefiningLibrary().getDefiningCompilationUnit().getSource();
  }

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
    return definedNames.get(name);
  }
}
