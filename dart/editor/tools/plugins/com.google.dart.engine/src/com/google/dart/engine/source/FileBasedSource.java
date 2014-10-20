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
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.internal.context.PerformanceStatistics;
import com.google.dart.engine.internal.context.TimestampedData;
import com.google.dart.engine.utilities.general.TimeCounter;
import com.google.dart.engine.utilities.general.TimeCounter.TimeCounterHandle;
import com.google.dart.engine.utilities.instrumentation.Instrumentation;
import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;
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
   * The URI from which this source was originally derived.
   */
  private final URI uri;

  /**
   * The file represented by this source.
   */
  private final File file;

  /**
   * The cached encoding for this source.
   */
  private String encoding;

  /**
   * The character set used to decode bytes into characters.
   */
  @DartOmit
  private static final Charset UTF_8_CHARSET = Charset.forName("UTF-8");

  /**
   * Initialize a newly created source object.
   * 
   * @param file the file represented by this source
   */
  public FileBasedSource(File file) {
    this(file.toURI(), file);
  }

  /**
   * Initialize a newly created source object.
   * 
   * @param file the file represented by this source
   * @param uri the URI from which this source was originally derived
   */
  public FileBasedSource(URI uri, File file) {
    this.uri = uri;
    this.file = file;
  }

  @Override
  public boolean equals(Object object) {
    return object != null && object instanceof FileBasedSource
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
      reportIfSlowIO(handle.stop());
    }
  }

  @Override
  @DartOmit
  public void getContentsToReceiver(ContentReceiver receiver) throws Exception {
    TimeCounterHandle handle = PerformanceStatistics.io.start();
    try {
      getContentsFromFileToReceiver(receiver);
    } finally {
      reportIfSlowIO(handle.stop());
    }
  }

  @Override
  public String getEncoding() {
    if (encoding == null) {
      encoding = uri.toString();
    }
    return encoding;
  }

  /**
   * Return the file represented by this source. This is an internal method that is only intended to
   * be used by subclasses of {@link UriResolver} that are designed to work with file-based sources.
   * 
   * @return the file represented by this source
   */
  public File getFile() {
    return file;
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
  public URI getUri() {
    return uri;
  }

  @Override
  public UriKind getUriKind() {
    String scheme = uri.getScheme();
    if (scheme.equals(DartUriResolver.DART_SCHEME)) {
      return UriKind.DART_URI;
    } else if (scheme.equals(FileUriResolver.FILE_SCHEME)) {
      return UriKind.FILE_URI;
    } else if (scheme.equals(JavaUriResolver.JAVA_SCHEME)) {
      return UriKind.JAVA_URI;
    } else if (scheme.equals(PackageUriResolver.PACKAGE_SCHEME)) {
      return UriKind.PACKAGE_URI;
    }
    return UriKind.FILE_URI;
  }

  @Override
  public int hashCode() {
    return file.hashCode();
  }

  @Override
  public boolean isInSystemLibrary() {
    return uri.getScheme().equals(DartUriResolver.DART_SCHEME);
  }

  @Override
  public URI resolveRelativeUri(URI containedUri) throws AnalysisException {
    try {
      URI baseUri = uri;
      boolean isOpaque = uri.isOpaque();
      if (isOpaque) {
        String scheme = uri.getScheme();
        String part = uri.getRawSchemeSpecificPart();
        if (scheme.equals(DartUriResolver.DART_SCHEME) && part.indexOf('/') < 0) {
          part = part + "/" + part + ".dart";
        }
        baseUri = new URI(scheme + ":/" + part);
      }
      URI result = baseUri.resolve(containedUri).normalize();
      if (isOpaque) {
        result = new URI(result.getScheme() + ":" + result.getRawSchemeSpecificPart().substring(1));
      }
      return result;
    } catch (Exception exception) {
      throw new AnalysisException("Could not resolve URI (" + containedUri
          + ") relative to source (" + uri + ")", exception);
    }
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
    long modificationTime = file.lastModified();
    try {
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
        skipOptionalBOM(byteBuffer);
        return new TimestampedData<CharSequence>(modificationTime, UTF_8_CHARSET.decode(byteBuffer));
      }
    } catch (IOException exception) {
      // Ignored so that we can try reading using non-native I/O
    }
    //
    // Eclipse appears to be interrupting the thread sometimes. If we couldn't read the file using
    // the native I/O support, try using the non-native support.
    //
    InputStreamReader reader = null;
    String contents;
    try {
      reader = new InputStreamReader(getFileInputStreamWithoutBOM(), "UTF-8");
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
    long modificationTime = file.lastModified();
    try {
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
        skipOptionalBOM(byteBuffer);
        receiver.accept(UTF_8_CHARSET.decode(byteBuffer), modificationTime);
        return;
      }
    } catch (IOException exception) {
      // Ignored so that we can try reading using non-native I/O
    }
    //
    // Eclipse appears to be interrupting the thread sometimes. If we couldn't read the file using
    // the native I/O support, try using the non-native support.
    //
    InputStreamReader reader = null;
    String contents;
    try {
      reader = new InputStreamReader(getFileInputStreamWithoutBOM(), "UTF-8");
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
   * Returns a {@link FileInputStream} for the {@link #file} with skipped optional leading UTF-8
   * BOM.
   */
  @DartOmit
  private FileInputStream getFileInputStreamWithoutBOM() throws Exception {
    FileInputStream in = new FileInputStream(file);
    // check if there is an UTF-8 BOM
    if (in.read() == (byte) 0xEF && in.read() == (byte) 0xBB && in.read() == (byte) 0xBF) {
      return in;
    }
    // re-open stream
    in.close();
    return new FileInputStream(file);
  }

  /**
   * Record the time the IO took if it was slow
   */
  private void reportIfSlowIO(long nanos) {
    //If slower than 10ms
    if (nanos > 10 * TimeCounter.NANOS_PER_MILLI) {
      InstrumentationBuilder builder = Instrumentation.builder("SlowIO");
      try {
        builder.data("fileName", getFullName());
        builder.metric("IO-Time-Nanos", nanos);
      } finally {
        builder.log();
      }
    }
  }

  /**
   * Skips an optional UTF-8 BOM.
   */
  @DartOmit
  private void skipOptionalBOM(ByteBuffer byteBuffer) {
    if (byteBuffer.remaining() >= 3 && byteBuffer.get(0) == (byte) 0xEF
        && byteBuffer.get(1) == (byte) 0xBB && byteBuffer.get(2) == (byte) 0xBF) {
      byteBuffer.position(3);
    }
  }
}
