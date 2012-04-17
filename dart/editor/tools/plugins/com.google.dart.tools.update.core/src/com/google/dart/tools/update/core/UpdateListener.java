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
package com.google.dart.tools.update.core;

/**
 * Instances of <code>UpdateListener</code> are registered with the {@link UpdateManager} and
 * notified of changes in update status.
 */
public interface UpdateListener {

  /**
   * Called when an update check is complete.
   */
  void checkComplete();

  /**
   * Called when an update check has started.
   */
  void checkStarted();

  /**
   * Called when a download is cancelled.
   */
  void downloadCancelled();

  /**
   * Called when an update download is complete.
   */
  void downloadComplete();

  /**
   * Called when a download has started.
   */
  void downloadStarted();

  /**
   * Called when an update has been applied.
   */
  void updateApplied();

  /**
   * Called when an update is available for download.
   */
  void updateAvailable();

  /**
   * Called when an update is staged and ready to be applied.
   */
  void updateStaged();

}
