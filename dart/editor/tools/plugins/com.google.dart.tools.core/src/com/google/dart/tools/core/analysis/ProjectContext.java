/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.tools.core.analysis;

import com.google.common.collect.Lists;
import com.google.dart.compiler.PackageLibraryManager;

import java.io.File;

/**
 * A context for analyzing applications whose project has a preference setting for package root.
 * Analysis of libraries directly or indirectly referenced by this context's applications are cached
 * in this context.
 */
public class ProjectContext extends PackageContext {

  ProjectContext(AnalysisServer server, File applicationDirectory) {
    super(server, new PackageLibraryManager(), applicationDirectory);
    getLibraryManager().setPackageRoots(Lists.newArrayList(applicationDirectory));
  }
}
