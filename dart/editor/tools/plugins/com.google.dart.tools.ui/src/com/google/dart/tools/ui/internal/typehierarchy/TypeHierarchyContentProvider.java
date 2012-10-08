/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.ui.internal.typehierarchy;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.dart.compiler.util.apache.ArrayUtils;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.model.TypeMember;
import com.google.dart.tools.internal.corext.refactoring.rename.RenameAnalyzeUtil;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class TypeHierarchyContentProvider implements ITreeContentProvider {
  private String memberName;
  private final List<Type> superTypes = Lists.newArrayList();
  private final Map<Type, List<Type>> subTypesMap = Maps.newHashMap();
  private final Map<Type, Type> subTypesSuper = Maps.newHashMap();

  @Override
  public void dispose() {
  }

  @Override
  public Object[] getChildren(Object parentElement) {
    // super types
    {
      int superIndex = superTypes.indexOf(parentElement);
      if (superIndex >= 0 && superIndex < superTypes.size() - 1) {
        return new Object[] {superTypes.get(superIndex + 1)};
      }
    }
    // sub types
    {
      List<Type> subTypes = subTypesMap.get(parentElement);
      if (subTypes != null) {
        return subTypes.toArray(new Type[subTypes.size()]);
      }
    }
    // no children
    return ArrayUtils.EMPTY_OBJECT_ARRAY;
  }

  @Override
  public Object[] getElements(Object inputElement) {
    if (!superTypes.isEmpty()) {
      return new Object[] {superTypes.get(0)};
    }
    return ArrayUtils.EMPTY_OBJECT_ARRAY;
  }

  @Override
  public Object getParent(Object element) {
    int superIndex = superTypes.indexOf(element);
    if (superIndex >= 0 && superIndex < superTypes.size()) {
      if (superIndex == 0) {
        return null;
      }
      return superTypes.get(superIndex - 1);
    }
    return subTypesSuper.get(element);
  }

  @Override
  public boolean hasChildren(Object element) {
    return getChildren(element).length != 0;
  }

  @Override
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    memberName = null;
    superTypes.clear();
    try {
      if (newInput instanceof Object[] && ((Object[]) newInput).length == 1) {
        Object inputObject = ((Object[]) newInput)[0];
        if (inputObject instanceof Method) {
          Method inputMethod = (Method) inputObject;
          memberName = inputMethod.getElementName();
          inputObject = inputMethod.getAncestor(Type.class);
        }
        if (inputObject instanceof Type) {
          Type inputType = (Type) inputObject;
          // super types
          superTypes.addAll(RenameAnalyzeUtil.getSuperClasses(inputType));
          superTypes.add(inputType);
          // sub types
          fillSubTypes(inputType);
          if (memberName != null) {
            keepBranchesWithMemberOverride(inputType);
          }
        }
      }
    } catch (Throwable e) {
      DartToolsPlugin.log(e);
    }
  }

  /**
   * @return the {@link Type} or {@link TypeMember} for given {@link Type} selection.
   */
  Object convertSelectedElement(Object o) {
    try {
      if (memberName != null && o instanceof Type) {
        Type type = (Type) o;
        TypeMember[] members = type.getExistingMembers(memberName);
        if (members.length != 0) {
          return members[0];
        }
      }
    } catch (Throwable e) {
      DartToolsPlugin.log(e);
    }
    return o;
  }

  /**
   * @return the {@link Predicate} to check if given {@link Type} is not interesting part of
   *         hierarchy, so should be displayed using light color.
   */
  Predicate<Object> getLightPredicate() {
    return new Predicate<Object>() {
      @Override
      public boolean apply(Object input) {
        if (memberName != null && input instanceof Type) {
          Type type = (Type) input;
          try {
            return type.getExistingMembers(memberName).length == 0;
          } catch (Throwable e) {
          }
        }
        return false;
      }
    };
  }

  /**
   * Builds complete hierarchy in {@link #subTypesMap}.
   */
  private void fillSubTypes(Type type) throws Exception {
    List<Type> subTypes = RenameAnalyzeUtil.getDirectSubTypes(type);
    for (Type subType : subTypes) {
      subTypesSuper.put(subType, type);
      fillSubTypes(subType);
    }
    subTypesMap.put(type, subTypes);
  }

  /**
   * @return <code>true</code> if branch of given {@link Type} has override for member.
   */
  private boolean keepBranchesWithMemberOverride(Type type) throws Exception {
    List<Type> subTypes = subTypesMap.get(type);
    if (subTypes != null) {
      for (Iterator<Type> I = subTypes.iterator(); I.hasNext();) {
        Type subType = I.next();
        if (!keepBranchesWithMemberOverride(subType)) {
          I.remove();
          subTypesMap.remove(subType);
        }
      }
      if (!subTypes.isEmpty()) {
        return true;
      }
    }
    return type.getExistingMembers(memberName).length != 0;
  }

}
