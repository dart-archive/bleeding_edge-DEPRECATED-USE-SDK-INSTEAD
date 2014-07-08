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
import com.google.dart.server.TypeHierarchyItem;

import org.apache.commons.lang3.StringUtils;

/**
 * A concrete implementation of {@link TypeHierarchyItem}.
 * 
 * @coverage dart.server
 */
public class TypeHierarchyItemImpl implements TypeHierarchyItem {
  private final Element classElement;
  private final String displayName;
  private final Element memberElement;
  private final TypeHierarchyItem superclass;
  private final TypeHierarchyItem[] interfaces;
  private final TypeHierarchyItem[] mixins;
  private final TypeHierarchyItem[] subclasses;

  public TypeHierarchyItemImpl(Element classElement, String displayName, Element memberElement,
      TypeHierarchyItem superclass, TypeHierarchyItem[] interfaces, TypeHierarchyItem[] mixins,
      TypeHierarchyItem[] subclasses) {
    this.classElement = classElement;
    this.displayName = displayName;
    this.memberElement = memberElement;
    this.superclass = superclass;
    this.mixins = mixins;
    this.interfaces = interfaces;
    this.subclasses = subclasses;
  }

  @Override
  public String getBestName() {
    if (displayName == null) {
      return classElement.getName();
    } else {
      return displayName;
    }
  }

  @Override
  public Element getClassElement() {
    return classElement;
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public TypeHierarchyItem[] getInterfaces() {
    return interfaces;
  }

  @Override
  public Element getMemberElement() {
    return memberElement;
  }

  @Override
  public TypeHierarchyItem[] getMixins() {
    return mixins;
  }

  @Override
  public TypeHierarchyItem[] getSubclasses() {
    return subclasses;
  }

  @Override
  public TypeHierarchyItem getSuperclass() {
    return superclass;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[classElement=");
    builder.append(classElement);
    builder.append("displayName=");
    builder.append(displayName);
    builder.append(", memberElement=");
    builder.append(memberElement);
    builder.append(", superclass=");
    builder.append(superclass);
    builder.append(", mixins=[");
    builder.append(StringUtils.join(mixins, ", "));
    builder.append("], interfaces=[");
    builder.append(StringUtils.join(interfaces, ", "));
    builder.append("], subclasses=[");
    builder.append(StringUtils.join(subclasses, ", "));
    builder.append("]]");
    return builder.toString();
  }
}
