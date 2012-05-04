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
package com.google.dart.tools.core.search;

import com.google.dart.tools.core.model.DartFunction;
import com.google.dart.tools.core.model.DartFunctionTypeAlias;
import com.google.dart.tools.core.model.DartImport;
import com.google.dart.tools.core.model.DartVariableDeclaration;
import com.google.dart.tools.core.model.Field;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.Type;

import org.eclipse.core.runtime.IProgressMonitor;

import java.util.List;

/**
 * The interface <code>SearchEngine</code> defines the behavior of objects that can be used to
 * search for various pieces of information.
 */
public interface SearchEngine {
  /**
   * Synchronously search for all constructors matching the given pattern within the given scope.
   * Return all matches that pass the optional filter.
   * 
   * @param scope the scope containing the constructor declarations to be searched
   * @param pattern the pattern used to determine which constructor declarations are to be returned
   * @param filter the filter used to determine which matches should be returned, or
   *          <code>null</code> if all of the matches should be returned
   * @param monitor the progress monitor to use for reporting progress to the user. It is the
   *          caller's responsibility to call done() on the given monitor. Accepts
   *          <code>null,</code> indicating that no progress should be reported and that the
   *          operation cannot be canceled.
   * @throws SearchException if the results could not be computed
   */
  public List<SearchMatch> searchConstructorDeclarations(SearchScope scope, SearchPattern pattern,
      SearchFilter filter, IProgressMonitor monitor) throws SearchException;

  /**
   * Search for all constructors matching the given pattern within the given scope.
   * 
   * @param scope the scope containing the constructor declarations to be searched
   * @param pattern the pattern used to determine which constructor declarations are to be returned
   * @param filter the filter used to determine which matches should be passed to the listener
   *          (those that pass the filter), or <code>null</code> if all of the matches should be
   *          passed to the listener
   * @param listener the listener that will be notified when matches are found
   * @param monitor the progress monitor to use for reporting progress to the user. It is the
   *          caller's responsibility to call done() on the given monitor. Accepts <code>null</code>
   *          , indicating that no progress should be reported and that the operation cannot be
   *          canceled.
   * @throws SearchException if the results could not be computed
   */
  public void searchConstructorDeclarations(SearchScope scope, SearchPattern pattern,
      SearchFilter filter, SearchListener listener, IProgressMonitor monitor)
      throws SearchException;

  /**
   * Synchronously search for all fields matching the given pattern within the given scope. Return
   * all matches that pass the optional filter.
   * 
   * @param scope the scope containing the field declarations to be searched
   * @param pattern the pattern used to determine which field declarations are to be returned
   * @param filter the filter used to determine which matches should be returned, or
   *          <code>null</code> if all of the matches should be returned
   * @param monitor the progress monitor to use for reporting progress to the user. It is the
   *          caller's responsibility to call done() on the given monitor. Accepts
   *          <code>null,</code> indicating that no progress should be reported and that the
   *          operation cannot be canceled.
   * @throws SearchException if the results could not be computed
   */
  public List<SearchMatch> searchFieldDeclarations(SearchScope scope, SearchPattern pattern,
      SearchFilter filter, IProgressMonitor monitor) throws SearchException;

  /**
   * Search for all fields matching the given pattern within the given scope.
   * 
   * @param scope the scope containing the field declarations to be searched
   * @param pattern the pattern used to determine which field declarations are to be returned
   * @param filter the filter used to determine which matches should be passed to the listener
   *          (those that pass the filter), or <code>null</code> if all of the matches should be
   *          passed to the listener
   * @param listener the listener that will be notified when matches are found
   * @param monitor the progress monitor to use for reporting progress to the user. It is the
   *          caller's responsibility to call done() on the given monitor. Accepts <code>null</code>
   *          , indicating that no progress should be reported and that the operation cannot be
   *          canceled.
   * @throws SearchException if the results could not be computed
   */
  public void searchFieldDeclarations(SearchScope scope, SearchPattern pattern,
      SearchFilter filter, SearchListener listener, IProgressMonitor monitor)
      throws SearchException;

