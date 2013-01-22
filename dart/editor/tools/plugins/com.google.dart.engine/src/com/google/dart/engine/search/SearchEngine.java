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
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.TypeAliasElement;
import com.google.dart.engine.element.VariableElement;

import java.util.List;

/**
 * The interface <code>SearchEngine</code> defines the behavior of objects that can be used to
 * search for various pieces of information.
 */
public interface SearchEngine {

  /**
   * Synchronously search for declarations of the given name within the given scope. Return all
   * matches that pass the optional filter.
   * 
   * @param name the name being declared by the found matches
   * @param filter the filter used to determine which matches should be returned, or
   *          <code>null</code> if all of the matches should be returned
   * @throws SearchException if the results could not be computed
   */
  public List<SearchMatch> searchDeclarations(String name, SearchScope scope, SearchFilter filter)
      throws SearchException;

  /**
   * Search for declarations of the given name within the given scope.
   * 
   * @param name the name being declared by the found matches
   * @param filter the filter used to determine which matches should be passed to the listener
   *          (those that pass the filter), or <code>null</code> if all of the matches should be
   *          passed to the listener
   * @param listener the listener that will be notified when matches are found
   * @throws SearchException if the results could not be computed
   */
  public void searchDeclarations(String name, SearchScope scope, SearchFilter filter,
      SearchListener listener) throws SearchException;

  /**
   * Synchronously search for all functions matching the given pattern within the given scope.
   * Return all matches that pass the optional filter.
   * 
   * @param scope the scope containing the function declarations to be searched, may be
   *          <code>null</code> if all declarations should be returned
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
   * @param scope the scope containing the function declarations to be searched, may be
   *          <code>null</code> if all declarations should be returned
   * @param pattern the pattern used to determine which function declarations are to be returned
   * @param filter the filter used to determine which matches should be passed to the listener
   *          (those that pass the filter), or <code>null</code> if all of the matches should be
   *          passed to the listener
   * @param listener the listener that will be notified when matches are found
   * @throws SearchException if the results could not be computed
   */
  public void searchFunctionDeclarations(SearchScope scope, SearchPattern pattern,
      SearchFilter filter, SearchListener listener) throws SearchException;

  /**
   * Synchronously search for references to the given type within the given scope. Return all
   * matches that pass the optional filter.
   * 
   * @param type the type being referenced by the found matches
   * @param scope the scope containing the references to be searched, may be <code>null</code> if
   *          all references should be returned
   * @param filter the filter used to determine which matches should be returned, or
   *          <code>null</code> if all of the matches should be returned
   * @throws SearchException if the results could not be computed
   */
  public List<SearchMatch> searchReferences(ClassElement type, SearchScope scope,
      SearchFilter filter) throws SearchException;

  /**
   * Search for references to the given type within the given scope.
   * 
   * @param type the type being referenced by the found matches
   * @param scope the scope containing the references to be searched, may be <code>null</code> if
   *          all references should be returned
   * @param filter the filter used to determine which matches should be passed to the listener
   *          (those that pass the filter), or <code>null</code> if all of the matches should be
   *          passed to the listener
   * @param listener the listener that will be notified when matches are found
   * @throws SearchException if the results could not be computed
   */
  public void searchReferences(ClassElement type, SearchScope scope, SearchFilter filter,
      SearchListener listener) throws SearchException;

  /**
   * Synchronously search for references to the given {@link CompilationUnitElement} within the
   * given scope. Return all matches that pass the optional filter.
   * 
   * @param unit the {@link CompilationUnitElement} being referenced by the found matches
   * @param scope the scope containing the references to be searched, may be <code>null</code> if
   *          all references should be returned
   * @param filter the filter used to determine which matches should be returned, or
   *          <code>null</code> if all of the matches should be returned
   * @throws SearchException if the results could not be computed
   */
  public List<SearchMatch> searchReferences(CompilationUnitElement unit, SearchScope scope,
      SearchFilter filter) throws SearchException;

