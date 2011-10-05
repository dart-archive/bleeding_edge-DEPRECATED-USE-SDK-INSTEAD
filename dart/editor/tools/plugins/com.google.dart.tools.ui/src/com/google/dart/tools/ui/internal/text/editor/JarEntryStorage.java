/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.tools.ui.DartUI;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class JarEntryStorage extends PlatformObject implements IStorage {

  /**
   * Return <code>true</code> if this URI's scheme equals "jar"
   */
  public static boolean isJarUri(URI uri) {
    return uri != null && "jar".equals(uri.getScheme());
  }

  private final URI uri;

  public JarEntryStorage(URI uri) {
    this.uri = uri;
    if (!isJarUri(uri)) {
      throw new IllegalArgumentException();
    }
  }

  @Override
  public InputStream getContents() throws CoreException {
    String path = uri.getSchemeSpecificPart();
    int index = path.indexOf('!');
    try {
      // skip the leading "file:" in the file path
      ZipFile zipFile = new ZipFile(path.substring(5, index));
      // skip the leading "/" in the entry path
      ZipEntry entry = zipFile.getEntry(path.substring(index + 2));
      return zipFile.getInputStream(entry);
    } catch (IOException e) {
      String errMsg = "Failed to open " + uri;
      throw new CoreException(new Status(IStatus.ERROR, DartUI.ID_PLUGIN, errMsg, e));
    }
  }

  @Override
  public IPath getFullPath() {
    return new Path(uri.getSchemeSpecificPart());
  }

  @Override
  public String getName() {
    return getFullPath().lastSegment();
  }

  @Override
  public boolean isReadOnly() {
    return true;
  }
}
