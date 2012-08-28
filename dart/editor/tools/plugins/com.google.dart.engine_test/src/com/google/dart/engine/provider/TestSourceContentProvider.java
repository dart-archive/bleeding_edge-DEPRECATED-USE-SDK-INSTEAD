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
package com.google.dart.engine.provider;

import com.google.dart.engine.source.Source;

import java.util.HashMap;

/**
 * Instances of the class {@code TestSourceContentProvider} implement a provider that can return the
 * contents of a given source.
 */
public class TestSourceContentProvider {
  /**
   * A table mapping sources to the contents of those sources.
   */
  private HashMap<Source, String> contentMap = new HashMap<Source, String>();

  /**
   * Initialize a newly created provider to have knowledge of no sources.
   */
  public TestSourceContentProvider() {
    super();
  }

  /**
   * Add a mapping from the given source to the given contents.
   * 
   * @param source the source representing the file with the given contents
   * @param contents the contents of the given source
   */
  public void addSource(Source source, String contents) {
    contentMap.put(source, contents);
  }

  /**
   * Return the contents associated with the given source.
   * 
   * @param source the source whose contents are to be returned
   * @return the contents associated with the given source
   */
  public String getSource(Source source) {
    return contentMap.get(source);
  }
}
