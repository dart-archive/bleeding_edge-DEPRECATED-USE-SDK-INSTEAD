/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.tools.debug.ui.launch;

import com.google.common.collect.Lists;
import com.google.dart.tools.debug.ui.internal.util.LaunchUtils;

import org.eclipse.core.resources.IResource;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;

import java.util.List;

/**
 * Launching service.
 */
public class DartLaunchService {

  /**
   * Provides launch lifecycle notifications.
   */
  public interface LaunchListener {

    void launchStarted(ILaunch launch);

    void launchTerminated(ILaunch launch);
  }

  private final List<LaunchListener> listeners = Lists.newArrayList();

  private final ILaunchesListener2 debugLaunchListener = new ILaunchesListener2() {

    @Override
    public void launchesAdded(ILaunch[] launches) {
      notifyLaunchStarted(launches);
    }

    @Override
    public void launchesChanged(ILaunch[] launches) {
      //Ignore
    }

    @Override
    public void launchesRemoved(ILaunch[] launches) {
      //Ignore
    }

    @Override
    public void launchesTerminated(ILaunch[] launches) {
      notifyLaunchTerminated(launches);
    }
  };

  private static DartLaunchService instance;

  public static DartLaunchService getInstance() {
    if (instance == null) {
      instance = new DartLaunchService();
    }
    return instance;
  }

  private DartLaunchService() {
    DebugPlugin.getDefault().getLaunchManager().addLaunchListener(debugLaunchListener);
  }

  public void addListener(LaunchListener listener) {
    listeners.add(listener);
  }

  public void launchInDartium(IResource resource) {
    launch(resource, LaunchUtils.getDartiumLaunchShortcut());
  }

  public void launchInServer(IResource resource) {
    launch(resource, LaunchUtils.getServerLaunchShortcut());
  }

  public void removeListener(LaunchListener listener) {
    listeners.remove(listener);
  }

  private void launch(IResource resource, ILaunchShortcut launchShortcut) {
    ISelection launchedSelection = new StructuredSelection(resource);
    launchShortcut.launch(launchedSelection, ILaunchManager.DEBUG_MODE);
  }

  private void notifyLaunchStarted(ILaunch[] launches) {
    for (LaunchListener listener : listeners) {
      for (ILaunch launch : launches) {
        listener.launchStarted(launch);
      }
    }
  }

  private void notifyLaunchTerminated(ILaunch[] launches) {
    for (LaunchListener listener : listeners) {
      for (ILaunch launch : launches) {
        listener.launchStarted(launch);
      }
    }
  }

}
