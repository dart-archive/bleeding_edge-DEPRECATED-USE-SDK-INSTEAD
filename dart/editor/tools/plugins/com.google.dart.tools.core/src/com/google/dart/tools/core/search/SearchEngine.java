/*
 * Copyright (c) 2011, the Dart project authors.
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
import com.google.dart.tools.core.model.Field;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.Type;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * The interface <code>SearchEngine</code> defines the behavior of objects that can be used to
 * search for various pieces of information.
 */
public interface SearchEngine {
  /**
   * Search for all constructors matching the given pattern within the given scope.
   * 
   * @param scope the scope containing the constructor declarations to be searched
   * @param pattern the pattern used to determine which constructor declarations are to be returned
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
   * Search for references to the given function within the given scope.
   * 
   * @param function the function being referenced by the found matches
   * @param scope the scope containing the function declarations to be searched
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
   * Search for references to the given function type alias within the given scope.
   * 
   * @param alias the function type alias being referenced by the found matches
   * @param scope the scope containing the function type alias declarations to be searched
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
   * Search for references to the given field within the given scope.
   * 
   * @param field the field being referenced by the found matches
   * @param scope the scope containing the field declarations to be searched
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
   * Search for references to the given method within the given scope.
   * 
   * @param method the method being referenced by the found matches
   * @param scope the scope containing the method declarations to be searched
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
   * Search for references to the given type within the given scope.
   * 
   * @param type the type being referenced by the found matches
   * @param scope the scope containing the type declarations to be searched
   * @param listener the listener that will be notified when matches are found
   * @param monitor the progress monitor to use for reporting progress to the user. It is the
   *          caller's responsibility to call done() on the given monitor. Accepts <code>null</code>
   *          , indicating that no progress should be reported and that the operation cannot be
   *          canceled.
   * @throws SearchException if the results could not be computed
   */
  public void searchReferences(Type type, SearchScope scope, SearchFilter filter,
      SearchListener listener, IProgressMonitor monitor) throws SearchException;

  /**
   * Search for all of the type declarations that are defined in the given scope, and match the
   * given pattern.
   * 
   * @param scope the scope containing the type declarations to be searched
   * @param pattern the pattern used to determine which type declarations are to be returned
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
   *          caller's responsibility to call done() on the given monitor. Accepts <code>null</code>
   *          , indicating that no progress should be reported and that the operation cannot be
   *          canceled.
   * @throws SearchException if the results could not be computed
   * @deprecated use searchTypeDeclarations(SearchScope, SearchPattern, SearchFilter,
   *             SearchListener, IProgressMonitor)
   */
  @Deprecated
  public void searchTypeDeclarations(SearchScope scope, SearchPattern pattern,
      SearchListener listener, IProgressMonitor monitor) throws SearchException;
}
