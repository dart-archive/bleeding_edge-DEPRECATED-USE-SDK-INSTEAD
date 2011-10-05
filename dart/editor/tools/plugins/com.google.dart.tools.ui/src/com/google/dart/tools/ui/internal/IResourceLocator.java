/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal;

import com.google.dart.tools.core.model.DartModelException;

import org.eclipse.core.resources.IResource;

/**
 * This interface allows you to locate different resources which are related to an object.
 */
public interface IResourceLocator {
  /**
   * Returns the resource that contains the element. If the element is not directly contained by a
   * resource then a helper resource or <code>null</code> is returned. Clients define the helper
   * resource as needed.
   * 
   * @param element the element for which the resource is located
   * @return the containing resource
   * @exception JavaModelException if the element does not exist or if an exception occurs while
   *              accessing its containing resource
   */
  IResource getContainingResource(Object element) throws DartModelException;

  /**
   * Returns the resource that corresponds directly to the element, or <code>null</code> if there is
   * no resource that corresponds to the element.
   * <p>
   * For example, the corresponding resource for an <code>ICompilationUnit</code> is its underlying
   * <code>IFile</code>. The corresponding resource for an <code>IPackageFragment</code> that is not
   * contained in an archive is its underlying <code>IFolder</code>. An
   * <code>IPackageFragment</code> contained in an archive has no corresponding resource. Similarly,
   * there are no corresponding resources for <code>IMethods</code>, <code>IFields</code>, etc.
   * 
   * @param element the element for which the resource is located
   * @return the corresponding resource
   * @exception JavaModelException if the element does not exist or if an exception occurs while
   *              accessing its corresponding resource
   * @see org.eclipse.jdt.core.IJavaElement#getCorrespondingResource()
   */
  IResource getCorrespondingResource(Object element) throws DartModelException;

  /**
   * Returns the underlying finest granularity resource that contains the element, or
   * <code>null</code> if the element is not contained in a resource (for example, a working copy,
   * or an element contained in an external archive).
   * 
   * @param element the element for which the resource is located
   * @return the underlying resource
   * @exception JavaModelException if the element does not exist or if an exception occurs while
   *              accessing its underlying resource
   * @see org.eclipse.jdt.core.IJavaElement#getUnderlyingResource()
   */
  IResource getUnderlyingResource(Object element) throws DartModelException;
}
