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
 *
 * This file has been automatically generated.  Please do not edit it manually.
 * To regenerate the file, use the script "pkg/analysis_server/spec/generate_files".
 */
package com.google.dart.server.generated.types;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import com.google.dart.server.utilities.general.ObjectUtilities;
import org.apache.commons.lang3.StringUtils;

/**
 * A single result from a search request.
 *
 * @coverage dart.server.generated.types
 */
@SuppressWarnings("unused")
public class SearchResult {

  /**
   * True if the result is a potential match but cannot be confirmed to be a match. For example, if
   * all references to a method m defined in some class were requested, and a reference to a method m
   * from an unknown class were found, it would be marked as being a potential match.
   */
  private final boolean isPotential;

  /**
   * The kind of element that was found or the kind of reference that was found.
   */
  private final String kind;

  /**
   * The location of the code that matched the search criteria.
   */
  private final Location location;

  /**
   * The elements that contain the result, starting with the most immediately enclosing ancestor and
   * ending with the library.
   */
  private final List<Element> path;

  /**
   * Constructor for {@link SearchResult}.
   */
  public SearchResult(Location location, String kind, boolean isPotential, List<Element> path) {
    this.location = location;
    this.kind = kind;
    this.isPotential = isPotential;
    this.path = path;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof SearchResult) {
      SearchResult other = (SearchResult) obj;
      return
        ObjectUtilities.equals(other.location, location) &&
        ObjectUtilities.equals(other.kind, kind) &&
        other.isPotential == isPotential &&
        ObjectUtilities.equals(other.path, path);
    }
    return false;
  }

  /**
   * True if the result is a potential match but cannot be confirmed to be a match. For example, if
   * all references to a method m defined in some class were requested, and a reference to a method m
   * from an unknown class were found, it would be marked as being a potential match.
   */
  public boolean getIsPotential() {
    return isPotential;
  }

  /**
   * The kind of element that was found or the kind of reference that was found.
   */
  public String getKind() {
    return kind;
  }

  /**
   * The location of the code that matched the search criteria.
   */
  public Location getLocation() {
    return location;
  }

  /**
   * The elements that contain the result, starting with the most immediately enclosing ancestor and
   * ending with the library.
   */
  public List<Element> getPath() {
    return path;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    builder.append("location=");
    builder.append(location.toString() + ", ");
    builder.append("kind=");
    builder.append(kind.toString() + ", ");
    builder.append("isPotential=");
    builder.append(isPotential + ", ");
    builder.append("path=");
    builder.append(StringUtils.join(path, ", ") + ", ");
    builder.append("]");
    return builder.toString();
  }

}
