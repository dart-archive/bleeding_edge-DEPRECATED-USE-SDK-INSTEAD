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
package org.eclipse.equinox.internal.transforms;

import java.io.IOException;
import java.io.InputStream;

/**
 * An input stream that is based off of another stream. This other stream is provided as needed by
 * an {@link InputStreamProvider} so that the underlying stream is not eagerly loaded.
 */
public class LazyInputStream extends InputStream {

  /**
   * An interface to be implemented by clients that wish to utilize {@link LazyInputStream}s. The
   * implementation of this interface should defer obtaining the desired input stream until
   * absolutely necessary.
   */
  public static interface InputStreamProvider {
    /**
     * Return the input stream.
     * 
     * @return the input stream
     * @throws IOException thrown if there is an issue obtaining the stream
     */
    InputStream getInputStream() throws IOException;
  }

  private InputStreamProvider provider;

  private InputStream original = null;

  /**
   * Construct a new lazy stream based off the given provider.
   * 
   * @param provider the input stream provider. Must not be <code>null</code>.
   */
  public LazyInputStream(InputStreamProvider provider) {
    if (provider == null) {
      throw new IllegalArgumentException();
    }
    this.provider = provider;
  }

  @Override
  public int available() throws IOException {
    initOriginal();
    return original.available();
  }

  @Override
  public void close() throws IOException {
    initOriginal();
    original.close();
  }

  @Override
  public boolean equals(Object obj) {
    try {
      initOriginal();
      return original.equals(obj);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public int hashCode() {
    try {
      initOriginal();
      return original.hashCode();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void mark(int readlimit) {
    try {
      initOriginal();
      original.mark(readlimit);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean markSupported() {
    try {
      initOriginal();
      return original.markSupported();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public int read() throws IOException {
    initOriginal();
    return original.read();
  }

  @Override
  public int read(byte[] b) throws IOException {
    initOriginal();
    return original.read(b);
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    initOriginal();
    return original.read(b, off, len);
  }

  @Override
  public void reset() throws IOException {
    initOriginal();
    original.reset();
  }

  @Override
  public long skip(long n) throws IOException {
    initOriginal();
    return original.skip(n);
  }

  @Override
  public String toString() {
    try {
      initOriginal();
      return original.toString();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void initOriginal() throws IOException {
    if (original == null) {
      original = provider.getInputStream();
    }
  }
}
