/*
 * Copyright (c) 2013, the Dart project authors.
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

package com.google.dart.tools.internal.search.ui;

import com.google.common.annotations.VisibleForTesting;

import org.eclipse.ui.part.Page;

/**
 * {@link Page} to show in {@link SearchView}.
 * 
 * @coverage dart.editor.ui.search
 */
public abstract class SearchPage extends Page {
  /**
   * @return the time when query was last time finished.
   */
  @VisibleForTesting
  public abstract long getLastQueryFinishTime();

  /**
   * @return the time when query was last time started.
   */
  @VisibleForTesting
  public abstract long getLastQueryStartTime();

  /**
   * Notifies this {@link SearchPage} that it is shown now.
   */
  public abstract void show();
}
