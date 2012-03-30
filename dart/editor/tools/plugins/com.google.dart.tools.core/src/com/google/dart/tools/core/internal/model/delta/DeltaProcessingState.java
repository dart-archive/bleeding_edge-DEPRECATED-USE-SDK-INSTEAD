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

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.DartModelManager;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.ElementChangedListener;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.SafeRunner;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Instances of the class <code>DeltaProcessingState</code> keep the global states used during Dart
 * element delta processing.
 */
public class DeltaProcessingState implements IResourceChangeListener {

  /**
   * Collection of listeners for Dart element deltas. These listeners are notified from
   * {@link DeltaProcessor#fire(com.google.dart.tools.core.model.DartElementDelta, int)}.
   * <p>
   * A NPE occurs when the two arrays are different sizes, thus we enforce this with a private int.
   */
  private final static int INIT_ELEMENT_CHANGED_LISTENER_ARRAY_SIZE = 8;
  public ElementChangedListener[] elementChangedListeners = new ElementChangedListener[INIT_ELEMENT_CHANGED_LISTENER_ARRAY_SIZE];
  public int[] elementChangedListenerMasks = new int[INIT_ELEMENT_CHANGED_LISTENER_ARRAY_SIZE];
  public int elementChangedListenerCount = 0;

  /**
   * Collection of pre Dart resource change listeners. These listeners are notified when
   * {@link #resourceChanged(IResourceChangeEvent)} is called, just before when the
   * {@link DeltaProcessor} is called.
   * <p>
   * A NPE occurs when the two arrays are different sizes, thus we enforce this with a private int.
   */
  private final static int INIT_PRE_RESOURCE_CHANGE_ARRAY_SIZE = 1;
  private IResourceChangeListener[] preResourceChangeListeners = new IResourceChangeListener[INIT_PRE_RESOURCE_CHANGE_ARRAY_SIZE];
  private int[] preResourceChangeEventMasks = new int[INIT_PRE_RESOURCE_CHANGE_ARRAY_SIZE];
  private int preResourceChangeListenerCount = 0;

  /**
   * The delta processor for the current thread.
   */
  private ThreadLocal<DeltaProcessor> deltaProcessors = new ThreadLocal<DeltaProcessor>();

  /**
   * Threads that are currently running initializeRoots()
   */
  private Set<Thread> initializingThreads = Collections.synchronizedSet(new HashSet<Thread>());

  /**
   * Workaround for bug 15168 circular errors not reported This is a cache of the projects before
   * any project addition/deletion has started.
   */
  private HashSet<String> dartProjectNamesCache;

  /**
   * A list of DartElement used as a scope for external archives refresh during POST_CHANGE. This is
   * null if no refresh is needed.
   */
  private HashSet<DartElement> externalElementsToRefresh;

  /**
   * Need to clone defensively the listener information, in case some listener is reacting to some
   * notification iteration by adding/changing/removing any of the other (for example, if it
   * deregisters itself).
   * 
   * @see #removeElementChangedListener(ElementChangedListener)
   */
  public synchronized void addElementChangedListener(ElementChangedListener listener, int eventMask) {
    for (int i = 0; i < elementChangedListenerCount; i++) {
      if (elementChangedListeners[i] == listener) {

        // only clone the masks, since we could be in the middle of
        // notifications and one listener decide to change
        // any event mask of another listeners (yet not notified).
        int cloneLength = elementChangedListenerMasks.length;
        System.arraycopy(elementChangedListenerMasks, 0,
            elementChangedListenerMasks = new int[cloneLength], 0, cloneLength);
        elementChangedListenerMasks[i] |= eventMask; // could be different
        return;
      }
    }
    // may need to grow, no need to clone, since iterators will have cached
    // original arrays and max boundary and we only add to the end.
    int length;
    if ((length = elementChangedListeners.length) == elementChangedListenerCount) {
      System.arraycopy(elementChangedListeners, 0,
          elementChangedListeners = new ElementChangedListener[length * 2], 0, length);
      System.arraycopy(elementChangedListenerMasks, 0,
          elementChangedListenerMasks = new int[length * 2], 0, length);
    }
    elementChangedListeners[elementChangedListenerCount] = listener;
    elementChangedListenerMasks[elementChangedListenerCount] = eventMask;
    elementChangedListenerCount++;
  }

