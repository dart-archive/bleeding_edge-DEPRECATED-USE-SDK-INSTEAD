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

import java.nio.CharBuffer;

/**
 * The interface {@code Source} defines the behavior of objects representing source code that can be
 * compiled.
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
     */
    public void accept(CharBuffer contents);

    /**
     * Accept the contents of a source represented as a string.
     * 
     * @param contents the contents of the source
     */
    public void accept(String contents);
  }

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
   * Return the container containing this source.
   * 
   * @return the container containing this source
   */
  public SourceContainer getContainer();

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
   * Return the full (long) version of the name that can be displayed to the user to denote this
   * source. For example, for a source representing a file this would typically be the absolute path
   * of the file.
   * 
   * @return a name that can be displayed to the user to denote this source
   */
  public String getFullName();

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
   * The container containing this source might have changed to it needs to be re-computed.
   */
  public void resetContainer();

  /**
   * Resolve the given URI relative to the location of this source.
   * 
   * @param uri the URI to be resolved against this source
   * @return a source representing the resolved URI
   */
  public Source resolve(String uri);
}
