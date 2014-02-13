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

package com.google.dart.eclipse.ui.internal.actions;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.DartProjectNature;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import java.util.Iterator;

/**
 * An action to toggle the Dart nature on and off for a project.
 */
public class ToggleDartNatureAction implements IObjectActionDelegate {
  private ISelection selection;

  public ToggleDartNatureAction() {

  }

  @Override
  public void run(IAction action) {
    if (selection instanceof IStructuredSelection) {
      for (Iterator<?> it = ((IStructuredSelection) selection).iterator(); it.hasNext();) {
        Object element = it.next();
        IProject project = null;
        if (element instanceof IProject) {
          project = (IProject) element;
        } else if (element instanceof IAdaptable) {
          project = (IProject) ((IAdaptable) element).getAdapter(IProject.class);
        }
        if (project != null) {
          toggleNature(project);
        }
      }
    }
  }

  @Override
  public void selectionChanged(IAction action, ISelection selection) {
    this.selection = selection;
  }

  @Override
  public void setActivePart(IAction action, IWorkbenchPart targetPart) {

  }

  private void toggleNature(IProject project) {
    try {
      IProjectDescription description = project.getDescription();
      String[] natures = description.getNatureIds();

      if (DartProjectNature.hasDartNature(project)) {
        // Remove the nature.
        for (int i = 0; i < natures.length; ++i) {
          if (DartCore.DART_PROJECT_NATURE.equals(natures[i])) {
            String[] newNatures = new String[natures.length - 1];
            System.arraycopy(natures, 0, newNatures, 0, i);
            System.arraycopy(natures, i + 1, newNatures, i, natures.length - i - 1);
            description.setNatureIds(newNatures);
            project.setDescription(description, null);
          }
        }
      } else {
        // Add the nature.
        String[] newNatures = new String[natures.length + 1];
        System.arraycopy(natures, 0, newNatures, 0, natures.length);
        newNatures[natures.length] = DartCore.DART_PROJECT_NATURE;
        description.setNatureIds(newNatures);
        project.setDescription(description, null);
      }
    } catch (CoreException e) {

    }
  }
}
