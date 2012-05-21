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
package com.google.dart.eclipse.wizards;

import com.google.dart.eclipse.DartEclipseUI;
import com.google.dart.tools.core.DartCore;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;

/**
 * Standard workbench wizard that creates a new Dart project resource in the workspace.
 */
public class DartProjectWizard extends BasicNewProjectResourceWizard {

  //TODO(pquitslund): remove BasicNewProjectResourceWizard dependency

  @Override
  public boolean performFinish() {
    boolean finished = super.performFinish();
    if (finished) {
      IProject project = getNewProject();
      try {
        IProjectDescription description = project.getDescription();
        description.setNatureIds(new String[] {DartCore.DART_PROJECT_NATURE});
      } catch (CoreException e) {
        DartEclipseUI.logError(e);
      }
    }
    return finished;
  }

}