  /**
   * Search for references to the given {@link CompilationUnitElement} within the given scope.
   * 
   * @param unit the {@link CompilationUnitElement} being referenced by the found matches
   * @param scope the scope containing the references to be searched, may be <code>null</code> if
   *          all references should be returned
   * @param filter the filter used to determine which matches should be passed to the listener
   *          (those that pass the filter), or <code>null</code> if all of the matches should be
   *          passed to the listener
   * @param listener the listener that will be notified when matches are found
   * @throws SearchException if the results could not be computed
   */
  public void searchReferences(CompilationUnitElement unit, SearchScope scope, SearchFilter filter,
      SearchListener listener) throws SearchException;

  /**
   * Synchronously search for references to the given {@link Element} within the given scope. This
   * method will call corresponding <code>searchReferences</code> method depending on concrete type
   * of the given {@link Element}.
   * 
   * @param element the type being referenced by the found matches
   * @param scope the scope containing the references to be searched, may be <code>null</code> if
   *          all references should be returned
   * @param filter the filter used to determine which matches should be returned, or
   *          <code>null</code> if all of the matches should be returned
   * @throws SearchException if the results could not be computed
   */
  public List<SearchMatch> searchReferences(Element element, SearchScope scope, SearchFilter filter)
      throws SearchException;

  /**
   * Search for references to the given type within the given scope. This method will call
   * corresponding <code>searchReferences</code> method depending on concrete type of the given
   * {@link Element}.
   * 
   * @param type the type being referenced by the found matches
   * @param scope the scope containing the references to be searched, may be <code>null</code> if
   *          all references should be returned
   * @param filter the filter used to determine which matches should be passed to the listener
   *          (those that pass the filter), or <code>null</code> if all of the matches should be
   *          passed to the listener
   * @param listener the listener that will be notified when matches are found
   * @throws SearchException if the results could not be computed
   */
  public void searchReferences(Element element, SearchScope scope, SearchFilter filter,
      SearchListener listener) throws SearchException;

  /**
   * Synchronously search for references to the given field within the given scope. Return all
   * matches that pass the optional filter.
   * 
   * @param field the field being referenced by the found matches
   * @param scope the scope containing the references to be searched, may be <code>null</code> if
   *          all references should be returned
   * @param filter the filter used to determine which matches should be returned, or
   *          <code>null</code> if all of the matches should be returned
   * @throws SearchException if the results could not be computed
   */
  public List<SearchMatch> searchReferences(FieldElement field, SearchScope scope,
      SearchFilter filter) throws SearchException;

  /**
   * Search for references to the given field within the given scope.
   * 
   * @param field the field being referenced by the found matches
   * @param scope the scope containing the references to be searched, may be <code>null</code> if
   *          all references should be returned
   * @param filter the filter used to determine which matches should be passed to the listener
   *          (those that pass the filter), or <code>null</code> if all of the matches should be
   *          passed to the listener
   * @param listener the listener that will be notified when matches are found
   * @throws SearchException if the results could not be computed
   */
  public void searchReferences(FieldElement field, SearchScope scope, SearchFilter filter,
      SearchListener listener) throws SearchException;

  /**
   * Synchronously search for references to the given function within the given scope. Return all
   * matches that pass the optional filter.
   * 
   * @param function the function being referenced by the found matches
   * @param scope the scope containing the references to be searched, may be <code>null</code> if
   *          all references should be returned
   * @param filter the filter used to determine which matches should be returned, or
   *          <code>null</code> if all of the matches should be returned
   * @throws SearchException if the results could not be computed
   */
  public List<SearchMatch> searchReferences(FunctionElement function, SearchScope scope,
      SearchFilter filter) throws SearchException;

  /**
   * Search for references to the given function within the given scope.
   * 
   * @param function the function being referenced by the found matches
   * @param scope the scope containing the references to be searched, may be <code>null</code> if
   *          all references should be returned
   * @param filter the filter used to determine which matches should be passed to the listener
   *          (those that pass the filter), or <code>null</code> if all of the matches should be
   *          passed to the listener
   * @param listener the listener that will be notified when matches are found
   * @throws SearchException if the results could not be computed
   */
  public void searchReferences(FunctionElement function, SearchScope scope, SearchFilter filter,
      SearchListener listener) throws SearchException;

