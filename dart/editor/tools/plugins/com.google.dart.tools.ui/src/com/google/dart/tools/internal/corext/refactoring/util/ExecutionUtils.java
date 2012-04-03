package com.google.dart.tools.internal.corext.refactoring.util;

import com.google.dart.tools.core.DartCore;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * Utilities for executing actions, such as {@link RunnableObjectEx}.
 */
public class ExecutionUtils {
  /**
   * Helper class used in {@link #propagate(Throwable)}.
   */
  private static class ExceptionThrower {
    private static Throwable throwable;

    public static synchronized void spit(Throwable t) {
      ExceptionThrower.throwable = t;
      try {
        ExceptionThrower.class.newInstance();
      } catch (InstantiationException e) {
      } catch (IllegalAccessException e) {
      } finally {
        ExceptionThrower.throwable = null;
      }
    }

    private ExceptionThrower() throws Throwable {
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
    ExceptionThrower.spit(throwable);
    return null;
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

}
