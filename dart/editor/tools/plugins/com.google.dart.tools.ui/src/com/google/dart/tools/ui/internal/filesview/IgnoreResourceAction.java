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
package com.google.dart.tools.ui.internal.filesview;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.DartIgnoreManager;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.SelectionListenerAction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Action to add (or remove) resources from the dart ignore list.
 */
public class IgnoreResourceAction extends SelectionListenerAction {

  private final Shell shell;
  private List<IResource> resources = Arrays.asList(new IResource[0]);

  protected IgnoreResourceAction(Shell shell) {
    super(FilesViewMessages.IgnoreResourcesAction_dont_analyze_label);
    this.shell = shell;
  }

  @Override
  public void run() {
    try {
      for (IResource r : resources) {
        toggleIgnoreState(r);
      }
    } catch (IOException e) {
      MessageDialog.openError(shell, "Error Ignoring Resource", e.getMessage());
      DartCore.logInformation("Could not access ignore file", e);
    } catch (CoreException e) {
      MessageDialog.openError(shell, "Error Deleting Markers", e.getMessage()); //$NON-NLS-1$
    } finally {
      updateLabel();
    }
  }

  @Override
  protected List<IResource> getSelectedResources() {
    @SuppressWarnings("unchecked")
    List<Object> res = super.getSelectedResources();
    ArrayList<IResource> resources = new ArrayList<IResource>();
    for (Object r : res) {
      resources.add((IResource) r);
    }
    return resources;
  }

  @Override
  protected boolean updateSelection(IStructuredSelection selection) {

    resources = getSelectedResources();

    if (resources.isEmpty() || !sameAnalysisState(resources) || anyIgnoredByDefault(resources)) {
      return false;
    }

    updateLabel();

    return true;
  }

  void updateLabel() {
    if (DartCore.isAnalyzed(resources.get(0))) {
      setText(FilesViewMessages.IgnoreResourcesAction_dont_analyze_label);
    } else {
      setText(FilesViewMessages.IgnoreResourcesAction_do_analyze_label);
    }
  }

  private boolean anyIgnoredByDefault(List<IResource> resources) {
    for (IResource res : resources) {
      if (DartIgnoreManager.isIgnoredByDefault(res)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Ensures that all selected resources are in the same state of analysis.
   */
  private boolean sameAnalysisState(List<IResource> resources) {
    if (resources.isEmpty()) {
      return true;
    }

    boolean isAnalyzed = DartCore.isAnalyzed(resources.get(0));

    for (IResource resource : resources.subList(0, resources.size())) {
      if (DartCore.isAnalyzed(resource) != isAnalyzed) {
        return false;
      }
    }

    return true;
  }

  private void toggleIgnoreState(IResource resource) throws IOException, CoreException {
    if (DartCore.isAnalyzed(resource)) {
      DartCore.addToIgnores(resource);
    } else {
      DartCore.removeFromIgnores(resource);
    }
  }

}
