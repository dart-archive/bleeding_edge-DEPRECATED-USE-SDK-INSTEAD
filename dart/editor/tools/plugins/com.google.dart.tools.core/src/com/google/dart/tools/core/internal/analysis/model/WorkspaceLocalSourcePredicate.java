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

package com.google.dart.tools.core.internal.analysis.model;

import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.LocalSourcePredicate;
import com.google.dart.engine.source.Source;

import org.eclipse.core.resources.IContainer;

import java.io.File;

/**
 * Implementation of {@link LocalSourcePredicate} for Eclipse {@link IContainer}.
 */
public class WorkspaceLocalSourcePredicate implements LocalSourcePredicate {
  private final String containerPath;

  public WorkspaceLocalSourcePredicate(IContainer container) {
    containerPath = container.getLocation().toFile().getAbsolutePath() + File.separator;
  }

  @Override
  public boolean isLocal(Source source) {
    if (source instanceof FileBasedSource) {
      FileBasedSource fileBasedSource = (FileBasedSource) source;
      String sourcePath = fileBasedSource.getFullName();
      return sourcePath.startsWith(containerPath);
    }
    return false;
  }
}
