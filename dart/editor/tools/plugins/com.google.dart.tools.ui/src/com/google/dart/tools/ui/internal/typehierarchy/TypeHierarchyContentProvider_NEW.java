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
package com.google.dart.tools.ui.internal.typehierarchy;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.dart.server.GetTypeHierarchyConsumer;
import com.google.dart.server.generated.types.Element;
import com.google.dart.server.generated.types.TypeHierarchyItem;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.ui.internal.text.functions.PositionElement;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class TypeHierarchyContentProvider_NEW implements ITreeContentProvider {
  public static class TypeItem {
    final TypeHierarchyItem type;
    final Element element;
    final int[] mixins;
    final int[] interfaces;

    public TypeItem(TypeHierarchyItem type) {
      this.type = type;
      this.element = type.getClassElement();
      this.mixins = type.getMixins();
      this.interfaces = type.getInterfaces();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj instanceof TypeItem) {
        return ((TypeItem) obj).element.equals(element);
      }
      return false;
    }

    public Element getElement() {
      return element;
    }

    @Override
    public int hashCode() {
      return element.hashCode();
    }

    @Override
    public String toString() {
      return element.toString();
    }

    public StyledString toStyledString() {
      StyledString styledString = new StyledString(type.getBestName());
      if (mixins != null && mixins.length != 0) {
        styledString.append(" with ", StyledString.QUALIFIER_STYLER);
        // TODO (jwren/ scheglov) get new TypeHierarchyItem to work with the Editor
//        appendItems(styledString, mixins);
      }
      if (interfaces != null && interfaces.length != 0) {
        styledString.append(" implements ", StyledString.QUALIFIER_STYLER);
        // TODO (jwren/ scheglov) get new TypeHierarchyItem to work with the Editor
//        appendItems(styledString, interfaces);
      }
      return styledString;
    }

    private void appendItems(StyledString styledString, TypeHierarchyItem[] items) {
      for (int i = 0; i < items.length; i++) {
        TypeHierarchyItem item = items[i];
        if (i != 0) {
          styledString.append(", ");
        }
        styledString.append(item.getBestName());
      }
    }
  }

  private final List<TypeItem> superList = Lists.newArrayList();
  private final Map<TypeItem, List<TypeItem>> superToSubsMap = Maps.newHashMap();
  private final Map<TypeItem, TypeItem> subToSuperMap = Maps.newHashMap();
  private boolean isMemberHierarchy;

  @Override
  public void dispose() {
  }

  @Override
  public Object[] getChildren(Object parentElement) {
    // super
    {
      int superIndex = superList.indexOf(parentElement);
      if (superIndex >= 0 && superIndex < superList.size() - 1) {
        return new Object[] {superList.get(superIndex + 1)};
      }
    }
    // subs
    {
      List<TypeItem> subs = superToSubsMap.get(parentElement);
      if (subs != null) {
        return subs.toArray(new TypeItem[subs.size()]);
      }
    }
    // no children
    return ArrayUtils.EMPTY_OBJECT_ARRAY;
  }

  @Override
  public Object[] getElements(Object inputElement) {
    if (!superList.isEmpty()) {
      return new Object[] {superList.get(0)};
    }
    return ArrayUtils.EMPTY_OBJECT_ARRAY;
  }

  @Override
  public Object getParent(Object element) {
    int superIndex = superList.indexOf(element);
    if (superIndex >= 0 && superIndex < superList.size()) {
      if (superIndex == 0) {
        return null;
      }
      return superList.get(superIndex - 1);
    }
    return subToSuperMap.get(element);
  }

  @Override
  public boolean hasChildren(Object element) {
    return getChildren(element).length != 0;
  }

  @Override
  public void inputChanged(final Viewer viewer, Object oldInput, Object newInput) {
    superList.clear();
    superToSubsMap.clear();
    subToSuperMap.clear();
    if (!(newInput instanceof PositionElement)) {
      return;
    }
    PositionElement element = (PositionElement) newInput;
    DartCore.getAnalysisServer().search_getTypeHierarchy(
        element.file,
        element.offset,
        new GetTypeHierarchyConsumer() {
          @Override
          public void computedHierarchy(List<TypeHierarchyItem> hierarchyItems) {
            // TODO (jwren/ scheglov) get new TypeHierarchyItem to work with the Editor
            //inputHierarchyChanged(viewer, hierarchyItems);
          }
        });
  }

  /**
   * @return the {@link Element} for given selection.
   */
  Object convertSelectedElement(Object o) {
    TypeHierarchyItem type = ((TypeItem) o).type;
    Element memberElement = type.getMemberElement();
    if (memberElement != null) {
      return memberElement;
    }
    return type.getClassElement();
  }

  /**
   * @return the {@link Predicate} to check if given object is not interesting part of hierarchy, so
   *         should be displayed using light color.
   */
  Predicate<Object> getLightPredicate() {
    return new Predicate<Object>() {
      @Override
      public boolean apply(Object input) {
        if (!isMemberHierarchy) {
          return false;
        }
        TypeItem item = (TypeItem) input;
        return item.type.getMemberElement() == null;
      }
    };
  }

  void inputHierarchyChanged(final Viewer viewer, TypeHierarchyItem target) {
    isMemberHierarchy = target.getMemberElement() != null;
    final TypeItem targetItem = new TypeItem(target);
    // full super hierarchy
    TypeHierarchyItem superclass = target;
    while (superclass != null) {
      TypeItem superItem = new TypeItem(superclass);
      superList.add(0, superItem);
      if (superclass == target) {
        superItem = targetItem;
      }
      // try "extends"
      // TODO (jwren/ scheglov) get new TypeHierarchyItem to work with the Editor
//      superclass = superItem.type.getSuperclass();
//      if (superclass == null) {
//        break;
//      }
//      // try to use something better than "Object"
//      if (superclass.getClassElement().getName().equals("Object")) {
//        if (superItem.mixins.length != 0) {
//          superclass = superItem.mixins[0];
//        } else if (superItem.interfaces.length != 0) {
//          superclass = superItem.interfaces[0];
//        }
//      }
    }
    // prepare sub types
    fillSubTypes(target, targetItem);
    if (isMemberHierarchy) {
      keepBranchesWithMemberOverride(targetItem);
    }
    // refresh viewer
    Display.getDefault().asyncExec(new Runnable() {
      @Override
      public void run() {
        Control control = viewer.getControl();
        if (control != null && !control.isDisposed()) {
          viewer.refresh();
          if (viewer instanceof TreeViewer) {
            ((TreeViewer) viewer).expandAll();
            viewer.setSelection(new StructuredSelection(targetItem), true);
          }
        }
      }
    });
  }

  /**
   * Builds complete sub-types hierarchy in {@link #superToSubsMap}.
   */
  private void fillSubTypes(TypeHierarchyItem type, TypeItem item) {
    // TODO (jwren/ scheglov) get new TypeHierarchyItem to work with the Editor
//    TypeHierarchyItem[] subTypes = type.getSubclasses();
//    List<TypeItem> subItems = Lists.newArrayList();
//    for (TypeHierarchyItem subType : subTypes) {
//      TypeItem subItem = new TypeItem(subType);
//      subToSuperMap.put(subItem, item);
//      subItems.add(subItem);
//      fillSubTypes(subType, subItem);
//    }
//    superToSubsMap.put(item, subItems);
  }

  /**
   * @return <code>true</code> if branch of given {@link TypeItem} has override for member.
   */
  private boolean keepBranchesWithMemberOverride(TypeItem typeItem) {
    List<TypeItem> subTypeItems = superToSubsMap.get(typeItem);
    if (subTypeItems != null) {
      for (Iterator<TypeItem> I = subTypeItems.iterator(); I.hasNext();) {
        TypeItem subTypeItem = I.next();
        if (!keepBranchesWithMemberOverride(subTypeItem)) {
          I.remove();
          superToSubsMap.remove(subTypeItem);
        }
      }
      if (!subTypeItems.isEmpty()) {
        return true;
      }
    }
    return typeItem.type.getMemberElement() != null;
  }
}
