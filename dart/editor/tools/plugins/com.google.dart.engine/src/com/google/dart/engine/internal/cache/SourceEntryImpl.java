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

import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.utilities.collection.BooleanArray;
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
   * A bit-encoding of boolean flags associated with this element.
   */
  private int flags;

  /**
   * The exception that caused one or more values to have a state of {@link CacheState#ERROR}.
   */
  private AnalysisException exception;

  /**
   * The state of the cached content.
   */
  private CacheState contentState = CacheState.INVALID;

  /**
   * The content of the source, or {@code null} if the content is not currently cached.
   */
  private CharSequence content;

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
   * The index of the flag indicating whether the source was explicitly added to the context or
   * whether the source was implicitly added because it was referenced by another source.
   */
  private static final int EXPLICITLY_ADDED_FLAG = 0;

  /**
   * Initialize a newly created cache entry to be empty.
   */
  public SourceEntryImpl() {
    super();
  }

  /**
   * Fix the state of the {@link #exception} to match the current state of the entry.
   */
  public void fixExceptionState() {
    if (hasErrorState()) {
      if (exception == null) {
        //
        // This code should never be reached, but is a fail-safe in case an exception is not
        // recorded when it should be.
        //
        exception = new AnalysisException("State set to ERROR without setting an exception");
      }
    } else {
      exception = null;
    }
  }

  /**
   * Return the exception that caused one or more values to have a state of {@link CacheState#ERROR}
   * .
   * 
   * @return the exception that caused one or more values to be uncomputable
   */
  @Override
  public AnalysisException getException() {
    return exception;
  }

  /**
   * Return {@code true} if the source was explicitly added to the context or {@code false} if the
   * source was implicitly added because it was referenced by another source.
   * 
   * @return {@code true} if the source was explicitly added to the context
   */
  @Override
  public boolean getExplicitlyAdded() {
    return getFlag(EXPLICITLY_ADDED_FLAG);
  }

  @Override
  public long getModificationTime() {
    return modificationTime;
  }

  @Override
  public CacheState getState(DataDescriptor<?> descriptor) {
    if (descriptor == CONTENT) {
      return contentState;
    } else if (descriptor == LINE_INFO) {
      return lineInfoState;
    } else {
      throw new IllegalArgumentException("Invalid descriptor: " + descriptor);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <E> E getValue(DataDescriptor<E> descriptor) {
    if (descriptor == CONTENT) {
      return (E) content;
    } else if (descriptor == LINE_INFO) {
      return (E) lineInfo;
    } else {
      throw new IllegalArgumentException("Invalid descriptor: " + descriptor);
    }
  }

  /**
   * Invalidate all of the information associated with this source.
   */
  public void invalidateAllInformation() {
    content = null;
    contentState = CacheState.INVALID;
    lineInfo = null;
    lineInfoState = CacheState.INVALID;
  }

  /**
   * Record that an error occurred while attempting to get the contents of the source represented by
   * this entry. This will set the state of all information, including any resolution-based
   * information, as being in error.
   */
  public void recordContentError() {
    content = null;
    contentState = CacheState.ERROR;
  }

  /**
   * Set the exception that caused one or more values to have a state of {@link CacheState#ERROR} to
   * the given exception.
   * 
   * @param exception the exception that caused one or more values to be uncomputable
   */
  public void setException(AnalysisException exception) {
    this.exception = exception;
  }

  /**
   * Set whether the source was explicitly added to the context to match the given value.
   * 
   * @param explicitlyAdded {@code true} if the source was explicitly added to the context
   */
  public void setExplicitlyAdded(boolean explicitlyAdded) {
    setFlag(EXPLICITLY_ADDED_FLAG, explicitlyAdded);
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
    if (descriptor == CONTENT) {
      content = updatedValue(state, content, null);
      contentState = state;
    } else if (descriptor == LINE_INFO) {
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
    if (descriptor == CONTENT) {
      content = (CharSequence) value;
      contentState = CacheState.VALID;
    } else if (descriptor == LINE_INFO) {
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
   * Set the value of all of the flags with the given indexes to false.
   * 
   * @param indexes the indexes of the flags whose value is to be set to false
   */
  protected void clearFlags(int... indexes) {
    for (int i = 0; i < indexes.length; i++) {
      flags = BooleanArray.set(flags, indexes[i], false);
    }
  }

  /**
   * Copy the information from the given cache entry.
   * 
   * @param entry the cache entry from which information will be copied
   */
  protected void copyFrom(SourceEntryImpl entry) {
    modificationTime = entry.modificationTime;
    flags = entry.flags;
    exception = entry.exception;
    contentState = entry.contentState;
    content = entry.content;
    lineInfoState = entry.lineInfoState;
    lineInfo = entry.lineInfo;
  }

  /**
   * Return the value of the flag with the given index.
   * 
   * @param index the index of the flag whose value is to be returned
   * @return the value of the flag with the given index
   */
  protected boolean getFlag(int index) {
    return BooleanArray.get(flags, index);
  }

  /**
   * Return {@code true} if the state of any data value is {@link CacheState#ERROR}.
   * 
   * @return {@code true} if the state of any data value is {@link CacheState#ERROR}
   */
  protected boolean hasErrorState() {
    return contentState == CacheState.ERROR || lineInfoState == CacheState.ERROR;
  }

  /**
   * Set the value of the flag with the given index to the given value.
   * 
   * @param index the index of the flag whose value is to be returned
   * @param value the value of the flag with the given index
   */
  protected void setFlag(int index, boolean value) {
    flags = BooleanArray.set(flags, index, value);
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
    builder.append(modificationTime);
    builder.append("; content = ");
    builder.append(contentState);
    builder.append("; lineInfo = ");
    builder.append(lineInfoState);
  }
}
