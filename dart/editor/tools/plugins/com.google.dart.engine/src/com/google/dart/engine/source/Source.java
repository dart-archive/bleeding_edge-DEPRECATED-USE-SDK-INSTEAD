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

import java.net.URI;
import java.nio.CharBuffer;

/**
 * The interface {@code Source} defines the behavior of objects representing source code that can be
 * compiled.
 * 
 * @coverage dart.engine.source
 */
public interface Source {
  /**
   * The interface {@code ContentReceiver} defines the behavior of objects that can receive the
   * content of a source.
   */
  public interface ContentReceiver {
    /**
     * Accept the contents of a source represented as a character buffer.
     * 
     * @param contents the contents of the source
     * @param modificationTime the time at which the contents were last set
     */
    public void accept(CharBuffer contents, long modificationTime);

    /**
     * Accept the contents of a source represented as a string.
     * 
     * @param contents the contents of the source
     * @param modificationTime the time at which the contents were last set
     */
    public void accept(String contents, long modificationTime);
  }

  /**
   * An empty array of sources.
   */
  public static final Source[] EMPTY_ARRAY = new Source[0];

  /**
   * Return {@code true} if the given object is a source that represents the same source code as
   * this source.
   * 
   * @param object the object to be compared with this object
   * @return {@code true} if the given object is a source that represents the same source code as
   *         this source
   * @see Object#equals(Object)
   */
  @Override
  public boolean equals(Object object);

  /**
   * Return {@code true} if this source exists.
   * 
   * @return {@code true} if this source exists
   */
  public boolean exists();

  /**
   * Get the contents of this source and pass it to the given receiver. Exactly one of the methods
   * defined on the receiver will be invoked unless an exception is thrown. The method that will be
   * invoked depends on which of the possible representations of the contents is the most efficient.
   * Whichever method is invoked, it will be invoked before this method returns.
   * 
   * @param receiver the content receiver to which the content of this source will be passed
   * @throws Exception if the contents of this source could not be accessed
   */
  public void getContents(ContentReceiver receiver) throws Exception;

  /**
   * Return an encoded representation of this source that can be used to create a source that is
   * equal to this source.
   * 
   * @return an encoded representation of this source
   * @see SourceFactory#fromEncoding(String)
   */
  public String getEncoding();

  /**
   * Return the full (long) version of the name that can be displayed to the user to denote this
   * source. For example, for a source representing a file this would typically be the absolute path
   * of the file.
   * 
   * @return a name that can be displayed to the user to denote this source
   */
  public String getFullName();

  /**
   * Return the modification stamp for this source. A modification stamp is a non-negative integer
   * with the property that if the contents of the source have not been modified since the last time
   * the modification stamp was accessed then the same value will be returned, but if the contents
   * of the source have been modified one or more times (even if the net change is zero) the stamps
   * will be different.
   * 
   * @return the modification stamp for this source
   */
  public long getModificationStamp();

  /**
   * Return a short version of the name that can be displayed to the user to denote this source. For
   * example, for a source representing a file this would typically be the name of the file.
   * 
   * @return a name that can be displayed to the user to denote this source
   */
  public String getShortName();

  /**
   * Return a hash code for this source.
   * 
   * @return a hash code for this source
   * @see Object#hashCode()
   */
  @Override
  public int hashCode();

  /**
   * Return {@code true} if this source is in one of the system libraries.
   * 
   * @return {@code true} if this is in a system library
   */
  public boolean isInSystemLibrary();

  /**
   * Resolve the relative URI against the URI associated with this source object. Return a
   * {@link Source source} representing the URI to which it was resolved, or {@code null} if it
   * could not be resolved.
   * <p>
   * Note: This method is not intended for public use, it is only visible out of necessity. It is
   * only intended to be invoked by a {@link SourceFactory source factory}. Source factories will
   * only invoke this method if the URI is relative, so implementations of this method are not
   * required to, and generally do not, verify the argument. The result of invoking this method with
   * an absolute URI is intentionally left unspecified.
   * 
   * @param relativeUri the relative URI to be resolved against the containing source
   * @return a {@link Source source} representing the URI to which given URI was resolved
   */
  public Source resolveRelative(URI relativeUri);
}
