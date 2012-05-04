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
package com.google.dart.tools.core.internal.model;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.cache.ElementCache;
import com.google.dart.tools.core.internal.model.info.CompilationUnitInfo;
import com.google.dart.tools.core.internal.model.info.DartElementInfo;
import com.google.dart.tools.core.internal.model.info.DartFieldInfo;
import com.google.dart.tools.core.internal.model.info.DartFunctionInfo;
import com.google.dart.tools.core.internal.model.info.DartFunctionTypeAliasInfo;
import com.google.dart.tools.core.internal.model.info.DartImportInfo;
import com.google.dart.tools.core.internal.model.info.DartLibraryFolderInfo;
import com.google.dart.tools.core.internal.model.info.DartLibraryInfo;
import com.google.dart.tools.core.internal.model.info.DartMethodInfo;
import com.google.dart.tools.core.internal.model.info.DartModelInfo;
import com.google.dart.tools.core.internal.model.info.DartProjectInfo;
import com.google.dart.tools.core.internal.model.info.DartResourceInfo;
import com.google.dart.tools.core.internal.model.info.DartTypeInfo;
import com.google.dart.tools.core.internal.model.info.DartVariableInfo;
import com.google.dart.tools.core.internal.model.info.HTMLFileInfo;
import com.google.dart.tools.core.internal.model.info.OpenableElementInfo;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.OpenableElement;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Instances of the class <code>DartModelCache</code> implement a cache mapping Dart model elements
 * to the corresponding information objects.
 */
public class DartModelCache {
  /**
   * A flag indicating whether tracing information should be logged.
   */
  public static boolean VERBOSE = false;

  /**
   * The default size for the project cache. This was computed based on an average of 25552 bytes
   * per project.
   */
  public static final int DEFAULT_PROJECT_SIZE = 5;

  /**
   * The default size for the library cache.
   */
  public static final int DEFAULT_LIBRARY_SIZE = 50;

  /**
   * The default size for the openable cache. This was computed based on an average of 6629 bytes
   * per openable (includes children) -> maximum size : 662900*BASE_VALUE bytes
   */
  public static final int DEFAULT_OPENABLE_SIZE = 250;

  /**
   * The default size for the children cache. This allows for an average of 20 children per
   * openable.
   */
  public static final int DEFAULT_CHILDREN_SIZE = DEFAULT_OPENABLE_SIZE * 20;

  public static final String RATIO_PROPERTY = "com.google.dart.tools.core.dartmodelcache.ratio"; //$NON-NLS-1$

  // public static final Object NON_EXISTING_JAR_TYPE_INFO = new Object();

  /**
   * The memory ratio that should be applied to the above constants.
   */
  private double memoryRatio = -1;

  /**
   * Active Dart Model Info
   */
  private DartModelInfo modelInfo;

  /**
   * Cache of open projects.
   */
  private HashMap<DartElement, DartElementInfo> projectCache;

  /**
   * Cache of open libraries.
   */
  private ElementCache libraryCache;

  /**
   * Cache of open compilation unit and class files
   */
  private ElementCache openableCache;

  /**
   * Cache of open children of openable Dart Model elements
   */
  private Map<DartElement, DartElementInfo> childrenCache;

  /**
   * Cache of open binary type (inside a jar) that have a non-open parent
   */
  // protected LRUCache jarTypeCache;

  public DartModelCache() {
    // set the size of the caches in function of the maximum amount of memory
    // available
    double ratio = getMemoryRatio();
    // adjust the size of the openable cache in function of the RATIO_PROPERTY
    // property
    double openableRatio = getOpenableRatio();
    // NB: Don't use a LRUCache for projects as they are constantly reopened (e.g. during delta
    // processing)
    projectCache = new HashMap<DartElement, DartElementInfo>(DEFAULT_PROJECT_SIZE);
    // if (VERBOSE) {
    //    openableCache = new VerboseElementCache((int) (DEFAULT_OPENABLE_SIZE * ratio * openableRatio), "Openable cache"); //$NON-NLS-1$
    // } else {
    libraryCache = new ElementCache((int) (DEFAULT_LIBRARY_SIZE * ratio));
    openableCache = new ElementCache((int) (DEFAULT_OPENABLE_SIZE * ratio * openableRatio));
    // }
    childrenCache = new HashMap<DartElement, DartElementInfo>(
        (int) (DEFAULT_CHILDREN_SIZE * ratio * openableRatio));
    // resetJarTypeCache();
  }