  /**
   * Synchronously search for all functions matching the given pattern within the given scope.
   * Return all matches that pass the optional filter.
   * 
   * @param scope the scope containing the function declarations to be searched
   * @param pattern the pattern used to determine which function declarations are to be returned
   * @param filter the filter used to determine which matches should be returned, or
   *          <code>null</code> if all of the matches should be returned
   * @param monitor the progress monitor to use for reporting progress to the user. It is the
   *          caller's responsibility to call done() on the given monitor. Accepts
   *          <code>null,</code> indicating that no progress should be reported and that the
   *          operation cannot be canceled.
   * @throws SearchException if the results could not be computed
   */
  public List<SearchMatch> searchFunctionDeclarations(SearchScope scope, SearchPattern pattern,
      SearchFilter filter, IProgressMonitor monitor) throws SearchException;

  /**
   * Search for all functions matching the given pattern within the given scope.
   * 
   * @param scope the scope containing the function declarations to be searched
   * @param pattern the pattern used to determine which function declarations are to be returned
   * @param filter the filter used to determine which matches should be passed to the listener
   *          (those that pass the filter), or <code>null</code> if all of the matches should be
   *          passed to the listener
   * @param listener the listener that will be notified when matches are found
   * @param monitor the progress monitor to use for reporting progress to the user. It is the
   *          caller's responsibility to call done() on the given monitor. Accepts <code>null</code>
   *          , indicating that no progress should be reported and that the operation cannot be
   *          canceled.
   * @throws SearchException if the results could not be computed
   */
  public void searchFunctionDeclarations(SearchScope scope, SearchPattern pattern,
      SearchFilter filter, SearchListener listener, IProgressMonitor monitor)
      throws SearchException;

  /**
   * Synchronously search for implementors of the given type within the given scope. Return all
   * matches that pass the optional filter.
   * 
   * @param type the interface being implemented by the results
   * @param scope the scope containing the type declarations to be searched
   * @param filter the filter used to determine which matches should be returned, or
   *          <code>null</code> if all of the matches should be returned
   * @param monitor the progress monitor to use for reporting progress to the user. It is the
   *          caller's responsibility to call done() on the given monitor. Accepts
   *          <code>null,</code> indicating that no progress should be reported and that the
   *          operation cannot be canceled.
   * @throws SearchException if the results could not be computed
   */
  public List<SearchMatch> searchImplementors(Type type, SearchScope scope, SearchFilter filter,
      IProgressMonitor monitor) throws SearchException;

  /**
   * Search for implementors of the given type within the given scope.
   * 
   * @param type the interface being implemented by the results
   * @param scope the scope containing the type declarations to be searched
   * @param filter the filter used to determine which matches should be passed to the listener
   *          (those that pass the filter), or <code>null</code> if all of the matches should be
   *          passed to the listener
   * @param listener the listener that will be notified when matches are found
   * @param monitor the progress monitor to use for reporting progress to the user. It is the
   *          caller's responsibility to call done() on the given monitor. Accepts <code>null</code>
   *          , indicating that no progress should be reported and that the operation cannot be
   *          canceled.
   * @throws SearchException if the results could not be computed
   */
  public void searchImplementors(Type type, SearchScope scope, SearchFilter filter,
      SearchListener listener, IProgressMonitor monitor) throws SearchException;

  /**
   * Synchronously search for all methods matching the given pattern within the given scope. Return
   * all matches that pass the optional filter.
   * 
   * @param scope the scope containing the method declarations to be searched
   * @param pattern the pattern used to determine which method declarations are to be returned
   * @param filter the filter used to determine which matches should be returned, or
   *          <code>null</code> if all of the matches should be returned
   * @param monitor the progress monitor to use for reporting progress to the user. It is the
   *          caller's responsibility to call done() on the given monitor. Accepts
   *          <code>null,</code> indicating that no progress should be reported and that the
   *          operation cannot be canceled.
   * @throws SearchException if the results could not be computed
   */
  public List<SearchMatch> searchMethodDeclarations(SearchScope scope, SearchPattern pattern,
      SearchFilter filter, IProgressMonitor monitor) throws SearchException;

