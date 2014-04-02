/*
 * Copyright (c) 2013, the Dart project authors.
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
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.PropertyInducingElement;
import com.google.dart.engine.element.visitor.GeneralizingElementVisitor;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.services.util.HierarchyUtils;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TypeHierarchyContentProvider implements ITreeContentProvider {
  public static class SuperItem extends TypeItem {
    private final InterfaceType[] withTypes;
    private final InterfaceType[] implementsTypes;

    public SuperItem(InterfaceType type, InterfaceType[] withTypes) {
      super(type);
      this.withTypes = withTypes;
      this.implementsTypes = type.getInterfaces();
    }

    @Override
    public StyledString toStyledString() {
      StyledString styledString = super.toStyledString();
      if (withTypes.length != 0) {
        styledString.append(" with ", StyledString.QUALIFIER_STYLER);
        styledString.append(StringUtils.join(withTypes, ", "));
      }
      if (implementsTypes.length != 0) {
        styledString.append(" implements ", StyledString.QUALIFIER_STYLER);
        styledString.append(StringUtils.join(implementsTypes, ", "));
      }
      return styledString;
    }
  }

  public static class TypeItem {
    public final InterfaceType type;
    public final Element element;

    public TypeItem(InterfaceType type) {
      this.type = type;
      this.element = type.getElement();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj instanceof ClassElement) {
        return obj.equals(element);
      }
      if (obj instanceof TypeItem) {
        return ((TypeItem) obj).element.equals(element);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return element.hashCode();
    }

    public StyledString toStyledString() {
      return new StyledString(type.toString());
    }
  }

  public static final InterfaceType[] NO_TYPES = new InterfaceType[0];

  private static final SearchEngine searchEngine = DartCore.getProjectManager().newSearchEngine();

  private static SuperItem createSuperItem(Set<InterfaceType> addedTypes, InterfaceType type) {
    InterfaceType superType = type.getSuperclass();
    if (superType != null && superType.isObject()) {
      InterfaceType[] interfaces = type.getInterfaces();
      if (interfaces.length != 0) {
        superType = interfaces[0];
      }
    }
    if (superType == null) {
      return null;
    }
    if (!addedTypes.add(superType)) {
      return null;
    }
    return new SuperItem(superType, type.getMixins());
  }

  /**
   * @return any local member of the given {@link ClassElement} with given name.
   */
  private static Element findLocalMember(final ClassElement type, final String name) {
    final Element result[] = {null};
    type.accept(new GeneralizingElementVisitor<Void>() {
      @Override
      public Void visitElement(Element element) {
        if (element instanceof ConstructorElement) {
          return null;
        }
        if (element.isSynthetic()) {
          return null;
        }
        if (element == type) {
          super.visitElement(element);
        } else if (name == null || element.getDisplayName().equals(name)) {
          result[0] = element;
        }
        return null;
      }
    });
    return result[0];
  }

  /**
   * @return <code>true</code> if given {@link ClassElement} has local member with given name.
   */
  private static boolean hasLocalMember(ClassElement type, String name) {
    return findLocalMember(type, name) != null;
  }

  private String memberName;
  private final List<SuperItem> superList = Lists.newArrayList();
  private final Map<ClassElement, List<ClassElement>> superToSubsMap = Maps.newHashMap();
  private final Map<ClassElement, ClassElement> subToSuperMap = Maps.newHashMap();

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
    // TypeItem -> ClassElement
    if (parentElement instanceof TypeItem) {
      parentElement = ((TypeItem) parentElement).element;
    }
    // subs
    {
      List<ClassElement> subs = superToSubsMap.get(parentElement);
      if (subs != null) {
        return subs.toArray(new ClassElement[subs.size()]);
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
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    memberName = null;
    superList.clear();
    superToSubsMap.clear();
    subToSuperMap.clear();
    if (newInput instanceof Object[] && ((Object[]) newInput).length == 1) {
      Object inputObject = ((Object[]) newInput)[0];
      if (!(inputObject instanceof Element)) {
        return;
      }
      Element inputElement = (Element) inputObject;
      if (inputElement.getEnclosingElement() instanceof ClassElement) {
        if (inputElement instanceof ExecutableElement
            || inputElement instanceof PropertyInducingElement) {
          memberName = inputElement.getDisplayName();
          inputObject = inputElement.getEnclosingElement();
        }
      }
      if (inputObject instanceof ClassElement) {
        ClassElement inputClass = (ClassElement) inputObject;
        InterfaceType inputType = inputClass.getType();
        InterfaceType type = inputType;
        Set<InterfaceType> addedTypes = Sets.<InterfaceType> newHashSet();
        while (true) {
          SuperItem item = createSuperItem(addedTypes, type);
          if (item == null) {
            break;
          }
          superList.add(0, item);
          type = item.type;
        }
        superList.add(new SuperItem(inputType, NO_TYPES));
        // sub types
        scheduleSubTypesSearch(viewer, inputClass);
      }
    }
  }

  /**
   * @return the {@link ClassElement} or {@link ExecutableElement} for given {@link ClassElement}
   *         selection.
   */
  Object convertSelectedElement(Object o) {
    try {
      if (o instanceof TypeItem) {
        o = ((TypeItem) o).element;
      }
      if (memberName != null && o instanceof ClassElement) {
        ClassElement type = (ClassElement) o;
        return findLocalMember(type, memberName);
      }
    } catch (Throwable e) {
      DartToolsPlugin.log(e);
    }
    return o;
  }

  /**
   * @return the {@link Predicate} to check if given {@link ClassElement} is not interesting part of
   *         hierarchy, so should be displayed using light color.
   */
  Predicate<Object> getLightPredicate() {
    return new Predicate<Object>() {
      @Override
      public boolean apply(Object input) {
        if (input instanceof TypeItem) {
          input = ((TypeItem) input).element;
        }
        if (memberName != null && input instanceof ClassElement) {
          ClassElement type = (ClassElement) input;
          return !hasLocalMember(type, memberName);
        }
        return false;
      }
    };
  }

  /**
   * Builds complete sub-types hierarchy in {@link #superToSubsMap}.
   */
  private void fillSubTypes(ClassElement type) {
    List<ClassElement> subTypes = HierarchyUtils.getDirectSubClasses(searchEngine, type);
    for (ClassElement subType : subTypes) {
      subToSuperMap.put(subType, type);
      fillSubTypes(subType);
    }
    superToSubsMap.put(type, subTypes);
  }

  /**
   * @return <code>true</code> if branch of given {@link ClassElement} has override for member.
   */
  private boolean keepBranchesWithMemberOverride(ClassElement type) {
    List<ClassElement> subTypes = superToSubsMap.get(type);
    if (subTypes != null) {
      for (Iterator<ClassElement> I = subTypes.iterator(); I.hasNext();) {
        ClassElement subType = I.next();
        if (!keepBranchesWithMemberOverride(subType)) {
          I.remove();
          superToSubsMap.remove(subType);
        }
      }
      if (!subTypes.isEmpty()) {
        return true;
      }
    }
    return hasLocalMember(type, memberName);
  }

  /**
   * Schedules possibly long-running sub-types search operation.
   */
  private void scheduleSubTypesSearch(final Viewer viewer, final ClassElement inputClass) {
    Thread thread = new Thread() {
      @Override
      public void run() {
        // prepare sub types
        fillSubTypes(inputClass);
        if (memberName != null) {
          keepBranchesWithMemberOverride(inputClass);
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
              }
            }
          }
        });
      }
    };
    thread.setDaemon(true);
    thread.start();
  }
}
