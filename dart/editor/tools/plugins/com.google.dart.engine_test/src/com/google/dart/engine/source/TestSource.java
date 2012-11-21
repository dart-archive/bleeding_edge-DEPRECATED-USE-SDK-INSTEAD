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
 * Instances of the class {@code TestSource} implement a source object that can be used for testing
 * purposes.
 */
public class TestSource extends SourceImpl {
  /**
   * The contents of the file represented by this source.
   */
  private String contents;

  /**
   * Initialize a newly created source object.
   */
  public TestSource() {
    this(null, new File("/test.dart"), "");
  }

  /**
   * Initialize a newly created source object. The source object is assumed to not be in a system
   * library.
   * 
   * @param factory the source factory that created this source
   * @param file the file represented by this source
   * @param contents the contents of the file represented by this source
   */
  public TestSource(SourceFactory factory, File file, String contents) {
    super(factory, file);
    this.contents = contents;
  }

  @Override
  public void getContents(ContentReceiver receiver) throws Exception {
    receiver.accept(contents);
  }
}
