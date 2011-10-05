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
import java.util.HashMap;
import java.util.Map;

public class MockLibrarySource extends MockSource implements LibrarySource {
  private final Map<String, MockDartSource> sourceMap;

  public MockLibrarySource(String name) throws URISyntaxException {
    super(name);
    sourceMap = new HashMap<String, MockDartSource>();
  }

  public void add(MockDartSource sourceFile) {
    sourceMap.put(sourceFile.getName(), sourceFile);
  }

  @Override
  public LibrarySource getImportFor(String relPath) throws IOException {
    throw new RuntimeException("Should not be called");
  }

  @Override
  public DartSource getSourceFor(String relPath) {
    return sourceMap.get(relPath);
  }

  @Override
  public Reader getSourceReader() throws IOException {
    StringBuilder builder = new StringBuilder(100);
    builder.append("library { source = [");
    for (MockDartSource sourceFile : sourceMap.values()) {
      builder.append(" \"");
      builder.append(sourceFile.getName());
      builder.append("\", ");
    }
    builder.append("] }");
    return new StringReader(builder.toString());
  }
}
