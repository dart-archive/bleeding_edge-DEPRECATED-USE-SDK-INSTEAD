/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.server.internal.local.source;

import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.UriKind;

import java.io.File;

/**
 * A Java {@link File} based implementation of {@link Resource}.
 * 
 * @coverage dart.server.local
 */
public class FileResource implements Resource {
  private final File file;

  public FileResource(File file) {
    this.file = file;
  }

  @Override
  public Source createSource(UriKind uriKind) {
    return new FileBasedSource(file, uriKind);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof FileResource)) {
      return false;
    }
    FileResource other = (FileResource) obj;
    return other.file.equals(file);
  }

  @Override
  public boolean exists() {
    return file.exists();
  }

  @Override
  public Resource getChild(String path) {
    File childFile = new File(file, path.replace('/', File.separatorChar));
    return new FileResource(childFile);
  }

  @Override
  public String getPath() {
    return file.getAbsolutePath();
  }

  @Override
  public int hashCode() {
    return file.hashCode();
  }
}
