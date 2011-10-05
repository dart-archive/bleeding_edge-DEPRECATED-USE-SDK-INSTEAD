/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.indexer.tests.internal;

import java.io.StringReader;

/**
 * Wraps a String as an InputStream.
 */
public class StringInputStream extends ReaderInputStream {
  /**
   * Composes a stream from a String
   * 
   * @param source The string to read from. Must not be <code>null</code>.
   */
  public StringInputStream(String source) {
    super(new StringReader(source));
  }

  /**
   * Composes a stream from a String with the specified encoding
   * 
   * @param source The string to read from. Must not be <code>null</code>.
   * @param encoding The encoding scheme. Also must not be <code>null</code>.
   */
  public StringInputStream(String source, String encoding) {
    super(new StringReader(source), encoding);
  }
}
