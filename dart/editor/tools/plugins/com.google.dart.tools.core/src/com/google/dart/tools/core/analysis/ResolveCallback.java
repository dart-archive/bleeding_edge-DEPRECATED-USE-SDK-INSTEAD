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

import com.google.dart.compiler.ast.LibraryUnit;

/**
 * Used by {@link AnalysisServer#resolve(java.io.File, ResolveCallback)} to provide asynchronous
 * results.
 */
public interface ResolveCallback {

  /**
   * Utility class for synchronously obtaining a resolved library.
   * 
   * @see AnalysisServer#resolve(java.io.File, int)
   */
  public static class Sync implements ResolveCallback {
    private final Object lock = new Object();
    private LibraryUnit result;

    @Override
    public void resolved(LibraryUnit libraryUnit) {
      synchronized (lock) {
        this.result = libraryUnit;
        lock.notifyAll();
      }
    }

    /**
     * Wait the specified number of milliseconds for the library to be resolved.
     * 
     * @param milliseconds the maximum number of milliseconds to wait.
     * @return the resolved library or <code>null</code> if the library was not resolved within the
     *         specified amount of time.
     */
    public LibraryUnit waitForResolve(long milliseconds) {
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
   * Called when a library has been resolved
   * 
   * @param libraryUnit the library unit (not <code>null</code>)
   */
  void resolved(LibraryUnit libraryUnit);
}
