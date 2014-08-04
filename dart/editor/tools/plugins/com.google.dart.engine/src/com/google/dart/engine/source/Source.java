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
import com.google.dart.engine.internal.context.TimestampedData;
import com.google.dart.engine.utilities.translation.DartOmit;

import java.net.URI;

/**
 * The interface {@code Source} defines the behavior of objects representing source code that can be
 * analyzed by the analysis engine.
 * <p>
 * Implementations of this interface need to be aware of some assumptions made by the analysis
 * engine concerning sources:
 * <ul>
 * <li>Sources are not required to be unique. That is, there can be multiple instances representing
 * the same source.</li>
 * <li>Sources are long lived. That is, the engine is allowed to hold on to a source for an extended
 * period of time and that source must continue to report accurate and up-to-date information.</li>
 * </ul>
 * Because of these assumptions, most implementations will not maintain any state but will delegate
 * to an authoritative system of record in order to implement this API. For example, a source that
 * represents files on disk would typically query the file system to determine the state of the
 * file.
 * <p>
 * If the instances that implement this API are the system of record, then they will typically be
 * unique. In that case, sources that are created that represent non-existent files must also be
 * retained so that if those files are created at a later date the long-lived sources representing
 * those files will know that they now exist.
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
     * Accept the contents of a source.
     * 
     * @param contents the contents of the source
     * @param modificationTime the time at which the contents were last set
     */
    public void accept(CharSequence contents, long modificationTime);
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
   * <p>
   * Clients should consider using the the method {@link AnalysisContext#exists(Source)} because
   * contexts can have local overrides of the content of a source that the source is not aware of
   * and a source with local content is considered to exist even if there is no file on disk.
   * 
   * @return {@code true} if this source exists
   */
  public boolean exists();

  /**
   * Get the contents and timestamp of this source.
   * <p>
   * Clients should consider using the the method {@link AnalysisContext#getContents(Source)}
   * because contexts can have local overrides of the content of a source that the source is not
   * aware of.
   * 
   * @return the contents and timestamp of the source
   * @throws Exception if the contents of this source could not be accessed
   */
  public TimestampedData<CharSequence> getContents() throws Exception;

  /**
   * Get the contents of this source and pass it to the given content receiver.
   * <p>
   * Clients should consider using the the method
   * {@link AnalysisContext#getContentsToReceiver(Source, ContentReceiver)} because contexts can
   * have local overrides of the content of a source that the source is not aware of.
   * 
   * @param receiver the content receiver to which the content of this source will be passed
   * @throws Exception if the contents of this source could not be accessed
   */
  @Deprecated
  @DartOmit
  public void getContentsToReceiver(ContentReceiver receiver) throws Exception;

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
   * <p>
   * Clients should consider using the the method
   * {@link AnalysisContext#getModificationStamp(Source)} because contexts can have local overrides
   * of the content of a source that the source is not aware of.
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
   * Return the URI from which this source was originally derived.
   * 
   * @return the URI from which this source was originally derived
   */
  public URI getUri();

  /**
   * Return the kind of URI from which this source was originally derived. If this source was
   * created from an absolute URI, then the returned kind will reflect the scheme of the absolute
   * URI. If it was created from a relative URI, then the returned kind will be the same as the kind
   * of the source against which the relative URI was resolved.
   * 
   * @return the kind of URI from which this source was originally derived
   */
  @Deprecated
  public UriKind getUriKind();

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
   * Resolve the relative URI against the URI associated with this source object.
   * <p>
   * Note: This method is not intended for public use, it is only visible out of necessity. It is
   * only intended to be invoked by a {@link SourceFactory source factory}. Source factories will
   * only invoke this method if the URI is relative, so implementations of this method are not
   * required to, and generally do not, verify the argument. The result of invoking this method with
   * an absolute URI is intentionally left unspecified.
   * 
   * @param relativeUri the relative URI to be resolved against this source
   * @return the URI to which given URI was resolved
   * @throws AnalysisException if the relative URI could not be resolved
   */
  public URI resolveRelative(URI relativeUri) throws AnalysisException;
}
