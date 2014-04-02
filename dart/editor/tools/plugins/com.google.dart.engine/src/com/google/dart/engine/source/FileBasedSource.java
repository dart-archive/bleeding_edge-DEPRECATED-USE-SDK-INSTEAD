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

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.internal.context.PerformanceStatistics;
import com.google.dart.engine.internal.context.TimestampedData;
import com.google.dart.engine.utilities.general.TimeCounter.TimeCounterHandle;
import com.google.dart.engine.utilities.io.FileUtilities;
import com.google.dart.engine.utilities.translation.DartBlockBody;
import com.google.dart.engine.utilities.translation.DartOmit;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

/**
 * Instances of the class {@code FileBasedSource} implement a source that represents a file.
 * 
 * @coverage dart.engine.source
 */
public class FileBasedSource implements Source {
  /**
   * The file represented by this source.
   */
  private final File file;

  /**
   * The cached encoding for this source.
   */
  private String encoding;

  /**
   * The kind of URI from which this source was originally derived.
   */
  private final UriKind uriKind;

  /**
   * The character set used to decode bytes into characters.
   */
  @DartOmit
  private static final Charset UTF_8_CHARSET = Charset.forName("UTF-8");

  /**
   * Initialize a newly created source object. The source object is assumed to not be in a system
   * library.
   * 
   * @param file the file represented by this source
   */
  public FileBasedSource(File file) {
    this(file, UriKind.FILE_URI);
  }

  /**
   * Initialize a newly created source object.
   * 
   * @param file the file represented by this source
   * @param flags {@code true} if this source is in one of the system libraries
   */
  public FileBasedSource(File file, UriKind uriKind) {
    this.file = file;
    this.uriKind = uriKind;
  }

  @Override
  public boolean equals(Object object) {
    return object != null && this.getClass() == object.getClass()
        && file.equals(((FileBasedSource) object).file);
  }

  @Override
  public boolean exists() {
    return file.isFile();
  }

  @Override
  public TimestampedData<CharSequence> getContents() throws Exception {
    TimeCounterHandle handle = PerformanceStatistics.io.start();
    try {
      return getContentsFromFile();
    } finally {
      handle.stop();
    }
  }

  @Override
  @DartOmit
  public void getContentsToReceiver(ContentReceiver receiver) throws Exception {
    TimeCounterHandle handle = PerformanceStatistics.io.start();
    try {
      getContentsFromFileToReceiver(receiver);
    } finally {
      handle.stop();
    }
  }

  @Override
  public String getEncoding() {
    if (encoding == null) {
      encoding = uriKind.getEncoding() + file.toURI().toString();
    }
    return encoding;
  }

  @Override
  public String getFullName() {
    return file.getAbsolutePath();
  }

  @Override
  public long getModificationStamp() {
    return file.lastModified();
  }

  @Override
  public String getShortName() {
    return file.getName();
  }

  @Override
  public UriKind getUriKind() {
    return uriKind;
  }

  @Override
  public int hashCode() {
    return file.hashCode();
  }

  @Override
  public boolean isInSystemLibrary() {
    return uriKind == UriKind.DART_URI;
  }

  @Override
  public Source resolveRelative(URI containedUri) {
    try {
      URI resolvedUri = getFile().toURI().resolve(containedUri).normalize();
      return new FileBasedSource(new File(resolvedUri), uriKind);
    } catch (Exception exception) {
      // Fall through to return null
    }
    return null;
  }

  @Override
  public String toString() {
    if (file == null) {
      return "<unknown source>";
    }
    return file.getAbsolutePath();
  }

  /**
   * Get the contents and timestamp of the underlying file.
   * <p>
   * Clients should consider using the the method {@link AnalysisContext#getContents(Source)}
   * because contexts can have local overrides of the content of a source that the source is not
   * aware of.
   * 
   * @return the contents of the source paired with the modification stamp of the source
   * @throws Exception if the contents of this source could not be accessed
   * @see #getContents()
   */
  @DartBlockBody({"return new TimestampedData<String>(file.lastModified(), file.readAsStringSync());"})
  protected TimestampedData<CharSequence> getContentsFromFile() throws Exception {
    String contents;
    long modificationTime = file.lastModified();
    RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
    FileChannel channel = null;
    ByteBuffer byteBuffer = null;
    try {
      channel = randomAccessFile.getChannel();
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
    } catch (ClosedByInterruptException exception) {
      byteBuffer = null;
    } finally {
      try {
        randomAccessFile.close();
      } catch (IOException closeException) {
        // Ignored
      }
    }
    if (byteBuffer != null) {
      byteBuffer.rewind();
      return new TimestampedData<CharSequence>(modificationTime, UTF_8_CHARSET.decode(byteBuffer));
    }
    //
    // Eclipse appears to be interrupting the thread sometimes. If we couldn't read the file using
    // the native I/O support, try using the non-native support.
    //
    InputStreamReader reader = null;
    try {
      reader = new InputStreamReader(new FileInputStream(file), "UTF-8");
      contents = FileUtilities.getContents(reader);
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException closeException) {
          // Ignored
        }
      }
    }
    return new TimestampedData<CharSequence>(modificationTime, contents);
  }

  /**
   * Get the contents of underlying file and pass it to the given receiver.
   * 
   * @param receiver the content receiver to which the content of this source will be passed
   * @throws Exception if the contents of this source could not be accessed
   * @see #getContentsToReceiver(ContentReceiver)
   */
  @DartOmit
  protected void getContentsFromFileToReceiver(ContentReceiver receiver) throws Exception {
    String contents;
    long modificationTime = file.lastModified();
    RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
    FileChannel channel = null;
    ByteBuffer byteBuffer = null;
    try {
      channel = randomAccessFile.getChannel();
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
    } catch (ClosedByInterruptException exception) {
      byteBuffer = null;
    } finally {
      try {
        randomAccessFile.close();
      } catch (IOException closeException) {
        // Ignored
      }
    }
    if (byteBuffer != null) {
      byteBuffer.rewind();
      receiver.accept(UTF_8_CHARSET.decode(byteBuffer), modificationTime);
      return;
    }
    //
    // Eclipse appears to be interrupting the thread sometimes. If we couldn't read the file using
    // the native I/O support, try using the non-native support.
    //
    InputStreamReader reader = null;
    try {
      reader = new InputStreamReader(new FileInputStream(file), "UTF-8");
      contents = FileUtilities.getContents(reader);
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException closeException) {
          // Ignored
        }
      }
    }
    receiver.accept(contents, modificationTime);
  }

  /**
   * Return the file represented by this source. This is an internal method that is only intended to
   * be used by subclasses of {@link UriResolver} that are designed to work with file-based sources.
   * 
   * @return the file represented by this source
   */
  File getFile() {
    return file;
  }
}
