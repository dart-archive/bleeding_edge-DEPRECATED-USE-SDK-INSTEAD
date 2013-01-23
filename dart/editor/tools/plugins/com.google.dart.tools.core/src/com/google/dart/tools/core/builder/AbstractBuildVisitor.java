/*
 * Copyright 2013 Dart project authors.
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
package com.google.dart.tools.core.builder;

import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * An abstract implementation of a {@link BuildVisitor}.
 */
public abstract class AbstractBuildVisitor implements BuildVisitor {

  @Override
  public boolean visit(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
    return false;
  }

  @Override
  public boolean visit(IResourceProxy proxy, IProgressMonitor monitor) throws CoreException {
    return false;
  }

}
