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

package com.google.dart.server.internal;

import com.google.dart.server.Element;
import com.google.dart.server.SearchResult;
import com.google.dart.server.SearchResultKind;

/**
 * A concrete implementation of {@link SearchResult}.
 * <p>
 * TODO (jwren) SeaarchResult[Impl] API has changed in the API
 * 
 * @coverage dart.server
 */
public class SearchResultImpl implements SearchResult {
  private final Element[] path;
  private final SearchResultKind kind;
  private final int offset;
  private final int length;
  private final boolean isPotential;

  public SearchResultImpl(Element[] path, SearchResultKind kind, int offset, int length,
      boolean isPotential) {
    this.path = path;
    this.kind = kind;
    this.offset = offset;
    this.length = length;
    this.isPotential = isPotential;
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
  public boolean isPotential() {
    return isPotential;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[kind=");
    builder.append(kind);
    builder.append(", offset=");
    builder.append(offset);
    builder.append(", length=");
    builder.append(length);
    builder.append(", potential=");
    builder.append(isPotential);
    builder.append(", path=");
    builder.append(path);
    builder.append("]");
    return builder.toString();
  }
}
