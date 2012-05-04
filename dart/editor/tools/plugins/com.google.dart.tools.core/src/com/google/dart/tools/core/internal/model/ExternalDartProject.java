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
package com.google.dart.tools.core.internal.model;

import com.google.dart.tools.core.DartCore;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;

/**
 * Instances of the class <code>ExternalDartProject</code> represent a Dart project that does not
 * exist.
 */
public class ExternalDartProject extends DartProjectImpl {
  /**
   * The name of the external project. Note that this name can be surfaced in the UI (see
   * https://bugs.eclipse.org/bugs/show_bug.cgi?id=128258)
   */
  public static final String EXTERNAL_PROJECT_NAME = " "; //$NON-NLS-1$

  /**
   * Initialize a newly created Dart project to represent the given project.
   */
  public ExternalDartProject() {
    super(
        DartModelManager.getInstance().getDartModel(),
        ResourcesPlugin.getWorkspace().getRoot().getProject(EXTERNAL_PROJECT_NAME));
  }

  @Override
  public boolean equals(Object o) {
    return this == o;
  }

  @Override
  public boolean exists() {
    // external project never exists
    return false;
  }

  @Override
  public String getOption(String optionName, boolean inheritJavaCoreOptions) {
    DartCore.notYetImplemented();
    // if (JavaCore.COMPILER_PB_FORBIDDEN_REFERENCE.equals(optionName)
    // || JavaCore.COMPILER_PB_DISCOURAGED_REFERENCE.equals(optionName))
    // return JavaCore.IGNORE;
    return super.getOption(optionName, inheritJavaCoreOptions);
  }

  @Override
  protected IStatus validateExistence(IResource underlyingResource) {
    // allow opening of external project
    return DartModelStatusImpl.VERIFIED_OK;
  }
}
