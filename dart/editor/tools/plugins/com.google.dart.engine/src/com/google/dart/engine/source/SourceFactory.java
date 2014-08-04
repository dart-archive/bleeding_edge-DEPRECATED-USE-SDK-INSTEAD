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

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.sdk.DartSdk;

import java.net.URI;

/**
 * Instances of the class {@code SourceFactory} resolve possibly relative URI's against an existing
 * {@link Source source}.
 * 
 * @coverage dart.engine.source
 */
public class SourceFactory {
  /**
   * The analysis context that this source factory is associated with.
   */
  private AnalysisContext context;

  /**
   * The resolvers used to resolve absolute URI's.
   */
  private UriResolver[] resolvers;

  /**
   * The predicate to determine is {@link Source} is local.
   */
  private LocalSourcePredicate localSourcePredicate = LocalSourcePredicate.NOT_SDK;

  /**
   * Initialize a newly created source factory.
   * 
   * @param resolvers the resolvers used to resolve absolute URI's
   */
  public SourceFactory(UriResolver... resolvers) {
    this.resolvers = resolvers;
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
      URI uri = new URI(absoluteUri);
      if (uri.isAbsolute()) {
        return internalResolveUri(null, uri);
      }
    } catch (Exception exception) {
      AnalysisEngine.getInstance().getLogger().logError(
          "Could not resolve URI: " + absoluteUri,
          exception);
    }
    return null;
  }

  /**
   * Return a source object representing the given absolute URI, or {@code null} if the URI is not
   * an absolute URI.
   * 
   * @param absoluteUri the absolute URI to be resolved
   * @return a source object representing the absolute URI
   */
  public Source forUri(URI absoluteUri) {
    if (absoluteUri.isAbsolute()) {
      try {
        return internalResolveUri(null, absoluteUri);
      } catch (AnalysisException exception) {
        AnalysisEngine.getInstance().getLogger().logError(
            "Could not resolve URI: " + absoluteUri,
            exception);
      }
    }
    return null;
  }

  /**
   * Return a source object that is equal to the source object used to obtain the given encoding.
   * 
   * @param encoding the encoding of a source object
   * @return a source object that is described by the given encoding
   * @throws IllegalArgumentException if the argument is not a valid encoding
   * @see Source#getEncoding()
   */
  public Source fromEncoding(String encoding) {
    Source source = forUri(encoding);
    if (source == null) {
      throw new IllegalArgumentException("Invalid source encoding: " + encoding);
    }
    return source;
  }

  /**
   * Return the analysis context that this source factory is associated with.
   * 
   * @return the analysis context that this source factory is associated with
   */
  public AnalysisContext getContext() {
    return context;
  }

  /**
   * Return the {@link DartSdk} associated with this {@link SourceFactory}, or {@code null} if there
   * is no such SDK.
   * 
   * @return the {@link DartSdk} associated with this {@link SourceFactory}, or {@code null} if
   *         there is no such SDK
   */
  public DartSdk getDartSdk() {
    for (UriResolver resolver : resolvers) {
      if (resolver instanceof DartUriResolver) {
        DartUriResolver dartUriResolver = (DartUriResolver) resolver;
        return dartUriResolver.getDartSdk();
      }
    }
    return null;
  }

  /**
   * Determines if the given {@link Source} is local.
   * 
   * @param source the {@link Source} to analyze
   * @return {@code true} if the given {@link Source} is local
   */
  public boolean isLocalSource(Source source) {
    return localSourcePredicate.isLocal(source);
  }

  /**
   * Return a source object representing the URI that results from resolving the given (possibly
   * relative) contained URI against the URI associated with an existing source object, whether or
   * not the resulting source exists, or {@code null} if either the contained URI is invalid or if
   * it cannot be resolved against the source object's URI.
   * 
   * @param containingSource the source containing the given URI
   * @param containedUri the (possibly relative) URI to be resolved against the containing source
   * @return the source representing the contained URI
   */
  public Source resolveUri(Source containingSource, String containedUri) {
    if (containedUri == null || containedUri.isEmpty()) {
      return null;
    }
    try {
      // Force the creation of an escaped URI to deal with spaces, etc.
      return internalResolveUri(containingSource, new URI(containedUri));
    } catch (Exception exception) {
      AnalysisEngine.getInstance().getLogger().logError(
          "Could not resolve URI (" + containedUri + ") relative to source ("
              + containingSource.getFullName() + ")",
          exception);
      return null;
    }
  }

  /**
   * Return an absolute URI that represents the given source, or {@code null} if a valid URI cannot
   * be computed.
   * 
   * @param source the source to get URI for
   * @return the absolute URI representing the given source
   */
  public URI restoreUri(Source source) {
    for (UriResolver resolver : resolvers) {
      URI uri = resolver.restoreAbsolute(source);
      if (uri != null) {
        return uri;
      }
    }
    return null;
  }

  /**
   * Set the analysis context that this source factory is associated with to the given context.
   * <p>
   * <b>Note:</b> This method should only be invoked by
   * {@link AnalysisContextImpl#setSourceFactory(SourceFactory)} and is only public out of
   * necessity.
   * 
   * @param context the analysis context that this source factory is associated with
   */
  public void setContext(AnalysisContext context) {
    this.context = context;
  }

  /**
   * Sets the {@link LocalSourcePredicate}.
   * 
   * @param localSourcePredicate the predicate to determine is {@link Source} is local
   */
  public void setLocalSourcePredicate(LocalSourcePredicate localSourcePredicate) {
    this.localSourcePredicate = localSourcePredicate;
  }

  /**
   * Return a source object representing the URI that results from resolving the given (possibly
   * relative) contained URI against the URI associated with an existing source object, or
   * {@code null} if the URI could not be resolved.
   * 
   * @param containingSource the source containing the given URI
   * @param containedUri the (possibly relative) URI to be resolved against the containing source
   * @return the source representing the contained URI
   * @throws AnalysisException if either the contained URI is invalid or if it cannot be resolved
   *           against the source object's URI
   */
  private Source internalResolveUri(Source containingSource, URI containedUri)
      throws AnalysisException {
    if (!containedUri.isAbsolute()) {
      if (containingSource == null) {
        throw new AnalysisException("Cannot resolve a relative URI without a containing source: "
            + containedUri);
      }
      containedUri = containingSource.resolveRelative(containedUri);
    }
    for (UriResolver resolver : resolvers) {
      Source result = resolver.resolveAbsolute(containedUri);
      if (result != null) {
        return result;
      }
    }
    return null;
  }
}
