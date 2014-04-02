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

import com.google.dart.engine.context.AnalysisContext;

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
   * {@link AnalysisContext#getContents(Source, Source.ContentReceiver))}.
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
   * {@link AnalysisContext#getModificationStamp(Source)}.
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
   * @return the original cached contents or {@code null} if none
   */
  public String setContents(Source source, String contents) {
    if (contents == null) {
      stampMap.remove(source);
      return contentMap.remove(source);
    } else {
      Long newStamp = Long.valueOf(System.currentTimeMillis());
      Long oldStamp = stampMap.put(source, newStamp);
      // Occasionally, if this method is called in rapid succession, the timestamps are equal.
      // Guard against this by artificially incrementing the new timestamp
      if (newStamp.equals(oldStamp)) {
        stampMap.put(source, newStamp + 1);
      }
      return contentMap.put(source, contents);
    }
  }
}
