/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.server;

import com.google.dart.engine.source.Source;

import org.apache.commons.lang3.StringUtils;

import java.util.Collection;

/**
 * A {@link SourceSetKind#LIST} implementation of {@link SourceSet}.
 * 
 * @coverage dart.server
 */
public class ListSourceSet implements SourceSet {
  /**
   * Creates a new list-based {@link SourceSet} instance.
   */
  public static SourceSet create(Collection<Source> sourceCollection) {
    Source[] sources = sourceCollection.toArray(new Source[sourceCollection.size()]);
    return new ListSourceSet(sources);
  }

  /**
   * Creates a new list-based {@link SourceSet} instance.
   */
  public static SourceSet create(Source... sources) {
    return new ListSourceSet(sources);
  }

  private final Source[] sources;

  private ListSourceSet(Source... sources) {
    this.sources = sources;
  }

  @Override
  public SourceSetKind getKind() {
    return SourceSetKind.LIST;
  }

  @Override
  public Source[] getSources() {
    return sources;
  }

  @Override
  public String toString() {
    return "[" + StringUtils.join(sources, ", ") + "]";
  }
}
