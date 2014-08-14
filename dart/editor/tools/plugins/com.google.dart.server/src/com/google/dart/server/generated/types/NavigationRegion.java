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
 * A description of a region from which the user can navigate to the declaration of an element.
 *
 * @coverage dart.server.generated.types
 */
@SuppressWarnings("unused")
public class NavigationRegion {

  /**
   * An empty array of {@link NavigationRegion}s.
   */
  public static final NavigationRegion[] EMPTY_ARRAY = new NavigationRegion[0];

  /**
   * The offset of the region from which the user can navigate.
   */
  private final int offset;

  /**
   * The length of the region from which the user can navigate.
   */
  private final int length;

  /**
   * The elements to which the given region is bound. By opening the declaration of the elements,
   * clients can implement one form of navigation.
   */
  private final List<Element> targets;

  /**
   * Constructor for {@link NavigationRegion}.
   */
  public NavigationRegion(int offset, int length, List<Element> targets) {
    this.offset = offset;
    this.length = length;
    this.targets = targets;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof NavigationRegion) {
      NavigationRegion other = (NavigationRegion) obj;
      return
        other.offset == offset &&
        other.length == length &&
        ObjectUtilities.equals(other.targets, targets);
    }
    return false;
  }

  /**
   * The length of the region from which the user can navigate.
   */
  public int getLength() {
    return length;
  }

  /**
   * The offset of the region from which the user can navigate.
   */
  public int getOffset() {
    return offset;
  }

  /**
   * The elements to which the given region is bound. By opening the declaration of the elements,
   * clients can implement one form of navigation.
   */
  public List<Element> getTargets() {
    return targets;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    builder.append("offset=");
    builder.append(offset + ", ");
    builder.append("length=");
    builder.append(length + ", ");
    builder.append("targets=");
    builder.append(StringUtils.join(targets, ", "));
    builder.append("]");
    return builder.toString();
  }

}
