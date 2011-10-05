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
package com.google.dart.tools.core.mock;

import org.eclipse.core.resources.FileInfoMatcherDescription;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceFilterDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

public class MockContainer extends MockResource implements IContainer {
  public MockContainer(IContainer parent, String name) {
    super(parent, name);
  }

  @Override
  public IResourceFilterDescription createFilter(int type,
      FileInfoMatcherDescription matcherDescription, int updateFlags, IProgressMonitor monitor)
      throws CoreException {
    return null;
  }

  @Override
  public boolean exists(IPath path) {
    return false;
  }

  @Override
  public IFile[] findDeletedMembersWithHistory(int depth, IProgressMonitor monitor)
      throws CoreException {
    return null;
  }

  @Override
  public IResource findMember(IPath path) {
    return null;
  }

  @Override
  public IResource findMember(IPath path, boolean includePhantoms) {
    return null;
  }

  @Override
  public IResource findMember(String name) {
    return null;
  }

  @Override
  public IResource findMember(String name, boolean includePhantoms) {
    return null;
  }

  @Override
  public String getDefaultCharset() throws CoreException {
    return null;
  }

  @Override
  public String getDefaultCharset(boolean checkImplicit) throws CoreException {
    return null;
  }

  @Override
  public IFile getFile(IPath path) {
    return null;
  }

  @Override
  public IResourceFilterDescription[] getFilters() throws CoreException {
    return null;
  }

  @Override
  public IFolder getFolder(IPath path) {
    return null;
  }

  @Override
  public IResource[] members() throws CoreException {
    return null;
  }

  @Override
  public IResource[] members(boolean includePhantoms) throws CoreException {
    return null;
  }

  @Override
  public IResource[] members(int memberFlags) throws CoreException {
    return null;
  }

  @Override
  public void setDefaultCharset(String charset) throws CoreException {
  }

  @Override
  public void setDefaultCharset(String charset, IProgressMonitor monitor) throws CoreException {
  }
}
