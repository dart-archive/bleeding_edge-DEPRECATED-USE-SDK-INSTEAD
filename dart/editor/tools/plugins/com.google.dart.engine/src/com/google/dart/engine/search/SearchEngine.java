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
package com.google.dart.engine.search;

import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.PropertyInducingElement;
import com.google.dart.engine.type.Type;

import java.util.List;
import java.util.Set;

/**
 * The interface <code>SearchEngine</code> defines the behavior of objects that can be used to
 * search for various pieces of information.
 * 
 * @coverage dart.engine.search
 */
public interface SearchEngine {

  /**
   * Synchronously search for the types assigned to the given field or top-level variable.
   * 
   * @param variable the field or top-level variable to find assigned types for
   * @param scope the scope containing the assignments to be searched, may be {@code null} if all
   *          assignments should be returned
   */
  Set<Type> searchAssignedTypes(PropertyInducingElement variable, SearchScope scope);

  /**
   * Synchronously search for declarations of the given name within the given scope. Return all
   * matches that pass the optional filter.
   * 
   * @param name the name being declared by the found matches
   * @param filter the filter used to determine which matches should be returned, or {@code null} if
   *          all of the matches should be returned
   */
  List<SearchMatch> searchDeclarations(String name, SearchScope scope, SearchFilter filter);

  /**
   * Search for declarations of the given name within the given scope.
   * 
   * @param name the name being declared by the found matches
   * @param filter the filter used to determine which matches should be passed to the listener
   *          (those that pass the filter), or {@code null} if all of the matches should be passed
   *          to the listener
   * @param listener the listener that will be notified when matches are found @ if the results
   *          could not be computed
   */
  void searchDeclarations(String name, SearchScope scope, SearchFilter filter,
      SearchListener listener);

  /**
   * Synchronously search for all functions matching the given pattern within the given scope.
   * Return all matches that pass the optional filter.
   * 
   * @param scope the scope containing the function declarations to be searched, may be {@code null}
   *          if all declarations should be returned
   * @param pattern the pattern used to determine which function declarations are to be returned
   * @param filter the filter used to determine which matches should be returned, or {@code null} if
   *          all of the matches should be returned
   */
  List<SearchMatch> searchFunctionDeclarations(SearchScope scope, SearchPattern pattern,
      SearchFilter filter);

  /**
   * Search for all functions matching the given pattern within the given scope.
   * 
   * @param scope the scope containing the function declarations to be searched, may be {@code null}
   *          if all declarations should be returned
   * @param pattern the pattern used to determine which function declarations are to be returned
   * @param filter the filter used to determine which matches should be passed to the listener
   *          (those that pass the filter), or {@code null} if all of the matches should be passed
   *          to the listener
   * @param listener the listener that will be notified when matches are found @ if the results
   *          could not be computed
   */
  void searchFunctionDeclarations(SearchScope scope, SearchPattern pattern, SearchFilter filter,
      SearchListener listener);

  /**
   * Synchronously search for resolved and unresolved qualified references to the class members with
   * given name within the given scope. Return all matches that pass the optional filter.
   * 
   * @param name the name being referenced by the found matches
   * @param scope the scope containing the references to be searched, may be {@code null} if all
   *          references should be returned
   * @param filter the filter used to determine which matches should be returned, or {@code null} if
   *          all of the matches should be returned
   */
  List<SearchMatch> searchQualifiedMemberReferences(String name, SearchScope scope,
      SearchFilter filter);

  /**
   * Search for resolved and unresolved qualified references to the class members with given name
   * within the given scope.
   * 
   * @param name the name being referenced by the found matches
   * @param scope the scope containing the references to be searched, may be {@code null} if all
   *          references should be returned
   * @param filter the filter used to determine which matches should be passed to the listener
   *          (those that pass the filter), or {@code null} if all of the matches should be passed
   *          to the listener
   * @param listener the listener that will be notified when matches are found @ if the results
   *          could not be computed
   */
  void searchQualifiedMemberReferences(String name, SearchScope scope, SearchFilter filter,
      SearchListener listener);

  /**
   * Synchronously search for references to the given {@link Element} within the given scope. This
   * method will call corresponding <code>searchReferences</code> method depending on concrete type
   * of the given {@link Element}.
   * 
   * @param element the type being referenced by the found matches
   * @param scope the scope containing the references to be searched, may be {@code null} if all
   *          references should be returned
   * @param filter the filter used to determine which matches should be returned, or {@code null} if
   *          all of the matches should be returned
   */
  List<SearchMatch> searchReferences(Element element, SearchScope scope, SearchFilter filter);

