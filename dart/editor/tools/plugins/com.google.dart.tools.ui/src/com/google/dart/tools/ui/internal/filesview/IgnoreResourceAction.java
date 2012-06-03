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
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.internal.builder.DartcBuildHandler;
import com.google.dart.tools.core.internal.model.DartModelManager;
import com.google.dart.tools.core.internal.model.SystemLibraryManagerProvider;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.SelectionListenerAction;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * Action to add (or remove) resources from the dart ignore list.
 */
public class IgnoreResourceAction extends SelectionListenerAction {

  class AnalyzeProjectJob extends WorkspaceJob {

    IProject project;

    public AnalyzeProjectJob(IProject project) {
      super("Analyzing source");
      this.project = project;

    }

    @Override
    public IStatus runInWorkspace(IProgressMonitor monitor) {
      try {
        monitor.beginTask("Analyzing...", IProgressMonitor.UNKNOWN);

        new DartcBuildHandler().buildLibrariesIn(project, resource, monitor);
      } catch (CoreException e) {
        DartToolsPlugin.log(e);

        return Status.CANCEL_STATUS;
      } finally {
        monitor.done();
      }

      return Status.OK_STATUS;
    }
  }

  private static boolean allElementsAreResources(IStructuredSelection selection) {
    for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
      Object selectedElement = iterator.next();
      if (!(selectedElement instanceof IResource)) {
        return false;
      }
    }
    return true;
  }

  private IResource resource;
  private final Shell shell;

  protected IgnoreResourceAction(Shell shell) {
    super(FilesViewMessages.IgnoreResourcesAction_dont_analyze_label);
    this.shell = shell;
  }

  @Override
  public void run() {

    if (resource != null) {
      try {
        if (DartCore.isAnalyzed(resource)) {
          DartModelManager.getInstance().addToIgnores(resource);
          if (DartCoreDebug.ANALYSIS_SERVER) {
            SystemLibraryManagerProvider.getDefaultAnalysisServer().discard(
                resource.getLocation().toFile());
          }
        } else {
          DartModelManager.getInstance().removeFromIgnores(resource);
          if (DartCoreDebug.ANALYSIS_SERVER) {
            File file = resource.getLocation().toFile();
            SystemLibraryManagerProvider.getDefaultAnalysisServer().scan(file, true);
          } else {
            AnalyzeProjectJob analyzeProjectJob = new AnalyzeProjectJob(resource.getProject());
            analyzeProjectJob.schedule();
          }
        }
      } catch (IOException e) {
        MessageDialog.openError(shell, "Error Ignoring Resource", e.getMessage());
        DartCore.logInformation("Could not access ignore file", e);
      } catch (CoreException e) {
        MessageDialog.openError(shell, "Error Deleting Markers", e.getMessage()); //$NON-NLS-1$
      }
    }
  }

  @Override
  protected boolean updateSelection(IStructuredSelection selection) {
    if (selection.size() != 1 || !allElementsAreResources(selection)) {
      resource = null;
      return false;
    }

    resource = (IResource) selection.getFirstElement();

    updateLabel();

    return true;
  }

  void updateLabel() {
    if (DartCore.isAnalyzed(resource)) {
      setText(FilesViewMessages.IgnoreResourcesAction_dont_analyze_label);
    } else {
      setText(FilesViewMessages.IgnoreResourcesAction_do_analyze_label);
    }
  }

}