  /**
   * Synchronously search for references to the given import within the given scope. Return all
   * matches that pass the optional filter.
   * 
   * @param imp the import being referenced by the found matches
   * @param scope the scope containing the references to be searched, may be <code>null</code> if
   *          all references should be returned
   * @param filter the filter used to determine which matches should be returned, or
   *          <code>null</code> if all of the matches should be returned
   * @throws SearchException if the results could not be computed
   */
  public List<SearchMatch> searchReferences(ImportElement imp, SearchScope scope,
      SearchFilter filter) throws SearchException;

  /**
   * Search for references to the given type import the given scope.
   * 
   * @param imp the import being referenced by the found matches
   * @param scope the scope containing the references to be searched, may be <code>null</code> if
   *          all references should be returned
   * @param filter the filter used to determine which matches should be passed to the listener
   *          (those that pass the filter), or <code>null</code> if all of the matches should be
   *          passed to the listener
   * @param listener the listener that will be notified when matches are found
   * @throws SearchException if the results could not be computed
   */
  public void searchReferences(ImportElement imp, SearchScope scope, SearchFilter filter,
      SearchListener listener) throws SearchException;

  /**
   * Synchronously search for references to the given {@link LibraryElement} within the given scope.
   * Return all matches that pass the optional filter.
   * 
   * @param library the {@link LibraryElement} being referenced by the found matches
   * @param scope the scope containing the references to be searched, may be <code>null</code> if
   *          all references should be returned
   * @param filter the filter used to determine which matches should be returned, or
   *          <code>null</code> if all of the matches should be returned
   * @throws SearchException if the results could not be computed
   */
  public List<SearchMatch> searchReferences(LibraryElement library, SearchScope scope,
      SearchFilter filter) throws SearchException;

  /**
   * Search for references to the given {@link LibraryElement} within the given scope.
   * 
   * @param library the {@link LibraryElement} being referenced by the found matches
   * @param scope the scope containing the references to be searched, may be <code>null</code> if
   *          all references should be returned
   * @param filter the filter used to determine which matches should be passed to the listener
   *          (those that pass the filter), or <code>null</code> if all of the matches should be
   *          passed to the listener
   * @param listener the listener that will be notified when matches are found
   * @throws SearchException if the results could not be computed
   */
  public void searchReferences(LibraryElement library, SearchScope scope, SearchFilter filter,
      SearchListener listener) throws SearchException;

  /**
   * Synchronously search for references to the given method within the given scope. Return all
   * matches that pass the optional filter.
   * 
   * @param method the method being referenced by the found matches
   * @param scope the scope containing the references to be searched, may be <code>null</code> if
   *          all references should be returned
   * @param filter the filter used to determine which matches should be returned, or
   *          <code>null</code> if all of the matches should be returned
   * @throws SearchException if the results could not be computed
   */
  public List<SearchMatch> searchReferences(MethodElement method, SearchScope scope,
      SearchFilter filter) throws SearchException;

  /**
   * Search for references to the given method within the given scope.
   * 
   * @param method the method being referenced by the found matches
   * @param scope the scope containing the references to be searched, may be <code>null</code> if
   *          all references should be returned
   * @param filter the filter used to determine which matches should be passed to the listener
   *          (those that pass the filter), or <code>null</code> if all of the matches should be
   *          passed to the listener
   * @param listener the listener that will be notified when matches are found
   * @throws SearchException if the results could not be computed
   */
  public void searchReferences(MethodElement method, SearchScope scope, SearchFilter filter,
      SearchListener listener) throws SearchException;

  /**
   * Synchronously search for references to the given parameter within the given scope. Return all
   * matches that pass the optional filter.
   * 
   * @param parameter the parameter being referenced by the found matches
   * @param scope the scope containing the references to be searched, may be <code>null</code> if
   *          all references should be returned
   * @param filter the filter used to determine which matches should be returned, or
   *          <code>null</code> if all of the matches should be returned
   * @throws SearchException if the results could not be computed
   */
  public List<SearchMatch> searchReferences(ParameterElement parameter, SearchScope scope,
      SearchFilter filter) throws SearchException;

