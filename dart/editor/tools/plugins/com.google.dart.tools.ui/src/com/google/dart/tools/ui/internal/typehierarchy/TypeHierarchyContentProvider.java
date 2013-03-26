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
import com.google.dart.compiler.util.apache.ArrayUtils;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.visitor.GeneralizingElementVisitor;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.services.util.HierarchyUtils;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class TypeHierarchyContentProvider implements ITreeContentProvider {
  private static final SearchEngine searchEngine = DartCore.getProjectManager().newSearchEngine();

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
        if (element == type) {
          super.visitElement(element);
        } else if (name == null || element.getName().equals(name)) {
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
  private final List<ClassElement> superList = Lists.newArrayList();
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
      if (inputElement instanceof ExecutableElement
          && inputElement.getEnclosingElement() instanceof ClassElement) {
        memberName = inputElement.getName();
        inputObject = inputElement.getEnclosingElement();
      }
      if (inputObject instanceof ClassElement) {
        ClassElement inputClass = (ClassElement) inputObject;
        // super types
        superList.addAll(HierarchyUtils.getSuperClasses(inputClass));
        superList.add(inputClass);
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