  /**
   * Search for all methods matching the given pattern within the given scope.
   * 
   * @param scope the scope containing the method declarations to be searched
   * @param pattern the pattern used to determine which method declarations are to be returned
   * @param filter the filter used to determine which matches should be passed to the listener
   *          (those that pass the filter), or <code>null</code> if all of the matches should be
   *          passed to the listener
   * @param listener the listener that will be notified when matches are found
   * @param monitor the progress monitor to use for reporting progress to the user. It is the
   *          caller's responsibility to call done() on the given monitor. Accepts <code>null</code>
   *          , indicating that no progress should be reported and that the operation cannot be
   *          canceled.
   * @throws SearchException if the results could not be computed
   */
  public void searchMethodDeclarations(SearchScope scope, SearchPattern pattern,
      SearchFilter filter, SearchListener listener, IProgressMonitor monitor)
      throws SearchException;

  /**
   * Synchronously search for references to the given function within the given scope. Return all
   * matches that pass the optional filter.
   * 
   * @param function the function being referenced by the found matches
   * @param scope the scope containing the function declarations to be searched
   * @param filter the filter used to determine which matches should be returned, or
   *          <code>null</code> if all of the matches should be returned
   * @param monitor the progress monitor to use for reporting progress to the user. It is the
   *          caller's responsibility to call done() on the given monitor. Accepts
   *          <code>null,</code> indicating that no progress should be reported and that the
   *          operation cannot be canceled.
   * @throws SearchException if the results could not be computed
   */
  public List<SearchMatch> searchReferences(DartFunction function, SearchScope scope,
      SearchFilter filter, IProgressMonitor monitor) throws SearchException;

  /**
   * Search for references to the given function within the given scope.
   * 
   * @param function the function being referenced by the found matches
   * @param scope the scope containing the function declarations to be searched
   * @param filter the filter used to determine which matches should be passed to the listener
   *          (those that pass the filter), or <code>null</code> if all of the matches should be
   *          passed to the listener
   * @param listener the listener that will be notified when matches are found
   * @param monitor the progress monitor to use for reporting progress to the user. It is the
   *          caller's responsibility to call done() on the given monitor. Accepts <code>null</code>
   *          , indicating that no progress should be reported and that the operation cannot be
   *          canceled.
   * @throws SearchException if the results could not be computed
   */
  public void searchReferences(DartFunction function, SearchScope scope, SearchFilter filter,
      SearchListener listener, IProgressMonitor monitor) throws SearchException;

  /**
   * Synchronously search for references to the given function type alias within the given scope.
   * Return all matches that pass the optional filter.
   * 
   * @param alias the function type alias being referenced by the found matches
   * @param scope the scope containing the function type alias declarations to be searched
   * @param filter the filter used to determine which matches should be returned, or
   *          <code>null</code> if all of the matches should be returned
   * @param monitor the progress monitor to use for reporting progress to the user. It is the
   *          caller's responsibility to call done() on the given monitor. Accepts
   *          <code>null,</code> indicating that no progress should be reported and that the
   *          operation cannot be canceled.
   * @throws SearchException if the results could not be computed
   */
  public List<SearchMatch> searchReferences(DartFunctionTypeAlias alias, SearchScope scope,
      SearchFilter filter, IProgressMonitor monitor) throws SearchException;

  /**
   * Search for references to the given function type alias within the given scope.
   * 
   * @param alias the function type alias being referenced by the found matches
   * @param scope the scope containing the function type alias declarations to be searched
   * @param filter the filter used to determine which matches should be passed to the listener
   *          (those that pass the filter), or <code>null</code> if all of the matches should be
   *          passed to the listener
   * @param listener the listener that will be notified when matches are found
   * @param monitor the progress monitor to use for reporting progress to the user. It is the
   *          caller's responsibility to call done() on the given monitor. Accepts <code>null</code>
   *          , indicating that no progress should be reported and that the operation cannot be
   *          canceled.
   * @throws SearchException if the results could not be computed
   */
  public void searchReferences(DartFunctionTypeAlias alias, SearchScope scope, SearchFilter filter,
      SearchListener listener, IProgressMonitor monitor) throws SearchException;

