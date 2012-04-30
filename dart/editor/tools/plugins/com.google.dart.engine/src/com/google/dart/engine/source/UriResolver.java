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

/**
 * The abstract class <code>UriResolver</code> defines the behavior of objects that are used to
 * resolve URI's for a source factory. Subclasses of this class are expected to resolve a single
 * scheme of absolute URI.
 */
public abstract class UriResolver {
  /**
   * Initialize a newly created resolver.
   */
  public UriResolver() {
    super();
  }

  /**
   * Working on behalf of the given source factory, resolve the (possibly relative) contained URI
   * against the URI associated with the containing source object. Return a {@link Source source}
   * representing the file to which it was resolved, or <code>null</code> if it could not be
   * resolved.
   * 
   * @param factory the source factory requesting the resolution of the URI
   * @param containingSource the source containing the given URI
   * @param containedUri the (possibly relative) URI to be resolved against the containing source
   * @return a {@link Source source} representing the URI to which given URI was resolved
   */
  public Source resolve(SourceFactory factory, Source containingSource, URI containedUri) {
    if (containedUri.isAbsolute()) {
      return resolveAbsolute(factory, containedUri);
    } else {
      return resolveRelative(factory, containingSource, containedUri);
    }
  }

  /**
   * Resolve the given absolute URI. Return a {@link Source source} representing the file to which
   * it was resolved, or <code>null</code> if it could not be resolved.
   * 
   * @param uri the URI to be resolved
   * @return a {@link Source source} representing the URI to which given URI was resolved
   */
  protected abstract Source resolveAbsolute(SourceFactory factory, URI uri);

  /**
   * Resolve the relative (contained) URI against the URI associated with the containing source
   * object. Return a {@link Source source} representing the file to which it was resolved, or
   * <code>null</code> if it could not be resolved.
   * 
   * @param containingSource the source containing the given URI
   * @param containedUri the (possibly relative) URI to be resolved against the containing source
   * @return a {@link Source source} representing the URI to which given URI was resolved
   */
  protected Source resolveRelative(SourceFactory factory, Source containingSource, URI containedUri) {
    try {
      URI resolvedUri = containingSource.getFile().toURI().resolve(containedUri).normalize();
      return new SourceImpl(factory, new File(resolvedUri));
    } catch (Exception exception) {
      // Fall through to return null
    }
    return null;
  }
}
