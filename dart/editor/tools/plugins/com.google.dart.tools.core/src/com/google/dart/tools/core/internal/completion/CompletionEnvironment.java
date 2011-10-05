/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.internal.completion;

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;

public class CompletionEnvironment {
  private DartProject project;
  private WorkingCopyOwner owner;

  public CompletionEnvironment(DartProject project, WorkingCopyOwner owner,
      CompilationUnit unitToSkip) {
    this.project = project;
    this.owner = owner;
  }

  public WorkingCopyOwner getOwner() {
    return owner;
  }

  public DartProject getProject() {
    return project;
  }
}
