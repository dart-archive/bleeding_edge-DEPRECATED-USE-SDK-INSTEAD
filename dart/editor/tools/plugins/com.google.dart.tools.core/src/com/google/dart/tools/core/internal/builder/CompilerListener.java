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
package com.google.dart.tools.core.internal.builder;

import com.google.dart.compiler.DartCompilationError;
import com.google.dart.compiler.DartCompilerContext;
import com.google.dart.compiler.DartCompilerListener;
import com.google.dart.compiler.DartSource;
import com.google.dart.compiler.ErrorSeverity;
import com.google.dart.compiler.Source;
import com.google.dart.compiler.SubSystem;
import com.google.dart.compiler.SystemLibraryManager;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.internal.index.impl.InMemoryIndex;
import com.google.dart.tools.core.internal.index.util.ResourceFactory;
import com.google.dart.tools.core.internal.model.DartLibraryImpl;
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;

import static com.google.dart.tools.core.internal.builder.BuilderUtil.createErrorMarker;
import static com.google.dart.tools.core.internal.builder.BuilderUtil.createWarningMarker;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import java.net.URI;

/**
 * An Eclipse specific implementation of {@link DartCompilerContext} for intercepting compilation
 * errors and translating them into {@link IResource} markers.
 */
class CompilerListener implements DartCompilerListener {
  /**
   * The top-level library containing the code being compiled.
   */
  private DartLibrary library;

  /**
   * The project associated with the library.
   */
  private final IProject project;

  private boolean createMarkers;

  CompilerListener(DartLibrary library, IProject project, boolean createMarkers) {
    this.project = project;
    this.library = library;
    this.createMarkers = createMarkers;
  }

  @Override
  public void onError(DartCompilationError event) {
    if (createMarkers) {
      if (event.getErrorCode().getSubSystem() == SubSystem.STATIC_TYPE) {
        processWarning(event);
      } else if (event.getErrorCode().getErrorSeverity() == ErrorSeverity.ERROR) {
        processError(event);
      } else if (event.getErrorCode().getErrorSeverity() == ErrorSeverity.WARNING) {
        processWarning(event);
      }
    }
  }

  @Override
  public void unitAboutToCompile(DartSource source, boolean diet) {
    if (diet || !createMarkers) {
      return;
    }

    IResource resource = ResourceUtil.getResource(source);

    if (resource == null && source != null) {
      // Don't report issues removing markers for dart: based source files.
      if (DartCoreDebug.VERBOSE && !"dart".equals(source.getUri().getScheme())) {
        DartCore.logInformation("Unable to remove markers for source \""
            + source.getUri().toString() + "\"");
      }
      return;
    }

    BuilderUtil.clearErrorMarkers(resource);
  }

  @Override
  public void unitCompiled(DartUnit unit) {
    if (unit.isDiet()) {
      return;
    }
    DartSource source = (DartSource) unit.getSourceInfo().getSource();
    if (source != null) {
      IResource[] resources = ResourceUtil.getResources(source);
      if (resources == null || resources.length != 1) {
        URI sourceUri = source.getUri();
        if (!SystemLibraryManager.isDartUri(sourceUri)) {
          DartCore.logError("Could not find compilation unit corresponding to " + sourceUri + " ("
              + (resources == null ? "no" : resources.length) + " files found)");
        }
        return;
      }
      DartElement element = DartCore.create(resources[0]);
      if (element instanceof DartLibrary) {
        try {
          element = ((DartLibrary) element).getDefiningCompilationUnit();
        } catch (DartModelException exception) {
          DartCore.logError("Could not get defining compilation unit for library "
              + ((DartLibraryImpl) element).getLibrarySourceFile().getUri(), exception);
        }
      }
      if (element instanceof CompilationUnit) {
        CompilationUnit compilationUnit = (CompilationUnit) element;
        try {
          InMemoryIndex.getInstance().indexResource(
              ResourceFactory.getResource(compilationUnit),
              compilationUnit,
              unit);
        } catch (Exception exception) {
          DartCore.logError("Could not index " + source.getUri(), exception);
        }
      } else {
        StringBuilder builder = new StringBuilder();
        builder.append("Could not find compilation unit corresponding to ");
        builder.append(source.getUri());
        if (element == null) {
          builder.append(" (resource did not map)");
        } else {
          builder.append(" (resource mapped to a ");
          builder.append(element.getClass().getName());
          builder.append(")");
        }
        DartCore.logError(builder.toString());
      }
    }
  }

  /**
   * @return the library's resource, or the project's resource if we get a model exception
   */
  @SuppressWarnings("unused")
  // This was used to associate errors in missing resources.
  private IResource getLibraryResource() {
    try {
      return library.getDefiningCompilationUnit().getCorrespondingResource();
    } catch (DartModelException exception) {
      // Fall through to use the project as a resource.
      return project;
    }
  }

  /**
   * Return the Eclipse resource associated with the given error's source, or null if the real
   * resource cannot be found.
   * 
   * @param error the compilation error defining the source
   * @return the resource associated with the error's source
   */
  private IResource getResource(DartCompilationError error) {
    Source source = error.getSource();
    IResource res = ResourceUtil.getResource(source);
    if (res == null) {
      if (DartCoreDebug.VERBOSE) {
        // Don't flood the log
        StringBuilder builder = new StringBuilder();
        if (source == null) {
          builder.append("No source associated with compilation error (");
        } else {
          builder.append("Could not find file for source \"");
          builder.append(source.getUri().toString());
          builder.append("\" (");
        }
        builder.append(error.getErrorCode().getErrorSeverity().getName());
        builder.append(" ");
        builder.append(error.getLineNumber());
        builder.append(":");
        builder.append(error.getColumnNumber());
        builder.append("): ");
        builder.append(error.getMessage());
        DartCore.logInformation(builder.toString());
      }
    }

    return res;
  }

  /**
   * Create a marker for the given compilation error.
   * 
   * @param error the compilation error for which a marker is to be created
   */
  private void processError(DartCompilationError error) {
    IResource res = getResource(error);
    if (res != null) {
      if (res.exists() && res.getProject().equals(project) && DartCore.isAnalyzed(res)) {
        createErrorMarker(
            res,
            error.getStartPosition(),
            error.getLength(),
            error.getLineNumber(),
            error.getMessage());
      }
//    } else {
//      createErrorMarker(getLibraryResource(), 0, 0, 1, error.getMessage());
    }
  }

  /**
   * Create a marker for the given compilation error.
   * 
   * @param error the compilation error for which a marker is to be created
   */
  private void processWarning(DartCompilationError error) {
    IResource res = getResource(error);

    if (res != null) {
      if (res.exists() && res.getProject().equals(project) && DartCore.isAnalyzed(res)) {
        createWarningMarker(
            res,
            error.getStartPosition(),
            error.getLength(),
            error.getLineNumber(),
            error.getMessage());
      }
//    } else {
//      createWarningMarker(getLibraryResource(), 0, 0, 1, error.getMessage());
    }
  }

}
