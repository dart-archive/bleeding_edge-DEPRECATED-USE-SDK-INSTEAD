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
 * A representation of a class in a type hierarchy.
 *
 * @coverage dart.server.generated.types
 */
@SuppressWarnings("unused")
public class TypeHierarchyItem {

  /**
   * An empty array of {@link TypeHierarchyItem}s.
   */
  public static final TypeHierarchyItem[] EMPTY_ARRAY = new TypeHierarchyItem[0];

  /**
   * The class element represented by this item.
   */
  private final Element classElement;

  /**
   * The name to be displayed for the class. This field will be omitted if the display name is the
   * same as the name of the element. The display name is different if there is additional type
   * information to be displayed, such as type arguments.
   */
  private final String displayName;

  /**
   * The member in the class corresponding to the member on which the hierarchy was requested. This
   * field will be omitted if the hierarchy was not requested for a member or if the class does not
   * have a corresponding member.
   */
  private final Element memberElement;

  /**
   * The index of the item representing the superclass of this class. This field will be omitted if
   * this item represents the class Object.
   */
  private final int superclass;

  /**
   * The indexes of the items representing the interfaces implemented by this class. The list will be
   * empty if there are no implemented interfaces.
   */
  private final int[] interfaces;

  /**
   * The indexes of the items representing the mixins referenced by this class. The list will be
   * empty if there are no classes mixed in to this class.
   */
  private final int[] mixins;

  /**
   * The indexes of the items representing the subtypes of this class. The list will be empty if
   * there are no subtypes or if this item represents a supertype of the pivot type.
   */
  private final int[] subclasses;

  /**
   * Constructor for {@link TypeHierarchyItem}.
   */
  public TypeHierarchyItem(Element classElement, String displayName, Element memberElement, int superclass, int[] interfaces, int[] mixins, int[] subclasses) {
    this.classElement = classElement;
    this.displayName = displayName;
    this.memberElement = memberElement;
    this.superclass = superclass;
    this.interfaces = interfaces;
    this.mixins = mixins;
    this.subclasses = subclasses;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof TypeHierarchyItem) {
      TypeHierarchyItem other = (TypeHierarchyItem) obj;
      return
        ObjectUtilities.equals(other.classElement, classElement) &&
        ObjectUtilities.equals(other.displayName, displayName) &&
        ObjectUtilities.equals(other.memberElement, memberElement) &&
        other.superclass == superclass &&
        Arrays.equals(other.interfaces, interfaces) &&
        Arrays.equals(other.mixins, mixins) &&
        Arrays.equals(other.subclasses, subclasses);
    }
    return false;
  }

  /**
   * The class element represented by this item.
   */
  public Element getClassElement() {
    return classElement;
  }

  /**
   * The name to be displayed for the class. This field will be omitted if the display name is the
   * same as the name of the element. The display name is different if there is additional type
   * information to be displayed, such as type arguments.
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * The indexes of the items representing the interfaces implemented by this class. The list will be
   * empty if there are no implemented interfaces.
   */
  public int[] getInterfaces() {
    return interfaces;
  }

  /**
   * The member in the class corresponding to the member on which the hierarchy was requested. This
   * field will be omitted if the hierarchy was not requested for a member or if the class does not
   * have a corresponding member.
   */
  public Element getMemberElement() {
    return memberElement;
  }

  /**
   * The indexes of the items representing the mixins referenced by this class. The list will be
   * empty if there are no classes mixed in to this class.
   */
  public int[] getMixins() {
    return mixins;
  }

  /**
   * The indexes of the items representing the subtypes of this class. The list will be empty if
   * there are no subtypes or if this item represents a supertype of the pivot type.
   */
  public int[] getSubclasses() {
    return subclasses;
  }

  /**
   * The index of the item representing the superclass of this class. This field will be omitted if
   * this item represents the class Object.
   */
  public int getSuperclass() {
    return superclass;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    builder.append("classElement=");
    builder.append(classElement + ", ");
    builder.append("displayName=");
    builder.append(displayName + ", ");
    builder.append("memberElement=");
    builder.append(memberElement + ", ");
    builder.append("superclass=");
    builder.append(superclass + ", ");
    builder.append("interfaces=");
    builder.append(StringUtils.join(interfaces, ", ") + ", ");
    builder.append("mixins=");
    builder.append(StringUtils.join(mixins, ", ") + ", ");
    builder.append("subclasses=");
    builder.append(StringUtils.join(subclasses, ", "));
    builder.append("]");
    return builder.toString();
  }

}
