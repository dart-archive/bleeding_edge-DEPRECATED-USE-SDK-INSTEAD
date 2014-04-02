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
import com.google.dart.engine.source.SourceKind;
import com.google.dart.engine.utilities.source.LineInfo;

/**
 * The interface {@code SourceEntry} defines the behavior of objects that maintain the information
 * cached by an analysis context about an individual source, no matter what kind of source it is.
 * <p>
 * Source entries should be treated as if they were immutable unless a writable copy of the entry
 * has been obtained and has not yet been made visible to other threads.
 * 
 * @coverage dart.engine
 */
public interface SourceEntry {
  /**
   * The data descriptor representing the contents of the source.
   */
  public static final DataDescriptor<CharSequence> CONTENT = new DataDescriptor<CharSequence>(
      "DartEntry.CONTENT");

  /**
   * The data descriptor representing the line information.
   */
  public static final DataDescriptor<LineInfo> LINE_INFO = new DataDescriptor<LineInfo>(
      "SourceEntry.LINE_INFO");

  /**
   * Return the exception that caused one or more values to have a state of {@link CacheState#ERROR}
   * .
   * 
   * @return the exception that caused one or more values to be uncomputable
   */
  public AnalysisException getException();

  /**
   * Return {@code true} if the source was explicitly added to the context or {@code false} if the
   * source was implicitly added because it was referenced by another source.
   * 
   * @return {@code true} if the source was explicitly added to the context
   */
  public boolean getExplicitlyAdded();

  /**
   * Return the kind of the source, or {@code null} if the kind is not currently cached.
   * 
   * @return the kind of the source
   */
  public SourceKind getKind();

  /**
   * Return the most recent time at which the state of the source matched the state represented by
   * this entry.
   * 
   * @return the modification time of this entry
   */
  public long getModificationTime();

  /**
   * Return the state of the data represented by the given descriptor.
   * 
   * @param descriptor the descriptor representing the data whose state is to be returned
   * @return the state of the data represented by the given descriptor
   */
  public CacheState getState(DataDescriptor<?> descriptor);

  /**
   * Return the value of the data represented by the given descriptor, or {@code null} if the data
   * represented by the descriptor is not in the cache.
   * 
   * @param descriptor the descriptor representing which data is to be returned
   * @return the value of the data represented by the given descriptor
   */
  public <E> E getValue(DataDescriptor<E> descriptor);

  /**
   * Return a new entry that is initialized to the same state as this entry but that can be
   * modified.
   * 
   * @return a writable copy of this entry
   */
  public SourceEntryImpl getWritableCopy();
}
