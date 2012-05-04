/*
 * Copyright (c) 2012, the Dart project authors.
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
import com.google.dart.compiler.DefaultLibrarySource;
import com.google.dart.compiler.LibrarySource;
import com.google.dart.compiler.util.Paths;

import java.io.File;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * An in-memory {@link LibrarySource} for testing
 */
public class TestLibrarySource implements LibrarySource {
  private final String name;
  private final List<String> imports = new ArrayList<String>();
  private final Map<String, DartSource> dartSources = new HashMap<String, DartSource>();
  private String libSource;
  private String entryPoint;
  private long lastModified;

  public TestLibrarySource(String name) {
    this.name = name;
    libSourceChanged();
  }

  public void addImport(String relPath) {
    imports.add(relPath);
  }

  public void addSource(TestDartSource source) {
    dartSources.put(source.getName(), source);
    source.setLibrary(this);
    libSourceChanged();
  }

  @Override
  public boolean exists() {
    return true;
  }

  public String getEntryPoint() {
    return entryPoint;
  }

  @Override
  public LibrarySource getImportFor(String relPath) {
    return null;
  }

  @Override
  public long getLastModified() {
    return lastModified;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public DartSource getSourceFor(String relPath) {
    return dartSources.get(relPath);
  }

  @Override
  public Reader getSourceReader() {
    if (libSource == null) {
      List<String> sources = new ArrayList<String>(new TreeSet<String>(dartSources.keySet()));
      libSource = DefaultLibrarySource.generateSource(
          getName(),
          new File(getName()),
          Paths.toFiles(imports),
          Paths.toFiles(sources),
          getEntryPoint());
    }
    return new StringReader(libSource);
  }

  @Override
  public String getUniqueIdentifier() {
    return getName();
  }

  @Override
  public URI getUri() {
    try {
      return new URI(getName());
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  public void setEntryPoint(String entryPoint) {
    this.entryPoint = entryPoint;
    libSourceChanged();
  }

  private void libSourceChanged() {
    libSource = null;
    lastModified = System.currentTimeMillis();
  }
}