  /**
   * Search for references to the given parameter within the given scope.
   * 
   * @param parameter the parameter being referenced by the found matches
   * @param scope the scope containing the references to be searched, may be <code>null</code> if
   *          all references should be returned
   * @param filter the filter used to determine which matches should be passed to the listener
   *          (those that pass the filter), or <code>null</code> if all of the matches should be
   *          passed to the listener
   * @param listener the listener that will be notified when matches are found
   * @throws SearchException if the results could not be computed
   */
  public void searchReferences(ParameterElement parameter, SearchScope scope, SearchFilter filter,
      SearchListener listener) throws SearchException;

  /**
   * Synchronously search for references to the given name within the given scope. Return all
   * matches that pass the optional filter.
   * 
   * @param name the name being referenced by the found matches
   * @param scope the scope containing the references to be searched, may be <code>null</code> if
   *          all references should be returned
   * @param filter the filter used to determine which matches should be returned, or
   *          <code>null</code> if all of the matches should be returned
   * @throws SearchException if the results could not be computed
   */
  public List<SearchMatch> searchReferences(String name, SearchScope scope, SearchFilter filter)
      throws SearchException;

  /**
   * Search for references to the given name within the given scope.
   * 
   * @param name the name being referenced by the found matches
   * @param scope the scope containing the references to be searched, may be <code>null</code> if
   *          all references should be returned
   * @param filter the filter used to determine which matches should be passed to the listener
   *          (those that pass the filter), or <code>null</code> if all of the matches should be
   *          passed to the listener
   * @param listener the listener that will be notified when matches are found
   * @throws SearchException if the results could not be computed
   */
  public void searchReferences(String name, SearchScope scope, SearchFilter filter,
      SearchListener listener) throws SearchException;

  /**
   * Synchronously search for references to the given function type alias within the given scope.
   * Return all matches that pass the optional filter.
   * 
   * @param alias the function type alias being referenced by the found matches
   * @param scope the scope containing the references to be searched, may be <code>null</code> if
   *          all references should be returned
   * @param filter the filter used to determine which matches should be returned, or
   *          <code>null</code> if all of the matches should be returned
   * @throws SearchException if the results could not be computed
   */
  public List<SearchMatch> searchReferences(TypeAliasElement alias, SearchScope scope,
      SearchFilter filter) throws SearchException;

  /**
   * Search for references to the given function type alias within the given scope.
   * 
   * @param alias the function type alias being referenced by the found matches
   * @param scope the scope containing the references to be searched, may be <code>null</code> if
   *          all references should be returned
   * @param filter the filter used to determine which matches should be passed to the listener
   *          (those that pass the filter), or <code>null</code> if all of the matches should be
   *          passed to the listener
   * @param listener the listener that will be notified when matches are found
   * @throws SearchException if the results could not be computed
   */
  public void searchReferences(TypeAliasElement alias, SearchScope scope, SearchFilter filter,
      SearchListener listener) throws SearchException;

  /**
   * Synchronously search for references to the given variable within the given scope. Return all
   * matches that pass the optional filter.
   * 
   * @param variable the variable being referenced by the found matches
   * @param scope the scope containing the references to be searched, may be <code>null</code> if
   *          all references should be returned
   * @param filter the filter used to determine which matches should be returned, or
   *          <code>null</code> if all of the matches should be returned
   * @throws SearchException if the results could not be computed
   */
  public List<SearchMatch> searchReferences(VariableElement variable, SearchScope scope,
      SearchFilter filter) throws SearchException;

  /**
   * Search for references to the given variable within the given scope.
   * 
   * @param variable the variable being referenced by the found matches
   * @param scope the scope containing the references to be searched, may be <code>null</code> if
   *          all references should be returned
   * @param filter the filter used to determine which matches should be passed to the listener
   *          (those that pass the filter), or <code>null</code> if all of the matches should be
   *          passed to the listener
   * @param listener the listener that will be notified when matches are found
   * @throws SearchException if the results could not be computed
   */
  public void searchReferences(VariableElement variable, SearchScope scope, SearchFilter filter,
      SearchListener listener) throws SearchException;

