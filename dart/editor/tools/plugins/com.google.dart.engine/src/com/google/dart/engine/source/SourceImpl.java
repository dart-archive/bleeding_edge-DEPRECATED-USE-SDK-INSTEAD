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
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

/**
 * Instances of the class {@code SourceImpl} implement a basic source object.
 */
public class SourceImpl implements Source {
  /**
   * The source factory that created this source and that should be used to resolve URI's against
   * this source.
   */
  private SourceFactory factory;

  /**
   * The container containing this source.
   */
  private SourceContainer container;

  /**
   * The file represented by this source.
   */
  private File file;

  /**
   * A flag indicating whether this source is in one of the system libraries.
   */
  private boolean inSystemLibrary;

  /**
   * The character set used to decode bytes into characters.
   */
  private static final Charset UTF_8_CHARSET = Charset.forName("UTF-8");

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
   * @param inSystemLibrary {@code true} if this source is in one of the system libraries
   */
  public SourceImpl(SourceFactory factory, File file, boolean inSystemLibrary) {
    this.factory = factory;
    this.file = file;
    this.inSystemLibrary = inSystemLibrary;
  }

  @Override
  public boolean equals(Object object) {
    return this.getClass() == object.getClass() && file.equals(((SourceImpl) object).file);
  }

  @Override
  public SourceContainer getContainer() {
    if (container == null) {
      container = factory.getContainerMapper().getContainerFor(this);
    }
    return container;
  }

  @Override
  public void getContents(ContentReceiver receiver) throws Exception {
    //
    // First check to see whether our factory has an override for our contents.
    //
    String contents = factory.getContents(this);
    if (contents != null) {
      receiver.accept(contents);
      return;
    }
    //
    // If not, read the contents from the file.
    //
    RandomAccessFile file = new RandomAccessFile(this.file, "r");
    FileChannel channel = null;
    ByteBuffer byteBuffer = null;
    try {
      channel = file.getChannel();
      long size = channel.size();
      if (size > Integer.MAX_VALUE) {
        throw new IllegalStateException("File is too long to be read");
      }
      int length = (int) size;
      byte[] bytes = new byte[length];
      byteBuffer = ByteBuffer.wrap(bytes);
      byteBuffer.position(0);
      byteBuffer.limit(length);
      channel.read(byteBuffer);
    } finally {
      if (channel != null) {
        try {
          channel.close();
        } catch (IOException exception) {
          // Ignored
        }
      }
    }
    byteBuffer.rewind();
    receiver.accept(UTF_8_CHARSET.decode(byteBuffer));
  }

  @Override
  public String getFullName() {
    return file.getAbsolutePath();
  }

  @Override
  public String getShortName() {
    return file.getName();
  }

  @Override
  public int hashCode() {
    return file.hashCode();
  }

  @Override
  public boolean isInSystemLibrary() {
    return inSystemLibrary;
  }

  @Override
  public void resetContainer() {
    container = null;
  }

  @Override
  public Source resolve(String uri) {
    return factory.resolveUri(this, uri);
  }

  /**
   * Return the file represented by this source. This is an internal method that is only intended to
   * be used by {@link UriResolver}.
   * 
   * @return the file represented by this source
   */
  File getFile() {
    return file;
  }
}
