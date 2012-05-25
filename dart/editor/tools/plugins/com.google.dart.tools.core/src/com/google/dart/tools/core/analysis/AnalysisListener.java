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

/**
 * Notified when analysis is performed
 * 
 * @see AnalysisServer#addAnalysisListener(AnalysisListener)
 * @see AnalysisServer#removeAnalysisListener(AnalysisListener)
 */
public interface AnalysisListener {

  /**
   * Implementation of {@link AnalysisListener} which does nothing.
   */
  public static class Empty implements AnalysisListener {
    @Override
    public void discarded(AnalysisEvent event) {
    }

    @Override
    public void idle(boolean idle) {
    }

    @Override
    public void parsed(AnalysisEvent event) {
    }

    @Override
    public void resolved(AnalysisEvent event) {
    }
  };

  /**
   * Called when the server is no longer analyzing a library
   */
  void discarded(AnalysisEvent event);

  /**
   * Called when the server's background thread transitions from busy to idle or idle to busy
   */
  void idle(boolean idle);

  /**
   * Called after files are parsed regardless of whether there were errors.
   */
  void parsed(AnalysisEvent event);

  /**
   * Called after a library has been resolved regardless of whether there were errors.
   */
  void resolved(AnalysisEvent event);
}
