/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.util;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.operation.ModalContext;
import org.eclipse.swt.custom.BusyIndicator;

import java.lang.reflect.InvocationTargetException;

/**
 * A runnable context that shows the busy cursor instead of a progress monitor. Note, that the UI
 * thread is blocked even if the runnable is executed in a separate thread by passing
 * <code>fork= true</code> to the context's run method. Furthermore this context doesn't provide any
 * UI to cancel the operation.
 */
public class BusyIndicatorRunnableContext implements IRunnableContext {

  private static class BusyRunnable implements Runnable {

    private static class ThreadContext extends Thread {
      IRunnableWithProgress fRunnable;
      Throwable fThrowable;

      public ThreadContext(IRunnableWithProgress runnable) {
        this(runnable, "BusyCursorRunnableContext-Thread"); //$NON-NLS-1$
      }

      protected ThreadContext(IRunnableWithProgress runnable, String name) {
        super(name);
        fRunnable = runnable;
      }

      @Override
      public void run() {
        try {
          fRunnable.run(new NullProgressMonitor());
        } catch (InvocationTargetException e) {
          fThrowable = e;
        } catch (InterruptedException e) {
          fThrowable = e;
        } catch (ThreadDeath e) {
          fThrowable = e;
          throw e;
        } catch (RuntimeException e) {
          fThrowable = e;
        } catch (Error e) {
          fThrowable = e;
        }
      }

      void sync() {
        try {
          join();
        } catch (InterruptedException e) {
          // ok to ignore exception
        }
      }
    }

    public Throwable fThrowable;
    private boolean fFork;
    private IRunnableWithProgress fRunnable;

    public BusyRunnable(boolean fork, IRunnableWithProgress runnable) {
      fFork = fork;
      fRunnable = runnable;
    }

    @Override
    public void run() {
      try {
        internalRun(fFork, fRunnable);
      } catch (InvocationTargetException e) {
        fThrowable = e;
      } catch (InterruptedException e) {
        fThrowable = e;
      }
    }

    private void internalRun(boolean fork, final IRunnableWithProgress runnable)
        throws InvocationTargetException, InterruptedException {
      Thread thread = Thread.currentThread();
      // Do not spawn another thread if we are already in a modal context
      // thread or inside a busy context thread.
      if (thread instanceof ThreadContext || ModalContext.isModalContextThread(thread)) {
        fork = false;
      }

      if (fork) {
        final ThreadContext t = new ThreadContext(runnable);
        t.start();
        t.sync();
        // Check if the separate thread was terminated by an exception
        Throwable throwable = t.fThrowable;
        if (throwable != null) {
          if (throwable instanceof InvocationTargetException) {
            throw (InvocationTargetException) throwable;
          } else if (throwable instanceof InterruptedException) {
            throw (InterruptedException) throwable;
          } else if (throwable instanceof OperationCanceledException) {
            throw new InterruptedException();
          } else {
            throw new InvocationTargetException(throwable);
          }
        }
      } else {
        try {
          runnable.run(new NullProgressMonitor());
        } catch (OperationCanceledException e) {
          throw new InterruptedException();
        }
      }
    }
  }

  /*
   * (non-Javadoc) Method declared on IRunnableContext.
   */
  @Override
  public void run(boolean fork, boolean cancelable, IRunnableWithProgress runnable)
      throws InvocationTargetException, InterruptedException {
    BusyRunnable busyRunnable = new BusyRunnable(fork, runnable);
    BusyIndicator.showWhile(null, busyRunnable);
    Throwable throwable = busyRunnable.fThrowable;
    if (throwable instanceof InvocationTargetException) {
      throw (InvocationTargetException) throwable;
    } else if (throwable instanceof InterruptedException) {
      throw (InterruptedException) throwable;
    }
  }
}
