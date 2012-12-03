/*
 * Copyright 2012 Dart project authors.
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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import java.util.ArrayList;

public class MockDelta implements IResourceDelta {

  private final MockResource resource;
  private final int kind;
  private ArrayList<MockDelta> children;

  public MockDelta(MockResource resource, int kind) {
    this.resource = resource;
    this.kind = kind;
  }

  @Override
  public void accept(IResourceDeltaVisitor visitor) throws CoreException {
    accept(visitor, 0);
  }

  @Override
  public void accept(IResourceDeltaVisitor visitor, boolean includePhantoms) throws CoreException {
    accept(visitor, includePhantoms ? IContainer.INCLUDE_PHANTOMS : 0);
  }

  @Override
  public void accept(IResourceDeltaVisitor visitor, int memberFlags) throws CoreException {
    if (visitor.visit(this)) {
      if (children != null) {
        for (MockDelta childDelta : children) {
          childDelta.accept(visitor, memberFlags);
        }
      }
    }
  }

  /**
   * Create a new child delta for the specified resource
   * 
   * @param resource the resource (not <code>null</code>)
   * @param kind the delta kind: one of {@link IResourceDelta#ADDED}, {@link IResourceDelta#CHANGED}
   *          , {@link IResourceDelta#REMOVED}
   * @return the new delta
   */
  public MockDelta add(MockResource resource, int kind) {
    if (!(this.resource instanceof MockContainer)) {
      throw new RuntimeException("Cannot add to a file delta: " + this.resource);
    }
    if (resource.getParent() != this.resource) {
      throw new RuntimeException("Added resource is not a child of the receiver's resource: "
          + resource);
    }
    if (this.kind == ADDED || this.kind == REMOVED) {
      throw new RuntimeException("Cannot add a delta to a delta marked ADDED or REMOVED");
    }
    MockDelta childDelta = new MockDelta(resource, kind);
    if (children == null) {
      children = new ArrayList<MockDelta>();
    }
    children.add(childDelta);
    return childDelta;
  }

  @Override
  public IResourceDelta findMember(IPath path) {
    // TODO Auto-generated method stub
    return null;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Object getAdapter(Class adapter) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public IResourceDelta[] getAffectedChildren() {
    return children.toArray(new IResourceDelta[children.size()]);
  }

  @Override
  public IResourceDelta[] getAffectedChildren(int kindMask) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public IResourceDelta[] getAffectedChildren(int kindMask, int memberFlags) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int getFlags() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public IPath getFullPath() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int getKind() {
    return kind;
  }

  @Override
  public IMarkerDelta[] getMarkerDeltas() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public IPath getMovedFromPath() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public IPath getMovedToPath() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public IPath getProjectRelativePath() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public IResource getResource() {
    return resource;
  }

  @Override
  public String toString() {
    String identifier;
    switch (kind) {
      case ADDED:
        identifier = "ADDED";
      case CHANGED:
        identifier = "CHANGED";
      case REMOVED:
        identifier = "REMOVED";
      default:
        identifier = "UNKNOWN-" + kind;
    }
    return getClass().getSimpleName() + "[" + resource + ", " + identifier + "]";
  }
}
