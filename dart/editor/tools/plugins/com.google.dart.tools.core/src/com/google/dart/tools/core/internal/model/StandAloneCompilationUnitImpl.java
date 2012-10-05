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

import com.google.dart.compiler.DartSource;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * A stand alone compilation unit that is not in a Dart project
 */
public class StandAloneCompilationUnitImpl extends CompilationUnitImpl {

  DartProjectImpl dartProject;

  public StandAloneCompilationUnitImpl(IFile file, DartSource dartSource, WorkingCopyOwner owner) {
    super(null, file, owner);
  }

  public StandAloneCompilationUnitImpl(IFile file, WorkingCopyOwner owner) {
    super(null, file, owner);
  }

  @Override
  public DartProject getDartProject() {
    if (dartProject == null) {
      dartProject = new DartProjectImpl(null, getFile().getProject());
    }
    return dartProject;
  }

  @Override
  public DartLibrary getLibrary() {
    return null;
  }

  @Override
  public DartUnit reconcile(boolean forceProblemDetection, WorkingCopyOwner workingCopyOwner,
      IProgressMonitor monitor) throws DartModelException {
    return null;
  }

}