  /**
   * Synchronously search for references to the given {@link DartImport} within the given scope.
   * Return all matches that pass the optional filter.
   * 
   * @param imprt the {@link DartImport} being referenced by the found matches
   * @param scope the scope containing the {@link DartImport} declarations to be searched
   * @param filter the filter used to determine which matches should be returned, or
   *          <code>null</code> if all of the matches should be returned
   * @param monitor the progress monitor to use for reporting progress to the user. It is the
   *          caller's responsibility to call done() on the given monitor. Accepts
   *          <code>null,</code> indicating that no progress should be reported and that the
   *          operation cannot be canceled.
   * @throws SearchException if the results could not be computed
   */
  public List<SearchMatch> searchReferences(DartImport imprt, SearchScope scope,
      SearchFilter filter, IProgressMonitor monitor) throws SearchException;

  /**
   * Search for references to the given {@link DartImport} within the given scope.
   * 
   * @param imprt the {@link DartImport} being referenced by the found matches
   * @param scope the scope containing the {@link DartImport} declarations to be searched
   * @param filter the filter used to determine which matches should be passed to the listener
   *          (those that pass the filter), or <code>null</code> if all of the matches should be
   *          passed to the listener
   * @param listener the listener that will be notified when matches are found
   * @param monitor the progress monitor to use for reporting progress to the user. It is the
   *          caller's responsibility to call done() on the given monitor. Accepts
   *          <code>null,</code> indicating that no progress should be reported and that the
   *          operation cannot be canceled.
   * @throws SearchException if the results could not be computed
   */
  public void searchReferences(DartImport imprt, SearchScope scope, SearchFilter filter,
      SearchListener listener, IProgressMonitor monitor) throws SearchException;

  /**
   * Synchronously search for references to the given variable within the given scope. Return all
   * matches that pass the optional filter.
   * 
   * @param variable the variable being referenced by the found matches
   * @param scope the scope containing the variable declarations to be searched
   * @param filter the filter used to determine which matches should be returned, or
   *          <code>null</code> if all of the matches should be returned
   * @param monitor the progress monitor to use for reporting progress to the user. It is the
   *          caller's responsibility to call done() on the given monitor. Accepts <code>null</code>
   *          , indicating that no progress should be reported and that the operation cannot be
   *          canceled.
   * @throws SearchException if the results could not be computed
   */
  public List<SearchMatch> searchReferences(DartVariableDeclaration variable, SearchScope scope,
      SearchFilter filter, IProgressMonitor monitor) throws SearchException;

  /**
   * Search for references to the given variable within the given scope.
   * 
   * @param variable the variable being referenced by the found matches
   * @param scope the scope containing the variable declarations to be searched
   * @param filter the filter used to determine which matches should be passed to the listener
   *          (those that pass the filter), or <code>null</code> if all of the matches should be
   *          passed to the listener
   * @param listener the listener that will be notified when matches are found
   * @param monitor the progress monitor to use for reporting progress to the user. It is the
   *          caller's responsibility to call done() on the given monitor. Accepts <code>null</code>
   *          , indicating that no progress should be reported and that the operation cannot be
   *          canceled.
   * @throws SearchException if the results could not be computed
   */
  public void searchReferences(DartVariableDeclaration variable, SearchScope scope,
      SearchFilter filter, SearchListener listener, IProgressMonitor monitor)
      throws SearchException;

