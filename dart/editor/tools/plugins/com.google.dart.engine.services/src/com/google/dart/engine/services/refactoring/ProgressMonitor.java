package com.google.dart.engine.services.refactoring;

/**
 * Interface for monitoring the progress of an activity; the methods in this interface are invoked
 * by code that performs the activity.
 */
public interface ProgressMonitor {
  /**
   * Notifies that the main task is beginning. This must only be called once on a given progress
   * monitor instance.
   */
  void beginTask(String name, int totalWork);

  /**
   * Notifies that the main task is completed. This must only be called once on a given progress
   * monitor instance.
   */
  void done();

  /**
   * Internal method to handle scaling correctly. This method must not be called by a client.
   * Clients should always use the method {@link #worked(int)}.
   * 
   * @param work the amount of work done
   */
  void internalWorked(double work);

  /**
   * Returns whether cancelation of current operation has been requested. Long-running operations
   * should poll to see if cancelation has been requested.
   */
  boolean isCanceled();

  /**
   * Sets the cancel state to the given value.
   */
  void setCanceled();

  /**
   * Notifies that a subtask of the main task is beginning. Subtasks are optional; the main task
   * might not have subtasks.
   * 
   * @param name the name (or description) of the subtask
   */
  void subTask(String name);

  /**
   * Notifies that a given number of work unit of the main task has been completed. Note that this
   * amount represents an installment, as opposed to a cumulative amount of work done to date.
   * 
   * @param work a non-negative number of work units just completed
   */
  void worked(int work);
}
