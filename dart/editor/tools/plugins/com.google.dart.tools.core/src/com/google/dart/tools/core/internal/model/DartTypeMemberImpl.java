/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.internal.model;

import com.google.dart.tools.core.internal.model.info.DartElementInfo;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.model.TypeMember;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Instances of the class <code>DartTypeMemberImpl</code> implement the behavior common to elements
 * defined within a type.
 */
public abstract class DartTypeMemberImpl extends SourceReferenceImpl implements TypeMember {
  /**
   * Initialize a newly created type member to be defined within the given type.
   * 
   * @param parent the type containing the member
   */
  public DartTypeMemberImpl(DartTypeImpl parent) {
    super(parent);
  }

  @Override
  public Type getDeclaringType() {
    return getAncestor(Type.class);
  }

  @Override
  public TypeMember[] getOverriddenMembers() throws DartModelException {
    String name = getElementName();
    List<TypeMember> overriddenMembers = new ArrayList<TypeMember>();
    Set<Type> visitedTypes = new HashSet<Type>();
    List<Type> startTypes = new ArrayList<Type>();
    startTypes.add(getDeclaringType());
    while (!startTypes.isEmpty()) {
      Type startType = startTypes.remove(0);
      DartLibrary library = startType.getLibrary();
      for (String supertypeName : startType.getSupertypeNames()) {
        Type supertype = library.findType(supertypeName);
        if (supertype != null) {
          TypeMember[] members = supertype.getExistingMembers(name);
          if (members.length > 0) {
            for (TypeMember member : members) {
              overriddenMembers.add(member);
            }
          } else if (!visitedTypes.contains(supertype)) {
            visitedTypes.add(supertype);
            startTypes.add(supertype);
          }
        }
      }
    }
    return overriddenMembers.toArray(new TypeMember[overriddenMembers.size()]);
  }

  @Override
  protected DartElementInfo createElementInfo() {
    return null;
  }
}
