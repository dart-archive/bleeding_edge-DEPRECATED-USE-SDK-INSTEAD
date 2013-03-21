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
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.tools.core.analysis.model.PubFolder;
import com.google.dart.tools.core.pub.PubspecModel;

import static com.google.dart.tools.core.DartCore.PUBSPEC_FILE_NAME;
import static com.google.dart.tools.core.utilities.io.FileUtilities.getContents;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * Represents a project or folder within a project containing a pubspec file
 */
public class PubFolderImpl implements PubFolder {

  /**
   * The container of the pubspec file (not {@code null})
   */
  private final IContainer container;

  /**
   * The analysis context used when analyzing sources contained in the receiver
   */
  private final AnalysisContext context;

  /**
   * The Dart SDK used when constructing the context.
   */
  private final DartSdk sdk;

  /**
   * The pubspec model or {@code null} if it is not yet cached.
   */
  private PubspecModel pubspec;

  public PubFolderImpl(IContainer container, AnalysisContext context, DartSdk sdk) {
    this.container = container;
    this.context = context;
    this.sdk = sdk;
  }

  @Override
  public AnalysisContext getContext() {
    return context;
  }

  @Override
  public PubspecModel getPubspec() throws CoreException, IOException {
    if (pubspec == null) {
      IFile file = container.getFile(new Path(PUBSPEC_FILE_NAME));
      Reader reader = new InputStreamReader(file.getContents(), file.getCharset());
      pubspec = new PubspecModel(getContents(reader));
    }
    return pubspec;
  }

  @Override
  public IContainer getResource() {
    return container;
  }

  @Override
  public DartSdk getSdk() {
    return sdk;
  }

  @Override
  public void invalidatePubspec() throws IOException, CoreException {
    pubspec = null;
  }
}
