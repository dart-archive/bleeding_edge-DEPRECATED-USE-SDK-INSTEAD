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
package com.google.dart.engine.internal.cache;

import com.google.dart.engine.utilities.source.LineInfo;

/**
 * Instances of the abstract class {@code SourceEntryImpl} implement the behavior common to all
 * {@link SourceEntry source entries}.
 * 
 * @coverage dart.engine
 */
public abstract class SourceEntryImpl implements SourceEntry {
  /**
   * The most recent time at which the state of the source matched the state represented by this
   * entry.
   */
  private long modificationTime;

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
   * Initialize a newly created cache entry to be empty.
   */
  public SourceEntryImpl() {
    super();
  }

  @Override
  public long getModificationTime() {
    return modificationTime;
  }

  @Override
  public CacheState getState(DataDescriptor<?> descriptor) {
    if (descriptor == LINE_INFO) {
      return lineInfoState;
    } else {
      throw new IllegalArgumentException("Invalid descriptor: " + descriptor);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <E> E getValue(DataDescriptor<E> descriptor) {
    if (descriptor == LINE_INFO) {
      return (E) lineInfo;
    } else {
      throw new IllegalArgumentException("Invalid descriptor: " + descriptor);
    }
  }

  /**
   * Set the most recent time at which the state of the source matched the state represented by this
   * entry to the given time.
   * 
   * @param time the new modification time of this entry
   */
  public void setModificationTime(long time) {
    modificationTime = time;
  }

  /**
   * Set the state of the data represented by the given descriptor to the given state.
   * 
   * @param descriptor the descriptor representing the data whose state is to be set
   * @param the new state of the data represented by the given descriptor
   */
  public void setState(DataDescriptor<?> descriptor, CacheState state) {
    if (descriptor == LINE_INFO) {
      lineInfo = updatedValue(state, lineInfo, null);
      lineInfoState = state;
    } else {
      throw new IllegalArgumentException("Invalid descriptor: " + descriptor);
    }
  }

  /**
   * Set the value of the data represented by the given descriptor to the given value.
   * 
   * @param descriptor the descriptor representing the data whose value is to be set
   * @param value the new value of the data represented by the given descriptor
   */
  public <E> void setValue(DataDescriptor<E> descriptor, E value) {
    if (descriptor == LINE_INFO) {
      lineInfo = (LineInfo) value;
      lineInfoState = CacheState.VALID;
    } else {
      throw new IllegalArgumentException("Invalid descriptor: " + descriptor);
    }
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    writeOn(builder);
    return builder.toString();
  }

  /**
   * Copy the information from the given cache entry.
   * 
   * @param entry the cache entry from which information will be copied
   */
  protected void copyFrom(SourceEntryImpl entry) {
    modificationTime = entry.modificationTime;
    lineInfoState = entry.lineInfoState;
    lineInfo = entry.lineInfo;
  }

  /**
   * Given that some data is being transitioned to the given state, return the value that should be
   * kept in the cache.
   * 
   * @param state the state to which the data is being transitioned
   * @param currentValue the value of the data before the transition
   * @param defaultValue the value to be used if the current value is to be removed from the cache
   * @return the value of the data that should be kept in the cache
   */
  protected <E> E updatedValue(CacheState state, E currentValue, E defaultValue) {
    if (state == CacheState.VALID) {
      throw new IllegalArgumentException("Use setValue() to set the state to VALID");
    } else if (state == CacheState.IN_PROCESS) {
      //
      // We can leave the current value in the cache for any 'get' methods to access.
      //
      return currentValue;
    }
    return defaultValue;
  }

  /**
   * Write a textual representation of this entry to the given builder. The result will only be used
   * for debugging purposes.
   * 
   * @param builder the builder to which the text should be written
   */
  protected void writeOn(StringBuilder builder) {
    builder.append("time = ");
    builder.append(Long.toString(modificationTime, 16));
    builder.append("; lineInfo = ");
    builder.append(lineInfoState);
  }
}
