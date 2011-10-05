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
package com.google.dart.tools.core.internal.refactoring.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.core.resources.IFile;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;

public class Changes {

  public static IFile[] getModifiedFiles(Change[] changes) {
    List<IFile> result = new ArrayList<IFile>();
    getModifiedFiles(result, changes);
    return result.toArray(new IFile[result.size()]);
  }

  private static void getModifiedFiles(List<IFile> result, Change[] changes) {
    for (int i = 0; i < changes.length; i++) {
      Change change = changes[i];
      Object modifiedElement = change.getModifiedElement();
      if (modifiedElement instanceof IAdaptable) {
        IFile file = (IFile) ((IAdaptable) modifiedElement).getAdapter(IFile.class);
        if (file != null)
          result.add(file);
      }
      if (change instanceof CompositeChange) {
        getModifiedFiles(result, ((CompositeChange) change).getChildren());
      }
    }
  }
}
