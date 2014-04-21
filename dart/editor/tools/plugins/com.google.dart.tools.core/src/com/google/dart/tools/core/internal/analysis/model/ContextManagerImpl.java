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
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceKind;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.ContextManager;
import com.google.dart.tools.core.analysis.model.ResourceMap;
import com.google.dart.tools.core.internal.builder.AnalysisWorker;

import org.eclipse.core.resources.IFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Instances of {@code ContextManagerImpl} manage and provide access to multiple instances of
 * {@link AnalysisContext}.
 * 
 * @coverage dart.tools.core.model
 */
public abstract class ContextManagerImpl implements ContextManager {

  /**
   * A list of active {@link AnalysisWorker} workers for this project.
   */
  private final List<AnalysisWorker> workers = new ArrayList<AnalysisWorker>();

  /**
   * The Dart SDK used when constructing the default context.
   */
  private final DartSdk sdk;

  /**
   * The identifier of the Dart SDK used when constructing the default context.
   */
  private final String sdkContextId;

  public ContextManagerImpl(DartSdk sdk, String sdkContextId) {
    this.sdk = sdk;
    this.sdkContextId = sdkContextId;
  }

  @Override
  public void addWorker(AnalysisWorker worker) {
    synchronized (workers) {
      workers.add(worker);
    }
  }

  @Override
  public LibraryElement getLibraryElement(IFile file) {
    ResourceMap map = getResourceMap(file);
    if (map == null) {
      return null;
    }
    Source source = map.getSource(file);
    if (source != null) {
      try {
        return map.getContext().computeLibraryElement(source);
      } catch (AnalysisException e) {
        DartCore.logError("Failed to compute library element: " + file, e);
      }
    }
    return null;
  }

  @Override
  public LibraryElement getLibraryElementOrNull(IFile file) {
    ResourceMap map = getResourceMap(file);
    if (map == null) {
      return null;
    }
    Source source = map.getSource(file);
    if (source != null) {
      return map.getContext().getLibraryElement(source);
    }
    return null;
  }

  @Override
  public DartSdk getSdk() {
    return sdk;
  }

  @Override
  public AnalysisContext getSdkContext() {
    return sdk.getContext();
  }

  @Override
  public String getSdkContextId() {
    return sdkContextId;
  }

  @Override
  public Source getSource(IFile file) {
    ResourceMap map = getResourceMap(file);
    if (map == null) {
      return null;
    }
    return map.getSource(file);
  }

  @Override
  public SourceKind getSourceKind(IFile file) {
    ResourceMap map = getResourceMap(file);
    if (map == null) {
      return SourceKind.UNKNOWN;
    }
    Source source = map.getSource(file);
    if (source != null) {
      return map.getContext().getKindOf(source);
    }
    return null;
  }

  @Override
  public AnalysisWorker[] getWorkers() {
    synchronized (workers) {
      return workers.toArray(new AnalysisWorker[workers.size()]);
    }
  }

  @Override
  public void removeWorker(AnalysisWorker analysisWorker) {
    synchronized (workers) {
      workers.remove(analysisWorker);
    }
  }

  @Override
  public void stopWorkers(AnalysisContext context) {
    for (AnalysisWorker worker : getWorkers()) {
      if (worker.getContext() == context) {
        worker.stop();
      }
    }
  }
}
