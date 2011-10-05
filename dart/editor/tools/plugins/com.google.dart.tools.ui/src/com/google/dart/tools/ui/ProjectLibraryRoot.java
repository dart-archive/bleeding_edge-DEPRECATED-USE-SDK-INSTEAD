/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui;

import org.eclipse.core.runtime.IAdaptable;

/**
 * TODO(devoncarew): this class is not used by Dart
 * 
 * @author childsb
 */
public class ProjectLibraryRoot implements IAdaptable {

//  private DartProject project;
//  private static final String LIBRARY_UI_DESC = Messages.getString("ProjectLibraryRoot.0"); //$NON-NLS-1$
//
//  public static final class WorkBenchAdapter implements IWorkbenchAdapter {
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see org.eclipse.ui.model.IWorkbenchAdapter#getChildren(java.lang.Object)
//     */
//    public Object[] getChildren(Object o) {
//
//      if (o instanceof ProjectLibraryRoot)
//        return ((ProjectLibraryRoot) o).getChildren();
//      return new Object[0];
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see
//     * org.eclipse.ui.model.IWorkbenchAdapter#getImageDescriptor(java.lang.Object
//     * )
//     */
//    public ImageDescriptor getImageDescriptor(Object object) {
//      return JavaPluginImages.DESC_OBJS_LIBRARY;
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see org.eclipse.ui.model.IWorkbenchAdapter#getLabel(java.lang.Object)
//     */
//    public String getLabel(Object o) {
//      if (o instanceof ProjectLibraryRoot) {
//        return LIBRARY_UI_DESC;
//      }
//      return null;
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see org.eclipse.ui.model.IWorkbenchAdapter#getParent(java.lang.Object)
//     */
//    public Object getParent(Object o) {
//      // TODO Auto-generated method stub
//      System.out.println("Unimplemented method:WorkBenchAdapter.getParent"); //$NON-NLS-1$
//      return null;
//    }
//
//  }
//
//  public ProjectLibraryRoot(DartProject project) {
//    this.project = project;
//
//  }
//
//  public DartProject getProject() {
//    return project;
//  }
//
//  public String getText() {
//    return ProjectLibraryRoot.LIBRARY_UI_DESC;
//  }
//
//  public boolean hasChildren() {
//    try {
//      return project.getPackageFragmentRoots().length > 0;
//    } catch (DartModelException e) {
//      e.printStackTrace();
//    }
//    return true;
//  }
//
//  public Object[] getChildren() {
//    if (!project.getProject().isOpen())
//      return new Object[0];
//    boolean addJARContainer = false;
//    ArrayList projectPackageFragmentRoots = new ArrayList();
//    IPackageFragmentRoot[] roots = new IPackageFragmentRoot[0];
//    try {
//      roots = project.getPackageFragmentRoots();
//    } catch (DartModelException e1) {
//    }
//    next : for (int i = 0; i < roots.length; i++) {
//      IPackageFragmentRoot root = roots[i];
//      IIncludePathEntry classpathEntry = null;
//      try {
//        classpathEntry = root.getRawIncludepathEntry();
//      } catch (DartModelException e) {
//      }
//
//      int entryKind = classpathEntry.getEntryKind();
//      IIncludePathAttribute[] attribs = classpathEntry.getExtraAttributes();
//
//      for (int k = 0; attribs != null && k < attribs.length; k++) {
//        if (attribs[k] == IIncludePathAttribute.HIDE)
//          continue next;
//
//      }
//
//      if ((entryKind != IIncludePathEntry.CPE_SOURCE)
//          && entryKind != IIncludePathEntry.CPE_CONTAINER) {
//        addJARContainer = true;
//        projectPackageFragmentRoots.add(root);
//      }
//    }
//
//    if (addJARContainer) {
//      projectPackageFragmentRoots.add(new LibraryContainer(project));
//    }
//
//    // separate loop to make sure all containers are on the classpath
//    IIncludePathEntry[] rawClasspath = new IIncludePathEntry[0];
//    try {
//      rawClasspath = project.getRawIncludepath();
//    } catch (DartModelException e) {
//    }
//    next2 : for (int i = 0; i < rawClasspath.length; i++) {
//      IIncludePathEntry classpathEntry = rawClasspath[i];
//      IIncludePathAttribute[] attribs = classpathEntry.getExtraAttributes();
//
//      for (int k = 0; attribs != null && k < attribs.length; k++) {
//        if (attribs[k].equals(IIncludePathAttribute.HIDE))
//          continue next2;
//      }
//
//      if (classpathEntry.getEntryKind() == IIncludePathEntry.CPE_CONTAINER) {
//        projectPackageFragmentRoots.add(new JsGlobalScopeContainer(project,
//            classpathEntry));
//      }
//    }
//    return projectPackageFragmentRoots.toArray();
//  }
//
//  /*
//   * (non-Javadoc)
//   * 
//   * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
//   */
  @Override
  public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
//    if (adapter == IWorkbenchAdapter.class) {
//      return new WorkBenchAdapter();
//    } else if (adapter == IProject.class) {
//      return getProject().getProject();
//    }
//
    return null;
  }
//
//  public boolean equals(Object obj) {
//    if (obj instanceof ProjectLibraryRoot)
//      return project.equals(((ProjectLibraryRoot) obj).project);
//    return super.equals(obj);
//  }
//
//  public int hashCode() {
//    return project.hashCode() + super.hashCode();
//  }
}
