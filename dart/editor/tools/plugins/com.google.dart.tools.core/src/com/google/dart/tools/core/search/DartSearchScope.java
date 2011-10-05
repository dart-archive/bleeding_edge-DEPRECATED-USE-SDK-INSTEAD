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

import com.google.dart.tools.core.model.DartElement;

/**
 * The interface <code>DartSearchScope</code> defines the behavior common to objects that define
 * where search result should be found by a <code>SearchEngine</code>. Clients must pass an instance
 * of this interface to the <code>search(...)</code> methods. Such an instance can be created using
 * the following factory methods on <code>SearchEngine</code>:
 * <code>createHierarchyScope(Type)</code>, <code>createDartSearchScope(IResource[])</code>,
 * <code>createWorkspaceScope()</code>, or clients may choose to implement this interface.
 * 
 * @deprecated Use the interface SearchScope instead
 */
@Deprecated
public interface DartSearchScope {
  /**
   * Return <code>true</code> if this scope encloses the given element.
   * 
   * @param element the element being checked
   * @return <code>true</code> if the element is in this scope
   */
  public boolean encloses(DartElement element);

  /**
   * Return the paths to the enclosing projects and JARs for this search scope.
   * <ul>
   * <li>If the path is a project path, this is the full path of the project (see
   * <code>IResource.getFullPath()</code>). For example, /MyProject</li>
   * <li>If the path is a JAR path and this JAR is internal to the workspace, this is the full path
   * of the JAR file (see <code>IResource.getFullPath()</code>). For example, /MyProject/mylib.jar</li>
   * <li>If the path is a JAR path and this JAR is external to the workspace, this is the full OS
   * path to the JAR file on the file system. For example, d:\libs\mylib.jar</li>
   * </ul>
   * 
   * @return an array of paths to the enclosing projects and JARS.
   */
//   public IPath[] enclosingProjectsAndJars();

  /**
   * Return <code>true</code> if the resource at the given path is enclosed by this scope.
   * 
   * @param resourcePath the path to the resource being checked
   * @return <code>true</code> if the resource is enclosed by this scope
   */
//  public boolean encloses(String resourcePath);
}
