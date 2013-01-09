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

package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.ElementChangedEvent;
import com.google.dart.tools.core.model.ElementChangedListener;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import java.util.ArrayList;

/**
 * This is a first cut of the content provider of the Outline view with the Elements created by the
 * Analysis Engine.
 */
public class DartOutlinePageEngineContentProvider implements ITreeContentProvider,
    ElementChangedListener {

//  private TreeViewer viewer;
//
//  private Object input;

  protected static final Object[] NO_CHILDREN = new Object[0];

  @Override
  public void dispose() {
//    System.out.println("DartOutlinePageEngineContentProvider.dispose()");
  }

  /**
   * @see {@link ElementChangedListener#elementChanged(ElementChangedEvent)}.
   */
  @Override
  public void elementChanged(ElementChangedEvent event) {
  }

  @Override
  public Object[] getChildren(Object parentElement) {
    ArrayList<Element> childrenList = new ArrayList<Element>();
    if (parentElement instanceof CompilationUnitElement) {
      // CompilationUnitElement
      CompilationUnitElement cu = (CompilationUnitElement) parentElement;
      putCompUnitChildrenInList(cu, childrenList);
    } else if (parentElement instanceof ClassElementImpl) {
      // ClassElementImpl
      ClassElementImpl ce = (ClassElementImpl) parentElement;
      Element[] fields = ce.getFields();
      Element[] constructors = ce.getConstructors();
      Element[] methods = ce.getMethods();
      for (int i = 0; i < fields.length; i++) {
        childrenList.add(fields[i]);
      }
      for (int i = 0; i < constructors.length; i++) {
        childrenList.add(constructors[i]);
      }
      for (int i = 0; i < methods.length; i++) {
        childrenList.add(methods[i]);
      }
    } else {
//      System.out.println("DartOutlinePageEngineContentProvider.getChildren() null ... "
//          + parentElement.getClass());
    }
    return childrenList.toArray(new Element[childrenList.size()]);
  }

  @Override
  public Object[] getElements(Object inputElement) {
//    System.out.println("DartOutlinePageEngineContentProvider.getElements()");
    ArrayList<Element> childrenList = new ArrayList<Element>();
    if (inputElement instanceof CompilationUnitElement) {
      CompilationUnitElement cu = (CompilationUnitElement) inputElement;
      putCompUnitChildrenInList(cu, childrenList);
    }
    return childrenList.toArray(new Element[childrenList.size()]);
  }

  @Override
  public Object getParent(Object element) {
//    System.out.println("DartOutlinePageEngineContentProvider.getParent() " + element.getClass());
    if (element instanceof Element) {
      Element e = (Element) element;
      // TODO (jwren) implement this
      return e.getLibrary();
    }
    return null;
  }

  @Override
  public boolean hasChildren(Object element) {
    return true;
  }

  @Override
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    //this.viewer = (TreeViewer) viewer;
    if (oldInput == null && newInput != null) {
      DartCore.addElementChangedListener(this);
    } else if (oldInput != null && newInput == null) {
      DartCore.removeElementChangedListener(this);
    }
    //input = newInput;
  }

  private void putCompUnitChildrenInList(CompilationUnitElement compUnitElt,
      ArrayList<Element> childrenList) {
    Element[] fields = compUnitElt.getFields();
    Element[] functions = compUnitElt.getFunctions();
    Element[] types = compUnitElt.getTypes();
    for (int i = 0; i < fields.length; i++) {
      childrenList.add(fields[i]);
    }
    for (int i = 0; i < functions.length; i++) {
      childrenList.add(functions[i]);
    }
    for (int i = 0; i < types.length; i++) {
      childrenList.add(types[i]);
    }
  }
}
