/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui;

import com.google.dart.tools.core.internal.model.DartLibraryImpl;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is used by the {@link StandardDartElementContentProvider} as the "Imported Libraries"
 * element. This children of this element is an array of {@link ImportedDartLibrary}s.
 */
public class ImportedDartLibraryContainer {

  public final static String IMPORTED_LIBRARIES_LABEL = "Imported Libraries";

  /**
   * Return the URI for the file defining the given library.
   * 
   * @param library the library whose URI is to be returned
   * @return the URI for the file defining the given library
   */
  public static URI getUri(DartLibrary library) {
    return ((DartLibraryImpl) library).getLibrarySourceFile().getUri();
  }

  /**
   * The parent of this imported library container. The value can either be a {@link DartLibrary} or
   * a {@link ImportedDartLibrary}.
   */
  private Object parent;

  private final DartLibrary dartLibrary;

  private ImportedDartLibrary[] importedDartLibs = null;

  public ImportedDartLibraryContainer(Object parent, DartLibrary dartLibrary) {
    this.parent = parent;
    this.dartLibrary = dartLibrary;
  }

  @Override
  public boolean equals(Object object) {
    if (object instanceof ImportedDartLibraryContainer) {
      ImportedDartLibraryContainer container = (ImportedDartLibraryContainer) object;
      return parent.equals(container.parent)
          && getUri(dartLibrary).equals(getUri(container.dartLibrary));
    }
    return false;
  }

  /**
   * If importedDartLibs is <code>null</code> the compute the set of imported Dart libraries wrapped
   * in an {@link ImportedDartLibrary}, if non-<code>null</code> just return pre-computed array.
   * 
   * @return the imported Dart libraries array returned each wrapped in to an
   *         {@link ImportedDartLibrary}
   */
  public ImportedDartLibrary[] getDartLibraries() {
    if (importedDartLibs == null) {
      List<DartLibrary> libraryList = new ArrayList<DartLibrary>();
      try {
        DartLibrary[] importedLibraries = dartLibrary.getImportedLibraries();
        for (DartLibrary library : importedLibraries) {
          if (!isNestedUnder(library)) {
            libraryList.add(library);
          }
        }
      } catch (DartModelException e) {
        importedDartLibs = ImportedDartLibrary.EMPTY_ARRAY;
        DartToolsPlugin.log(e);
        return importedDartLibs;
      }
      // Try to reference the implicit core library, if it cannot be found, or if we decide to not
      // to include the core library in the return of this method, then coreLibrary will be set to
      // null, see below for these cases.
      DartLibrary coreLibrary = null;
      URI coreLibUri = null;
      try {
        coreLibrary = dartLibrary.getDartModel().getCoreLibrary();
        coreLibUri = getUri(coreLibrary);
      } catch (DartModelException e) {
        DartToolsPlugin.log(e);
      }
      // if this ImportedDartLibrariesContainer's dartLibrary is the coreLibSource:
      if (isNestedUnder(coreLibrary)) {
        coreLibrary = null;
      }
      // if corelib is an explicit child, then also don't include it implicitly
      // loop through element in dartLibs
      if (coreLibrary != null && coreLibUri != null) {
        for (DartLibrary explicitDartLibImport : libraryList) {
          if (coreLibUri.equals(getUri(explicitDartLibImport))) {
            coreLibrary = null;
            break;
          }
        }
      }
      //
      // If the core library is implicitly imported then add it to the list of children.
      //
      if (coreLibrary != null) {
        libraryList.add(0, coreLibrary);
      }
      //
      // Then wrap each child as an imported library.
      //
      int count = libraryList.size();
      importedDartLibs = new ImportedDartLibrary[count];
      for (int i = 0; i < count; i++) {
        importedDartLibs[i] = new ImportedDartLibrary(libraryList.get(i), this);
      }
    }
    return importedDartLibs;
  }

  public DartLibrary getDartLibrary() {
    return dartLibrary;
  }

  /**
   * The name of this element for the label provider, currently this just returns
   * {@link #IMPORTED_LIBRARIES_LABEL}.
   */
  public String getName() {
    return IMPORTED_LIBRARIES_LABEL;
  }

  /**
   * Return the library that imports the libraries contained in this container.
   * 
   * @return the library that imports the libraries contained in this container
   */
  public Object getParent() {
    return parent;
  }

  /**
   * Always return <code>true</code> as all libraries always import at least one library.
   * 
   * @return always return <code>true</code>
   */
  public boolean hasChildren() {
    return true;
  }

  @Override
  public int hashCode() {
    return parent.hashCode() << 7 | getUri(dartLibrary).hashCode();
  }

  /**
   * Returns {@link #getName()}, this is currently only used by the {@link DartElementComparator}.
   */
  @Override
  public String toString() {
    return getName();
  }

  /**
   * Return <code>true</code> if the given library is the same as either this library or one of the
   * parents of this library.
   * 
   * @param library the library being tested
   * @return <code>true</code> if the given library is a parent of this library
   */
  private boolean isNestedUnder(DartLibrary library) {
    if (library == null) {
      return false;
    }
    URI libraryUri = getUri(library);
    Object parent = getParent();
    while (parent != null) {
      if (parent instanceof DartLibrary) {
        return libraryUri.equals(getUri((DartLibrary) parent));
      } else if (parent instanceof ImportedDartLibrary) {
        ImportedDartLibrary parentLibrary = (ImportedDartLibrary) parent;
        if (libraryUri.equals(getUri(parentLibrary.getDartLibrary()))) {
          return true;
        }
        // We skip over the Imported Libraries container because it would produce the same result as
        // it's parent.
        parent = parentLibrary.getParent().getParent();
      } else {
        // This should never happen, but if it does we don't know how to proceed anyway, so the safe
        // answer is to return false.
        return false;
      }
    }
    return false;
  }
}