  /*
   * Returns the existing element that is equal to the given element if present in the cache.
   * Returns the given element otherwise.
   */
  public DartElement getExistingElement(DartElement element) {
    switch (element.getElementType()) {
      case DartElement.DART_MODEL:
        return element;
      case DartElement.DART_PROJECT:
        // projectCache is a Hashtable and Hashtables don't
        // support getKey(...)
        return element;
      case DartElement.LIBRARY:
        return libraryCache.getKey((DartLibraryImpl) element);
      case DartElement.COMPILATION_UNIT:
        return openableCache.getKey((OpenableElement) element);
      case DartElement.TYPE:
        // jarTypeCache or childrenCache are Hashtables and
        // Hashtables don't support getKey(...)
        return element;
      default:
        // childrenCache is a Hashtable and Hashtables don't
        // support getKey(...)
        return element;
    }
  }

  /**
   * Returns the info for the element.
   */
  public DartElementInfo getInfo(DartElement element) {
    switch (element.getElementType()) {
      case DartElement.DART_MODEL:
        return modelInfo;
      case DartElement.DART_PROJECT:
        return projectCache.get(element);
      case DartElement.LIBRARY:
        return libraryCache.get((DartLibraryImpl) element);
      case DartElement.COMPILATION_UNIT:
        return openableCache.get((OpenableElement) element);
      case DartElement.TYPE:
        // Object result = jarTypeCache.get(element);
        // if (result != null)
        // return result;
        // else
        return childrenCache.get(element);
      default:
        return childrenCache.get(element);
    }
  }

  /**
   * Answer all library elements with cached information and existing in the specified directory
   * hierarchy.
   * 
   * @param prefixUri the URI of the directory containing the libraries to be returned.
   * @return an array of library elements (not <code>null</code>, contains no <code>null</code>s)
   */
  public DartLibraryImpl[] getLibrariesWithPrefix(URI prefixUri) {
    String prefix = prefixUri.toString();
    Iterator<OpenableElement> iter = libraryCache.keySet().iterator();
    Collection<DartLibraryImpl> result = null;
    while (iter.hasNext()) {
      DartLibraryImpl lib = (DartLibraryImpl) iter.next();
      if (lib.getElementName().startsWith(prefix)) {
        if (result == null) {
          result = new ArrayList<DartLibraryImpl>();
        }
        result.add(lib);
      }
    }
    if (result == null) {
      return DartLibraryImpl.EMPTY_LIBRARY_ARRAY;
    }
    return result.toArray(new DartLibraryImpl[result.size()]);
  }

  @Override
  public String toString() {
    return toStringFillingRation(""); //$NON-NLS-1$
  }

  public String toStringFillingRation(String prefix) {
    StringBuffer buffer = new StringBuffer();
    buffer.append(prefix);
    buffer.append("Project cache: "); //$NON-NLS-1$
    buffer.append(projectCache.size());
    buffer.append(" projects\n"); //$NON-NLS-1$
    buffer.append(prefix);
    buffer.append(libraryCache.toStringFillingRation("Library cache")); //$NON-NLS-1$
    buffer.append('\n');
    buffer.append(prefix);
    buffer.append(openableCache.toStringFillingRation("Openable cache")); //$NON-NLS-1$
    buffer.append('\n');
    // buffer.append(prefix);
    //    buffer.append(jarTypeCache.toStringFillingRation("Jar type cache")); //$NON-NLS-1$
    // buffer.append('\n');
    return buffer.toString();
  }

  protected double getMemoryRatio() {
    if ((int) memoryRatio == -1) {
      long maxMemory = Runtime.getRuntime().maxMemory();
      // if max memory is infinite, set the ratio to 4d which corresponds to the
      // 256MB that Eclipse defaults to
      // (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=111299)
      memoryRatio = maxMemory == Long.MAX_VALUE ? 4d : (double) maxMemory / (64 * 0x100000); // 64MB is the base memory
                                                                                             // for most JVM
    }
    return memoryRatio;
  }

  /**
   * Returns the info for this element without disturbing the cache ordering.
   */
  protected DartElementInfo peekAtInfo(DartElement element) {
    switch (element.getElementType()) {
      case DartElement.DART_MODEL:
        return modelInfo;
      case DartElement.DART_PROJECT:
        return projectCache.get(element);
      case DartElement.LIBRARY:
        return libraryCache.peek((DartLibraryImpl) element);
      case DartElement.COMPILATION_UNIT:
        return openableCache.peek((OpenableElement) element);
      case DartElement.TYPE:
        // Object result = jarTypeCache.peek(element);
        // if (result != null)
        // return result;
        // else
        return childrenCache.get(element);
      default:
        return childrenCache.get(element);
    }
  }

