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
package com.google.dart.tools.core.internal.completion;

import com.google.dart.compiler.DartSource;
import com.google.dart.compiler.LibrarySource;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URISyntaxException;

public class MockDartSource extends MockSource implements DartSource {
  private final MockLibrarySource library;
  private final String source;

  public MockDartSource(MockLibrarySource library, String name, String source)
      throws URISyntaxException {
    super(name);
    this.library = library;
    this.source = source;
    this.library.add(this);
  }

  @Override
  public LibrarySource getLibrary() {
    return library;
  }

  @Override
  public String getRelativePath() {
    return getName();
  }

  @Override
  public Reader getSourceReader() throws IOException {
    return new StringReader(source);
  }
}
