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
package com.google.dart.tools.search.core.text;

import com.google.dart.tools.search.internal.core.text.ExternalFilesOfScopeCalculator;
import com.google.dart.tools.search.internal.core.text.FileNamePatternSearchScope;
import com.google.dart.tools.search.internal.core.text.FilesOfScopeCalculator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.MultiStatus;

import java.io.File;
import java.util.regex.Pattern;

/**
 * A {@link TextSearchScope} defines the scope of a search. The scope consists of all workbench
 * resources that are accepted by {@link #contains(IResourceProxy)} and that either are a root
 * element ({@link #getRoots()}) or have a root element in their parent chain.
 * 
 * @see #newSearchScope(IResource[], java.util.regex.Pattern, boolean)
 */
public abstract class TextSearchScope {

  private static final File[] EMPTY_ARRAY = new File[0];

  /**
   * Creates a scope that consists of all files that match the <code>fileNamePattern</code> and that
   * either are one of the roots, or have one of the roots in their parent chain. If
   * <code>visitDerivedResources</code> is not enabled, all files that are marked derived or have a
   * derived container in their parent chain are not part of the scope.
   * 
   * @param rootResources the resources that are the roots of the scope
   * @param fileNamePattern file name pattern for this scope.
   * @param visitDerivedResources if set also derived folders and files are searched.
   * @return a scope the search scope
   */
  public static TextSearchScope newSearchScope(IResource[] rootResources, Pattern fileNamePattern,
      boolean visitDerivedResources) {
    FileNamePatternSearchScope scope = FileNamePatternSearchScope.newSearchScope(
        new String(),
        rootResources,
        visitDerivedResources);
    scope.setFileNamePattern(fileNamePattern);
    return scope;
  }

  /**
   * Returns if a given resource is part of the scope. If a container is not part of the scope, also
   * all its members are not part of the scope.
   * 
   * @param file the file to test.
   * @return returns <code>true</code> if a resource is part of the scope. if <code>false</code> is
   *         returned the resource and all its children are not part of the scope.
   */
  public abstract boolean contains(File file);

  /**
   * Returns if a given resource is part of the scope. If a container is not part of the scope, also
   * all its members are not part of the scope.
   * 
   * @param proxy the resource proxy to test.
   * @return returns <code>true</code> if a resource is part of the scope. if <code>false</code> is
   *         returned the resource and all its children are not part of the scope.
   */
  public abstract boolean contains(IResourceProxy proxy);

  /**
   * Evaluates all external files in this scope.
   * 
   * @param status a {@link MultiStatus} to collect the error status that occurred while collecting
   *          resources.
   * @return returns the files in the scope.
   */
  public File[] evaluateExternalFilesInScope(MultiStatus status) {
    return new ExternalFilesOfScopeCalculator(this, status).process();
  }

  /**
   * Evaluates all files in this scope.
   * 
   * @param status a {@link MultiStatus} to collect the error status that occurred while collecting
   *          resources.
   * @return returns the files in the scope.
   */
  public IFile[] evaluateFilesInScope(MultiStatus status) {
    return new FilesOfScopeCalculator(this, status).process();
  }

  /**
   * Returns the external resources that form the root. Roots can not contain each other. Root
   * elements are only part of the scope if they are also accepted by {@link #contains(File)}.
   * 
   * @return returns the set of root resources. The default behavior is to return an empty array.
   */
  public File[] getExternalRoots() {
    return EMPTY_ARRAY;
  }

  /**
   * Returns the resources that form the root. Roots can not contain each other. Root elements are
   * only part of the scope if they are also accepted by {@link #contains(IResourceProxy)}.
   * 
   * @return returns the set of root resources. The default behavior is to return the workspace
   *         root.
   */
  public IResource[] getRoots() {
    return new IResource[] {ResourcesPlugin.getWorkspace().getRoot()};
  }

}