  /**
   * Search for references to the given type within the given scope. This method will call
   * corresponding <code>searchReferences</code> method depending on concrete type of the given
   * {@link Element}.
   * 
   * @param type the type being referenced by the found matches
   * @param scope the scope containing the references to be searched, may be {@code null} if all
   *          references should be returned
   * @param filter the filter used to determine which matches should be passed to the listener
   *          (those that pass the filter), or {@code null} if all of the matches should be passed
   *          to the listener
   * @param listener the listener that will be notified when matches are found @ if the results
   *          could not be computed
   */
  void searchReferences(Element element, SearchScope scope, SearchFilter filter,
      SearchListener listener);

  /**
   * Synchronously search for subtypes of the given type within the given scope. Return all matches
   * that pass the optional filter.
   * 
   * @param type the type being subtyped by the found matches
   * @param scope the scope containing the subtypes to be searched, may be {@code null} if all
   *          subtypes should be returned
   * @param filter the filter used to determine which matches should be returned, or {@code null} if
   *          all of the matches should be returned
   */
  List<SearchMatch> searchSubtypes(ClassElement type, SearchScope scope, SearchFilter filter);

  /**
   * Search for subtypes of the given type within the given scope.
   * 
   * @param type the type being subtyped by the found matches
   * @param scope the scope containing the subtypes to be searched, may be {@code null} if all
   *          subtypes should be returned
   * @param filter the filter used to determine which matches should be passed to the listener
   *          (those that pass the filter), or {@code null} if all of the matches should be passed
   *          to the listener
   * @param listener the listener that will be notified when matches are found @ if the results
   *          could not be computed
   */
  void searchSubtypes(ClassElement type, SearchScope scope, SearchFilter filter,
      SearchListener listener);

  /**
   * Synchronously search for all of the type declarations that are defined in the given scope and
   * match the given pattern. Return all matches that pass the optional filter.
   * 
   * @param scope the scope containing the type declarations to be searched, may be {@code null} if
   *          all declarations should be returned
   * @param pattern the pattern used to determine which type declarations are to be returned
   * @param filter the filter used to determine which matches should be passed to the listener, or
   *          {@code null} if all of the matches should be passed to the listener
   * @return the matches that were found @ if the results could not be computed
   */
  List<SearchMatch> searchTypeDeclarations(SearchScope scope, SearchPattern pattern,
      SearchFilter filter);

  /**
   * Search for all of the type declarations (classes, class type aliases and function type aliases)
   * that are defined in the given scope and match the given pattern.
   * 
   * @param scope the scope containing the type declarations to be searched, may be {@code null} if
   *          all declarations should be returned
   * @param pattern the pattern used to determine which type declarations are to be returned
   * @param filter the filter used to determine which matches should be passed to the listener
   *          (those that pass the filter), or {@code null} if all of the matches should be passed
   *          to the listener
   * @param listener the listener that will be notified when matches are found @ if the results
   *          could not be computed
   */
  void searchTypeDeclarations(SearchScope scope, SearchPattern pattern, SearchFilter filter,
      SearchListener listener);

  /**
   * Synchronously search for all variables matching the given pattern within the given scope.
   * Return all matches that pass the optional filter.
   * 
   * @param scope the scope containing the variable declarations to be searched, may be {@code null}
   *          if all declarations should be returned
   * @param pattern the pattern used to determine which variable declarations are to be returned
   * @param filter the filter used to determine which matches should be returned, or {@code null} if
   *          all of the matches should be returned
   */
  List<SearchMatch> searchVariableDeclarations(SearchScope scope, SearchPattern pattern,
      SearchFilter filter);

  /**
   * Search for all variables matching the given pattern within the given scope.
   * 
   * @param scope the scope containing the variable declarations to be searched, may be {@code null}
   *          if all declarations should be returned
   * @param pattern the pattern used to determine which variable declarations are to be returned
   * @param filter the filter used to determine which matches should be passed to the listener
   *          (those that pass the filter), or {@code null} if all of the matches should be passed
   *          to the listener
   * @param listener the listener that will be notified when matches are found @ if the results
   *          could not be computed
   */
  void searchVariableDeclarations(SearchScope scope, SearchPattern pattern, SearchFilter filter,
      SearchListener listener);
}