  /**
   * Synchronously search for subtypes of the given type within the given scope. Return all matches
   * that pass the optional filter.
   * 
   * @param type the type being subtyped by the found matches
   * @param scope the scope containing the subtypes to be searched, may be <code>null</code> if all
   *          subtypes should be returned
   * @param filter the filter used to determine which matches should be returned, or
   *          <code>null</code> if all of the matches should be returned
   * @throws SearchException if the results could not be computed
   */
  public List<SearchMatch> searchSubtypes(ClassElement type, SearchScope scope, SearchFilter filter)
      throws SearchException;

  /**
   * Search for subtypes of the given type within the given scope.
   * 
   * @param type the type being subtyped by the found matches
   * @param scope the scope containing the subtypes to be searched, may be <code>null</code> if all
   *          subtypes should be returned
   * @param filter the filter used to determine which matches should be passed to the listener
   *          (those that pass the filter), or <code>null</code> if all of the matches should be
   *          passed to the listener
   * @param listener the listener that will be notified when matches are found
   * @throws SearchException if the results could not be computed
   */
  public void searchSubtypes(ClassElement type, SearchScope scope, SearchFilter filter,
      SearchListener listener) throws SearchException;

  /**
   * Synchronously search for all of the type declarations that are defined in the given scope and
   * match the given pattern. Return all matches that pass the optional filter.
   * 
   * @param scope the scope containing the type declarations to be searched, may be
   *          <code>null</code> if all declarations should be returned
   * @param pattern the pattern used to determine which type declarations are to be returned
   * @param filter the filter used to determine which matches should be passed to the listener, or
   *          <code>null</code> if all of the matches should be passed to the listener
   * @return the matches that were found
   * @throws SearchException if the results could not be computed
   */
  public List<SearchMatch> searchTypeDeclarations(SearchScope scope, SearchPattern pattern,
      SearchFilter filter) throws SearchException;

  /**
   * Search for all of the type declarations that are defined in the given scope and match the given
   * pattern.
   * 
   * @param scope the scope containing the type declarations to be searched, may be
   *          <code>null</code> if all declarations should be returned
   * @param pattern the pattern used to determine which type declarations are to be returned
   * @param filter the filter used to determine which matches should be passed to the listener
   *          (those that pass the filter), or <code>null</code> if all of the matches should be
   *          passed to the listener
   * @param listener the listener that will be notified when matches are found
   * @throws SearchException if the results could not be computed
   */
  public void searchTypeDeclarations(SearchScope scope, SearchPattern pattern, SearchFilter filter,
      SearchListener listener) throws SearchException;

  /**
   * Synchronously search for all variables matching the given pattern within the given scope.
   * Return all matches that pass the optional filter.
   * 
   * @param scope the scope containing the variable declarations to be searched, may be
   *          <code>null</code> if all declarations should be returned
   * @param pattern the pattern used to determine which variable declarations are to be returned
   * @param filter the filter used to determine which matches should be returned, or
   *          <code>null</code> if all of the matches should be returned
   * @throws SearchException if the results could not be computed
   */
  public List<SearchMatch> searchVariableDeclarations(SearchScope scope, SearchPattern pattern,
      SearchFilter filter) throws SearchException;

  /**
   * Search for all variables matching the given pattern within the given scope.
   * 
   * @param scope the scope containing the variable declarations to be searched, may be
   *          <code>null</code> if all declarations should be returned
   * @param pattern the pattern used to determine which variable declarations are to be returned
   * @param filter the filter used to determine which matches should be passed to the listener
   *          (those that pass the filter), or <code>null</code> if all of the matches should be
   *          passed to the listener
   * @param listener the listener that will be notified when matches are found
   * @throws SearchException if the results could not be computed
   */
  public void searchVariableDeclarations(SearchScope scope, SearchPattern pattern,
      SearchFilter filter, SearchListener listener) throws SearchException;
}
