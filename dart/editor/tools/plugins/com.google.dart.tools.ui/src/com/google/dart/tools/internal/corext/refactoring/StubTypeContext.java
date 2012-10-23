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
package com.google.dart.tools.internal.corext.refactoring;

import com.google.dart.tools.core.model.CompilationUnit;

public class StubTypeContext {
  private String fBeforeString;
  private String fAfterString;
  private final CompilationUnit fCuHandle;

  public StubTypeContext(CompilationUnit cuHandle, String beforeString, String afterString) {
    fCuHandle = cuHandle;
    fBeforeString = beforeString;
    fAfterString = afterString;
  }

  public String getAfterString() {
    return fAfterString;
  }

  public String getBeforeString() {
    return fBeforeString;
  }

  public CompilationUnit getCuHandle() {
    return fCuHandle;
  }
}
