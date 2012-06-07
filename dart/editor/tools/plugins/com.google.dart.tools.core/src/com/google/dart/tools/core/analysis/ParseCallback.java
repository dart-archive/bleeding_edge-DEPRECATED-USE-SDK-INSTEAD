/*
 * Copyright 2012 Dart project authors.
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
package com.google.dart.tools.core.analysis;

import com.google.dart.compiler.ast.DartUnit;

import java.io.File;

/**
 * Used by {@link Context#parse(java.io.File, File, ParseCallback)} to provide asynchronous results.
 */
public interface ParseCallback {

  /**
   * Utility class for synchronously obtaining a resolved library.
   * 
   * @see Context#resolve(java.io.File, int)
   */
  public static class Sync implements ParseCallback {
    private final Object lock = new Object();
    private DartUnit result;

    @Override
    public void parsed(DartUnit unit) {
      synchronized (lock) {
        this.result = unit;
        lock.notifyAll();
      }
    }

    /**
     * Wait the specified number of milliseconds for the file to be parsed.
     * 
     * @param milliseconds the maximum number of milliseconds to wait.
     * @return the parsed dart source file or <code>null</code> if the file was not parsed in the
     *         specified amount of time.
     */
    public DartUnit waitForParse(long milliseconds) {
      synchronized (lock) {
        long end = System.currentTimeMillis() + milliseconds;
        while (result == null) {
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
        return result;
      }
    }
  }

  /**
   * Called when the file has been parsed. This unit may or may not be resolved.
   * 
   * @param unit the parsed unit (not <code>null</code>)
   */
  void parsed(DartUnit unit);
}
