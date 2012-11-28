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
package com.google.dart.tools.ui.test.runnable;

import org.eclipse.swt.widgets.Display;

import java.util.ArrayList;

/**
 * Performs operations in the UI thread. If the {@link #run()} method of this class is called from
 * an non-UI thread, the instance ensures that it runs in the UI thread by invoking
 * {@link Display#syncExec(Runnable)}, else it executes in the UI thread. All operations are
 * blocking operations.
 */
public abstract class UIThreadRunnable implements Runnable {

  /**
   * The shared display instance.
   */
  private static Display DISPLAY = null;

  /**
   * Gets all the threads in the VM.
   * 
   * @return all the threads in the VM.
   */
  public static Thread[] allThreads() {
    ThreadGroup threadGroup = primaryThreadGroup();

    Thread[] threads = new Thread[64];
    int enumerate = threadGroup.enumerate(threads, true);

    Thread[] result = new Thread[enumerate];
    System.arraycopy(threads, 0, result, 0, enumerate);

    return result;
  }

  /**
   * Executes the {@code toExecute} on the UI thread asynchronously, and does not block the calling
   * thread.
   * 
   * @param display the display on which toExecute must be executed.
   * @param toExecute the runnable to execute.
   */
  public static void asyncExec(Display display, final VoidResult toExecute) {
    new UIThreadRunnable(display, true) {
      @Override
      protected void doRun() {
        toExecute.run();
      }
    }.run();
  }

  /**
   * Executes the {@code toExecute} on the UI thread asynchronously, and does not block the calling
   * thread.
   * 
   * @param toExecute the runnable to execute.
   */
  public static void asyncExec(final VoidResult toExecute) {
    asyncExec(display(), toExecute);
  }

  /**
   * Caches the display for later use.
   * 
   * @return the display.
   */
  public static Display display() {
    if ((DISPLAY == null) || DISPLAY.isDisposed()) {
      DISPLAY = null;
      Thread[] allThreads = allThreads();
      for (Thread thread : allThreads) {
        Display d = Display.findDisplay(thread);
        if (d != null) {
          DISPLAY = d;
          break;
        }
      }
      if (DISPLAY == null) {
        throw new IllegalStateException("Could not find a display"); //$NON-NLS-1$
      }
    }
    return DISPLAY;
  }

  /**
   * Return true if the current thread is the UI thread.
   * 
   * @param display the display
   * @return <code>true</code> if the current thread is the UI thread, <code>false</code> otherwise.
   */
  public static boolean isUIThread(Display display) {
    return display.getThread() == Thread.currentThread();
  }

  /**
   * Gets the primary thread group.
   * 
   * @return the top level thread group.
   */
  public static ThreadGroup primaryThreadGroup() {
    ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
    while (threadGroup.getParent() != null) {
      threadGroup = threadGroup.getParent();
    }
    return threadGroup;
  }

  /**
   * Executes the {@code toExecute} on the display thread, and blocks the calling thread.
   * 
   * @param <T> the type of the result.
   * @param toExecute the runnable to execute.
   * @return the object result of execution on the UI thread.
   */
  public static <T> T[] syncExec(final ArrayResult<T> toExecute) {
    return syncExec(display(), toExecute);
  }

  /**
   * Executes the {@code toExecute} on the display thread, and blocks the calling thread.
   * 
   * @param <T> the type of the result.
   * @param display the display on which toExecute must be executed.
   * @param toExecute the runnable to execute.
   * @return the object result of execution on the UI thread.
   */
  public static <T> T[] syncExec(Display display, final ArrayResult<T> toExecute) {
    final ArrayList<T[]> arrayList = new ArrayList<T[]>();
    new UIThreadRunnable(display) {
      @Override
      protected void doRun() {
        T[] run = toExecute.run();
        arrayList.add(run);
      }
    }.run();
    return arrayList.get(0);
  }

  /**
   * Executes the {@code toExecute} on the display thread, and blocks the calling thread.
   * 
   * @param <T> the type of the result.
   * @param display the display on which toExecute must be executed.
   * @param toExecute the runnable to execute.
   * @return the object result of execution on the UI thread.
   */
  public static <T> T syncExec(Display display, final Result<T> toExecute) {
    final ArrayList<T> arrayList = new ArrayList<T>();
    new UIThreadRunnable(display) {
      @Override
      protected void doRun() {
        arrayList.add(toExecute.run());
      }
    }.run();
    return arrayList.get(0);
  }

  /**
   * Executes the {@code toExecute} on the display thread, and blocks the calling thread.
   * 
   * @param display the display on which toExecute must be executed.
   * @param toExecute the runnable to execute.
   */
  public static void syncExec(Display display, final VoidResult toExecute) {
    new UIThreadRunnable(display) {
      @Override
      protected void doRun() {
        toExecute.run();
      }
    }.run();
  }

  /**
   * Executes the {@code toExecute} on the UI thread, and blocks the calling thread.
   * 
   * @param <T> the type of the result.
   * @param toExecute the runnable to execute.
   * @return the result of executing result on the UI thread.
   */
  public static <T> T syncExec(final Result<T> toExecute) {
    return syncExec(display(), toExecute);
  }

  /**
   * Executes the {@code toExecute} on the UI thread, and blocks the calling thread.
   * 
   * @param toExecute the runnable to execute.
   */
  public static void syncExec(final VoidResult toExecute) {
    syncExec(display(), toExecute);
  }

  /** the display on which runnables must be executed. */
  protected final Display display;

  /**
   * A flag to denote if the runnable should execute asynchronously.
   */
  private final boolean async;

  /**
   * Runs synchronously in the UI thread.
   * 
   * @param display The display to be used.
   */
  private UIThreadRunnable(Display display) {
    this(display, false);
  }

  /**
   * A private contructor use to create this object.
   * 
   * @param display The display to use.
   * @param async if the thread should run asynchronously or not.
   * @see Display#syncExec(Runnable)
   * @see Display#asyncExec(Runnable)
   */
  private UIThreadRunnable(Display display, boolean async) {
    this.display = display;
    this.async = async;
  }

  /**
   * This method is intelligent to execute in the UI thread.
   */
  @Override
  public void run() {
    if ((display == null) || display.isDisposed()) {
      return;
    }

    if (!isUIThread(display)) {
      if (async) {
        display.asyncExec(runnable());
      } else {
        display.syncExec(runnable());
      }
    } else {
      doRun();
    }
  }

  /**
   * Performs the run in the UI Thread.
   * <p>
   * This MUST be invoked in the UI thread.
   * </p>
   */
  protected abstract void doRun();

  /**
   * This dispatched events in the UI thread.
   * <p>
   * This must be called in the UI thread only. This method does not execute in a syncexec/asyncexec
   * block
   * </p>
   */
  private void dispatchAllEvents() {
    display.wake();
    // while (true)
    // if (!display.readAndDispatch())
    // break;
  }

  /**
   * A runnable instance that is used internally.
   * 
   * @return The runnable instance.
   */
  private Runnable runnable() {
    final Runnable runnable = new Runnable() {
      @Override
      public void run() {
        doRun();
        dispatchAllEvents();
      }
    };
    return runnable;
  }

}
