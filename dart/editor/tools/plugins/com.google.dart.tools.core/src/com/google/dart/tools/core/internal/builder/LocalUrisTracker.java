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
package com.google.dart.tools.core.internal.builder;

import com.google.common.collect.Maps;
import com.google.dart.tools.core.internal.util.ResourceUtil;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

import java.net.URI;
import java.util.Map;

/**
 * Helper for tracking which {@link URI}s are local or not.
 */
public class LocalUrisTracker {
  private static final Map<URI, Boolean> localUris = Maps.newHashMap();

  static {
    trackResourceChanges();
  }

  /**
   * @return <code>true</code> if the given {@link URI} has local {@link IResource}.
   */
  public static boolean isLocal(URI uri) {
    Boolean local = localUris.get(uri);
    if (local == null) {
      IResource resource = ResourceUtil.getResource(uri);
      local = resource != null && resource.getProject().isOpen();
      localUris.put(uri, local);
    }
    return local.booleanValue();
  }

  /**
   * Tracks changes in the workspace and clears {@link #localUris} cache.
   */
  private static void trackResourceChanges() {
    ResourcesPlugin.getWorkspace().addResourceChangeListener(new IResourceChangeListener() {
      @Override
      public void resourceChanged(IResourceChangeEvent event) {
        IResourceDelta delta = event.getDelta();
        if (delta != null && delta.getKind() == IResourceDelta.CHANGED) {
          try {
            delta.accept(new IResourceDeltaVisitor() {
              @Override
              public boolean visit(IResourceDelta delta) throws CoreException {
                int kind = delta.getKind();
                if (kind == IResourceDelta.ADDED || kind == IResourceDelta.REMOVED) {
                  localUris.clear();
                }
                return true;
              }
            });
          } catch (Throwable e) {
          }
        }
      }
    });
  }
}
