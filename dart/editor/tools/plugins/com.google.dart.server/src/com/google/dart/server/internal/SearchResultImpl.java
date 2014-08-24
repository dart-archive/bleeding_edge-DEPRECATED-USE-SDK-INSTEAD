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

import com.google.dart.server.SearchResult;
import com.google.dart.server.SearchResultKind;
import com.google.dart.server.generated.types.Element;
import com.google.dart.server.generated.types.Location;

import java.util.List;

/**
 * A concrete implementation of {@link SearchResult}.
 * <p>
 * TODO (jwren) SeaarchResult[Impl] API has changed in the API
 * 
 * @coverage dart.server
 */
public class SearchResultImpl implements SearchResult {
  private final List<Element> path;
  private final SearchResultKind kind;
  private final boolean isPotential;
  private final Location location;

  public SearchResultImpl(List<Element> path, SearchResultKind kind, Location location,
      boolean isPotential) {
    this.path = path;
    this.kind = kind;
    this.location = location;
    this.isPotential = isPotential;
  }

  @Override
  public SearchResultKind getKind() {
    return kind;
  }

  @Override
  public Location getLocation() {
    return location;
  }

  @Override
  public List<Element> getPath() {
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
    builder.append(", location=");
    builder.append(location);
    builder.append(", potential=");
    builder.append(isPotential);
    builder.append(", path=");
    builder.append(path);
    builder.append("]");
    return builder.toString();
  }
}
