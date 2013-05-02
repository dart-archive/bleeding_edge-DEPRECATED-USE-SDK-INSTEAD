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
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.DartCore;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import java.io.File;
import java.io.IOException;

public class PubResourceMapImpl extends SimpleResourceMapImpl {

  /**
   * The root "packages" folder (not {@code null}).
   */
  private IFolder packagesFolder;

  /**
   * The root "packages" location on disk (not {@code null}).
   */
  private IPath packagesLocation;

  /**
   * The root "lib" folder (not {@code null}).
   */
  private final IContainer libFolder;

  /**
   * The path to the root "lib" folder (not {@code null}).
   */
  private final String libPath;

  /**
   * The path of the package that maps to the "lib" folder.
   */
  private String selfPackagePath;

  public PubResourceMapImpl(IContainer container, AnalysisContext context) {
    super(container, context);
    packagesFolder = container.getFolder(new Path(DartCore.PACKAGES_DIRECTORY_NAME));
    packagesLocation = container.getLocation().append(DartCore.PACKAGES_DIRECTORY_NAME);
    libFolder = container.getFolder(new Path(DartCore.LIB_DIRECTORY_NAME));
    libPath = libFolder.getLocation().toOSString() + File.separator;
  }

  @Override
  public IFile getResource(Source source) {
    String sourcePath = source.getFullName();
    // may be self-reference
    if (sourcePath.startsWith(libPath)) {
      return super.getResource(source);
    }
    if (selfPackagePath != null && sourcePath.startsWith(selfPackagePath)) {
      String relPath = sourcePath.substring(selfPackagePath.length());
      return libFolder.getFile(new Path(relPath));
    }
    // analyze installed packages from "packages" folder
    String[] pkgNames = packagesLocation.toFile().list();
    if (pkgNames != null) {
      for (String pkgName : pkgNames) {
        File pkgDir = packagesLocation.append(pkgName).toFile();
        String pkgPath;
        try {
          pkgPath = pkgDir.getCanonicalPath();
        } catch (IOException e) {
          DartCore.logError("Failed to determine canonical location of " + pkgDir, e);
          continue;
        }
        pkgPath += File.separator;
        if (sourcePath.startsWith(pkgPath)) {
          String relPath = sourcePath.substring(pkgPath.length());
          return packagesFolder.getFile(new Path(pkgName).append(relPath));
        }
      }
    }
    return super.getResource(source);
  }

  @Override
  public Source getSource(IFile resource) {
    if (resource == null) {
      return null;
    }
    IPath fileLocation = resource.getLocation();
    if (fileLocation == null) {
      return null;
    }
    int index = packagesLocation.segmentCount();
    File file;
    if (fileLocation.segmentCount() > index && packagesLocation.isPrefixOf(fileLocation)) {
      File pkgDir = fileLocation.uptoSegment(index + 1).toFile();
      try {
        pkgDir = pkgDir.getCanonicalFile();
      } catch (IOException e) {
        DartCore.logError("Failed to determine canonical location of " + pkgDir, e);
        return null;
      }
      if (fileLocation.segmentCount() > index + 1) {
        file = new File(pkgDir, fileLocation.removeFirstSegments(index + 1).toOSString());
      } else {
        file = pkgDir;
      }
    } else {
      file = fileLocation.toFile();
    }
    return new FileBasedSource(contentCache, file);
  }

  /**
   * Set the name of the package that maps to the "lib" directory.
   * 
   * @param name the package name or {@code null} if none
   */
  public void setSelfPackageName(String name) {
    if (name != null && name.length() > 0) {
      selfPackagePath = packagesLocation.append(name).toOSString();
    } else {
      selfPackagePath = null;
    }
  }

  /**
   * Answer the path to the package that maps to the "lib" directory.
   * 
   * @return the path or {@code null} if none
   */
  protected String getSelfPackagePath() {
    return selfPackagePath;
  }
}
