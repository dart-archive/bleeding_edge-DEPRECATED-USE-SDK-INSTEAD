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
  private final String libPath;

  public PubResourceMapImpl(IContainer container, AnalysisContext context) {
    super(container, context);
    packagesFolder = container.getFolder(new Path(DartCore.PACKAGES_DIRECTORY_NAME));
    packagesLocation = container.getLocation().append(DartCore.PACKAGES_DIRECTORY_NAME);
    libPath = container.getLocation().append(DartCore.LIB_DIRECTORY_NAME).toOSString();
  }

  @Override
  public IFile getResource(Source source) {
    String sourcePath = source.getFullName();
    // may be self-reference
    if (sourcePath.startsWith(libPath)) {
      return super.getResource(source);
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
}
