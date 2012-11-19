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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.MultiStatus;

import java.util.ArrayList;

public class FilesOfScopeCalculator implements IResourceProxyVisitor {

  private final TextSearchScope fScope;
  private final MultiStatus fStatus;
  private ArrayList<IResource> fFiles;

  public FilesOfScopeCalculator(TextSearchScope scope, MultiStatus status) {
    fScope = scope;
    fStatus = status;
  }

  public IFile[] process() {
    fFiles = new ArrayList<IResource>();
    try {
      IResource[] roots = fScope.getRoots();
      for (int i = 0; i < roots.length; i++) {
        try {
          IResource resource = roots[i];
          if (resource.isAccessible()) {
            resource.accept(this, 0);
          }
        } catch (CoreException ex) {
          // report and ignore
          fStatus.add(ex.getStatus());
        }
      }
      return fFiles.toArray(new IFile[fFiles.size()]);
    } finally {
      fFiles = null;
    }
  }

  @Override
  public boolean visit(IResourceProxy proxy) {
    boolean inScope = fScope.contains(proxy);

    if (inScope && proxy.getType() == IResource.FILE) {
      fFiles.add(proxy.requestResource());
    }
    return inScope;
  }
}
