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
package com.google.dart.tools.internal.corext.refactoring.util;

import com.google.dart.tools.core.model.CompilationUnit;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import java.util.ArrayList;
import java.util.List;

/**
 * @coverage dart.editor.ui.refactoring.core
 */
public class ResourceUtil {

  public static IFile getFile(CompilationUnit cu) {
    IResource resource = cu.getResource();
    if (resource != null && resource.getType() == IResource.FILE) {
      return (IFile) resource;
    } else {
      return null;
    }
  }

  public static IFile[] getFiles(CompilationUnit[] cus) {
    List<IResource> files = new ArrayList<IResource>(cus.length);
    for (int i = 0; i < cus.length; i++) {
      IResource resource = cus[i].getResource();
      if (resource != null && resource.getType() == IResource.FILE) {
        files.add(resource);
      }
    }
    return files.toArray(new IFile[files.size()]);
  }

  public static IResource getResource(Object o) {
    if (o instanceof IResource) {
      return (IResource) o;
    }
    return null;
  }

  //----- other ------------------------------

  private ResourceUtil() {
  }
}
