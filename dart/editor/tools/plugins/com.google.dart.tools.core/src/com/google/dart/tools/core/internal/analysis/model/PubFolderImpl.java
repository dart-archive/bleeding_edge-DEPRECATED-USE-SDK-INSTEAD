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

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.utilities.io.FileUtilities;
import com.google.dart.tools.core.analysis.model.PubFolder;
import com.google.dart.tools.core.pub.PubspecModel;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;

import java.io.IOException;

/**
 * Represents a project or folder within a project containing a pubspec file
 */
public class PubFolderImpl implements PubFolder {

  private final AnalysisContext context;

  private final IResource pubspecFile;
  private PubspecModel pubspec = new PubspecModel(null);

  public PubFolderImpl(IContainer container, IResource pubspecResource, AnalysisContext context) {
    this.context = context;
    pubspecFile = pubspecResource;
  }

  @Override
  public AnalysisContext getContext() {
    return context;
  }

  @Override
  public PubspecModel getPubspec() throws IOException {
    if (pubspec == null) {
      pubspec = new PubspecModel(FileUtilities.getContents(pubspecFile.getLocation().toFile()));
    }
    return pubspec;
  }
}