  /**
   * Adds the given element to the list of elements used as a scope for external jars refresh.
   */
  public synchronized void addForRefresh(DartElement externalElement) {
    if (externalElementsToRefresh == null) {
      externalElementsToRefresh = new HashSet<DartElement>();
    }
    externalElementsToRefresh.add(externalElement);
  }

  /**
   * Adds a pre-resource change listener onto this class. The listeners are called from
   * {@link DeltaProcessingState#resourceChanged(IResourceChangeEvent)}.
   * 
   * @see #removePreResourceChangedListener(IResourceChangeListener)
   */
  public synchronized void addPreResourceChangedListener(IResourceChangeListener listener,
      int eventMask) {
    for (int i = 0; i < preResourceChangeListenerCount; i++) {
      if (preResourceChangeListeners[i] == listener) {
        preResourceChangeEventMasks[i] |= eventMask;
        return;
      }
    }
    // may need to grow, no need to clone, since iterators will have cached
    // original arrays and max boundary and we only add to the end.
    int length;
    if ((length = preResourceChangeListeners.length) == preResourceChangeListenerCount) {
      System.arraycopy(preResourceChangeListeners, 0,
          preResourceChangeListeners = new IResourceChangeListener[length * 2], 0, length);
      System.arraycopy(preResourceChangeEventMasks, 0,
          preResourceChangeEventMasks = new int[length * 2], 0, length);
    }
    preResourceChangeListeners[preResourceChangeListenerCount] = listener;
    preResourceChangeEventMasks[preResourceChangeListenerCount] = eventMask;
    preResourceChangeListenerCount++;
  }

  public void doNotUse() {
    // reset the delta processor of the current thread to avoid to keep it in
    // memory
    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=269476
    deltaProcessors.set(null);
  }

  public DartProject findDartProject(String name) {
    if (getOldDartProjectNames().contains(name)) {
      return DartModelManager.getInstance().getDartModel().getDartProject(name);
    }
    return null;
  }

  public DeltaProcessor getDeltaProcessor() {
    DeltaProcessor deltaProcessor = deltaProcessors.get();
    if (deltaProcessor != null) {
      return deltaProcessor;
    }
    deltaProcessor = new DeltaProcessor(this, DartModelManager.getInstance());
    deltaProcessors.set(deltaProcessor);
    return deltaProcessor;
  }

  /**
   * Workaround for bug 15168 circular errors not reported Returns the list of Dart projects before
   * resource delta processing has started.
   */
  public synchronized HashSet<String> getOldDartProjectNames() {
    if (dartProjectNamesCache == null) {
      HashSet<String> result = new HashSet<String>();
      DartProject[] projects;
      try {
        projects = DartModelManager.getInstance().getDartModel().getDartProjects();
        for (DartProject dartProject : projects) {
          result.add(dartProject.getElementName());
        }
      } catch (DartModelException dme) {
        return dartProjectNamesCache;
      }
      return dartProjectNamesCache = result;
    }
    return dartProjectNamesCache;
  }

  /**
   * Removes the passed {@link ElementChangedListener} from the {@link #elementChangedListeners}
   * array.
   * 
   * @see #addElementChangedListener(ElementChangedListener, int)
   */
  public synchronized void removeElementChangedListener(ElementChangedListener listener) {
    for (int i = 0; i < elementChangedListenerCount; i++) {
      if (elementChangedListeners[i] == listener) {
        // need to clone defensively since we might be in the middle of listener
        // notifications (#fire)
        int length = elementChangedListeners.length;
        ElementChangedListener[] newListeners = new ElementChangedListener[length];
        System.arraycopy(elementChangedListeners, 0, newListeners, 0, i);
        int[] newMasks = new int[length];
        System.arraycopy(elementChangedListenerMasks, 0, newMasks, 0, i);

        // copy trailing listeners
        int trailingLength = elementChangedListenerCount - i - 1;
        if (trailingLength > 0) {
          System.arraycopy(elementChangedListeners, i + 1, newListeners, i, trailingLength);
          System.arraycopy(elementChangedListenerMasks, i + 1, newMasks, i, trailingLength);
        }

        // update manager listener state (#fire need to iterate over original
        // listeners through a local variable to hold onto
        // the original ones)
        elementChangedListeners = newListeners;
        elementChangedListenerMasks = newMasks;
        elementChangedListenerCount--;
        return;
      }
    }
  }

