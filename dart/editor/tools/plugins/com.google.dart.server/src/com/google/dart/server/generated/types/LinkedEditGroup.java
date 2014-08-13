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
 * A collection of positions that should be linked (edited simultaneously) for the purposes of
 * updating code after a source change. For example, if a set of edits introduced a new variable
 * name, the group would contain all of the positions of the variable name so that if the client
 * wanted to let the user edit the variable name after the operation, all occurrences of the name
 * could be edited simultaneously.
 *
 * @coverage dart.server.generated.types
 */
@SuppressWarnings("unused")
public class LinkedEditGroup {

  /**
   * The length of the regions that should be edited simultaneously.
   */
  private final int length;

  /**
   * The positions of the regions that should be edited simultaneously.
   */
  private final List<Position> positions;

  /**
   * Pre-computed suggestions for what every region might want to be changed to.
   */
  private final List<LinkedEditSuggestion> suggestions;

  /**
   * Constructor for {@link LinkedEditGroup}.
   */
  public LinkedEditGroup(List<Position> positions, int length, List<LinkedEditSuggestion> suggestions) {
    this.positions = positions;
    this.length = length;
    this.suggestions = suggestions;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof LinkedEditGroup) {
      LinkedEditGroup other = (LinkedEditGroup) obj;
      return
        ObjectUtilities.equals(other.positions, positions) &&
        other.length == length &&
        ObjectUtilities.equals(other.suggestions, suggestions);
    }
    return false;
  }

  /**
   * The length of the regions that should be edited simultaneously.
   */
  public int getLength() {
    return length;
  }

  /**
   * The positions of the regions that should be edited simultaneously.
   */
  public List<Position> getPositions() {
    return positions;
  }

  /**
   * Pre-computed suggestions for what every region might want to be changed to.
   */
  public List<LinkedEditSuggestion> getSuggestions() {
    return suggestions;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    builder.append("positions=");
    builder.append(StringUtils.join(positions, ", ") + ", ");
    builder.append("length=");
    builder.append(length + ", ");
    builder.append("suggestions=");
    builder.append(StringUtils.join(suggestions, ", ") + ", ");
    builder.append("]");
    return builder.toString();
  }

}
