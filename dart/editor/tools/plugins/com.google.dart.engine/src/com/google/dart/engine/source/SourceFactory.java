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
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Instances of the class {@code SourceFactory} resolve possibly relative URI's against an existing
 * {@link Source source}.
 */
public class SourceFactory {

  /**
   * The resolvers used to resolve absolute URI's.
   */
  private UriResolver[] resolvers;

  /**
   * A cache of content used to override the default content of a source.
   */
  private ContentCache contentCache;

  /**
   * Initialize a newly created source factory.
   * 
   * @param contentCache the cache holding content used to override the default content of a source.
   * @param resolvers the resolvers used to resolve absolute URI's
   */
  public SourceFactory(ContentCache contentCache, UriResolver... resolvers) {
    this.contentCache = contentCache;
    this.resolvers = resolvers;
  }

  /**
   * Initialize a newly created source factory.
   * 
   * @param resolvers the resolvers used to resolve absolute URI's
   */
  public SourceFactory(UriResolver... resolvers) {
    this(new ContentCache(), resolvers);
  }

  /**
   * Return a source container representing the given directory
   * 
   * @param directory the directory (not {@code null})
   * @return the source container representing the directory (not {@code null})
   */
  public SourceContainer forDirectory(File directory) {
    return new DirectoryBasedSourceContainer(directory);
  }

  /**
   * Return a source object representing the given file.
   * 
   * @param file the file to be represented by the returned source object
   * @return a source object representing the given file
   */
  public Source forFile(File file) {
    return new FileBasedSource(this, file);
  }

  /**
   * Return a source object representing the given absolute URI, or {@code null} if the URI is not a
   * valid URI or if it is not an absolute URI.
   * 
   * @param absoluteUri the absolute URI to be resolved
   * @return a source object representing the absolute URI
   */
  public Source forUri(String absoluteUri) {
    try {
      URI uri = new URI(null, null, absoluteUri, null);
      if (uri.isAbsolute()) {
        return resolveUri(null, uri);
      }
    } catch (URISyntaxException exception) {
      // Fall through to return null
    }
    return null;
  }

  /**
   * Return a source object representing the URI that results from resolving the given (possibly
   * relative) contained URI against the URI associated with an existing source object, or
   * {@code null} if either the contained URI is invalid or if it cannot be resolved against the
   * source object's URI.
   * 
   * @param containingSource the source containing the given URI
   * @param containedUri the (possibly relative) URI to be resolved against the containing source
   * @return the source representing the contained URI
   */
  public Source resolveUri(Source containingSource, String containedUri) {
    try {
      // Force the creation of an escaped URI to deal with spaces, etc.
      return resolveUri(containingSource, new URI(null, null, containedUri, null));
    } catch (URISyntaxException exception) {
      return null;
    }
  }

  /**
   * Set the contents of the given source to the given contents. This has the effect of overriding
   * the default contents of the source. If the contents are {@code null} the override is removed so
   * that the default contents will be returned.
   * 
   * @param source the source whose contents are being overridden
   * @param contents the new contents of the source
   */
  public void setContents(Source source, String contents) {
    contentCache.setContents(source, contents);
  }

  /**
   * Return the contents of the given source, or {@code null} if this factory does not override the
   * contents of the source.
   * <p>
   * <b>Note:</b> This method is not intended to be used except by
   * {@link FileBasedSource#getContents(com.google.dart.engine.source.Source.ContentReceiver)}.
   * 
   * @param source the source whose content is to be returned
   * @return the contents of the given source
   */
  protected String getContents(Source source) {
    return contentCache.getContents(source);
  }

  /**
   * Return a source object representing the URI that results from resolving the given (possibly
   * relative) contained URI against the URI associated with an existing source object, or
   * {@code null} if either the contained URI is invalid or if it cannot be resolved against the
   * source object's URI.
   * 
   * @param containingSource the source containing the given URI
   * @param containedUri the (possibly relative) URI to be resolved against the containing source
   * @return the source representing the contained URI
   */
  private Source resolveUri(Source containingSource, URI containedUri) {
    for (UriResolver resolver : resolvers) {
      Source result = resolver.resolve(this, containingSource, containedUri);
      if (result != null) {
        return result;
      }
    }
    return null;
  }
}
