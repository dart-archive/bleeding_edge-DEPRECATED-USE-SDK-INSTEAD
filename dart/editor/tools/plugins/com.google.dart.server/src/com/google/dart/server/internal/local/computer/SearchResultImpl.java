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

package com.google.dart.server.internal.local.computer;

import com.google.dart.engine.source.Source;
import com.google.dart.server.Element;
import com.google.dart.server.SearchResult;
import com.google.dart.server.SearchResultKind;

/**
 * A concrete implementation of {@link SearchResult}.
 * 
 * @coverage dart.server.local
 */
public class SearchResultImpl implements SearchResult {
  private final Element[] path;
  private final Source source;
  private final SearchResultKind kind;
  private final int offset;
  private final int length;

  public SearchResultImpl(Element[] path, Source source, SearchResultKind kind, int offset,
      int length) {
    this.path = path;
    this.source = source;
    this.kind = kind;
    this.offset = offset;
    this.length = length;
  }

  @Override
  public SearchResultKind getKind() {
    return kind;
  }

  @Override
  public int getLength() {
    return length;
  }

  @Override
  public int getOffset() {
    return offset;
  }

  @Override
  public Element[] getPath() {
    return path;
  }

  @Override
  public Source getSource() {
    return source;
  }

  @Override
  public boolean isPotential() {
    // TODO(scheglov) support for "potential"
    return false;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[source=");
    builder.append(source);
    builder.append(", kind=");
    builder.append(kind);
    builder.append(", offset=");
    builder.append(offset);
    builder.append(", length=");
    builder.append(length);
    builder.append(", path=");
    builder.append(path);
    builder.append("]");
    return builder.toString();
  }
}
