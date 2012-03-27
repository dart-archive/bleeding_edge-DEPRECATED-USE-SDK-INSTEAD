package com.google.dart.tools.internal.corext.refactoring.util;

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.OpenableElement;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import java.util.ArrayList;
import java.util.List;

public class ResourceUtil {

  public static IFile getFile(CompilationUnit cu) {
    IResource resource = cu.getResource();
    if (resource != null && resource.getType() == IResource.FILE) {
      return (IFile) resource;
    } else {
      return null;
    }
  }

  public static IFile[] getFiles(CompilationUnit[] cus) {
    List<IResource> files = new ArrayList<IResource>(cus.length);
    for (int i = 0; i < cus.length; i++) {
      IResource resource = cus[i].getResource();
      if (resource != null && resource.getType() == IResource.FILE) {
        files.add(resource);
      }
    }
    return files.toArray(new IFile[files.size()]);
  }

  public static IResource getResource(Object o) {
    if (o instanceof IResource) {
      return (IResource) o;
    }
    if (o instanceof DartElement) {
      return getResource((DartElement) o);
    }
    return null;
  }

  //----- other ------------------------------

  private static IResource getResource(DartElement element) {
    if (element.getElementType() == DartElement.COMPILATION_UNIT) {
      return ((CompilationUnit) element).getResource();
    } else if (element instanceof OpenableElement) {
      return element.getResource();
    } else {
      return null;
    }
  }

  private ResourceUtil() {
  }
}
