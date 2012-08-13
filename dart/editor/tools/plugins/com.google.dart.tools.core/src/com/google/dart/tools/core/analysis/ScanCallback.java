package com.google.dart.tools.core.analysis;

import java.io.File;

/**
 * Used by {@link AnalysisServer#scan(java.io.File, boolean, ScanCallback)} to provide asynchronous
 * feedback and the ability to cancel
 */
public interface ScanCallback {

  /**
   * Answer <code>true</code> if the scanning process should stop.
   */
  boolean isCanceled();

  /**
   * Called with information about the scanning progress
   * 
   * @param progress a number between 0 and 1 representing current scanning progress
   */
  void progress(float progress);

  /**
   * Called when the scan operation has stopped before completion.
   * 
   * @param rootFile the root directory being scanned.
   */
  void scanCanceled(File rootFile);

  /**
   * Called when the scanning process is complete.
   */
  void scanComplete();
}