  /**
   * Synchronously search for references to the given field within the given scope. Return all
   * matches that pass the optional filter.
   * 
   * @param field the field being referenced by the found matches
   * @param scope the scope containing the field declarations to be searched
   * @param filter the filter used to determine which matches should be returned, or
   *          <code>null</code> if all of the matches should be returned
   * @param monitor the progress monitor to use for reporting progress to the user. It is the
   *          caller's responsibility to call done() on the given monitor. Accepts
   *          <code>null,</code> indicating that no progress should be reported and that the
   *          operation cannot be canceled.
   * @throws SearchException if the results could not be computed
   */
  public List<SearchMatch> searchReferences(Field field, SearchScope scope, SearchFilter filter,
      IProgressMonitor monitor) throws SearchException;

  /**
   * Search for references to the given field within the given scope.
   * 
   * @param field the field being referenced by the found matches
   * @param scope the scope containing the field declarations to be searched
   * @param filter the filter used to determine which matches should be passed to the listener
   *          (those that pass the filter), or <code>null</code> if all of the matches should be
   *          passed to the listener
   * @param listener the listener that will be notified when matches are found
   * @param monitor the progress monitor to use for reporting progress to the user. It is the
   *          caller's responsibility to call done() on the given monitor. Accepts <code>null</code>
   *          , indicating that no progress should be reported and that the operation cannot be
   *          canceled.
   * @throws SearchException if the results could not be computed
   */
  public void searchReferences(Field field, SearchScope scope, SearchFilter filter,
      SearchListener listener, IProgressMonitor monitor) throws SearchException;

  /**
   * Synchronously search for references to the given method within the given scope. Return all
   * matches that pass the optional filter.
   * 
   * @param method the method being referenced by the found matches
   * @param scope the scope containing the method declarations to be searched
   * @param filter the filter used to determine which matches should be returned, or
   *          <code>null</code> if all of the matches should be returned
   * @param monitor the progress monitor to use for reporting progress to the user. It is the
   *          caller's responsibility to call done() on the given monitor. Accepts
   *          <code>null,</code> indicating that no progress should be reported and that the
   *          operation cannot be canceled.
   * @throws SearchException if the results could not be computed
   */
  public List<SearchMatch> searchReferences(Method method, SearchScope scope, SearchFilter filter,
      IProgressMonitor monitor) throws SearchException;

  /**
   * Search for references to the given method within the given scope.
   * 
   * @param method the method being referenced by the found matches
   * @param scope the scope containing the method declarations to be searched
   * @param filter the filter used to determine which matches should be passed to the listener
   *          (those that pass the filter), or <code>null</code> if all of the matches should be
   *          passed to the listener
   * @param listener the listener that will be notified when matches are found
   * @param monitor the progress monitor to use for reporting progress to the user. It is the
   *          caller's responsibility to call done() on the given monitor. Accepts <code>null</code>
   *          , indicating that no progress should be reported and that the operation cannot be
   *          canceled.
   * @throws SearchException if the results could not be computed
   */
  public void searchReferences(Method method, SearchScope scope, SearchFilter filter,
      SearchListener listener, IProgressMonitor monitor) throws SearchException;

  /**
   * Synchronously search for references to the given type within the given scope. Return all
   * matches that pass the optional filter.
   * 
   * @param type the type being referenced by the found matches
   * @param scope the scope containing the type declarations to be searched
   * @param filter the filter used to determine which matches should be returned, or
   *          <code>null</code> if all of the matches should be returned
   * @param monitor the progress monitor to use for reporting progress to the user. It is the
   *          caller's responsibility to call done() on the given monitor. Accepts
   *          <code>null,</code> indicating that no progress should be reported and that the
   *          operation cannot be canceled.
   * @throws SearchException if the results could not be computed
   */
  public List<SearchMatch> searchReferences(Type type, SearchScope scope, SearchFilter filter,
      IProgressMonitor monitor) throws SearchException;

  /**
   * Search for references to the given type within the given scope.
   * 
   * @param type the type being referenced by the found matches
   * @param scope the scope containing the type declarations to be searched
   * @param filter the filter used to determine which matches should be passed to the listener
   *          (those that pass the filter), or <code>null</code> if all of the matches should be
   *          passed to the listener
   * @param listener the listener that will be notified when matches are found
   * @param monitor the progress monitor to use for reporting progress to the user. It is the
   *          caller's responsibility to call done() on the given monitor. Accepts
   *          <code>null,</code> indicating that no progress should be reported and that the
   *          operation cannot be canceled.
   * @throws SearchException if the results could not be computed
   */
  public void searchReferences(Type type, SearchScope scope, SearchFilter filter,
      SearchListener listener, IProgressMonitor monitor) throws SearchException;

