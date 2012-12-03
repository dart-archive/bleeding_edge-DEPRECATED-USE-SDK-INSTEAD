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

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.PackageUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.builder.BuildEvent;
import com.google.dart.tools.core.builder.BuildParticipant;
import com.google.dart.tools.core.builder.BuildVisitor;
import com.google.dart.tools.core.builder.CleanEvent;
import com.google.dart.tools.core.builder.ParticipantEvent;
import com.google.dart.tools.core.model.DartSdkManager;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import static org.eclipse.core.resources.IResource.DEPTH_INFINITE;
import static org.eclipse.core.resources.IResource.FILE;
import static org.eclipse.core.resources.IResourceDelta.REMOVED;

import java.io.File;

/**
 * Performs source analysis using instances of {@link AnalysisContext}.
 * {@link AnalysisServerParticipant} should be disabled when this participant is enabled.
 * 
 * @see DartCoreDebug#ENABLE_NEW_ANALYSIS
 */
public class AnalysisEngineParticipant implements BuildParticipant, BuildVisitor {

  // For now using a single context because we are only parsing
  private AnalysisContext context;
  private SourceFactory factory;
  private final boolean enabled;

  public AnalysisEngineParticipant() {
    this(DartCoreDebug.ENABLE_NEW_ANALYSIS);
  }

  public AnalysisEngineParticipant(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public void build(BuildEvent event, IProgressMonitor monitor) throws CoreException {

    // This participant and AnalysisServerParticipant are mutually exclusive
    if (!enabled) {
      return;
    }

    setupContext(event);
    event.traverse(this, false);
  }

  @Override
  public void clean(CleanEvent event, IProgressMonitor monitor) throws CoreException {

    // This participant and AnalysisServerParticipant are mutually exclusive
    if (!enabled) {
      return;
    }

    // Discard any cached analysis
    context = null;

    event.getProject().deleteMarkers(DartCore.DART_PROBLEM_MARKER_TYPE, true, DEPTH_INFINITE);
  }

  @Override
  public boolean visit(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
    IResource resource = delta.getResource();
    if (delta.getKind() == REMOVED) {
      IPath location = resource.getLocation();
      if (location != null) {
        // TODO (danrubel): implement discard
        //context.discard(location.toFile());
      }
      return false;
    }
    if (resource.getType() == FILE && DartCore.isDartLikeFileName(resource.getName())) {
      parse(resource, true);
    }
    return true;
  }

  @Override
  public boolean visit(IResourceProxy proxy, IProgressMonitor monitor) throws CoreException {
    if (proxy.getType() == FILE && DartCore.isDartLikeFileName(proxy.getName())) {
      parse(proxy.requestResource(), false);
    }
    return true;
  }

  /**
   * Create an error marker for the specified resource
   * 
   * @param resource the resource (not {@code null})
   * @param error the error (not {@code null})
   */
  private void createMarker(IResource resource, AnalysisError error) {

    ErrorCode errorCode = error.getErrorCode();

    int severity;
    switch (errorCode.getErrorSeverity()) {
      case ERROR:
        severity = IMarker.SEVERITY_ERROR;
        break;
      case WARNING:
        severity = IMarker.SEVERITY_WARNING;
        break;
      default:
        severity = IMarker.SEVERITY_INFO;
        break;
    }

    int offset = error.getOffset();
    int length = error.getLength();
    // TODO (danrubel): calculate line number
    int lineNumber = 1;
    String errorMessage = error.getMessage();
    String errorCodeMessage = errorCode.getMessage();

    try {
      IMarker marker = resource.createMarker(DartCore.DART_PROBLEM_MARKER_TYPE);
      marker.setAttribute(IMarker.SEVERITY, severity);
      marker.setAttribute(IMarker.CHAR_START, offset);
      marker.setAttribute(IMarker.CHAR_END, offset + length);
      marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
      marker.setAttribute("errorCode", errorCodeMessage);
      marker.setAttribute(IMarker.MESSAGE, errorMessage);
    } catch (CoreException e) {
      DartCore.logError("Failed to create marker for " + resource + "\n   at " + offset
          + " message: " + errorMessage, e);
    }
  }

  /**
   * Parse the specified resource and report syntax errors to the user.
   * 
   * @param resource the resource (not {@code null})
   * @param {@code true} if the context should be notified that the source has changed before
   *        requesting that the source be parsed
   */
  private void parse(final IResource resource, boolean changed) throws CoreException {
    if (!resource.exists() || !DartCore.isAnalyzed(resource)) {
      return;
    }
    IPath location = resource.getLocation();
    if (location == null) {
      return;
    }
    Source source = factory.forFile(location.toFile());
    if (changed) {
      context.sourceChanged(source);
    }
    try {
      context.parse(source, new AnalysisErrorListener() {
        @Override
        public void onError(AnalysisError error) {
          createMarker(resource, error);
        }
      });
    } catch (AnalysisException e) {
      throw new CoreException(new Status(IStatus.ERROR, DartCore.PLUGIN_ID, "Failed to parse "
          + location, e));
    }
  }

  /**
   * Lazily initialize the {@link AnalysisContext}.
   * 
   * @param event the event
   */
  private void setupContext(ParticipantEvent event) {
    if (context == null) {
      if (factory == null) {

        DartSdkManager sdkManager = com.google.dart.tools.core.model.DartSdkManager.getManager();
        DartSdk sdk = new DartSdk(sdkManager.getSdk().getDirectory());
        DartUriResolver dartResolver = new DartUriResolver(sdk);
        // TODO (danrubel): for now assume that the package root == project directory
        File pkgRoot = event.getProject().getLocation().toFile();
        PackageUriResolver pkgResolver = new PackageUriResolver(pkgRoot);
        FileUriResolver fileResolver = new FileUriResolver();

        factory = new SourceFactory(dartResolver, pkgResolver, fileResolver);
      }
      context = AnalysisEngine.getInstance().createAnalysisContext();
      context.setSourceFactory(factory);
    }
  }
}
