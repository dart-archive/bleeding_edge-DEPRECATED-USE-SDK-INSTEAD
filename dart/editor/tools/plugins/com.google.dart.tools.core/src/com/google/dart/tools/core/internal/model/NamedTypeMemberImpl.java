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

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.info.DeclarationElementInfo;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartModifiers;

/**
 * Instances of the class <code>NamedTypeMemberImpl</code> implement the behavior common to named
 * elements defined within a type.
 */
public abstract class NamedTypeMemberImpl extends DartTypeMemberImpl {
  /**
   * The name of the member.
   */
  private String name;

  /**
   * Initialize a newly created type member to be defined within the given type and have the given
   * name.
   * 
   * @param parent the type containing the member
   * @param name the name of the member
   */
  public NamedTypeMemberImpl(DartTypeImpl parent, String name) {
    super(parent);
    this.name = name;
  }

  @Override
  public String getElementName() {
    return name;
  }

  @Override
  public DartModifiers getModifiers() {
    DeclarationElementInfo info;
    try {
      info = (DeclarationElementInfo) getElementInfo();
    } catch (DartModelException exception) {
      // TODO(brianwilkerson) It was originally believed that this exception could not be thrown and
      // that this catch block was unreachable, but we now know that this is not true and need to
      // understand when this occurs and how to handle the exception.
      DartCore.logError("Could not get modifiers for " + toStringWithAncestors(), exception);
      throw new IllegalStateException(exception);
    }
    return new DartModifiers(info.getModifiers());
  }

  @Override
  public boolean isStatic() {
    return getModifiers().isStatic();
  }
}
