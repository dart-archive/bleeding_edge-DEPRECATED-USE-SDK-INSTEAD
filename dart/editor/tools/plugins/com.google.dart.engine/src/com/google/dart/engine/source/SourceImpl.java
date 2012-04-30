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
package com.google.dart.engine.source;

import java.io.File;

/**
 * Instances of the class <code>SourceImpl</code> implement a basic source object.
 */
public class SourceImpl implements Source {
  /**
   * The source factory that created this source and that should be used to resolve URI's against
   * this source.
   */
  private SourceFactory factory;

  /**
   * The file represented by this source.
   */
  private File file;

  /**
   * A flag indicating whether this source is in one of the system libraries.
   */
  private boolean inSystemLibrary;

  /**
   * Initialize a newly created source object. The source object is assumed to not be in a system
   * library.
   * 
   * @param factory the source factory that created this source
   * @param file the file represented by this source
   */
  public SourceImpl(SourceFactory factory, File file) {
    this(factory, file, false);
  }

  /**
   * Initialize a newly created source object.
   * 
   * @param factory the source factory that created this source
   * @param file the file represented by this source
   * @param inSystemLibrary <code>true</code> if this source is in one of the system libraries
   */
  public SourceImpl(SourceFactory factory, File file, boolean inSystemLibrary) {
    this.factory = factory;
    this.file = file;
    this.inSystemLibrary = inSystemLibrary;
  }

  @Override
  public File getFile() {
    return file;
  }

  @Override
  public boolean isInSystemLibrary() {
    return inSystemLibrary;
  }

  @Override
  public Source resolve(String uri) {
    return factory.resolveUri(this, uri);
  }
}
