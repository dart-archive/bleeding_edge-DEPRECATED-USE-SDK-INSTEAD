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

import java.util.List;

/**
 * The interface <code>SearchEngine</code> defines the behavior of objects that can be used to
 * search for various pieces of information.
 */
public interface SearchEngine {

  /**
   * Synchronously search for all functions matching the given pattern within the given scope.
   * Return all matches that pass the optional filter.
   * 
   * @param scope the scope containing the function declarations to be searched
   * @param pattern the pattern used to determine which function declarations are to be returned
   * @param filter the filter used to determine which matches should be returned, or
   *          <code>null</code> if all of the matches should be returned
   * @throws SearchException if the results could not be computed
   */
  public List<SearchMatch> searchFunctionDeclarations(SearchScope scope, SearchPattern pattern,
      SearchFilter filter) throws SearchException;

  /**
   * Search for all functions matching the given pattern within the given scope.
   * 
   * @param scope the scope containing the function declarations to be searched
   * @param pattern the pattern used to determine which function declarations are to be returned
   * @param filter the filter used to determine which matches should be passed to the listener
   *          (those that pass the filter), or <code>null</code> if all of the matches should be
   *          passed to the listener
   * @param listener the listener that will be notified when matches are found
   * @throws SearchException if the results could not be computed
   */
  public void searchFunctionDeclarations(SearchScope scope, SearchPattern pattern,
      SearchFilter filter, SearchListener listener) throws SearchException;

//  /**
//   * Synchronously search for implementors of the given type within the given scope. Return all
//   * matches that pass the optional filter.
//   * 
//   * @param type the interface being implemented by the results
//   * @param scope the scope containing the type declarations to be searched
//   * @param filter the filter used to determine which matches should be returned, or
//   *          <code>null</code> if all of the matches should be returned
//   * @throws SearchException if the results could not be computed
//   */
//  public List<SearchMatch> searchImplementors(ClassElement type, SearchScope scope,
//      SearchFilter filter) throws SearchException;
//
//  /**
//   * Search for implementors of the given type within the given scope.
//   * 
//   * @param type the interface being implemented by the results
//   * @param scope the scope containing the type declarations to be searched
//   * @param filter the filter used to determine which matches should be passed to the listener
//   *          (those that pass the filter), or <code>null</code> if all of the matches should be
//   *          passed to the listener
//   * @param listener the listener that will be notified when matches are found
//   * @throws SearchException if the results could not be computed
//   */
//  public void searchImplementors(ClassElement type, SearchScope scope, SearchFilter filter,
//      SearchListener listener) throws SearchException;
//
//  /**
//   * Synchronously search for references to the given type within the given scope. Return all
//   * matches that pass the optional filter.
//   * 
//   * @param type the type being referenced by the found matches
//   * @param scope the scope containing the type declarations to be searched
//   * @param filter the filter used to determine which matches should be returned, or
//   *          <code>null</code> if all of the matches should be returned
//   * @throws SearchException if the results could not be computed
//   */
//  public List<SearchMatch> searchReferences(ClassElement type, SearchScope scope,
//      SearchFilter filter) throws SearchException;
//
//  /**
//   * Search for references to the given type within the given scope.
//   * 
//   * @param type the type being referenced by the found matches
//   * @param scope the scope containing the type declarations to be searched
//   * @param filter the filter used to determine which matches should be passed to the listener
//   *          (those that pass the filter), or <code>null</code> if all of the matches should be
//   *          passed to the listener
//   * @param listener the listener that will be notified when matches are found
//   * @throws SearchException if the results could not be computed
//   */
//  public void searchReferences(ClassElement type, SearchScope scope, SearchFilter filter,
//      SearchListener listener) throws SearchException;