  /**
   * Synchronously search for subtypes of the given type within the given scope. Return all matches
   * that pass the optional filter.
   * 
   * @param type the type being subtyped by the found matches
   * @param scope the scope containing the type declarations to be searched
   * @param filter the filter used to determine which matches should be returned, or
   *          <code>null</code> if all of the matches should be returned
   * @param monitor the progress monitor to use for reporting progress to the user. It is the
   *          caller's responsibility to call done() on the given monitor. Accepts
   *          <code>null,</code> indicating that no progress should be reported and that the
   *          operation cannot be canceled.
   * @throws SearchException if the results could not be computed
   */
  public List<SearchMatch> searchSubtypes(Type type, SearchScope scope, SearchFilter filter,
      IProgressMonitor monitor) throws SearchException;

  /**
   * Search for subtypes of the given type within the given scope.
   * 
   * @param type the type being subtyped by the found matches
   * @param scope the scope containing the type declarations to be searched
   * @param filter the filter used to determine which matches should be passed to the listener
   *          (those that pass the filter), or <code>null</code> if all of the matches should be
   *          passed to the listener
   * @param listener the listener that will be notified when matches are found
   * @param monitor the progress monitor to use for reporting progress to the user. It is the
   *          caller's responsibility to call done() on the given monitor. Accepts
   *          <code>null,</code> indicating that no progress should be reported and that the
   *          operation cannot be canceled.
   * @throws SearchException if the results could not be computed
   */
  public void searchSubtypes(Type type, SearchScope scope, SearchFilter filter,
      SearchListener listener, IProgressMonitor monitor) throws SearchException;

  /**
   * Synchronously search for supertypes of the given type within the given scope. Return all
   * matches that pass the optional filter.
   * 
   * @param type the type that subtypes the found matches
   * @param scope the scope containing the type declarations to be searched
   * @param filter the filter used to determine which matches should be returned, or
   *          <code>null</code> if all of the matches should be returned
   * @param monitor the progress monitor to use for reporting progress to the user. It is the
   *          caller's responsibility to call done() on the given monitor. Accepts
   *          <code>null,</code> indicating that no progress should be reported and that the
   *          operation cannot be canceled.
   * @throws SearchException if the results could not be computed
   */
  public List<SearchMatch> searchSupertypes(Type type, SearchScope scope, SearchFilter filter,
      IProgressMonitor monitor) throws SearchException;

  /**
   * Search for supertypes of the given type within the given scope.
   * 
   * @param type the type that subtypes the found matches
   * @param scope the scope containing the type declarations to be searched
   * @param filter the filter used to determine which matches should be passed to the listener
   *          (those that pass the filter), or <code>null</code> if all of the matches should be
   *          passed to the listener
   * @param listener the listener that will be notified when matches are found
   * @param monitor the progress monitor to use for reporting progress to the user. It is the
   *          caller's responsibility to call done() on the given monitor. Accepts
   *          <code>null,</code> indicating that no progress should be reported and that the
   *          operation cannot be canceled.
   * @throws SearchException if the results could not be computed
   */
  public void searchSupertypes(Type type, SearchScope scope, SearchFilter filter,
      SearchListener listener, IProgressMonitor monitor) throws SearchException;

