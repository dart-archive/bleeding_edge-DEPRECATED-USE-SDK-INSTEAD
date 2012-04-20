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
package com.google.dart.tools.core.internal.index.util;

import com.google.dart.tools.core.index.Resource;
import com.google.dart.tools.core.internal.model.CompilationUnitImpl;
import com.google.dart.tools.core.internal.model.DartModelStatusImpl;
import com.google.dart.tools.core.internal.model.ExternalCompilationUnitImpl;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.CompilationUnitElement;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartModelStatusConstants;

import org.eclipse.core.resources.IResource;

import java.net.URI;

/**
 * The class <code>ResourceFactory</code> defines utility methods used to create {@link Resource
 * resources}.
 */
public final class ResourceFactory {
  /**
   * The separator character used to compose identifiers.
   */
  public static char SEPARATOR_CHAR = '^';

  /**
   * Compose the given URI's into a resource id appropriate for the resource with the given URI.
   * 
   * @param libraryUri the URI of the library containing the resource whose id is being computed
   * @param resourceUri the URI of the resource whose id is being computed
   * @return the resource id appropriate for the resource
   */
  public static String composeResourceId(String libraryUri, String resourceUri) {
    StringBuilder builder = new StringBuilder();
    escape(builder, libraryUri);
    builder.append(SEPARATOR_CHAR);
    escape(builder, resourceUri);
    return builder.toString();
  }

  /**
   * Append the escaped version of the given id to the given builder.
   * 
   * @param builder the builder to which the escaped form of the id is to be appended
   * @param id the id to be appended to the builder
   */
  public static void escape(StringBuilder builder, String id) {
    if (id.indexOf(SEPARATOR_CHAR) >= 0) {
      int length = id.length();
      for (int i = 0; i < length; i++) {
        char currentChar = id.charAt(i);
        if (currentChar == SEPARATOR_CHAR) {
          builder.append(SEPARATOR_CHAR);
          builder.append(SEPARATOR_CHAR);
        } else {
          builder.append(currentChar);
        }
      }
    } else {
      builder.append(id);
    }
  }

  /**
   * Return the resource representing the given compilation unit, or <code>null</code> if a resource
   * could not be created.
   * 
   * @param compilationUnit the compilation unit to be represented as a resource
   * @return the resource representing the given compilation unit
   * @throws DartModelException if a resource could not be created to represent the compilation unit
   */
  public static Resource getResource(CompilationUnit compilationUnit) throws DartModelException {
    if (compilationUnit == null) {
      throw new DartModelException(new DartModelStatusImpl(
          DartModelStatusConstants.INVALID_RESOURCE, "Compilation unit is null")); //$NON-NLS-0$
    }
    DartLibrary library = compilationUnit.getLibrary();
    if (library == null) {
      throw new DartModelException(new DartModelStatusImpl(
          DartModelStatusConstants.INVALID_RESOURCE, compilationUnit,
          "No library associated with compilation unit")); //$NON-NLS-0$
    }
    CompilationUnit libraryDefiningUnit = library.getDefiningCompilationUnit();
    return new Resource(composeResourceId(getUri(libraryDefiningUnit), getUri(compilationUnit)));
  }

  /**
   * Return a resource representing the compilation unit containing the given element.
   * 
   * @param element the element contained in the compilation unit to be returned
   * @return a resource representing the compilation unit containing the given element
   * @throws DartModelException if a resource could not be created to represent the compilation unit
   */
  public static Resource getResource(CompilationUnitElement element) throws DartModelException {
    CompilationUnitImpl unit = (CompilationUnitImpl) element.getCompilationUnit();
    if (unit == null) {
      // TODO(brianwilkerson) Figure out whether this can ever happen and whether there's anything
      // we can do about it if it can.
      throw new DartModelException(new DartModelStatusImpl(
          DartModelStatusConstants.INVALID_RESOURCE,
          "No compilation unit associated with " + element.getElementName())); //$NON-NLS-0$
    }
    return getResource(unit);
  }

  /**
   * Return the URI of the given compilation unit.
   * 
   * @param compilationUnit the compilation unit whose URI is to be returned
   * @return the URI of the given compilation unit
   * @throws DartModelException if the URI could not be computed
   */
  private static String getUri(CompilationUnit compilationUnit) throws DartModelException {
    if (compilationUnit == null) {
      throw new DartModelException(new DartModelStatusImpl(
          DartModelStatusConstants.INVALID_RESOURCE));
    }
    if (compilationUnit instanceof ExternalCompilationUnitImpl) {
      return ((ExternalCompilationUnitImpl) compilationUnit).getSourceRef().getUri().toString();
    }
    IResource resource = compilationUnit.getUnderlyingResource();
    if (resource != null) {
      URI locationUri = resource.getLocationURI();
      if (locationUri == null) {
        throw new DartModelException(new DartModelStatusImpl(
            DartModelStatusConstants.INVALID_RESOURCE));
      }
      return locationUri.toString();
    }
    throw new DartModelException(new DartModelStatusImpl(DartModelStatusConstants.INVALID_RESOURCE,
        compilationUnit));
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private ResourceFactory() {
    super();
  }
}
