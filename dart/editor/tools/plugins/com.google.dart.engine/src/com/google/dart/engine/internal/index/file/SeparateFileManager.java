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

package com.google.dart.engine.internal.index.file;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An implementation of {@link FileManager} that keeps each file in a separate file system file.
 */
public class SeparateFileManager implements FileManager {
  private final File base;

  public SeparateFileManager(File base) {
    this.base = base;
    clear();
  }

  @Override
  public void clear() {
    File[] files = base.listFiles();
    if (files != null) {
      for (File file : files) {
        file.delete();
      }
    }
  }

  @Override
  public void delete(String name) {
    new File(base, name).delete();
  }

  @Override
  public InputStream openInputStream(String name) throws Exception {
    File file = getFile(name);
    if (!file.isFile()) {
      return null;
    }
    InputStream stream = new FileInputStream(file);
    return new BufferedInputStream(stream);
  }

  @Override
  public OutputStream openOutputStream(String name) throws Exception {
    File file = getFile(name);
    OutputStream stream = new FileOutputStream(file);
    return new BufferedOutputStream(stream);
  }

  private File getFile(String name) {
    return new File(base, name);
  }
}
