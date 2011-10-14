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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.IProgressMonitor;

import java.util.List;

/**
 * The interface <code>DartModel</code> defines the behavior of the root Dart element corresponding
 * to the workspace.
 */
public interface DartModel extends DartElement {
  /**
   * Return <code>true</code> if this Dart model contains a <code>DartElement</code> whose resource
   * is the given resource or a non-Dart resource which is the given resource.
   * <p>
   * Note: no existence check is performed on the argument resource. If it is not accessible (see
   * <code>IResource.isAccessible()</code>) yet but would be located in Dart model range, then this
   * method will return <code>true</code>.
   * <p>
   * If the resource is accessible, it can be reached by navigating the Dart model down using the
   * <code>getChildren()</code> and/or <code>getNonJavaResources()</code> methods.
   * 
   * @param resource the resource to check
   * @return <code>true</code> if the resource is accessible through the Dart model
   */
  public boolean contains(IResource resource);

  /**
   * Copies the given elements to the specified container(s). If one container is specified, all
   * elements are copied to that container. If more than one container is specified, the number of
   * elements and containers must match, and each element is copied to its associated container.
   * <p>
   * Optionally, each copy can be positioned before a sibling element. If <code>null</code> is
   * specified for a given sibling, the copy is inserted as the last child of its associated
   * container.
   * </p>
   * <p>
   * Optionally, each copy can be renamed. If <code>null</code> is specified for the new name, the
   * copy is not renamed.
   * </p>
   * <p>
   * Optionally, any existing child in the destination container with the same name can be replaced
   * by specifying <code>true</code> for <code>replace</code>. Otherwise an exception is thrown in
   * the event that a name collision occurs.
   * </p>
   * 
   * @param elements the elements to be copied
   * @param containers the container, or list of containers, to which the elements will be copied
   * @param siblings the list of siblings element any of which may be <code>null</code>; or
   *          <code>null</code>
   * @param renamings the list of new names any of which may be <code>null</code>; or
   *          <code>null</code>
   * @param replace <code>true</code> if any existing child in a target container with the target
   *          name should be replaced, and <code>false</code> to throw an exception in the event of
   *          a name collision
   * @param monitor a progress monitor
   * @throws DartModelException if an element could not be copied. Reasons include:
   *           <ul>
   *           <li>There is no element to process ( <code>NO_ELEMENTS_TO_PROCESS</code>) because the
   *           given elements is <code>null</code> or empty</li>
   *           <li>A specified element, container, or sibling does not exist (
   *           <code>ELEMENT_DOES_NOT_EXIST</code>)</li>
   *           <li>A <code>CoreException</code> occurred while updating an underlying resource</li>
   *           <li>A container is of an incompatible type ( <code>INVALID_DESTINATION</code>)</li>
   *           <li>A sibling is not a child of its associated container (
   *           <code>INVALID_SIBLING</code>)</li>
   *           <li>A new name is invalid (<code>INVALID_NAME</code>)</li>
   *           <li>A child in its associated container already exists with the same name and
   *           <code>replace</code> has been specified as <code>false</code> (
   *           <code>NAME_COLLISION</code>)</li>
   *           <li>A container or element is read-only (<code>READ_ONLY</code>)</li>
   *           </ul>
   */
  public void copy(DartElement[] elements, DartElement[] containers, DartElement[] siblings,
      String[] renamings, boolean replace, IProgressMonitor monitor) throws DartModelException;

  /**
   * Deletes the given elements, forcing the operation if necessary and specified.
   * 
   * @param elements the elements to be deleted
   * @param force a flag controlling whether underlying resources that are not in sync with the
   *          local file system will be tolerated
   * @param monitor a progress monitor
   * @throws DartModelException if an element could not be deleted. Reasons include:
   *           <ul>
   *           <li>There is no element to process ( <code>NO_ELEMENTS_TO_PROCESS</code>) because the
   *           given elements is <code>null</code> or empty</li>
   *           <li>A specified element does not exist ( <code>ELEMENT_DOES_NOT_EXIST</code>)</li>
   *           <li>A <code>CoreException</code> occurred while updating an underlying resource</li>
   *           <li>An element is read-only (<code>READ_ONLY</code>)</li>
   *           </ul>
   */
  public void delete(DartElement[] elements, boolean force, IProgressMonitor monitor)
      throws DartModelException;

  /**
   * Return an array containing the bundled libraries.
   * 
   * @return an array containing the bundled libraries
   * @throws DartModelException if the bundled libraries could not be accessed for some reason
   */
  public DartLibrary[] getBundledLibraries() throws DartModelException;

  /**
   * Return the bundled core library.
   * 
   * @return the bundled core library
   * @throws DartModelException if the core library could not be accessed for some reason
   */
  public DartLibrary getCoreLibrary() throws DartModelException;

  /**
   * Return the Dart project containing or corresponding to the given resource.
   * 
   * @param resource the resource whose project is to be returned
   * @return the Dart project containing or corresponding to the given resource
   */
  public DartProject getDartProject(IResource resource);

  /**
   * Return the Dart project with the given name.
   * 
   * @param projectName the name of the project to be returned
   * @return the Dart project with the given name
   */
  public DartProject getDartProject(String projectName);

