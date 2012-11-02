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
import com.google.dart.tools.core.model.ElementChangedListener;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
   * The delta processor for the current thread.
   */
  private ThreadLocal<DeltaProcessor> deltaProcessors = new ThreadLocal<DeltaProcessor>();

  private ExecutorService threadPool;

  /**
   * Create a DeltaProcessingState instance. This constructor call should be paired with a call to
   * dispose().
   */
  public DeltaProcessingState() {
    ResourcesPlugin.getWorkspace().addResourceChangeListener(
        this,
        IResourceChangeEvent.POST_CHANGE | IResourceChangeEvent.PRE_DELETE
            | IResourceChangeEvent.PRE_CLOSE);

    threadPool = Executors.newSingleThreadExecutor();
  }

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
        System.arraycopy(
            elementChangedListenerMasks,
            0,
            elementChangedListenerMasks = new int[cloneLength],
            0,
            cloneLength);
        elementChangedListenerMasks[i] |= eventMask; // could be different
        return;
      }
    }
    // may need to grow, no need to clone, since iterators will have cached
    // original arrays and max boundary and we only add to the end.
    int length;
    if ((length = elementChangedListeners.length) == elementChangedListenerCount) {
      System.arraycopy(
          elementChangedListeners,
          0,
          elementChangedListeners = new ElementChangedListener[length * 2],
          0,
          length);
      System.arraycopy(
          elementChangedListenerMasks,
          0,
          elementChangedListenerMasks = new int[length * 2],
          0,
          length);
    }
    elementChangedListeners[elementChangedListenerCount] = listener;
    elementChangedListenerMasks[elementChangedListenerCount] = eventMask;
    elementChangedListenerCount++;
  }

  public void dispose() {
    ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);

    try {
      threadPool.awaitTermination(200, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      DartCore.logError(e);
    }
  }

  public IDeltaProcessor getDeltaProcessor() {
    DeltaProcessor deltaProcessor = deltaProcessors.get();
    if (deltaProcessor != null) {
      return deltaProcessor;
    }
    deltaProcessor = new DeltaProcessor(this, DartModelManager.getInstance());
    deltaProcessors.set(deltaProcessor);
    return deltaProcessor;
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
    try {
      if (event.getDelta() != null) {
        IResourceDelta[] children = event.getDelta().getAffectedChildren();

        if (children.length > 0
            && children[0].getResource().getProject().hasNature(DartCore.DART_PROJECT_NATURE)) {
          processEvent(event);
        }
      } else if (event.getResource().getProject().hasNature(DartCore.DART_PROJECT_NATURE)) {
        processEvent(event);
      }
    } catch (CoreException e) {
      DartCore.logError(e);
    }
  }

  /**
   * Given an IResourceChangeEvent, convert its information into a DeltaProcessorDelta object, and
   * process that information in a separate thread.
   * 
   * @param event
   */
  private void processEvent(IResourceChangeEvent event) {
    final int eventType = event.getType();
    final IResource resource = event.getResource();
    final DeltaProcessorDelta delta = DeltaProcessorDelta.createFrom(event.getDelta());

    // Add the processing work to the end of the thread queue.
    threadPool.submit(new Runnable() {
      @Override
      public void run() {
        try {
          DeltaProcessor deltaProcessor = ((DeltaProcessor) getDeltaProcessor());

          deltaProcessor.handleResourceChanged(eventType, resource, delta);
        } catch (Throwable t) {
          DartCore.logError(t);
        } finally {
          if (eventType == IResourceChangeEvent.POST_CHANGE) {
            deltaProcessors.set(null);
          }
        }
      }
    });
  }

}
