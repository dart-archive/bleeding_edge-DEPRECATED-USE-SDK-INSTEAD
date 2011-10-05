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
package com.google.dart.tools.core.internal.compiler;

import com.google.dart.compiler.DartSource;
import com.google.dart.compiler.LibrarySource;

import java.io.Reader;
import java.io.StringReader;
import java.net.URI;

/**
 * An in-memory {@link DartSource} for testing
 */
public class TestDartSource implements DartSource {
  private String name;
  private String source;
  private long lastModified;
  private TestLibrarySource library;

  /**
   * Initialize a new Dart source to have the given content.
   * 
   * @param source the source being represented
   */
  public TestDartSource(String name, String source) {
    this.name = name;
    setSource(source);
  }

  @Override
  public boolean exists() {
    return true;
  }

  @Override
  public long getLastModified() {
    return lastModified;
  }

  @Override
  public LibrarySource getLibrary() {
    return library;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getRelativePath() {
    return name;
  }

  public String getSource() {
    return source;
  }

  @Override
  public Reader getSourceReader() {
    return new StringReader(getSource());
  }

  @Override
  public URI getUri() {
    return URI.create(getName()).normalize();
  }

  public void setLibrary(TestLibrarySource library) {
    this.library = library;
  }

  public void setSource(String source) {
    this.source = source;
    lastModified = System.currentTimeMillis();
  }
}
