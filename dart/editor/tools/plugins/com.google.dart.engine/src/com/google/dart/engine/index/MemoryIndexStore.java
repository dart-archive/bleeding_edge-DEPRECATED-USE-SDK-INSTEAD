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

package com.google.dart.engine.index;

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.translation.DartOmit;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * {@link IndexStore} which keeps all information in memory, but can write it to stream and read
 * later.
 * 
 * @coverage dart.engine.index
 */
public interface MemoryIndexStore extends IndexStore {
  /**
   * Reads {@link MemoryIndexStore} for specified {@link AnalysisContext} from the given
   * {@link InputStream}.
   * 
   * @param context the {@link AnalysisContext} to read {@link Element}s and {@link Source}s.
   * @param input the {@link InputStream} from which this index will be read
   * @throws IOException if the index could not be read (for example because of version mismatch)
   */
  @DartOmit
  void readIndex(AnalysisContext context, InputStream input) throws IOException;

  /**
   * Write the contents of this index to the given {@link OutputStream}.
   * 
   * @param context the {@link AnalysisContext} to write {@link Element}s and {@link Source}s.
   * @param output the {@link OutputStream} to which this index will be written
   * @throws IOException if the index could not be written
   */
  @DartOmit
  void writeIndex(AnalysisContext context, OutputStream output) throws IOException;
}