  public synchronized HashSet<DartElement> removeExternalElementsToRefresh() {
    HashSet<DartElement> result = externalElementsToRefresh;
    externalElementsToRefresh = null;
    return result;
  }

  /**
   * Removes the passed listener from the {@link #preResourceChangeListeners} array.
   * 
   * @see #addPreResourceChangedListener(IResourceChangeListener, int)
   */
  public synchronized void removePreResourceChangedListener(IResourceChangeListener listener) {
    for (int i = 0; i < preResourceChangeListenerCount; i++) {
      if (preResourceChangeListeners[i] == listener) {
        // need to clone defensively since we might be in the middle of listener
        // notifications (#fire)
        int length = preResourceChangeListeners.length;
        IResourceChangeListener[] newListeners = new IResourceChangeListener[length];
        int[] newEventMasks = new int[length];
        System.arraycopy(preResourceChangeListeners, 0, newListeners, 0, i);
        System.arraycopy(preResourceChangeEventMasks, 0, newEventMasks, 0, i);

        // copy trailing listeners
        int trailingLength = preResourceChangeListenerCount - i - 1;
        if (trailingLength > 0) {
          System.arraycopy(preResourceChangeListeners, i + 1, newListeners, i, trailingLength);
          System.arraycopy(preResourceChangeEventMasks, i + 1, newEventMasks, i, trailingLength);
        }

        // update manager listener state (#fire need to iterate over original
        // listeners through a local variable to hold onto
        // the original ones)
        preResourceChangeListeners = newListeners;
        preResourceChangeEventMasks = newEventMasks;
        preResourceChangeListenerCount--;
        return;
      }
    }
  }

  /**
   * Calling this method resets the project names map cache by setting
   * {@link #dartProjectNamesCache} to <code>null</code>.
   */
  public synchronized void resetOldDartProjectNames() {
    dartProjectNamesCache = null;
  }

  /**
   * This is the only method from {@link IResourceChangeListener}, the workspace change listener
   * (this), is attached to the workspace in {@link DartModelManager#startup()}, which is called
   * when this plug-in is loaded from {@link DartCore#start(org.osgi.framework.BundleContext)}.
   * <p>
   * This method is called when a new {@link IResourceChangeEvent} occurs in the workspace.
   * <p>
   * First, this method passes the resource changed event to any listeners
   */
  @Override
  public void resourceChanged(final IResourceChangeEvent event) {
    // for each of the pre-resource change listeners, loop through and notify the listener of the change event
    for (int i = 0; i < preResourceChangeListenerCount; i++) {
      // wrap callbacks with Safe runnable for subsequent listeners to be called
      // when some are causing grief
      final IResourceChangeListener listener = preResourceChangeListeners[i];
      if ((preResourceChangeEventMasks[i] & event.getType()) != 0) {
        SafeRunner.run(new ISafeRunnable() {
          @Override
          public void handleException(Throwable exception) {
            DartCore.logError(
                "Exception occurred in listener of pre Dart resource change notification", exception); //$NON-NLS-1$
          }

          @Override
          public void run() throws Exception {
            listener.resourceChanged(event);
          }
        });
      }
    }
    try {
      getDeltaProcessor().resourceChanged(event);
    } finally {
      // TODO (jerome) see 47631, may want to get rid of following so as to
      // reuse delta processor ?
      if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
        deltaProcessors.set(null);
      } else {
        // If we are going to reuse the delta processor of this thread, don't
        // hang on to state
        // that isn't meant to be reused.
        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=273385
        getDeltaProcessor().overridenEventType = -1;
      }
    }
  }
}
