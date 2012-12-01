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
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import java.util.ArrayList;

public abstract class MockContainer extends MockResource implements IContainer {
  private ArrayList<MockResource> children;

  public MockContainer(IContainer parent, String name) {
    super(parent, name);
  }

  public MockContainer(IContainer parent, String name, boolean exists) {
    super(parent, name, exists);
  }

  @Override
  public void accept(IResourceProxyVisitor visitor, int memberFlags) throws CoreException {
    if (visitor.visit(new MockProxy(this))) {
      if (children != null) {
        for (MockResource child : children) {
          child.accept(visitor, memberFlags);
        }
      }
    }
  }

  /**
   * Add the specified child to the receiver.
   * 
   * @param child the child to be added (not <code>null</code>)
   * @return the child added
   */
  public MockResource add(MockResource child) {
    if (child == null) {
      throw new IllegalArgumentException();
    }
    if (children == null) {
      children = new ArrayList<MockResource>();
    }
    children.add(child);
    return child;
  }

  /**
   * Create a {@link MockFile} and add it to the receiver as a child
   */
  public MockFile addFile(String name) {
    MockFile file = new MockFile(this, name);
    add(file);
    return file;
  }

  /**
   * Create a {@link MockFolder} and add it to the receiver as a child
   */
  public MockFolder addFolder(String name) {
    MockFolder folder = new MockFolder(this, name);
    add(folder);
    return folder;
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
    if (path == null || path.segmentCount() == 0) {
      return null;
    }
    String firstSegment = path.segment(0);
    if (children != null) {
      for (MockResource child : children) {
        if (child.getName().equals(firstSegment)) {
          if (path.segmentCount() == 1) {
            if (child instanceof MockFile) {
              return (IFile) child;
            }
            return new MockFile(this, firstSegment, false);
          }
          return ((MockContainer) child).getFile(path.removeFirstSegments(1));
        }
      }
    }
    if (path.segmentCount() == 1) {
      return new MockFile(this, firstSegment, false);
    }
    return new MockFolder(this, firstSegment, false).getFile(path.removeFirstSegments(1));
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
