/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.tools.core.analysis.model;

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.builder.AnalysisWorker;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

/**
 * A lightweight version of the core model. This version can be queried quickly and is guaranteed to
 * have reasonably up-to-date information. As the latest data is available from the analysis engine,
 * this model is updated.
 */
public class LightweightModel_OLD extends LightweightModel {
  protected LightweightModel_OLD() {
    AnalysisWorker.addListener(new AnalysisListener() {
      @Override
      public void complete(AnalysisEvent event) {
      }

      @Override
      public void resolved(ResolvedEvent event) {
        IResource resource = event.getResource();
        if (resource != null) {
          AnalysisContext context = event.getContext();
          ResourceMap resourceMap = event.getResourceMap();
          Source source = event.getSource();
          // in tests some information may be missing
          if (context == null || resourceMap == null || source == null) {
            return;
          }
          // OK, update the Source
          try {
            recalculateForResource(context, resourceMap, source, resource);
          } catch (CoreException e) {
            DartCore.logInformation("Exception updating: " + source, e);
          }
        }
      }

      @Override
      public void resolvedHtml(ResolvedHtmlEvent event) {
        IResource htmlResource = event.getResource();
        if (htmlResource instanceof IFile) {
          IFile htmlFile = (IFile) htmlResource;
          AnalysisContext context = event.getContext();
          ResourceMap resourceMap = event.getResourceMap();
          Source htmlSource = event.getSource();
          // in tests some information may be missing
          if (context == null || resourceMap == null || htmlSource == null) {
            return;
          }
          // OK, process the change
          try {
            Source[] librarySources = context.getLibrariesReferencedFromHtml(htmlSource);
            for (Source librarySource : librarySources) {
              IFile libraryFile = resourceMap.getResource(librarySource);
              setFileProperty(libraryFile, HTML_FILE, htmlFile);
            }
          } catch (CoreException e) {
            DartCore.logInformation("Exception updating: " + htmlSource);
          }
        }
      }
    });
  }

  private String getLibraryName(Source source, AnalysisContext context) {
    if (source == null) {
      return null;
    }

    LibraryElement element = context.getLibraryElement(source);

    if (element == null) {
      return null;
    }

    String name = element.getDisplayName();

    if (name != null && name.isEmpty()) {
      return null;
    } else {
      return name;
    }
  }

  private void recalculateForResource(AnalysisContext context, ResourceMap resourceMap,
      Source source, IResource resource) throws CoreException {

    // Check existence before setting persistent properties
    if (!resource.exists()) {
      DartCore.logInformation(getClass().getSimpleName()
          + "#recalculateForResource cannot update persistent properties on non-existant resource: "
          + resource);
      return;
    }
    IFile file = (IFile) resource;

    // Set the library name.
    String libraryName = getLibraryName(source, context);
    setFileProperty(file, DartCore.LIBRARY_NAME, libraryName);

    // Set the client library property.
    boolean clientLaunchable = source == null ? false : context.isClientLibrary(source);
    setFileProperty(file, CLIENT_LIBRARY, clientLaunchable);

    // Set the server library property.
    boolean serverLaunchable = source == null ? false : context.isServerLibrary(source);
    setFileProperty(file, SERVER_LIBRARY, serverLaunchable);

    // Set the HTML file property.
    Source[] htmlSources = source == null ? null : context.getHtmlFilesReferencing(source);
    if (htmlSources == null || htmlSources.length == 0) {
      setFileProperty(file, HTML_FILE, (String) null);
    } else {
      setFileProperty(file, HTML_FILE, resourceMap.getResource(htmlSources[0]));
    }

    // Set the containing library property.
    Source[] containingSources = source == null ? null : context.getLibrariesContaining(source);
    if (containingSources == null || containingSources.length == 0) {
      setFileProperty(file, CONTAINING_LIBRARY, (String) null);
    } else {
      setFileProperty(file, CONTAINING_LIBRARY, resourceMap.getResource(containingSources[0]));
    }
  }
}
