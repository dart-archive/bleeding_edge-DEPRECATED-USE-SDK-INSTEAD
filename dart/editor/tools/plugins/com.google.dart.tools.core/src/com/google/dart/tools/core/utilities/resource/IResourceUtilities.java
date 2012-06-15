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
package com.google.dart.tools.core.utilities.resource;

import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.core.model.DartModelException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import java.util.ArrayList;
import java.util.List;

/**
 * The class <code>IResourceUtilities</code> defines utility methods used to work with resources.
 */
public final class IResourceUtilities {

  /**
   * Given a list of file names, resolves them against the given resource and returns a list of file
   * paths
   * 
   * @param resource used to resolve the paths
   * @param fileNames list of file names to resolve
   * @return list of resolved file paths
   * @throws DartModelException
   */
  public static List<String> getResolvedFilePaths(IResource resource, List<String> fileNames)
      throws DartModelException {
    List<String> filePaths = new ArrayList<String>();
    for (String name : fileNames) {
      IFile file = ResourceUtil.getFile(resource.getLocationURI().resolve(name));
      if (file != null) {
        filePaths.add(file.getLocation().toPortableString());
      }
    }
    return filePaths;
  }

}
