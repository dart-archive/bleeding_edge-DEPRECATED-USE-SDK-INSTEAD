package com.google.dart.tools.internal.corext.refactoring.util;

/**
 * Analog of {@link Runnable} where method <code>run</code> can throw {@link Exception} and returns
 * some {@link Object} value.
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public interface RunnableObjectEx<T> {
  /**
   * Executes operation that can cause {@link Exception}.
   * 
   * @return some {@link Object} result for caller.
   */
  T runObject() throws Exception;
}
