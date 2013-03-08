/*
 * Copyright 2013 Dart project authors.
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

import java.util.HashMap;

/**
 * Instances of class {@code ContentCache} hold content used to override the default content of a
 * {@link Source}.
 * 
 * @coverage dart.engine.source
 */
public class ContentCache {
  /**
   * A table mapping sources to the contents of those sources. This is used to override the default
   * contents of a source.
   */
  private HashMap<Source, String> contentMap = new HashMap<Source, String>();

  /**
   * A table mapping sources to the modification stamps of those sources. This is used when the
   * default contents of a source has been overridden.
   */
  private HashMap<Source, Long> stampMap = new HashMap<Source, Long>();

  /**
   * Initialize a newly created cache to be empty.
   */
  public ContentCache() {
    super();
  }

  /**
   * Return the contents of the given source, or {@code null} if this cache does not override the
   * contents of the source.
   * <p>
   * <b>Note:</b> This method is not intended to be used except by
   * {@link SourceFactory#getContents(com.google.dart.engine.source.Source.ContentReceiver)}.
   * 
   * @param source the source whose content is to be returned
   * @return the contents of the given source
   */
  public String getContents(Source source) {
    return contentMap.get(source);
  }

  /**
   * Return the modification stamp of the given source, or {@code null} if this cache does not
   * override the contents of the source.
   * <p>
   * <b>Note:</b> This method is not intended to be used except by
   * {@link SourceFactory#getModificationStamp(com.google.dart.engine.source.Source)}.
   * 
   * @param source the source whose modification stamp is to be returned
   * @return the modification stamp of the given source
   */
  public Long getModificationStamp(Source source) {
    return stampMap.get(source);
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
      stampMap.remove(source);
    } else {
      contentMap.put(source, contents);
      stampMap.put(source, Long.valueOf(System.currentTimeMillis()));
    }
  }
}
