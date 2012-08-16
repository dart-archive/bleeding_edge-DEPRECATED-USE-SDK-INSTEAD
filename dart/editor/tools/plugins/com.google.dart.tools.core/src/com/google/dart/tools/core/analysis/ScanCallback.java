package com.google.dart.tools.core.analysis;

import java.io.File;

/**
 * Used by {@link AnalysisServer#scan(java.io.File, boolean, ScanCallback)} to provide asynchronous
 * feedback and the ability to cancel
 */
public interface ScanCallback {

  /**
   * Utility class for synchronously waiting for a scan to complete.
   * 
   * @see AnalysisServer#scan(File, ScanCallback)
   */
  public static class Sync implements ScanCallback {
    private final Object lock = new Object();
    private boolean complete = false;

    @Override
    public boolean isCanceled() {
      return false;
    }

    @Override
    public void progress(float progress) {
      // ignored
    }

    @Override
    public void scanCanceled(File rootFile) {
      // ignored
    }

    @Override
    public void scanComplete() {
      synchronized (lock) {
        complete = true;
        lock.notifyAll();
      }
    }

    /**
     * Wait the specified number of milliseconds for the scan to complete.
     * 
     * @param milliseconds the maximum number of milliseconds to wait.
     * @return <code>true</code> if the scan completed or <code>false</code> if the scan did not
     *         complete within the specified amount of time.
     */
    public boolean waitForScan(long milliseconds) {
      synchronized (lock) {
        long end = System.currentTimeMillis() + milliseconds;
        while (!complete) {
          long delta = end - System.currentTimeMillis();
          if (delta <= 0) {
            break;
          }
          try {
            lock.wait(delta);
          } catch (InterruptedException e) {
            //$FALL-THROUGH$
          }
        }
        return complete;
      }
    }
  }

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
