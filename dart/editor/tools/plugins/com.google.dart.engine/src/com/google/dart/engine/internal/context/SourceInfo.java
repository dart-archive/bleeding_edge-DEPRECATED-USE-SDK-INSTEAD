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
package com.google.dart.engine.internal.context;

import com.google.dart.engine.source.SourceKind;
import com.google.dart.engine.utilities.source.LineInfo;

/**
 * Instances of the class {@code SourceInfo} maintain the information cached by an analysis context
 * about an individual source.
 * 
 * @coverage dart.engine
 */
public abstract class SourceInfo {
  /**
   * The state of the cached line information.
   */
  private CacheState lineInfoState = CacheState.INVALID;

  /**
   * The line information computed for the source, or {@code null} if the line information is not
   * currently cached.
   */
  private LineInfo lineInfo;

  /**
   * Initialize a newly created information holder to be empty.
   */
  public SourceInfo() {
    super();
  }

  /**
   * Remove the line information from the cache.
   */
  public void clearLineInfo() {
    lineInfo = null;
    lineInfoState = CacheState.FLUSHED;
  }

  /**
   * Return a copy of this information holder.
   * 
   * @return a copy of this information holder
   */
  public abstract SourceInfo copy();

  /**
   * Return the kind of the source, or {@code null} if the kind is not currently cached.
   * 
   * @return the kind of the source
   */
  public abstract SourceKind getKind();

  /**
   * Return the line information computed for the source, or {@code null} if the line information is
   * not currently cached.
   * 
   * @return the line information computed for the source
   */
  public LineInfo getLineInfo() {
    return lineInfo;
  }

  /**
   * Return {@code true} if the line information needs to be recomputed.
   * 
   * @return {@code true} if the line information needs to be recomputed
   */
  public boolean hasInvalidLineInfo() {
    return lineInfoState == CacheState.INVALID;
  }

  /**
   * Mark the line information as needing to be recomputed.
   */
  public void invalidateLineInfo() {
    lineInfoState = CacheState.INVALID;
    lineInfo = null;
  }

  /**
   * Set the line information for the source to the given line information.
   * <p>
   * <b>Note:</b> Do not use this method to clear or invalidate the element. Use either
   * {@link #clearLineInfo()} or {@link #invalidateLineInfo()}.
   * 
   * @param info the line information for the source
   */
  public void setLineInfo(LineInfo info) {
    lineInfo = info;
    lineInfoState = CacheState.VALID;
  }

  /**
   * Copy the information from the given information holder.
   * 
   * @param info the information holder from which information will be copied
   */
  protected void copyFrom(SourceInfo info) {
    lineInfoState = info.lineInfoState;
    lineInfo = info.lineInfo;
  }
}
