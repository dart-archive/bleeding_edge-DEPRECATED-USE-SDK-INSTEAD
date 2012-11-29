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
import java.util.HashMap;

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
   * A table mapping sources to the contents of those sources. This is used to override the default
   * contents of a source.
   */
  private HashMap<Source, String> contentMap = new HashMap<Source, String>();

  /**
   * The mapper used by file-based sources to compute their container.
   */
  private ContainerMapper containerMapper = DefaultContainerMapper.getInstance();

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
   * Return the container mapper that should be used by file-based sources to map files to their
   * container.
   * 
   * @return the container mapper that should be used by file-based sources to map files to their
   *         container
   */
  public ContainerMapper getContainerMapper() {
    return containerMapper;
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
   * Set the container mapper that should be used by file-based sources to map files to their
   * container to the given mapper.
   * 
   * @param containerMapper the container mapper that should be used by file-based sources to map
   *          files to their container
   */
  public void setContainerMapper(ContainerMapper containerMapper) {
    this.containerMapper = containerMapper == null ? DefaultContainerMapper.getInstance()
        : containerMapper;
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
    if (contents == null) {
      contentMap.remove(source);
    } else {
      contentMap.put(source, contents);
    }
  }

  /**
   * Return the contents of the given source, or {@code null} if this factory does not override the
   * contents of the source.
   * <p>
   * <b>Note:</b> This method is not intended to be used except by
   * {@link SourceImpl#getContents(com.google.dart.engine.source.Source.ContentReceiver)}.
   * 
   * @param source the source whose content is to be returned
   * @return the contents of the given source
   */
  protected String getContents(Source source) {
    return contentMap.get(source);
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
