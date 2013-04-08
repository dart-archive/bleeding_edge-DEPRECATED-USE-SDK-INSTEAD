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

package com.google.dart.tools.debug.core.server;

import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.util.NullStreamsProxy;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamsProxy;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * An IProcess implementation for remote command-line debug connections.
 */
public class ServerRemoteProcess extends PlatformObject implements IProcess {
  private ILaunch launch;
  private ServerDebugTarget target;
  private IStreamsProxy streamsProxy;

  private Map<String, String> attributes = new HashMap<String, String>();

  public ServerRemoteProcess(ILaunch launch) {
    this.launch = launch;
  }

  @Override
  public boolean canTerminate() {
    return true;
  }

  public void fireCreateEvent() {
    fireEvent(new DebugEvent(this, DebugEvent.CREATE));
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Object getAdapter(Class adapter) {
    if (adapter.equals(IProcess.class)) {
      return this;
    }

    if (adapter.equals(IDebugTarget.class)) {
      ILaunch launch = getLaunch();
      IDebugTarget[] targets = launch.getDebugTargets();

      for (int i = 0; i < targets.length; i++) {
        if (this.equals(targets[i].getProcess())) {
          return targets[i];
        }
      }

      return null;
    }

    if (adapter.equals(ILaunch.class)) {
      return getLaunch();
    }

    if (adapter.equals(ILaunchConfiguration.class)) {
      return getLaunch().getLaunchConfiguration();
    }

    return super.getAdapter(adapter);
  }

  @Override
  public String getAttribute(String key) {
    return attributes.get(key);
  }

  @Override
  public int getExitValue() throws DebugException {
    if (!isTerminated()) {
      throw new DebugException(new Status(
          IStatus.ERROR,
          DartDebugCorePlugin.PLUGIN_ID,
          "Not yet terminated"));
    }

    return 0;
  }

  @Override
  public String getLabel() {
    return launch.getLaunchConfiguration().getName();
  }

  @Override
  public ILaunch getLaunch() {
    return launch;
  }

  @Override
  public IStreamsProxy getStreamsProxy() {
    if (streamsProxy == null) {
      streamsProxy = new NullStreamsProxy();
    }

    return streamsProxy;
  }

  @Override
  public boolean isTerminated() {
    return !target.getConnection().isConnected();
  }

  @Override
  public void setAttribute(String key, String value) {
    attributes.put(key, value);
  }

  public void setTarget(ServerDebugTarget target) {
    this.target = target;

    this.target.getConnection().addListener(new VMListenerAdapter() {
      @Override
      public void connectionClosed(VmConnection connection) {
        fireTerminateEvent();
      }
    });
  }

  @Override
  public void terminate() throws DebugException {
    try {
      target.getConnection().close();
    } catch (IOException e) {
      throw new DebugException(new Status(
          IStatus.ERROR,
          DartDebugCorePlugin.PLUGIN_ID,
          e.getMessage(),
          e));
    }
  }

  protected void fireTerminateEvent() {
    fireEvent(new DebugEvent(this, DebugEvent.TERMINATE));
  }

  private void fireEvent(DebugEvent event) {
    DebugPlugin manager = DebugPlugin.getDefault();

    if (manager != null) {
      manager.fireDebugEventSet(new DebugEvent[] {event});
    }
  }

}