  /**
   * Return an array containing all of the Dart projects currently defined in the workspace.
   * 
   * @return all of the Dart projects currently defined in the workspace
   * @throws DartModelException if the contents of the model cannot be accessed
   */
  public DartProject[] getDartProjects() throws DartModelException;

  /**
   * Return an array of non-Dart resources (that is, non-Dart projects) in the workspace.
   * <p>
   * Non-Dart projects include all projects that are closed (even if they have the Dart nature).
   * 
   * @return an array of non-Dart projects (<code>IProject</code>s) contained in the workspace
   * @throws DartModelException if this element does not exist or if an exception occurs while
   *           accessing its corresponding resource
   */
  public IResource[] getNonDartResources() throws DartModelException;

  /**
   * Return a list containing all of the libraries that are not referenced. A library is referenced
   * if it is either a top-level library or if it is imported by a referenced library.
   * 
   * @return all of the libraries that are not referenced
   * @throws DartModelException if the list of libraries cannot be determined
   */
  public List<DartLibrary> getUnreferencedLibraries() throws DartModelException;

  /**
   * Return the workspace corresponding to this element.
   * 
   * @return the workspace corresponding to this element
   */
  public IWorkspace getWorkspace();

  /**
   * Moves the given elements to the specified container(s). If one container is specified, all
   * elements are moved to that container. If more than one container is specified, the number of
   * elements and containers must match, and each element is moved to its associated container.
   * <p>
   * Optionally, each element can be positioned before a sibling element. If <code>null</code> is
   * specified for a given sibling, the element is inserted as the last child of its associated
   * container.
   * </p>
   * <p>
   * Optionally, each element can be renamed. If <code>null</code> is specified for the new name,
   * the element is not renamed.
   * </p>
   * <p>
   * Optionally, any existing child in the destination container with the same name can be replaced
   * by specifying <code>true</code> for <code>replace</code>. Otherwise an exception is thrown in
   * the event that a name collision occurs.
   * </p>
   * 
   * @param elements the elements to move
   * @param containers the container, or list of containers
   * @param siblings the list of siblings element any of which may be <code>null</code>; or
   *          <code>null</code>
   * @param renamings the list of new names any of which may be <code>null</code>; or
   *          <code>null</code>
   * @param replace <code>true</code> if any existing child in a target container with the target
   *          name should be replaced, and <code>false</code> to throw an exception in the event of
   *          a name collision
   * @param monitor a progress monitor
   * @exception DartModelException if an element could not be moved. Reasons include:
   *              <ul>
   *              <li>There is no element to process ( <code>NO_ELEMENTS_TO_PROCESS</code>) because
   *              the given elements is <code>null</code> or empty</li>
   *              <li>A specified element, container, or sibling does not exist (
   *              <code>ELEMENT_DOES_NOT_EXIST</code>)</li>
   *              <li>A <code>CoreException</code> occurred while updating an underlying resource</li>
   *              <li>A container is of an incompatible type ( <code>INVALID_DESTINATION</code>)</li>
   *              <li>A sibling is not a child of it associated container (
   *              <code>INVALID_SIBLING</code>)</li>
   *              <li>A new name is invalid (<code>INVALID_NAME</code>)</li>
   *              <li>A child in its associated container already exists with the same name and
   *              <code>replace</code> has been specified as <code>false</code> (
   *              <code>NAME_COLLISION</code>)</li>
   *              <li>A container or element is read-only ( <code>READ_ONLY</code>)</li>
   *              </ul>
   * @throws IllegalArgumentException any element or container is <code>null</code>
   */
  public void move(DartElement[] elements, DartElement[] containers, DartElement[] siblings,
      String[] renamings, boolean replace, IProgressMonitor monitor) throws DartModelException;

  /**
   * Renames the given elements as specified. If one container is specified, all elements are
   * renamed within that container. If more than one container is specified, the number of elements
   * and containers must match, and each element is renamed within its associated container.
   * 
   * @param elements the elements to be renamed
   * @param destinations the container, or list of containers
   * @param names the list of new names
   * @param replace <code>true</code> if an existing child in a target container with the target
   *          name should be replaced, and <code>false</code> to throw an exception in the event of
   *          a name collision
   * @param monitor a progress monitor
   * @throws DartModelException if an element could not be renamed. Reasons include:
   *           <ul>
   *           <li>There is no element to process ( <code>NO_ELEMENTS_TO_PROCESS</code>) because the
   *           given elements is <code>null</scode> or empty</li>
   *           <li>A specified element does not exist ( <code>ELEMENT_DOES_NOT_EXIST</code>)</li>
   *           <li>A <code>CoreException</code> occurred while updating an underlying resource
   *           <li>A new name is invalid (<code>INVALID_NAME</code>)
   *           <li>A child already exists with the same name and <code>replace</code> has been
   *           specified as <code>false</code> ( <code>NAME_COLLISION</code>)
   *           <li>An element is read-only (<code>READ_ONLY</code>)
   *           </ul>
   */
  public void rename(DartElement[] elements, DartElement[] destinations, String[] names,
      boolean replace, IProgressMonitor monitor) throws DartModelException;
}
