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
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @coverage dart.tools.core.model
 */
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
   * The path of the package that maps to the "lib" folder.
   */
  private String selfPackagePath;

  /**
   * The canonical path of the container or {@code null} if the path could not be determined
   */
  private String canonicalContainerPath;

  public PubResourceMapImpl(IContainer container, AnalysisContext context, String contextId) {
    super(container, context, contextId);
    packagesFolder = container.getFolder(new Path(DartCore.PACKAGES_DIRECTORY_NAME));
    packagesLocation = container.getLocation().append(DartCore.PACKAGES_DIRECTORY_NAME);
    try {
      canonicalContainerPath = getResource().getLocation().toFile().getCanonicalPath();
    } catch (IOException e) {
      DartCore.logError("Failed to determine canonical location of " + getResource(), e);
    }
  }

  @Override
  public IFile getResource(Source source) {
    String sourcePath = source.getFullName();
    // analyze installed packages from "packages" folder
    String[] pkgNames = packagesLocation.toFile().list();
    if (pkgNames != null && canonicalContainerPath != null) {
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
          if (pkgPath.startsWith(canonicalContainerPath)) {
            return getResource().getFile(
                new Path(pkgPath.substring(canonicalContainerPath.length())).append(relPath));
          } else {
            return packagesFolder.getFile(new Path(pkgName).append(relPath));
          }
        } else {
          if (sourcePath.length() > packagesLocation.toString().length()) {
            String relPath = sourcePath.substring(packagesLocation.toString().length());
            String pkgNamePath = "/" + pkgName + "/";
            if (pkgPath.startsWith(canonicalContainerPath) && relPath.startsWith(pkgNamePath)) {
              return getResource().getFile(
                  new Path(pkgPath.substring(canonicalContainerPath.length())).append(relPath.substring(pkgNamePath.length())));
            }
          }
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
    String fileName = resource.getName();
    if (!DartCore.isDartLikeFileName(fileName) && !DartCore.isHtmlLikeFileName(fileName)) {
      return null;
    }
    IPath fileLocation = resource.getLocation();
    if (fileLocation == null) {
      return null;
    }
    int index = packagesLocation.segmentCount();
    URI uri;
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
        IPath fileRelPath = fileLocation.removeFirstSegments(index + 1).setDevice(null);
        try {
          uri = new URI("package:/" + fileLocation.segment(index) + "/"
              + fileRelPath.toPortableString());
        } catch (URISyntaxException exception) {
          return null;
        }
        file = new File(pkgDir, fileRelPath.toOSString());
      } else {
        try {
          uri = new URI("package:/" + fileLocation.segment(index));
        } catch (URISyntaxException exception) {
          return null;
        }
        file = pkgDir;
      }
    } else {
      file = fileLocation.toFile();
      uri = file.toURI();
    }
    return new FileBasedSource(uri, file);
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
