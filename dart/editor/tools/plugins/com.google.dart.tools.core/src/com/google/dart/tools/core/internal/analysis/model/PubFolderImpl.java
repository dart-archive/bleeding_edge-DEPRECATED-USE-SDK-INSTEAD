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
import com.google.dart.engine.source.DirectoryBasedSourceContainer;
import com.google.dart.engine.source.ExplicitPackageUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceContainer;
import com.google.dart.engine.source.UriResolver;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.PubFolder;
import com.google.dart.tools.core.pub.PubspecModel;

import static com.google.dart.tools.core.DartCore.PUBSPEC_FILE_NAME;
import static com.google.dart.tools.core.utilities.io.FileUtilities.getContents;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a project or folder within a project containing a pubspec file.
 *
 * @coverage dart.tools.core.model
 */
public class PubFolderImpl extends PubResourceMapImpl implements PubFolder {

  /**
   * The Dart SDK used when constructing the context.
   */
  private final DartSdk sdk;

  /**
   * The pubspec model or {@code null} if it is not yet cached.
   */
  private PubspecModel pubspec;

  /**
   * The package resolver used to resolve package: uris
   */
  private UriResolver pkgResolver;

  public PubFolderImpl(
      IContainer container, AnalysisContext context, DartSdk sdk, UriResolver pkgResolver) {
    super(container, context);
    this.sdk = sdk;
    this.pkgResolver = pkgResolver;
  }

  @Override
  public InvertedSourceContainer getInvertedSourceContainer() {
    return new InvertedSourceContainer(getSourceContainer());
  }

  @Override
  public PubspecModel getPubspec() throws CoreException, IOException {
    if (pubspec == null) {
      IFile file = container.getFile(new Path(PUBSPEC_FILE_NAME));
      Reader reader = new InputStreamReader(file.getContents(), file.getCharset());
      pubspec = new PubspecModel(file, getContents(reader));
      setSelfPackageName(pubspec.getName());
    }
    return pubspec;
  }

  @Override
  public IFile getResource(Source source) {
    // Ensure that the setPackagePath has been set
    if (getSelfPackagePath() == null) {
      IFile pubspecFile = container.getFile(new Path(PUBSPEC_FILE_NAME));
      if (pubspecFile.exists()) {
        try {
          getPubspec();
        } catch (Exception e) {
          DartCore.logError("Failed to read " + pubspecFile, e);
          //$FALL-THROUGH$
        }
      }
    }
    return super.getResource(source);
  }

  @Override
  public DartSdk getSdk() {
    return sdk;
  }

  @Override
  public void invalidatePubspec() throws IOException, CoreException {
    pubspec = null;
  }

  @Override
  public String resolvePathToPackage(String path) {
    if (pkgResolver instanceof ExplicitPackageUriResolver) {
      return ((ExplicitPackageUriResolver) pkgResolver).resolvePathToPackage(path);
    }
    return null;
  }

  private CompositeSourceContainer getSourceContainer() {
    List<SourceContainer> containers = new ArrayList<SourceContainer>();
    containers.add(new DirectoryBasedSourceContainer(container.getLocation().toFile()));
    IFolder packagesFolder = container.getFolder(new Path(DartCore.PACKAGES_DIRECTORY_NAME));

    if (packagesFolder != null) {
      File packagesDir = packagesFolder.getLocation().toFile();
      File[] packages = packagesDir.listFiles(new FileFilter() {
        @Override
        public boolean accept(File pathname) {
          if (pathname.isDirectory()) {
            return true;
          }
          return false;
        }
      });
      if (packages != null) {
        for (File file : packages) {
          try {
            containers.add(new DirectoryBasedSourceContainer(file.getCanonicalFile()));
          } catch (IOException e) {
            DartCore.logError(e);
          }
        }
      }
    }
    return new CompositeSourceContainer(containers);
  }
}
