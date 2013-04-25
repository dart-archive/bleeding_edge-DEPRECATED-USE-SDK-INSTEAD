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
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceKind;
import com.google.dart.tools.core.analysis.model.ContextManager;
import com.google.dart.tools.core.internal.builder.AnalysisWorker;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;

import java.util.ArrayList;
import java.util.List;

/**
 * Instances of {@code ContextManagerImpl} manage and provide access to multiple instances of
 * {@link AnalysisContext}.
 */
public abstract class ContextManagerImpl implements ContextManager {

  /**
   * A list of active {@link AnalysisWorker} workers for this project.
   */
  private List<AnalysisWorker> workers = new ArrayList<AnalysisWorker>();

  @Override
  public void addWorker(AnalysisWorker worker) {
    synchronized (workers) {
      workers.add(worker);
    }
  }

  @Override
  public LibraryElement getLibraryElement(IFile file) {
    AnalysisContext context = getContext(file);
    if (context != null) {
      IPath location = file.getLocation();
      if (location != null) {
        Source source = new FileBasedSource(
            context.getSourceFactory().getContentCache(),
            location.toFile());
        try {
          return context.computeLibraryElement(source);
        } catch (AnalysisException exception) {
          return null;
        }
      }
    }
    return null;
  }

  @Override
  public LibraryElement getLibraryElementOrNull(IFile file) {
    AnalysisContext context = getContext(file);
    if (context != null) {
      IPath location = file.getLocation();
      if (location != null) {
        Source source = new FileBasedSource(
            context.getSourceFactory().getContentCache(),
            location.toFile());
        return context.getLibraryElement(source);
      }
    }
    return null;
  }

  @Override
  public Source getSource(IFile file) {
    Source source = null;
    AnalysisContext context = getContext(file);
    if (context != null) {
      IPath location = file.getLocation();
      if (location != null) {
        source = new FileBasedSource(
            context.getSourceFactory().getContentCache(),
            location.toFile());
      }
    }
    return source;
  }

  @Override
  public SourceKind getSourceKind(IFile file) {
    AnalysisContext context = getContext(file);
    if (context != null) {
      IPath location = file.getLocation();
      if (location != null) {
        Source source = new FileBasedSource(
            context.getSourceFactory().getContentCache(),
            location.toFile());
        return context.getKindOf(source);
      }
    }
    return null;
  }

  @Override
  public void removeWorker(AnalysisWorker analysisWorker) {
    synchronized (workers) {
      workers.remove(analysisWorker);
    }
  }

  /**
   * Stop workers for the specified context.
   * 
   * @param context the context
   */
  protected void stopWorkers(AnalysisContext context) {
    AnalysisWorker[] workerArray;
    synchronized (workers) {
      workerArray = workers.toArray(new AnalysisWorker[workers.size()]);
    }
    for (AnalysisWorker worker : workerArray) {
      if (worker.getContext() == context) {
        worker.stop();
      }
    }
  }
}
