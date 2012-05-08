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
package com.google.dart.tools.search.internal.core.text;

import com.google.dart.tools.search.core.text.TextSearchScope;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.MultiStatus;

import java.io.File;
import java.util.ArrayList;

/**
 * Calculates external files in a given scope.
 */
public class ExternalFilesOfScopeCalculator {

  private ArrayList<File> files;
  private final TextSearchScope textSearchScope;
  private final MultiStatus status;

  public ExternalFilesOfScopeCalculator(TextSearchScope textSearchScope, MultiStatus status) {
    this.textSearchScope = textSearchScope;
    this.status = status;
  }

  public File[] process() {
    files = new ArrayList<File>();
    try {
      for (File root : textSearchScope.getExternalRoots()) {
        try {
          if (root.canRead()) {
            visit(root);
          }
        } catch (CoreException ex) {
          // report and ignore
          status.add(ex.getStatus());
        }
      }
      return files.toArray(new File[files.size()]);
    } finally {
      files = null;
    }
  }

  private void visit(File root) throws CoreException {
    if (root.isDirectory()) {
      for (File file : root.listFiles()) {
        visit(file);
      }
    } else {
      files.add(root);
    }
  }
}
