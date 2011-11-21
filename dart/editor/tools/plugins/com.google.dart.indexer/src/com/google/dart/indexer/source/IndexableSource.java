/*
 * Copyright 2011, the Dart project authors.
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
package com.google.dart.indexer.source;

import org.eclipse.core.resources.IFile;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * The class <code>IndexableSource</code> defines the behavior common to objects that represent a
 * source of data that can be indexed.
 */
public abstract class IndexableSource {
  /**
   * The scheme used to represent a file.
   */
  protected static final String FILE_SCHEME = "file"; //$NON-NLS-1$

  /**
   * The scheme used to represent an entry in a JAR file.
   */
  protected static final String JAR_SCHEME = "jar"; //$NON-NLS-1$

  /**
   * The separator used between the URI of the JAR file and the path to the entry in a jar: URI.
   */
  protected static final String JAR_SUFFIX = "!/"; //$NON-NLS-1$

  /**
   * Return a representation of the data source represented by the given file. The file must be a
   * local file that produces either a file: or jar: URI.
   * 
   * @param file the file to be represented
   * @return a representation of the data source represented by the given file
   */
  @Deprecated
  public static IndexableSource from(IFile file) {
    return from(file.getLocationURI());
  }

  /**
   * Return a representation of the data source represented by the given URI. The URI must have one
   * the following schemes, which will be interpreted as described: <blockquote>
   * <dl>
   * <dt>file</dt>
   * <dd>The URI is interpreted as described by {@link File#File(URI)}.</dd>
   * <dt>jar</dt>
   * <dd>The scheme specific part of the URI is split at the first occurrence of the JAR file suffix
   * ("!/"). The portion before the suffix is interpreted as a file: URI as described by
   * {@link File#File(URI)}. The portion after the suffix is interpreted as a path to the entry
   * within the JAR file that contains the data to be indexed. Other portions of the URI are
   * ignored.</dd>
   * </dl>
   * </blockquote> Any other form of URI will result in an exception being thrown. In particular, a
   * URI with an empty scheme will <b>not</b> be interpreted as a file: URI.
   * 
   * @param uri the URI to be used to create the indexable source
   * @return a representation of the data source represented by the given URI
   */
  public static IndexableSource from(URI uri) {
    String scheme = uri.getScheme();
    if (scheme.equals(FILE_SCHEME)) {
      return new FileSource(new File(uri));
    } else if (scheme.equals(JAR_SCHEME)) {
      String entrySpecification = uri.getSchemeSpecificPart();
      int index = entrySpecification.indexOf(JAR_SUFFIX);
      if (index < 0) {
        throw new IllegalArgumentException("Invalid jar URI syntax: " + uri.toString());
      }
      String fileUri = entrySpecification.substring(0, index);
      String entryPath = entrySpecification.substring(index + JAR_SUFFIX.length());
      if (fileUri.length() == 0) {
        throw new IllegalArgumentException("Invalid jar URI syntax - missing jar file URI: "
            + uri.toString());
      } else if (entryPath.length() == 0) {
        throw new IllegalArgumentException("Invalid jar URI syntax - missing entry path: "
            + uri.toString());
      }
      try {
        return new JarSource(new File(new URI(fileUri)), entryPath);
      } catch (URISyntaxException exception) {
        if (fileUri.length() == 0 || entryPath.length() == 0) {
          throw new IllegalArgumentException("Invalid jar URI syntax - invalid jar file URI: "
              + uri.toString(), exception);
        }
      }
    }
    throw new IllegalArgumentException("Cannot create indexable source from URI: " + uri.toString());
  }

  /**
   * Return the file extension for this source.
   * 
   * @return the file extension for this source
   */
  public abstract String getFileExtension();

  /**
   * Return an indication of when the source was last modified. The stamp can be any monotonically
   * increasing value, such as a modification time for a file or a simple counter.
   * 
   * @return the modification stamp associated with the source
   */
  public abstract long getModificationStamp();

  /**
   * Return the URI uniquely identifying this source.
   * 
   * @return the URI uniquely identifying this source
   */
  public abstract URI getUri();
}
