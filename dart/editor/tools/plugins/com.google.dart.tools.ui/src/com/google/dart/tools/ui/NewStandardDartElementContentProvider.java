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

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

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
public class NewStandardDartElementContentProvider implements ITreeContentProvider,
    IWorkingCopyProvider {

  protected static final Object[] NO_CHILDREN = new Object[0];

  @Override
  public void dispose() {
    // TODO Auto-generated method stub

  }

  @Override
  public Object[] getChildren(Object parentElement) {
    // TODO Auto-generated method stub
    return NO_CHILDREN;
  }

  @Override
  public Object[] getElements(Object inputElement) {
    // TODO Auto-generated method stub
    return NO_CHILDREN;
  }

  @Override
  public Object getParent(Object element) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean hasChildren(Object element) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    // TODO Auto-generated method stub

  }

  @Override
  public boolean providesWorkingCopies() {
    // TODO Auto-generated method stub
    return false;
  }

}
