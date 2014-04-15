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

import com.google.dart.engine.internal.context.TimestampedData;

import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

import java.io.File;
import java.io.IOException;

/**
 * Instances of the class {@code TestSource} implement a source object that can be used for testing
 * purposes.
 */
public class TestSource extends FileBasedSource {
  /**
   * The contents of the file represented by this source.
   */
  private String contents;

  /**
   * The modification stamp associated with this source.
   */
  private long modificationStamp;

  /**
   * A flag indicating whether an exception should be generated when an attempt is made to access
   * the contents of this source.
   */
  private boolean generateExceptionOnRead = false;

  /**
   * The number of times that the contents of this source have been requested.
   */
  private int readCount = 0;

  /**
   * Initialize a newly created source object.
   */
  public TestSource() {
    this(createFile("/test.dart"), "");
  }

  /**
   * Initialize a newly created source object. The source object is assumed to not be in a system
   * library.
   * 
   * @param file the file represented by this source
   * @param contents the contents of the file represented by this source
   */
  public TestSource(File file, String contents) {
    super(file);
    this.contents = contents;
    modificationStamp = System.currentTimeMillis();
  }

  /**
   * Initialize a newly created source object with the specified contents.
   * 
   * @param contents the contents of the file represented by this source
   */
  public TestSource(String contents) {
    this(createFile("/test.dart"), contents);
  }

  @Override
  public long getModificationStamp() {
    return modificationStamp;
  }

  /**
   * The number of times that the contents of this source have been requested.
   */
  public int getReadCount() {
    return readCount;
  }

  /**
   * Set the contents of this source to the given contents. This has the side-effect of updating the
   * modification stamp of the source.
   * 
   * @param contents the new contents of this source
   */
  public void setContents(String contents) {
    this.contents = contents;
    modificationStamp = System.currentTimeMillis();
  }

  /**
   * A flag indicating whether an exception should be generated when an attempt is made to access
   * the contents of this source.
   */
  public void setGenerateExceptionOnRead(boolean generate) {
    generateExceptionOnRead = generate;
  }

  @Override
  protected TimestampedData<CharSequence> getContentsFromFile() throws Exception {
    readCount++;
    if (generateExceptionOnRead) {
      throw new IOException("I/O Exception while getting the contents of " + getFullName());
    }
    return new TimestampedData<CharSequence>(modificationStamp, contents);
  }

  @Override
  protected void getContentsFromFileToReceiver(ContentReceiver receiver) throws Exception {
    readCount++;
    if (generateExceptionOnRead) {
      throw new IOException("I/O Exception while getting the contents of " + getFullName());
    }
    receiver.accept(contents, modificationStamp);
  }
}