  // TODO(scheglov) decide what to use here - ImportSpecification?
//  /**
//   * Synchronously search for references to the given {@link DartImport} within the given scope.
//   * Return all matches that pass the optional filter.
//   * 
//   * @param imprt the {@link DartImport} being referenced by the found matches
//   * @param scope the scope containing the {@link DartImport} declarations to be searched
//   * @param filter the filter used to determine which matches should be returned, or
//   *          <code>null</code> if all of the matches should be returned
//   * @throws SearchException if the results could not be computed
//   */
//  public List<SearchMatch> searchReferences(DartImport imprt, SearchScope scope, SearchFilter filter)
//      throws SearchException;
//
//  /**
//   * Search for references to the given {@link DartImport} within the given scope.
//   * 
//   * @param imprt the {@link DartImport} being referenced by the found matches
//   * @param scope the scope containing the {@link DartImport} declarations to be searched
//   * @param filter the filter used to determine which matches should be passed to the listener
//   *          (those that pass the filter), or <code>null</code> if all of the matches should be
//   *          passed to the listener
//   * @param listener the listener that will be notified when matches are found
//   * @throws SearchException if the results could not be computed
//   */
//  public void searchReferences(DartImport imprt, SearchScope scope, SearchFilter filter,
//      SearchListener listener) throws SearchException;

//  /**
//   * Synchronously search for references to the given field within the given scope. Return all
//   * matches that pass the optional filter.
//   * 
//   * @param field the field being referenced by the found matches
//   * @param scope the scope containing the field declarations to be searched
//   * @param filter the filter used to determine which matches should be returned, or
//   *          <code>null</code> if all of the matches should be returned
//   * @throws SearchException if the results could not be computed
//   */
//  public List<SearchMatch> searchReferences(FieldElement field, SearchScope scope,
//      SearchFilter filter) throws SearchException;
//
//  /**
//   * Search for references to the given field within the given scope.
//   * 
//   * @param field the field being referenced by the found matches
//   * @param scope the scope containing the field declarations to be searched
//   * @param filter the filter used to determine which matches should be passed to the listener
//   *          (those that pass the filter), or <code>null</code> if all of the matches should be
//   *          passed to the listener
//   * @param listener the listener that will be notified when matches are found
//   * @throws SearchException if the results could not be computed
//   */
//  public void searchReferences(FieldElement field, SearchScope scope, SearchFilter filter,
//      SearchListener listener) throws SearchException;
//
//  /**
//   * Synchronously search for references to the given function within the given scope. Return all
//   * matches that pass the optional filter.
//   * 
//   * @param function the function being referenced by the found matches
//   * @param scope the scope containing the function declarations to be searched
//   * @param filter the filter used to determine which matches should be returned, or
//   *          <code>null</code> if all of the matches should be returned
//   * @throws SearchException if the results could not be computed
//   */
//  public List<SearchMatch> searchReferences(FunctionElement function, SearchScope scope,
//      SearchFilter filter) throws SearchException;
//
//  /**
//   * Search for references to the given function within the given scope.
//   * 
//   * @param function the function being referenced by the found matches
//   * @param scope the scope containing the function declarations to be searched
//   * @param filter the filter used to determine which matches should be passed to the listener
//   *          (those that pass the filter), or <code>null</code> if all of the matches should be
//   *          passed to the listener
//   * @param listener the listener that will be notified when matches are found
//   * @throws SearchException if the results could not be computed
//   */
//  public void searchReferences(FunctionElement function, SearchScope scope, SearchFilter filter,
//      SearchListener listener) throws SearchException;
//
//  /**
//   * Synchronously search for references to the given method within the given scope. Return all
//   * matches that pass the optional filter.
//   * 
//   * @param method the method being referenced by the found matches
//   * @param scope the scope containing the method declarations to be searched
//   * @param filter the filter used to determine which matches should be returned, or
//   *          <code>null</code> if all of the matches should be returned
//   * @throws SearchException if the results could not be computed
//   */
//  public List<SearchMatch> searchReferences(Method method, SearchScope scope, SearchFilter filter)
//      throws SearchException;
//
//  /**
//   * Search for references to the given method within the given scope.
//   * 
//   * @param method the method being referenced by the found matches
//   * @param scope the scope containing the method declarations to be searched
//   * @param filter the filter used to determine which matches should be passed to the listener
//   *          (those that pass the filter), or <code>null</code> if all of the matches should be
//   *          passed to the listener
//   * @param listener the listener that will be notified when matches are found
//   * @throws SearchException if the results could not be computed
//   */
//  public void searchReferences(Method method, SearchScope scope, SearchFilter filter,
//      SearchListener listener) throws SearchException;
//
//  /**
//   * Synchronously search for references to the given {@link IFile} within the given scope. Return
//   * all matches that pass the optional filter.
//   * 
//   * @param file the {@link IFile} being referenced by the found matches
//   * @param scope the scope containing the {@link IFile} declarations to be searched
//   * @param filter the filter used to determine which matches should be returned, or
//   *          <code>null</code> if all of the matches should be returned
//   * @throws SearchException if the results could not be computed
//   */
//  public List<SearchMatch> searchReferences(Source file, SearchScope scope, SearchFilter filter)
//      throws SearchException;
//
//  /**
//   * Search for references to the given {@link IFile} within the given scope.
//   * 
//   * @param file the {@link IFile} being referenced by the found matches
//   * @param scope the scope containing the {@link IFile} declarations to be searched
//   * @param filter the filter used to determine which matches should be passed to the listener
//   *          (those that pass the filter), or <code>null</code> if all of the matches should be
//   *          passed to the listener
//   * @param listener the listener that will be notified when matches are found
//   * @throws SearchException if the results could not be computed
//   */
//  public void searchReferences(Source file, SearchScope scope, SearchFilter filter,
//      SearchListener listener) throws SearchException;
//
//  /**
//   * Synchronously search for references to the given function type alias within the given scope.
//   * Return all matches that pass the optional filter.
//   * 
//   * @param alias the function type alias being referenced by the found matches
//   * @param scope the scope containing the function type alias declarations to be searched
//   * @param filter the filter used to determine which matches should be returned, or
//   *          <code>null</code> if all of the matches should be returned
//   * @throws SearchException if the results could not be computed
//   */
//  public List<SearchMatch> searchReferences(TypeAliasElement alias, SearchScope scope,
//      SearchFilter filter) throws SearchException;
//
//  /**
//   * Search for references to the given function type alias within the given scope.
//   * 
//   * @param alias the function type alias being referenced by the found matches
//   * @param scope the scope containing the function type alias declarations to be searched
//   * @param filter the filter used to determine which matches should be passed to the listener
//   *          (those that pass the filter), or <code>null</code> if all of the matches should be
//   *          passed to the listener
//   * @param listener the listener that will be notified when matches are found
//   * @throws SearchException if the results could not be computed
//   */
//  public void searchReferences(TypeAliasElement alias, SearchScope scope, SearchFilter filter,
//      SearchListener listener) throws SearchException;
//
//  /**
//   * Synchronously search for references to the given variable within the given scope. Return all
//   * matches that pass the optional filter.
//   * 
//   * @param variable the variable being referenced by the found matches
//   * @param scope the scope containing the variable declarations to be searched
//   * @param filter the filter used to determine which matches should be returned, or
//   *          <code>null</code> if all of the matches should be returned
//   * @throws SearchException if the results could not be computed
//   */
//  public List<SearchMatch> searchReferences(VariableElement variable, SearchScope scope,
//      SearchFilter filter) throws SearchException;
//
//  /**
//   * Search for references to the given variable within the given scope.
//   * 
//   * @param variable the variable being referenced by the found matches
//   * @param scope the scope containing the variable declarations to be searched
//   * @param filter the filter used to determine which matches should be passed to the listener
//   *          (those that pass the filter), or <code>null</code> if all of the matches should be
//   *          passed to the listener
//   * @param listener the listener that will be notified when matches are found
//   * @throws SearchException if the results could not be computed
//   */
//  public void searchReferences(VariableElement variable, SearchScope scope, SearchFilter filter,
//      SearchListener listener) throws SearchException;
//
//  /**
//   * Synchronously search for subtypes of the given type within the given scope. Return all matches
//   * that pass the optional filter.
//   * 
//   * @param type the type being subtyped by the found matches
//   * @param scope the scope containing the type declarations to be searched
//   * @param filter the filter used to determine which matches should be returned, or
//   *          <code>null</code> if all of the matches should be returned
//   * @throws SearchException if the results could not be computed
//   */
//  public List<SearchMatch> searchSubtypes(ClassElement type, SearchScope scope, SearchFilter filter)
//      throws SearchException;
//
//  /**
//   * Search for subtypes of the given type within the given scope.
//   * 
//   * @param type the type being subtyped by the found matches
//   * @param scope the scope containing the type declarations to be searched
//   * @param filter the filter used to determine which matches should be passed to the listener
//   *          (those that pass the filter), or <code>null</code> if all of the matches should be
//   *          passed to the listener
//   * @param listener the listener that will be notified when matches are found
//   * @throws SearchException if the results could not be computed
//   */
//  public void searchSubtypes(ClassElement type, SearchScope scope, SearchFilter filter,
//      SearchListener listener) throws SearchException;
//
//  /**
//   * Synchronously search for all of the type declarations that are defined in the given scope and
//   * match the given pattern. Return all matches that pass the optional filter.
//   * 
//   * @param scope the scope containing the type declarations to be searched
//   * @param pattern the pattern used to determine which type declarations are to be returned
//   * @param filter the filter used to determine which matches should be passed to the listener, or
//   *          <code>null</code> if all of the matches should be passed to the listener
//   * @return the matches that were found
//   * @throws SearchException if the results could not be computed
//   */
//  public List<SearchMatch> searchTypeDeclarations(SearchScope scope, SearchPattern pattern,
//      SearchFilter filter) throws SearchException;
//
//  /**
//   * Search for all of the type declarations that are defined in the given scope and match the given
//   * pattern.
//   * 
//   * @param scope the scope containing the type declarations to be searched
//   * @param pattern the pattern used to determine which type declarations are to be returned
//   * @param filter the filter used to determine which matches should be passed to the listener
//   *          (those that pass the filter), or <code>null</code> if all of the matches should be
//   *          passed to the listener
//   * @param listener the listener that will be notified when matches are found
//   * @throws SearchException if the results could not be computed
//   */
//  public void searchTypeDeclarations(SearchScope scope, SearchPattern pattern, SearchFilter filter,
//      SearchListener listener) throws SearchException;
//
//  /**
//   * Synchronously search for all variables matching the given pattern within the given scope.
//   * Return all matches that pass the optional filter.
//   * 
//   * @param scope the scope containing the variable declarations to be searched
//   * @param pattern the pattern used to determine which variable declarations are to be returned
//   * @param filter the filter used to determine which matches should be returned, or
//   *          <code>null</code> if all of the matches should be returned
//   * @throws SearchException if the results could not be computed
//   */
//  public List<SearchMatch> searchVariableDeclarations(SearchScope scope, SearchPattern pattern,
//      SearchFilter filter) throws SearchException;
//
//  /**
//   * Search for all variables matching the given pattern within the given scope.
//   * 
//   * @param scope the scope containing the variable declarations to be searched
//   * @param pattern the pattern used to determine which variable declarations are to be returned
//   * @param filter the filter used to determine which matches should be passed to the listener
//   *          (those that pass the filter), or <code>null</code> if all of the matches should be
//   *          passed to the listener
//   * @param listener the listener that will be notified when matches are found
//   * @throws SearchException if the results could not be computed
//   */
//  public void searchVariableDeclarations(SearchScope scope, SearchPattern pattern,
//      SearchFilter filter, SearchListener listener) throws SearchException;
}
