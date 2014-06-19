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
package com.google.dart.engine.source;

import com.google.dart.engine.AnalysisEngine;

import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 * Instances of the class {@code PackageUriResolver} resolve {@code package} URI's in the context of
 * an application.
 * <p>
 * For the purposes of sharing analysis, the path to each package under the "packages" directory
 * should be canonicalized, but to preserve relative links within a package, the remainder of the
 * path from the package directory to the leaf should not.
 * 
 * @coverage dart.engine.source
 */
public class PackageUriResolver extends UriResolver {

  /**
   * The package directories that {@code package} URI's are assumed to be relative to.
   */
  private File[] packagesDirectories;

  /**
   * The name of the {@code package} scheme.
   */
  public static final String PACKAGE_SCHEME = "package";

  /**
   * Log exceptions thrown with the message "Required key not available" only once.
   */
  private static boolean CanLogRequiredKeyIoException = true;

  /**
   * Return {@code true} if the given URI is a {@code package} URI.
   * 
   * @param uri the URI being tested
   * @return {@code true} if the given URI is a {@code package} URI
   */
  public static boolean isPackageUri(URI uri) {
    return PACKAGE_SCHEME.equals(uri.getScheme());
  }

  /**
   * Initialize a newly created resolver to resolve {@code package} URI's relative to the given
   * package directories.
   * 
   * @param packagesDirectories the package directories that {@code package} URI's are assumed to be
   *          relative to
   */
  public PackageUriResolver(File... packagesDirectories) {
    if (packagesDirectories.length < 1) {
      throw new IllegalArgumentException("At least one package directory must be provided");
    }
    this.packagesDirectories = packagesDirectories;
  }

  @Override
  public Source fromEncoding(UriKind kind, URI uri) {
    if (kind == UriKind.PACKAGE_URI) {
      return new FileBasedSource(new File(uri), kind);
    }
    return null;
  }

  @Override
  public Source resolveAbsolute(URI uri) {
    if (!isPackageUri(uri)) {
      return null;
    }
    String path = uri.getPath();
    if (path == null) {
      path = uri.getSchemeSpecificPart();
      if (path == null) {
        return null;
      }
    }
    String pkgName;
    String relPath;
    int index = path.indexOf('/');
    if (index == -1) {
      // No slash
      pkgName = path;
      relPath = "";
    } else if (index == 0) {
      // Leading slash is invalid
      return null;
    } else {
      // <pkgName>/<relPath>
      pkgName = path.substring(0, index);
      relPath = path.substring(index + 1);
    }
    for (File packagesDirectory : packagesDirectories) {
      File resolvedFile = new File(packagesDirectory, path);
      if (resolvedFile.exists()) {
        File canonicalFile = getCanonicalFile(packagesDirectory, pkgName, relPath);
        UriKind uriKind = isSelfReference(packagesDirectory, canonicalFile) ? UriKind.FILE_URI
            : UriKind.PACKAGE_URI;
        return new FileBasedSource(canonicalFile, uriKind);
      }
    }
    return new FileBasedSource(
        getCanonicalFile(packagesDirectories[0], pkgName, relPath),
        UriKind.PACKAGE_URI);
  }

  @Override
  public URI restoreAbsolute(Source source) {
    if (source instanceof FileBasedSource) {
      String sourcePath = ((FileBasedSource) source).getFile().getPath();
      for (File packagesDirectory : packagesDirectories) {
        File[] pkgFolders = packagesDirectory.listFiles();
        if (pkgFolders != null) {
          for (File pkgFolder : pkgFolders) {
            try {
              String pkgCanonicalPath = pkgFolder.getCanonicalPath();
              if (sourcePath.startsWith(pkgCanonicalPath)) {
                String relPath = sourcePath.substring(pkgCanonicalPath.length());
                return URI.create(PACKAGE_SCHEME + ":" + pkgFolder.getName() + relPath);
              }
            } catch (Exception e) {
            }
          }
        }
      }
    }
    return null;
  }

  /**
   * Answer the canonical file for the specified package.
   * 
   * @param packagesDirectory the "packages" directory (not {@code null})
   * @param pkgName the package name (not {@code null}, not empty)
   * @param relPath the path relative to the package directory (not {@code null}, no leading slash,
   *          but may be empty string)
   * @return the file (not {@code null})
   */
  protected File getCanonicalFile(File packagesDirectory, String pkgName, String relPath) {
    File pkgDir = new File(packagesDirectory, pkgName);
    try {
      pkgDir = pkgDir.getCanonicalFile();
    } catch (IOException e) {
      if (!e.getMessage().contains("Required key not available")) {
        AnalysisEngine.getInstance().getLogger().logError("Canonical failed: " + pkgDir, e);
      } else if (CanLogRequiredKeyIoException) {
        CanLogRequiredKeyIoException = false;
        AnalysisEngine.getInstance().getLogger().logError("Canonical failed: " + pkgDir, e);
      }
    }
    return new File(pkgDir, relPath.replace('/', File.separatorChar));
  }

  /**
   * @return {@code true} if "file" was found in "packagesDir", and it is part of the "lib" folder
   *         of the application that contains in this "packagesDir".
   */
  private boolean isSelfReference(File packagesDir, File file) {
    File rootDir = packagesDir.getParentFile();
    if (rootDir == null) {
      return false;
    }
    String rootPath = rootDir.getAbsolutePath();
    String filePath = file.getAbsolutePath();
    return filePath.startsWith(rootPath + "/lib");
  }
}
