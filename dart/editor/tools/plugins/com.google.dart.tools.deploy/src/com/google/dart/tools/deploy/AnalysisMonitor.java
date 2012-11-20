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
package com.google.dart.tools.deploy;

import com.google.dart.tools.core.analysis.AnalysisServer;
import com.google.dart.tools.core.analysis.TaskListener;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

/**
 * Let the user know when analysis is occurring in the background.
 */
public class AnalysisMonitor {

  /**
   * Listen for the {@link AnalysisServer} idle state
   */
  private class Listener implements TaskListener {
    @Override
    public void idle(boolean idle) {
      synchronized (lock) {
        if (idle) {
          lock.notifyAll();
        } else if (job == null) {
          job = new NotificationJob();
          job.schedule();
        }
      }
    }

    @Override
    public void processing(int toBeProcessed) {
      // ignored
    }
  }

  /**
   * Job that lets the user know when background analysis is occurring
   */
  private class NotificationJob extends Job {
    private NotificationJob() {
      super("Background Analysis");
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
      monitor.beginTask("Analysis", IProgressMonitor.UNKNOWN);
      setName("Analyzing...");
      synchronized (lock) {
        while (!server.isIdle()) {
          try {
            lock.wait(5000);
          } catch (InterruptedException e) {
            //$FALL-THROUGH$
          }
        }
        job = null;
      }
      monitor.done();
      return Status.OK_STATUS;
    }
  }

  // Lock on this field before accessing other fields
  private final Object lock = new Object();
  private AnalysisServer server;
  private Job job;

  public AnalysisMonitor(AnalysisServer server) {
    this.server = server;
  }

  public void start() {
    synchronized (lock) {
      server.addIdleListener(new Listener());
    }
  }

}
