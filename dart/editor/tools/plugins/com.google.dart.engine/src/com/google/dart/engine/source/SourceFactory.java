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
 * Instances of the class <code>SourceFactory</code> resolve possibly relative URI's against an
 * existing {@link Source source}.
 */
public class SourceFactory {
  /**
   * The resolvers used to resolve absolute URI's.
   */
  private UriResolver[] resolvers;

  /**
   * Initialize a newly created source factory.
   * 
   * @param resolvers the resolvers used to resolve absolute URI's
   */
  public SourceFactory(UriResolver... resolvers) {
    this.resolvers = resolvers;
  }

  /**
   * Return a source object representing the given file.
   * 
   * @param file the file to be represented by the returned source object
   * @return a source object representing the given file
   */
  public Source forFile(File file) {
    return new SourceImpl(this, file);
  }

  /**
   * Return a source object representing the URI that results from resolving the given (possibly
   * relative) contained URI against the URI associated with an existing source object, or
   * <code>null</code> if either the contained URI is invalid or if it cannot be resolved against
   * the source object's URI.
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
   * Return a source object representing the URI that results from resolving the given (possibly
   * relative) contained URI against the URI associated with an existing source object, or
   * <code>null</code> if either the contained URI is invalid or if it cannot be resolved against
   * the source object's URI.
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