  /**
   * Remember the info for the element.
   */
  protected void putInfo(DartElement element, DartElementInfo info) {
//    assertConsistent(element, info);
    switch (element.getElementType()) {
      case DartElement.DART_MODEL:
        modelInfo = (DartModelInfo) info;
        break;
      case DartElement.DART_PROJECT:
        projectCache.put(element, info);
        libraryCache.ensureSpaceLimit(info, element);
        break;
      case DartElement.LIBRARY:
        libraryCache.put((DartLibraryImpl) element, (DartLibraryInfo) info);
        openableCache.ensureSpaceLimit(info, element);
        break;
      case DartElement.COMPILATION_UNIT:
        openableCache.put((OpenableElement) element, (OpenableElementInfo) info);
        break;
      default:
        childrenCache.put(element, info);
    }
  }

  /**
   * Removes the info of the element from the cache.
   */
  protected void removeInfo(DartElementImpl element) {
    switch (element.getElementType()) {
      case DartElement.DART_MODEL:
        modelInfo = null;
        break;
      case DartElement.DART_PROJECT:
        projectCache.remove(element);
        libraryCache.resetSpaceLimit((int) (DEFAULT_LIBRARY_SIZE * getMemoryRatio()), element);
        break;
      case DartElement.LIBRARY:
        libraryCache.remove((DartLibraryImpl) element);
        openableCache.resetSpaceLimit(
            (int) (DEFAULT_OPENABLE_SIZE * getMemoryRatio() * getOpenableRatio()),
            element);
        break;
      case DartElement.COMPILATION_UNIT:
        openableCache.remove((OpenableElement) element);
        break;
      default:
        childrenCache.remove(element);
    }
  }

  /**
   * Write to the log if the type of info being associated with the given element is not consistent.
   * 
   * @param element the element with which the info is being associated
   * @param info the info being associated with the element
   */
  private void assertConsistent(DartElement element, DartElementInfo info) {
    if (element instanceof CompilationUnitImpl) {
      if (info instanceof CompilationUnitInfo) {
        return;
      }
    } else if (element instanceof DartFieldImpl) {
      if (info instanceof DartFieldInfo) {
        return;
      }
    } else if (element instanceof DartFunctionImpl) {
      if (info instanceof DartFunctionInfo) {
        return;
      }
    } else if (element instanceof DartFunctionTypeAliasImpl) {
      if (info instanceof DartFunctionTypeAliasInfo) {
        return;
      }
    } else if (element instanceof DartImportImpl) {
      if (info instanceof DartImportInfo) {
        return;
      }
    } else if (element instanceof DartLibraryFolderImpl) {
      if (info instanceof DartLibraryFolderInfo) {
        return;
      }
    } else if (element instanceof DartLibraryImpl) {
      if (info instanceof DartLibraryInfo) {
        return;
      }
    } else if (element instanceof DartMethodImpl) {
      if (info instanceof DartMethodInfo) {
        return;
      }
    } else if (element instanceof DartModelImpl) {
      if (info instanceof DartModelInfo) {
        return;
      }
    } else if (element instanceof DartProjectImpl) {
      if (info instanceof DartProjectInfo) {
        return;
      }
    } else if (element instanceof DartResourceImpl) {
      if (info instanceof DartResourceInfo) {
        return;
      }
    } else if (element instanceof DartTypeImpl) {
      if (info instanceof DartTypeInfo) {
        return;
      }
    } else if (element instanceof DartVariableImpl) {
      if (info instanceof DartVariableInfo) {
        return;
      }
    } else if (element instanceof HTMLFileImpl) {
      if (info instanceof HTMLFileInfo) {
        return;
      }
    } else {
      // We are not yet explicitly checking this type.
      return;
    }
    DartCore.logError("Invalid type of info (" + info.getClass().getName() + ") for element ("
        + element.getClass().getName() + ")", new Exception());
  }

  // protected void resetJarTypeCache() {
  // jarTypeCache = new LRUCache(
  // (int) (DEFAULT_OPENABLE_SIZE * getMemoryRatio()));
  // }

  private double getOpenableRatio() {
    String property = System.getProperty(RATIO_PROPERTY);
    if (property != null) {
      try {
        return Double.parseDouble(property);
      } catch (NumberFormatException exception) {
        // ignore
        DartCore.logError(
            "Could not parse value for " + RATIO_PROPERTY + ": " + property, exception); //$NON-NLS-1$ //$NON-NLS-2$
      }
    }
    return 1.0;
  }
}
