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
package com.google.dart.compiler;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;

public class UrlLibrarySource implements LibrarySource {

  public UrlLibrarySource(File file) {

  }

  public UrlLibrarySource(URI uri, Object object) {

  }

  @Override
  public boolean exists() {

    return false;
  }

  @Override
  public LibrarySource getImportFor(String relPath) throws IOException {

    return null;
  }

  @Override
  public long getLastModified() {

    return 0;
  }

  @Override
  public String getName() {

    return null;
  }

  @Override
  public DartSource getSourceFor(String relPath) {

    return null;
  }

  @Override
  public Reader getSourceReader() throws IOException {

    return null;
  }

  @Override
  public String getUniqueIdentifier() {

    return null;
  }

  @Override
  public URI getUri() {

    return null;
  }

}
