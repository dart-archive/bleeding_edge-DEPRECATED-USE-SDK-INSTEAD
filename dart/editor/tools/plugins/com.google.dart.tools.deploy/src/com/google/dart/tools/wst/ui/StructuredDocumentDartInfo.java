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

package com.google.dart.tools.wst.ui;

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.internal.util.ResourceUtil;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;

import java.io.File;

/**
 * Adapts SSE {@link IDocument} to the {@link IResource} and other Dart related information.
 */
public class StructuredDocumentDartInfo {
  public static StructuredDocumentDartInfo create(IDocument document) {
    // prepare File
    ITextFileBufferManager fileManager = FileBuffers.getTextFileBufferManager();
    ITextFileBuffer fileBuffer = fileManager.getTextFileBuffer(document);
    File file;
    try {
      file = fileBuffer.getFileStore().toLocalFile(0, null);
    } catch (CoreException ex) {
      return null;
    }
    // prepare IResource
    IResource resource = ResourceUtil.getResource(file);
    if (resource == null) {
      return null;
    }
    // prepare model Project
    IProject resourceProject = resource.getProject();
    Project project = DartCore.getProjectManager().getProject(resourceProject);
    // done
    return new StructuredDocumentDartInfo(document, file, resourceProject, project);
  }

  private final IDocument document;
  private final File file;
  private final IResource resource;
  private final Project project;

  private StructuredDocumentDartInfo(IDocument document, File file, IResource resource,
      Project project) {
    this.document = document;
    this.file = file;
    this.resource = resource;
    this.project = project;
  }

  public AnalysisContext getContext() {
    return DartCore.getProjectManager().getContext(resource);
  }

  public IDocument getDocument() {
    return document;
  }

  public File getFile() {
    return file;
  }

  public Project getProject() {
    return project;
  }

  public IResource getResource() {
    return resource;
  }

  public Source getSource() {
    AnalysisContext analysisContext = getContext();
    if (analysisContext == null) {
      return null;
    }
    return new FileBasedSource(file);
  }
}
