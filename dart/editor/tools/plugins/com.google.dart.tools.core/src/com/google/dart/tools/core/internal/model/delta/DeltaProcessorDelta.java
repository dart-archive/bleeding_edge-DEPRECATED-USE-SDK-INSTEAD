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

package com.google.dart.tools.core.internal.model.delta;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.IPath;

/**
 * A class to hold information cloned from an IResourceDelta.
 */
class DeltaProcessorDelta {

  public static final int MOVED_TO = IResourceDelta.MOVED_TO;
  public static final int MOVED_FROM = IResourceDelta.MOVED_FROM;

  public static final int ADDED = IResourceDelta.ADDED;
  public static final int REMOVED = IResourceDelta.REMOVED;
  public static final int CHANGED = IResourceDelta.CHANGED;

  public static final int CONTENT = IResourceDelta.CONTENT;
  public static final int ENCODING = IResourceDelta.ENCODING;

  /**
   * @return a DeltaProcessorDelta cloned from an IResourceDelta
   */
  public static DeltaProcessorDelta createFrom(IResourceDelta delta) {
    if (delta == null) {
      return null;
    } else {
      return new DeltaProcessorDelta(delta);
    }
  }

  private int flags;
  private int kind;
  private IPath movedFromPath;
  private IResource resource;
  private DeltaProcessorDelta[] affectedChildren;

  private DeltaProcessorDelta(IResourceDelta delta) {
    this.flags = delta.getFlags();
    this.kind = delta.getKind();
    this.movedFromPath = delta.getMovedFromPath();
    this.resource = delta.getResource();

    // Create the affected children.
    IResourceDelta[] deltaChildren = delta.getAffectedChildren();

    affectedChildren = new DeltaProcessorDelta[deltaChildren.length];

    for (int i = 0; i < deltaChildren.length; i++) {
      affectedChildren[i] = new DeltaProcessorDelta(deltaChildren[i]);
    }
  }

  public DeltaProcessorDelta[] getAffectedChildren() {
    return affectedChildren;
  }

  public int getFlags() {
    return flags;
  }

  public int getKind() {
    return kind;
  }

  public IPath getMovedFromPath() {
    return movedFromPath;
  }

  public IResource getResource() {
    return resource;
  }

  public boolean isDeleteEvent() {
    if ((flags & REMOVED) != 0) {
      return true;
    }

    for (int i = 0; i < affectedChildren.length; i++) {
      if (affectedChildren[i].isDeleteEvent()) {
        return true;
      }
    }

    return false;
  }

}
