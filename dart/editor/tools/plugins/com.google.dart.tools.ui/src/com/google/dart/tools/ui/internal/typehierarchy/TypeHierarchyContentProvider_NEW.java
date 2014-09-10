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
import com.google.common.collect.Sets;
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
import java.util.Set;

public class TypeHierarchyContentProvider_NEW implements ITreeContentProvider {
  public class TypeItem {
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
        appendItems(styledString, mixins);
      }
      if (interfaces != null && interfaces.length != 0) {
        styledString.append(" implements ", StyledString.QUALIFIER_STYLER);
        appendItems(styledString, interfaces);
      }
      return styledString;
    }

    private void appendItems(StyledString styledString, int[] ids) {
      for (int i = 0; i < ids.length; i++) {
        int id = ids[i];
        TypeHierarchyItem item = items.get(id);
        if (i != 0) {
          styledString.append(", ");
        }
        styledString.append(item.getBestName());
      }
    }
  }

  private List<TypeHierarchyItem> items = Lists.newArrayList();
  private final Set<TypeHierarchyItem> seenItems = Sets.newHashSet();
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
    seenItems.clear();
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
          public void computedHierarchy(List<TypeHierarchyItem> computedItems) {
            items = computedItems;
            inputHierarchyChanged(viewer);
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

  void inputHierarchyChanged(final Viewer viewer) {
    if (items.isEmpty()) {
      return;
    }
    TypeHierarchyItem target = items.get(0);
    isMemberHierarchy = target.getMemberElement() != null;
    final TypeItem targetItem = new TypeItem(target);
    // fill super hierarchy
    TypeHierarchyItem superType = target;
    while (superType != null) {
      if (!seenItems.add(superType)) {
        break;
      }
      TypeItem superItem = new TypeItem(superType);
      superList.add(0, superItem);
      if (superType == target) {
        superItem = targetItem;
      }
      // try "extends"
      {
        Integer superId = superItem.type.getSuperclass();
        if (superId == null) {
          break;
        }
        superType = items.get(superId);
      }
      // try to use something better than "Object"
      if (superType.getClassElement().getName().equals("Object")) {
        if (superItem.mixins.length != 0) {
          int id = superItem.mixins[0];
          superType = items.get(id);
        } else if (superItem.interfaces.length != 0) {
          int id = superItem.interfaces[0];
          superType = items.get(id);
        }
      }
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
    int[] subIds = type.getSubclasses();
    TypeHierarchyItem[] subTypes = new TypeHierarchyItem[subIds.length];
    for (int i = 0; i < subIds.length; i++) {
      int id = subIds[i];
      subTypes[i] = items.get(id);
    }
    List<TypeItem> subItems = Lists.newArrayList();
    for (TypeHierarchyItem subType : subTypes) {
      if (seenItems.add(subType)) {
        TypeItem subItem = new TypeItem(subType);
        subToSuperMap.put(subItem, item);
        subItems.add(subItem);
        fillSubTypes(subType, subItem);
      }
    }
    superToSubsMap.put(item, subItems);
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
