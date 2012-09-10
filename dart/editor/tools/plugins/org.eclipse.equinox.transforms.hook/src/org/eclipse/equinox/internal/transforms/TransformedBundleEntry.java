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

import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * This class is capable of providing a transformed version of an entry contained within a base
 * bundle entity.
 */
public class TransformedBundleEntry extends BundleEntry {

  long timestamp;
  private InputStream stream;
  private BundleEntry original;
  private TransformedBundleFile bundleFile;
  private long size;

  /**
   * Create a wrapped bundle entry. Calls to obtain the content of this entry will be resolved via
   * the provided input stream rather than the original.
   * 
   * @param bundleFile the host bundle file
   * @param original the original entry
   * @param wrappedStream the override stream
   */
  public TransformedBundleEntry(TransformedBundleFile bundleFile, BundleEntry original,
      InputStream wrappedStream) {
    this.stream = wrappedStream;
    this.bundleFile = bundleFile;
    this.original = original;
    timestamp = System.currentTimeMillis();
    size = computeSize();
  }

  /**
   * Obtaining the size means inspecting the transformed stream. If this stream does not support
   * marks the stream is drained and a copy is retained for later use. Since OSGi fetches the stream
   * before asking for its size, the size must be determined before OSGi has a chance to ask for the
   * stream.
   */
  public long computeSize() {
    ByteArrayOutputStream tempBuffer = new ByteArrayOutputStream(1024);
    byte[] buffer = new byte[1024];
    int i = 0;
    try {
      while ((i = stream.read(buffer)) > -1) {
        tempBuffer.write(buffer, 0, i);
      }
      if (stream.markSupported()) {
        try {
          stream.reset();
        } catch (IOException e) {
          stream = new ByteArrayInputStream(tempBuffer.toByteArray());
        }
      } else {
        stream = new ByteArrayInputStream(tempBuffer.toByteArray());
      }
    } catch (IOException e) {
      TransformerHook.log(FrameworkLogEntry.ERROR,
          "Problem calculating size of stream for file.  Stream may now be corrupted : " //$NON-NLS-1$
              + getName(), e);

    }
    return tempBuffer.size();

  }

  @Override
  @SuppressWarnings("deprecation")
  public URL getFileURL() {
    try {
      File file = bundleFile.getFile(getName(), false);
      if (file != null) {
        return file.toURL();
      }
    } catch (MalformedURLException e) {
      // This can not happen.
    }
    return null;
  }

  @Override
  public InputStream getInputStream() {
    return stream;
  }

  @Override
  public URL getLocalURL() {
    return getFileURL();
  }

  @Override
  public String getName() {
    return original.getName();
  }

  @Override
  public long getSize() {
    return size;
  }

  @Override
  public long getTime() {
    return timestamp;
  }
}
