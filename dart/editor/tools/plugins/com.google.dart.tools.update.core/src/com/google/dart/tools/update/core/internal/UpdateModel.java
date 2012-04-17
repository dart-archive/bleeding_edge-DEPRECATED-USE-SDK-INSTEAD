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
package com.google.dart.tools.update.core.internal;

import com.google.dart.tools.update.core.UpdateCore;
import com.google.dart.tools.update.core.UpdateListener;

import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.SafeRunner;

import java.util.ArrayList;

/**
 * Maintains state during the update process.
 */
public class UpdateModel {

  /**
   * State enum.
   */
  enum State {
    UNCHECKED {
      @Override
      public void notify(UpdateListener listener) {
        //no-op
      }
    },
    CHECKING {
      @Override
      public void notify(UpdateListener listener) {
        listener.checkStarted();
      }
    },
    CHECKED {
      @Override
      public void notify(UpdateListener listener) {
        listener.checkComplete();
      }
    },
    AVAILABLE {
      @Override
      public void notify(UpdateListener listener) {
        listener.updateAvailable();
      }
    },
    DOWNLOADING {
      @Override
      public void notify(UpdateListener listener) {
        listener.downloadStarted();
      }
    },
    DOWNLOADED {
      @Override
      public void notify(UpdateListener listener) {
        listener.downloadComplete();
      }
    },
    DOWNLOAD_CANCELLED {
      @Override
      public void notify(UpdateListener listener) {
        listener.downloadCancelled();
      }
    },
    APPLIED {
      @Override
      public void notify(UpdateListener listener) {
        listener.updateApplied();
      }
    };

    public abstract void notify(UpdateListener listener);

  }

  /**
   * Dispatches events in a safe runnable to handle any exceptions.
   */
  private abstract class EventNotifier implements ISafeRunnable {
    @Override
    public void handleException(Throwable exception) {
      UpdateCore.logError(exception);
    }
  }

  State state = State.UNCHECKED;

  private final ArrayList<UpdateListener> listeners = new ArrayList<UpdateListener>();

  /**
   * Add the given update listener.
   * 
   * @param listener the listener to add
   */
  public void addListener(UpdateListener listener) {
    if (!listeners.contains(listener)) {
      listeners.add(listener);
    }
  }

  /**
   * Checks to see if an update is currently being downloaded.
   * 
   * @return <code>true</code> if an update is being downloaded, <code>false</code> otherwise
   */
  public boolean isDownloadingUpdate() {
    return state == State.DOWNLOADING;
  }

  /**
   * Check if the update process is idle (e.g., not downloading or checking for downloads).
   * 
   * @return <code>true</code> if idle, <code>false</code> otherwise
   */
  public boolean isIdle() {
    return state != State.DOWNLOADING && state != State.CHECKING;
  }

  /**
   * Checks to see if an update has been applied (implying we need a restart).
   * 
   * @return <code>true</code> if an update has been applied, <code>false</code> otherwise
   */
  public boolean isUpdateApplied() {
    return state == State.APPLIED;
  }

  /**
   * Checks to see if an update is available for download.
   * 
   * @return <code>true</code> if an update is available, <code>false</code> otherwise
   */
  public boolean isUpdateAvailable() {
    return state == State.AVAILABLE;
  }

  /**
   * Checks to see if an update is downloaded and ready to be applied.
   * 
   * @return <code>true</code> if an update is ready to be applied, <code>false</code> otherwise
   */
  public boolean isUpdateReadyToBeApplied() {
    return state == State.DOWNLOADED;
  }

  /**
   * Remove the given update listener.
   * 
   * @param listener the listener to remove
   */
  public void removeListener(UpdateListener listener) {
    listeners.remove(listener);
  }

  /**
   * Cause the model to transition to the given state (and notify listeners).
   * 
   * @param state the new state
   */
  void enterState(State state) {
    this.state = state;
    notifyListeners(state);
  }

  /**
   * Notify listeners of a state change.
   * 
   * @param newState the new state
   */
  private void notifyListeners(final State newState) {
    for (final UpdateListener listener : listeners) {
      SafeRunner.run(new EventNotifier() {
        @Override
        public void run() throws Exception {
          newState.notify(listener);
        }
      });
    }
  }

}
