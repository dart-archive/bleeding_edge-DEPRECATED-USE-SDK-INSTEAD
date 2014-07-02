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
  private final String name;
  private final Element classElement;
  private final Element memberElement;
  private final TypeHierarchyItem extendedType;
  private final TypeHierarchyItem[] mixedTypes;
  private final TypeHierarchyItem[] implementedTypes;
  private TypeHierarchyItem[] subTypes = TypeHierarchyItem.EMPTY_ARRAY;

  public TypeHierarchyItemImpl(String name, Element classElement, Element memberElement,
      TypeHierarchyItem extendedType, TypeHierarchyItem[] mixedTypes,
      TypeHierarchyItem[] implementedTypes) {
    this.name = name;
    this.classElement = classElement;
    this.memberElement = memberElement;
    this.extendedType = extendedType;
    this.mixedTypes = mixedTypes;
    this.implementedTypes = implementedTypes;
  }

  @Override
  public Element getClassElement() {
    return classElement;
  }

  @Override
  public TypeHierarchyItem getExtendedType() {
    return extendedType;
  }

  @Override
  public TypeHierarchyItem[] getImplementedTypes() {
    return implementedTypes;
  }

  @Override
  public Element getMemberElement() {
    return memberElement;
  }

  @Override
  public TypeHierarchyItem[] getMixedTypes() {
    return mixedTypes;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public TypeHierarchyItem[] getSubTypes() {
    return subTypes;
  }

  public void setSubTypes(TypeHierarchyItem[] subTypes) {
    this.subTypes = subTypes;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[name=");
    builder.append(name);
    builder.append(", classElement=");
    builder.append(classElement);
    builder.append(", memberElement=");
    builder.append(memberElement);
    builder.append(", extendedType=");
    builder.append(extendedType);
    builder.append(", mixedTypes=[");
    builder.append(StringUtils.join(mixedTypes, ", "));
    builder.append("], implementedTypes=[");
    builder.append(StringUtils.join(implementedTypes, ", "));
    builder.append("], subTypes=[");
    builder.append(StringUtils.join(subTypes, ", "));
    builder.append("]]");
    return builder.toString();
  }
}