  /**
   * Synchronously search for all of the type declarations that are defined in the given scope and
   * match the given pattern. Return all matches that pass the optional filter.
   * 
   * @param scope the scope containing the type declarations to be searched
   * @param pattern the pattern used to determine which type declarations are to be returned
   * @param filter the filter used to determine which matches should be passed to the listener, or
   *          <code>null</code> if all of the matches should be passed to the listener
   * @param monitor the progress monitor to use for reporting progress to the user. It is the
   *          caller's responsibility to call done() on the given monitor. Accepts
   *          <code>null,</code> indicating that no progress should be reported and that the
   *          operation cannot be canceled.
   * @return the matches that were found
   * @throws SearchException if the results could not be computed
   */
  public List<SearchMatch> searchTypeDeclarations(SearchScope scope, SearchPattern pattern,
      SearchFilter filter, IProgressMonitor monitor) throws SearchException;

  /**
   * Search for all of the type declarations that are defined in the given scope and match the given
   * pattern.
   * 
   * @param scope the scope containing the type declarations to be searched
   * @param pattern the pattern used to determine which type declarations are to be returned
   * @param filter the filter used to determine which matches should be passed to the listener
   *          (those that pass the filter), or <code>null</code> if all of the matches should be
   *          passed to the listener
   * @param listener the listener that will be notified when matches are found
   * @param monitor the progress monitor to use for reporting progress to the user. It is the
   *          caller's responsibility to call done() on the given monitor. Accepts
   *          <code>null,</code> indicating that no progress should be reported and that the
   *          operation cannot be canceled.
   * @throws SearchException if the results could not be computed
   */
  public void searchTypeDeclarations(SearchScope scope, SearchPattern pattern, SearchFilter filter,
      SearchListener listener, IProgressMonitor monitor) throws SearchException;

  /**
   * Search for all of the type declarations that are defined in the given scope, and match the
   * given pattern.
   * 
   * @param scope the scope containing the type declarations to be searched
   * @param pattern the pattern used to determine which type declarations are to be returned
   * @param listener the listener that will be notified when matches are found
   * @param monitor the progress monitor to use for reporting progress to the user. It is the
   *          caller's responsibility to call done() on the given monitor. Accepts
   *          <code>null,</code> indicating that no progress should be reported and that the
   *          operation cannot be canceled.
   * @throws SearchException if the results could not be computed
   * @deprecated use searchTypeDeclarations(SearchScope, SearchPattern, SearchFilter,
   *             SearchListener, IProgressMonitor)
   */
  @Deprecated
  public void searchTypeDeclarations(SearchScope scope, SearchPattern pattern,
      SearchListener listener, IProgressMonitor monitor) throws SearchException;

  /**
   * Synchronously search for all variables matching the given pattern within the given scope.
   * Return all matches that pass the optional filter.
   * 
   * @param scope the scope containing the variable declarations to be searched
   * @param pattern the pattern used to determine which variable declarations are to be returned
   * @param filter the filter used to determine which matches should be returned, or
   *          <code>null</code> if all of the matches should be returned
   * @param monitor the progress monitor to use for reporting progress to the user. It is the
   *          caller's responsibility to call done() on the given monitor. Accepts
   *          <code>null,</code> indicating that no progress should be reported and that the
   *          operation cannot be canceled.
   * @throws SearchException if the results could not be computed
   */
  public List<SearchMatch> searchVariableDeclarations(SearchScope scope, SearchPattern pattern,
      SearchFilter filter, IProgressMonitor monitor) throws SearchException;

  /**
   * Search for all variables matching the given pattern within the given scope.
   * 
   * @param scope the scope containing the variable declarations to be searched
   * @param pattern the pattern used to determine which variable declarations are to be returned
   * @param filter the filter used to determine which matches should be passed to the listener
   *          (those that pass the filter), or <code>null</code> if all of the matches should be
   *          passed to the listener
   * @param listener the listener that will be notified when matches are found
   * @param monitor the progress monitor to use for reporting progress to the user. It is the
   *          caller's responsibility to call done() on the given monitor. Accepts <code>null</code>
   *          , indicating that no progress should be reported and that the operation cannot be
   *          canceled.
   * @throws SearchException if the results could not be computed
   */
  public void searchVariableDeclarations(SearchScope scope, SearchPattern pattern,
      SearchFilter filter, SearchListener listener, IProgressMonitor monitor)
      throws SearchException;
}
