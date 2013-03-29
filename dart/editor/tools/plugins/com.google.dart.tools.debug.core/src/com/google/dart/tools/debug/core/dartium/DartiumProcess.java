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

package com.google.dart.tools.debug.core.dartium;

import com.google.dart.tools.debug.core.DartDebugCorePlugin;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.IStreamsProxy;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * This is a Dartium specific implementation of an IProcess.
 */
class DartiumProcess extends PlatformObject implements IProcess {
  private String browserName;
  private DartiumDebugTarget target;
  private Process javaProcess;
  private IStreamsProxy streamsProxy;
  private Map<String, String> attributes = new HashMap<String, String>();
  private Date launchTime;

  public DartiumProcess(DartiumDebugTarget target, String browserName, Process javaProcess) {
    this.target = target;
    this.browserName = browserName;
    this.javaProcess = javaProcess;

    launchTime = new Date();

    if (javaProcess != null) {
      new Thread(new Runnable() {
        @Override
        public void run() {
          waitForExit();
        }
      }).start();
    }
  }

  @Override
  public boolean canTerminate() {
    return !isTerminated();
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Object getAdapter(Class adapter) {
    if (adapter == ILaunch.class) {
      return getLaunch();
    }

    if (adapter == IDebugTarget.class) {
      return target;
    }

    return super.getAdapter(adapter);
  }

  @Override
  public String getAttribute(String key) {
    if (DebugPlugin.ATTR_CONSOLE_ENCODING.equals(key)) {
      return "UTF-8";
    }

    return attributes.get(key);
  }

  @Override
  public int getExitValue() throws DebugException {
    if (javaProcess == null) {
      if (isTerminated()) {
        return 0;
      } else {
        throw new DebugException(new Status(
            IStatus.ERROR,
            DartDebugCorePlugin.PLUGIN_ID,
            "Not yet terminated"));
      }
    } else {
      try {
        return javaProcess.exitValue();
      } catch (IllegalThreadStateException exception) {
        throw new DebugException(new Status(
            IStatus.ERROR,
            DartDebugCorePlugin.PLUGIN_ID,
            exception.toString()));
      }
    }
  }

  @Override
  public String getLabel() {
    return browserName + " (" + launchTime + ")";
  }

  @Override
  public ILaunch getLaunch() {
    return target.getLaunch();
  }

  @Override
  public IStreamsProxy getStreamsProxy() {
    if (streamsProxy == null) {
      streamsProxy = new IStreamsProxy() {
        @Override
        public IStreamMonitor getErrorStreamMonitor() {
          return null;
        }

        @Override
        public IStreamMonitor getOutputStreamMonitor() {
          return target.getOutputStreamMonitor();
        }

        @Override
        public void write(String input) throws IOException {
          // no-op
        }
      };
    }

    return streamsProxy;
  }

  @Override
  public boolean isTerminated() {
    try {
      if (javaProcess != null) {
        javaProcess.exitValue();
        return true;
      }
    } catch (IllegalThreadStateException exception) {

    }

    return false;
  }

  @Override
  public void setAttribute(String key, String value) {
    attributes.put(key, value);
  }

  @Override
  public void terminate() {
    if (javaProcess != null) {
      javaProcess.destroy();
    }
  }

  protected void waitForExit() {
    try {
      javaProcess.waitFor();

      fireTerminateEvent();
    } catch (InterruptedException e) {

    }
  }

  void fireCreationEvent() {
    fireEvent(new DebugEvent(this, DebugEvent.CREATE));
  }

  private void fireEvent(DebugEvent event) {
    DebugPlugin manager = DebugPlugin.getDefault();
    if (manager != null) {
      manager.fireDebugEventSet(new DebugEvent[] {event});
    }
  }

  private void fireTerminateEvent() {
    fireEvent(new DebugEvent(this, DebugEvent.TERMINATE));
  }

}
