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
package com.google.dart.tools.internal.corext.refactoring.util;

import com.google.dart.tools.core.DartCore;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * Utilities for executing actions, such as {@link RunnableObjectEx}.
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class ExecutionUtils {

  /**
   * Helper class used in {@link #propagate(Throwable)}.
   */
  private static class ExceptionThrower {
    private static Throwable throwable;

    public static synchronized void spit(Throwable t) {
      if (System.getProperty("de.ExecutionUtils.propagate().dontThrow") == null) {
        ExceptionThrower.throwable = t;
        try {
          ExceptionThrower.class.newInstance();
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {
        } finally {
          ExceptionThrower.throwable = null;
        }
      }
    }

    private ExceptionThrower() throws Throwable {
      if (System.getProperty("de.ExecutionUtils.propagate().InstantiationException") != null) {
        throw new InstantiationException();
      }
      if (System.getProperty("de.ExecutionUtils.propagate().IllegalAccessException") != null) {
        throw new IllegalAccessException();
      }
      throw throwable;
    }
  }

  /**
   * Propagates {@code Throwable} as-is without any wrapping. This is trick.
   * 
   * @return nothing will ever be returned; this return type is only for your convenience, to use
   *         this method in "throw" statement.
   */
  public static RuntimeException propagate(Throwable throwable) {
    if (System.getProperty("de.ExecutionUtils.propagate().forceReturn") == null) {
      ExceptionThrower.spit(throwable);
    }
    return null;
  }

  /**
   * Runs given {@link RunnableEx} and ignores exception.
   * 
   * @return <code>true</code> if execution was done without exception.
   */
  public static boolean runIgnore(RunnableEx runnable) {
    try {
      runnable.run();
      return true;
    } catch (Throwable e) {
    }
    return false;
  }

  /**
   * Runs given {@link RunnableEx} and logs exception.
   */
  public static boolean runLog(RunnableEx runnable) {
    try {
      runnable.run();
      return true;
    } catch (Throwable e) {
      DartCore.logError(e);
      return false;
    }
  }

  /**
   * Runs given {@link RunnableObjectEx} and re-throws exception.
   * 
   * @return the {@link Object} returned by {@link RunnableObjectEx#run()}.
   */
  public static <T> T runObject(RunnableObjectEx<T> runnable) {
    try {
      return runnable.runObject();
    } catch (Throwable e) {
      throw propagate(e);
    }
  }

  /**
   * Runs given {@link RunnableObjectEx} and re-throws exceptions as {@link CoreException}.
   * 
   * @return the {@link Object} returned by {@link RunnableObjectEx#run()}.
   */
  public static <T> T runObjectCore(RunnableObjectEx<T> runnable) throws CoreException {
    try {
      return runnable.runObject();
    } catch (Throwable e) {
      throw new CoreException(new Status(IStatus.ERROR, DartCore.PLUGIN_ID, e.getMessage(), e));
    }
  }

  /**
   * Runs given {@link RunnableObjectEx} and ignores exception.
   * 
   * @return the {@link Object} returned by {@link RunnableObjectEx#run()}, or default value if
   *         exception happens.
   */
  public static <T> T runObjectIgnore(RunnableObjectEx<T> runnable, T defaultValue) {
    try {
      return runnable.runObject();
    } catch (Throwable e) {
      return defaultValue;
    }
  }

  /**
   * Runs given {@link RunnableEx} and re-throws any exceptions without declaring it.
   */
  public static void runRethrow(RunnableEx runnable) {
    try {
      runnable.run();
    } catch (Throwable e) {
      propagate(e);
    }
  }

  /**
   * Runs given {@link RunnableEx} and re-throws exceptions as {@link CoreException}.
   */
  public static void runRethrowCore(RunnableEx runnable) throws CoreException {
    try {
      runnable.run();
    } catch (Throwable e) {
      throw new CoreException(new Status(IStatus.ERROR, DartCore.PLUGIN_ID, e.getMessage(), e));
    }
  }

  /**
   * Sleeps given number of milliseconds, ignoring exceptions.
   */
  public static void sleep(final int millis) {
    runIgnore(new RunnableEx() {
      @Override
      public void run() throws Exception {
        Thread.sleep(millis);
      }
    });
  }

}
