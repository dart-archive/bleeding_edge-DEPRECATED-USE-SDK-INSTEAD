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

package com.google.dart.engine.services.change;

/**
 * Composition of two {@link CompositeChange}s. First change should be displayed in preview, but
 * merged into second one before execution.
 */
public class MergeCompositeChange extends Change {
  private final CompositeChange previewChange;
  private final CompositeChange executeChange;

  public MergeCompositeChange(String name, CompositeChange previewChange,
      CompositeChange executeChange) {
    super(name);
    this.previewChange = previewChange;
    this.executeChange = executeChange;
  }

  /**
   * @return the {@link CompositeChange} to execute.
   */
  public CompositeChange getExecuteChange() {
    return executeChange;
  }

  /**
   * @return the {@link CompositeChange} that should be displayed in preview, but merged into
   *         {@link #getExecuteChange()} for execution.
   */
  public CompositeChange getPreviewChange() {
    return previewChange;
  }
}
