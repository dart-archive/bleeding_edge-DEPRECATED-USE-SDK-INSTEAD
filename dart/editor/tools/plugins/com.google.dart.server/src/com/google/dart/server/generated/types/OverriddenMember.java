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
 * A description of a member that is being overridden.
 *
 * @coverage dart.server.generated.types
 */
@SuppressWarnings("unused")
public class OverriddenMember {

  /**
   * An empty array of {@link OverriddenMember}s.
   */
  public static final OverriddenMember[] EMPTY_ARRAY = new OverriddenMember[0];

  /**
   * The element that is being overridden.
   */
  private final Element element;

  /**
   * The name of the class in which the member is defined.
   */
  private final String className;

  /**
   * Constructor for {@link OverriddenMember}.
   */
  public OverriddenMember(Element element, String className) {
    this.element = element;
    this.className = className;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof OverriddenMember) {
      OverriddenMember other = (OverriddenMember) obj;
      return
        ObjectUtilities.equals(other.element, element) &&
        ObjectUtilities.equals(other.className, className);
    }
    return false;
  }

  /**
   * The name of the class in which the member is defined.
   */
  public String getClassName() {
    return className;
  }

  /**
   * The element that is being overridden.
   */
  public Element getElement() {
    return element;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    builder.append("element=");
    builder.append(element + ", ");
    builder.append("className=");
    builder.append(className);
    builder.append("]");
    return builder.toString();
  }

}
