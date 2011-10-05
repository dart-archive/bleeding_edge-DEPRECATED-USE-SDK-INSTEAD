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
package com.google.dart.tools.core.model;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * The interface <code>SourceManipulation</code> defines behavior common to Dart elements that
 * support source code manipulations such as copy, move, rename, and delete.
 */
public interface SourceManipulation {
  /**
   * Copy this element to the given container.
   * 
   * @param container the container
   * @param sibling the sibling element before which the copy should be inserted, or
   *          <code>null</code> if the copy should be inserted as the last child of the container
   * @param rename the new name for the element, or <code>null</code> if the copy retains the name
   *          of this element
   * @param replace <code>true</code> if any existing child in the container with the target name
   *          should be replaced, and <code>false</code> to throw an exception in the event of a
   *          name collision
   * @param monitor a progress monitor
   * @throws DartModelException if this element could not be copied. Reasons include:
   *           <ul>
   *           <li>This Dart element, container element, or sibling does not exist
   *           (ELEMENT_DOES_NOT_EXIST)</li>
   *           <li>A <code>CoreException</code> occurred while updating an underlying resource
   *           <li>The container is of an incompatible type (INVALID_DESTINATION)
   *           <li>The sibling is not a child of the given container (INVALID_SIBLING)
   *           <li>The new name is invalid (INVALID_NAME)
   *           <li>A child in the container already exists with the same name (NAME_COLLISION) and
   *           <code>replace</code> has been specified as <code>false</code>
   *           <li>The container or this element is read-only (READ_ONLY)
   *           </ul>
   * @throws IllegalArgumentException if container is <code>null</code>
   */
  public void copy(DartElement container, DartElement sibling, String rename, boolean replace,
      IProgressMonitor monitor) throws DartModelException;

  /**
   * Delete this element, forcing if specified and necessary.
   * 
   * @param force a flag controlling whether underlying resources that are not in sync with the
   *          local file system will be tolerated (same as the force flag in IResource operations).
   * @param monitor a progress monitor
   * @throws DartModelException if this element could not be deleted. Reasons include:
   *           <ul>
   *           <li>This Dart element does not exist (ELEMENT_DOES_NOT_EXIST)</li>
   *           <li>A <code>CoreException</code> occurred while updating an underlying resource
   *           (CORE_EXCEPTION)</li>
   *           <li>This element is read-only (READ_ONLY)</li>
   *           </ul>
   */
  public void delete(boolean force, IProgressMonitor monitor) throws DartModelException;

  /**
   * Move this element to the given container.
   * 
   * @param container the container
   * @param sibling the sibling element before which the element should be inserted, or
   *          <code>null</code> if the element should be inserted as the last child of the container
   * @param rename the new name for the element, or <code>null</code> if the element retains its
   *          name
   * @param replace <code>true</code> if any existing child in the container with the target name
   *          should be replaced, and <code>false</code> to throw an exception in the event of a
   *          name collision
   * @param monitor a progress monitor
   * @throws DartModelException if this element could not be moved. Reasons include:
   *           <ul>
   *           <li>This Dart element, container element, or sibling does not exist
   *           (ELEMENT_DOES_NOT_EXIST)</li>
   *           <li>A <code>CoreException</code> occurred while updating an underlying resource
   *           <li>The container is of an incompatible type (INVALID_DESTINATION)
   *           <li>The sibling is not a child of the given container (INVALID_SIBLING)
   *           <li>The new name is invalid (INVALID_NAME)
   *           <li>A child in the container already exists with the same name (NAME_COLLISION) and
   *           <code>replace</code> has been specified as <code>false</code>
   *           <li>The container or this element is read-only (READ_ONLY)
   *           </ul>
   * @throws IllegalArgumentException if container is <code>null</code>
   */
  public void move(DartElement container, DartElement sibling, String rename, boolean replace,
      IProgressMonitor monitor) throws DartModelException;

  /**
   * Rename this element to the given name.
   * 
   * @param name the new name for the element
   * @param replace <code>true</code> if any existing element with the target name should be
   *          replaced, and <code>false</code> to throw an exception in the event of a name
   *          collision
   * @param monitor a progress monitor
   * @throws DartModelException if this element could not be renamed. Reasons include:
   *           <ul>
   *           <li>This Dart element does not exist (ELEMENT_DOES_NOT_EXIST)</li>
   *           <li>A <code>CoreException</code> occurred while updating an underlying resource
   *           <li>The new name is invalid (INVALID_NAME)
   *           <li>A child in the container already exists with the same name (NAME_COLLISION) and
   *           <code>replace</code> has been specified as <code>false</code>
   *           <li>This element is read-only (READ_ONLY)
   *           </ul>
   */
  public void rename(String name, boolean replace, IProgressMonitor monitor)
      throws DartModelException;
}
