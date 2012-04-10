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
package com.google.dart.tools.ui.internal.problemsview;

import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * This class provides notifications for marker and resource change events.
 */
class MarkersChangeService {

  public interface MarkerChangeListener {

    public void handleResourceChange();

  }

  private static MarkersChangeService SERVICE;

  public static MarkersChangeService getService() {
    if (SERVICE == null) {
      SERVICE = new MarkersChangeService();
    }

    return SERVICE;
  }

  private List<MarkerChangeListener> listeners = new ArrayList<MarkerChangeListener>();

  private IResourceChangeListener resourceChangeListener;

  private boolean inBuild;
  private boolean hadMarkerChanges;

  private MarkersChangeService() {

  }

  public synchronized void addListener(MarkerChangeListener listener) {
    if (!serviceRunning()) {
      startService();
    }

    if (!listeners.contains(listener)) {
      listeners.add(listener);
    }
  }

  public synchronized void removeListener(MarkerChangeListener listener) {
    listeners.remove(listener);

    if (listeners.isEmpty()) {
      stopService();
    }
  }

  private void handleChangeEvent(IResourceChangeEvent event) {
    if (event.getType() == IResourceChangeEvent.PRE_BUILD) {
      inBuild = true;
    } else if (event.getType() == IResourceChangeEvent.POST_BUILD) {
      inBuild = false;

      if (hadMarkerChanges) {
        notifyListeners();
      }
    } else {
      // POST_CHANGE
      int markerChangeCount = 0;

      for (String markerType : MarkersUtils.getInstance().getErrorsViewMarkerIds()) {
        IMarkerDelta[] markerDeltas = event.findMarkerDeltas(markerType, true);

        // * @see IResourceDelta#ADDED
        // * @see IResourceDelta#REMOVED
        // * @see IResourceDelta#CHANGED

        markerChangeCount += markerDeltas.length;

        // All we're interested in is that we had some changes.
        if (markerChangeCount > 0) {
          break;
        }
      }

      if (markerChangeCount > 0) {
        if (DartCoreDebug.ANALYSIS_SERVER) {
          notifyListeners();
        } else if (inBuild) {
          hadMarkerChanges = true;
        } else {
          notifyListeners();
        }
      }
    }
  }

  private void notifyListeners() {
    List<MarkerChangeListener> listenersCopy = new ArrayList<MarkerChangeListener>(listeners);

    for (MarkerChangeListener listener : listenersCopy) {
      try {
        listener.handleResourceChange();
      } catch (Throwable t) {
        DartToolsPlugin.log(t);
      }
    }
  }

  private boolean serviceRunning() {
    return resourceChangeListener != null;
  }

  private void startService() {
    resourceChangeListener = new IResourceChangeListener() {
      @Override
      public void resourceChanged(IResourceChangeEvent event) {
        handleChangeEvent(event);
      }
    };

    ResourcesPlugin.getWorkspace().addResourceChangeListener(
        resourceChangeListener,
        IResourceChangeEvent.POST_CHANGE | IResourceChangeEvent.PRE_BUILD
            | IResourceChangeEvent.POST_BUILD);
  }

  private void stopService() {
    if (resourceChangeListener != null) {
      ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangeListener);
    }

    resourceChangeListener = null;
  }

}
