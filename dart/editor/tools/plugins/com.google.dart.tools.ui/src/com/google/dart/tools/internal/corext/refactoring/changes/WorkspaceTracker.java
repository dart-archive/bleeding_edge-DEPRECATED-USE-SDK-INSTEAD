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
package com.google.dart.tools.internal.corext.refactoring.changes;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.ListenerList;

public class WorkspaceTracker {

  public interface Listener {
    public void workspaceChanged();
  }

  private class ResourceListener implements IResourceChangeListener {
    @Override
    public void resourceChanged(IResourceChangeEvent event) {
      workspaceChanged();
    }
  }

  public final static WorkspaceTracker INSTANCE = new WorkspaceTracker();
  private ListenerList listenerList;

  private ResourceListener fResourceListener;

  private WorkspaceTracker() {
    listenerList = new ListenerList();
  }

  public void addListener(Listener l) {
    listenerList.add(l);
    if (fResourceListener == null) {
      fResourceListener = new ResourceListener();
      ResourcesPlugin.getWorkspace().addResourceChangeListener(fResourceListener);
    }
  }

  public void removeListener(Listener l) {
    if (listenerList.size() == 0) {
      return;
    }
    listenerList.remove(l);
    if (listenerList.size() == 0) {
      ResourcesPlugin.getWorkspace().removeResourceChangeListener(fResourceListener);
      fResourceListener = null;
    }
  }

  private void workspaceChanged() {
    Object[] listeners = listenerList.getListeners();
    for (int i = 0; i < listeners.length; i++) {
      ((Listener) listeners[i]).workspaceChanged();
    }
  }
}
