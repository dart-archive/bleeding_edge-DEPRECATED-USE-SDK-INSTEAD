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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.commons.lang3.StringUtils;

/**
 * A description of a member that overrides an inherited member.
 *
 * @coverage dart.server.generated.types
 */
@SuppressWarnings("unused")
public class OverrideMember {

  /**
   * An empty array of {@link OverrideMember}s.
   */
  public static final OverrideMember[] EMPTY_ARRAY = new OverrideMember[0];

  /**
   * The offset of the name of the overriding member.
   */
  private final Integer offset;

  /**
   * The length of the name of the overriding member.
   */
  private final Integer length;

  /**
   * The member inherited from a superclass that is overridden by the overriding member. The field is
   * omitted if there is no superclass member, in which case there must be at least one interface
   * member.
   */
  private final OverriddenMember superclassMember;

  /**
   * The members inherited from interfaces that are overridden by the overriding member. The field is
   * omitted if there are no interface members, in which case there must be a superclass member.
   */
  private final List<OverriddenMember> interfaceMembers;

  /**
   * Constructor for {@link OverrideMember}.
   */
  public OverrideMember(Integer offset, Integer length, OverriddenMember superclassMember, List<OverriddenMember> interfaceMembers) {
    this.offset = offset;
    this.length = length;
    this.superclassMember = superclassMember;
    this.interfaceMembers = interfaceMembers;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof OverrideMember) {
      OverrideMember other = (OverrideMember) obj;
      return
        other.offset == offset &&
        other.length == length &&
        ObjectUtilities.equals(other.superclassMember, superclassMember) &&
        ObjectUtilities.equals(other.interfaceMembers, interfaceMembers);
    }
    return false;
  }

  /**
   * The members inherited from interfaces that are overridden by the overriding member. The field is
   * omitted if there are no interface members, in which case there must be a superclass member.
   */
  public List<OverriddenMember> getInterfaceMembers() {
    return interfaceMembers;
  }

  /**
   * The length of the name of the overriding member.
   */
  public Integer getLength() {
    return length;
  }

  /**
   * The offset of the name of the overriding member.
   */
  public Integer getOffset() {
    return offset;
  }

  /**
   * The member inherited from a superclass that is overridden by the overriding member. The field is
   * omitted if there is no superclass member, in which case there must be at least one interface
   * member.
   */
  public OverriddenMember getSuperclassMember() {
    return superclassMember;
  }

  public JsonObject toJson() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("offset", offset);
    jsonObject.addProperty("length", length);
    if (superclassMember != null) {
      jsonObject.add("superclassMember", superclassMember.toJson());
    }
    if (interfaceMembers != null) {
      JsonArray jsonArrayInterfaceMembers = new JsonArray();
      for(OverriddenMember elt : interfaceMembers) {
        jsonArrayInterfaceMembers.add(elt.toJson());
      }
      jsonObject.add("interfaceMembers", jsonArrayInterfaceMembers);
    }
    return jsonObject;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    builder.append("offset=");
    builder.append(offset + ", ");
    builder.append("length=");
    builder.append(length + ", ");
    builder.append("superclassMember=");
    builder.append(superclassMember + ", ");
    builder.append("interfaceMembers=");
    builder.append(StringUtils.join(interfaceMembers, ", "));
    builder.append("]");
    return builder.toString();
  }

}
