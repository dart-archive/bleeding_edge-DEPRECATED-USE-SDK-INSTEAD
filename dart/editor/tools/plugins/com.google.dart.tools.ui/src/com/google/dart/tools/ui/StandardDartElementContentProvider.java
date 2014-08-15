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
package com.google.dart.tools.ui;

import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.visitor.GeneralizingElementVisitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import java.util.ArrayList;
import java.util.Collections;

/**
 * "New" base content provider for Dart elements. It provides access to the Dart element hierarchy
 * without listening to changes in the Dart model. If updating the presentation on Dart model change
 * is required than clients have to subclass, listen to Dart model changes and have to update the UI
 * using corresponding methods provided by the JFace viewers or their own UI presentation.
 * <p>
 * The following Dart element hierarchy is surfaced by this content provider:
 * <p>
 * TODO(pquitslund): update this hierarchy to reflect the new element model
 * 
 * <pre>
 * Dart model (<code>DartModel</code>)
 *    Dart project (<code>DartProject</code>)
 *       library (<code>DartLibrary</code>)
 *          compilation unit (<code>CompilationUnit</code>)
 *          library file (<code>LibraryConfigurationFile</code>)
 *          Imported Libraries
 * </pre>
 * </p>
 */
public class StandardDartElementContentProvider implements ITreeContentProvider,
    IWorkingCopyProvider {

  protected static final Object[] NO_CHILDREN = new Object[0];

  // Whether to return members when asking a compilation unit for its children.
  private final boolean provideMembers;

  /**
   * Creates a new content provider. The content provider does not provide members of compilation
   * units or class files.
   */
  public StandardDartElementContentProvider() {
    this(true);
  }

  /**
   * Creates a new <code>NewStandardDartElementContentProvider</code>.
   * 
   * @param provideMembers if <code>true</code>, members below compilation units are provided
   */
  public StandardDartElementContentProvider(boolean provideMembers) {
    this.provideMembers = provideMembers;
  }

  @Override
  public void dispose() {
  }

  @Override
  public Object[] getChildren(Object element) {

    //TODO (pquitslund): complete cases

    if (!exists(element)) {
      return NO_CHILDREN;
    }

    if (element instanceof IProject) {
      IProject project = (IProject) element;
      if (!(project.isOpen())) {
        return NO_CHILDREN;
      }
      //TODO (pquitslund): support dart projects
      return NO_CHILDREN;
//      if (project.hasNature(DartCore.DART_PROJECT_NATURE)) {
//        DartProject dartProject = DartCore.create(project);
//        return dartProject.getDartLibraries();
//      }
    }

    try {

      if (element instanceof IFile) {
        return NO_CHILDREN;
      }

      if (element instanceof IFolder) {
        return getFolderContent((IFolder) element);
      }

      if (element instanceof CompilationUnitElement) {
        return getCompilationUnitMembers((CompilationUnitElement) element);
      }
      if (element instanceof ClassElement) {
        return getClassMembers((ClassElement) element);
      }

      if (element instanceof FunctionElement) {
        return NO_CHILDREN;
      }

    } catch (CoreException e) {
      // Fall through
    }

    return NO_CHILDREN;

  }

  @Override
  public Object[] getElements(Object parent) {
    return getChildren(parent);
  }

  @Override
  public Object getParent(Object element) {
    if (!exists(element)) {
      return null;
    }
    return internalGetParent(element);
  }

  @Override
  public boolean hasChildren(Object element) {

    //TODO (pquitslund): support all cases

    if (element instanceof CompilationUnitElement) {
      if (!provideMembers()) {
        return false;
      }
      return getChildren(element).length > 0;
    }

    if (element instanceof FunctionElement) {
      return false;
    }

    return getChildren(element).length > 0;
  }

  @Override
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
  }

  @Override
  public boolean providesWorkingCopies() {
    return true;
  }

  /**
   * Note: This method is for internal use only. Clients should not call this method.
   */
  protected boolean exists(Object element) {
    if (element == null) {
      return false;
    }
    if (element instanceof IResource) {
      return ((IResource) element).exists();
    }
    return true;
  }

  protected Object[] getFolderContent(IFolder folder) throws CoreException {

    //TODO (pquitslund): implement

    return NO_CHILDREN;
  }

  /**
   * Note: This method is for internal use only. Clients should not call this method.
   */
  protected Object internalGetParent(Object element) {

    //TODO (pquitslund): handle all cases

    if (element instanceof Element) {
      return ((Element) element).getEnclosingElement();
    }

    return null;
  }

  /**
   * Returns whether members are provided when asking for a compilation units or class file for its
   * children.
   * 
   * @return <code>true</code> if the content provider provides members; otherwise
   *         <code>false</code> is returned
   */
  protected boolean provideMembers() {
    return provideMembers;
  }

  private Object[] getClassMembers(ClassElement ce) {
    return getSortedMembers(ce);
  }

  private Object[] getCompilationUnitMembers(CompilationUnitElement cu) {
    return getSortedMembers(cu);
  }

  /**
   * Get members sorted by offset.
   */
  private Object[] getSortedMembers(Element element) {

    final ArrayList<Element> members = new ArrayList<Element>();

    element.visitChildren(new GeneralizingElementVisitor<Void>() {
      @Override
      public Void visitElement(Element element) {
        if (!element.isSynthetic()) {
          members.add(element);
        }
        return null;
      }
    });

    Collections.sort(members, Element.SORT_BY_OFFSET);

    return members.toArray(new Element[members.size()]);
  }

}
