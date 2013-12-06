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
package com.google.dart.tools.ui.actions;

import com.google.dart.tools.core.jobs.CleanLibrariesJob;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchSite;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * "Reanalyze sources" on the selected folders - does a clean build for the selected folders.
 */
public class CleanFoldersAction extends InstrumentedSelectionDispatchAction {

  private static final String ACTION_ID = "com.google.dart.tools.ui.cleanFolders"; //$NON-NLS-1$

  public CleanFoldersAction(IWorkbenchSite site) {
    super(site);
    setId(ACTION_ID);
    setText("Reanalyze Sources");
  }

  @Override
  public void selectionChanged(IStructuredSelection selection) {
    if (!selection.isEmpty() && allSelectedAreProjects(selection)) {
      setEnabled(true);
      return;
    }
    setEnabled(false);

  }

  @Override
  protected void doRun(IStructuredSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {

    if (!selection.isEmpty()) {
      List<IProject> projects = new ArrayList<IProject>();
      for (Object sel : selection.toArray()) {
        projects.add((IProject) sel);
      }
      CleanLibrariesJob job = new CleanLibrariesJob(projects);
      job.schedule();
    }
  }

  private boolean allSelectedAreProjects(IStructuredSelection selection) {
    for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
      Object selectedElement = iterator.next();
      if (!(selectedElement instanceof IProject)) {
        return false;
      }
    }
    return true;
  }

}
