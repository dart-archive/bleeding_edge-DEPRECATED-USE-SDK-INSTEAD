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
package com.google.dart.tools.core.internal.builder;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.CompileTimeErrorCode;
import com.google.dart.engine.index.Index;
import com.google.dart.engine.parser.ParserErrorCode;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceContainer;
import com.google.dart.engine.utilities.source.LineInfo;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.internal.model.DartIgnoreManager;

import junit.framework.TestCase;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

public class ProjectAnalyzerTest extends TestCase {

  private static final String TEST_ERROR_MESSAGE = "test error message";

  private IResource resource;
  private Source source;
  private Project project;
  private AnalysisContext context;
  private ProjectAnalyzer analyzer;
  private CompilationUnit unit;
  private IMarker parseErrorMarker;
  private IMarker resolutionErrorMarker;
  private Index index;

  public void test_packageSourceAdded() throws Exception {
    analyzer.packageSourceAdded(mockSourceEvent());
    verifyZeroInteractions(resource);
    analyzer.analyze(new NullProgressMonitor());
    // TODO (danrubel): don't show error markers on packages
//    verifyNoMarkersCreated();
    verify(index).indexUnit(unit);
  }

  public void test_packageSourceChanged() throws Exception {
    analyzer.packageSourceChanged(mockSourceEvent());
    verifyZeroInteractions(resource);
    analyzer.analyze(new NullProgressMonitor());
    // TODO (danrubel): don't show error markers on packages
//  verifyNoMarkersCreated();
    verify(index).indexUnit(unit);
  }

  public void test_packageSourceContainerRemoved() throws Exception {
    SourceContainerDeltaEvent event = mockSourceContainerEvent();
    analyzer.packageSourceContainerRemoved(event);
    verifyNoMarkersCreated();
    verify(index).removeSources(event.getSourceContainer());
  }

  public void test_packageSourceRemoved() throws Exception {
    analyzer.packageSourceRemoved(mockSourceEvent());
    verify(index).removeSource(source);
  }

  public void test_sourceAdded() throws Exception {
    analyzer.sourceAdded(mockSourceEvent());
    verifyZeroInteractions(resource);
    analyzer.analyze(new NullProgressMonitor());
    verifyMarkersCreated();
    verify(index).indexUnit(unit);
  }

  public void test_sourceChanged() throws Exception {
    analyzer.sourceChanged(mockSourceEvent());
    verifyZeroInteractions(resource);
    analyzer.analyze(new NullProgressMonitor());
    verifyMarkersCreated();
    verify(index).indexUnit(unit);
  }

  public void test_sourceContainerRemoved() throws Exception {
    SourceContainerDeltaEvent event = mockSourceContainerEvent();
    analyzer.sourceContainerRemoved(event);
    verifyNoMarkersCreated();
    verify(index).removeSources(event.getSourceContainer());
  }

  public void test_sourceRemoved() throws Exception {
    analyzer.sourceRemoved(mockSourceEvent());
    verifyNoMarkersCreated();
    verify(index).removeSource(source);
  }

  @Override
  protected void setUp() throws Exception {
    resource = mock(IResource.class);
    parseErrorMarker = mock(IMarker.class);
    when(resource.createMarker(DartCore.DART_PARSING_PROBLEM_MARKER_TYPE)).thenReturn(
        parseErrorMarker);
    resolutionErrorMarker = mock(IMarker.class);
    when(resource.createMarker(DartCore.DART_RESOLUTION_PROBLEM_MARKER_TYPE)).thenReturn(
        resolutionErrorMarker);
    when(resource.getLocation()).thenReturn(new Path("does_not_exist.dart"));
    source = mock(Source.class);
    ArrayList<Source> sources = new ArrayList<Source>();
    sources.add(source);

    unit = mock(CompilationUnit.class);
    AnalysisError parseError = mock(AnalysisError.class);
    when(parseError.getErrorCode()).thenReturn(ParserErrorCode.DUPLICATE_LABEL_IN_SWITCH_STATEMENT);
    when(parseError.getSource()).thenReturn(source);
    when(parseError.getMessage()).thenReturn(TEST_ERROR_MESSAGE);
    when(parseError.getOffset()).thenReturn(7);
    when(parseError.getLength()).thenReturn(95);
    when(unit.getParsingErrors()).thenReturn(new AnalysisError[] {parseError});
    AnalysisError resolutionError = mock(AnalysisError.class);
    when(resolutionError.getErrorCode()).thenReturn(CompileTimeErrorCode.LABEL_IN_OUTER_SCOPE);
    when(resolutionError.getSource()).thenReturn(source);
    when(resolutionError.getMessage()).thenReturn(TEST_ERROR_MESSAGE);
    when(resolutionError.getOffset()).thenReturn(120);
    when(resolutionError.getLength()).thenReturn(20);
    when(unit.getResolutionErrors()).thenReturn(new AnalysisError[] {resolutionError});
    when(unit.getLineInfo()).thenReturn(new LineInfo(new int[] {0, 20, 91}));

    context = mock(AnalysisContext.class);
    LibraryElement library = mock(LibraryElement.class);
    when(context.parse(source)).thenReturn(unit);
    when(context.sourcesToResolve(sources.toArray(new Source[sources.size()]))).thenReturn(sources);
    when(context.getLibraryElement(source)).thenReturn(library);
    when(context.resolve(source, library)).thenReturn(unit);

    project = mock(Project.class);
    when(project.getResource(source)).thenReturn(resource);

    index = mock(Index.class);

    analyzer = new ProjectAnalyzer(new DartIgnoreManager(), index);
  }

  private SourceContainerDeltaEvent mockSourceContainerEvent() {
    SourceContainerDeltaEvent event = mock(SourceContainerDeltaEvent.class);
    when(event.getContext()).thenReturn(context);
    when(event.getSourceContainer()).thenReturn(mock(SourceContainer.class));
    when(event.getResource()).thenReturn(resource);
    when(event.getProject()).thenReturn(project);
    return event;
  }

  private SourceDeltaEvent mockSourceEvent() {
    SourceDeltaEvent event = mock(SourceDeltaEvent.class);
    when(event.getContext()).thenReturn(context);
    when(event.getSource()).thenReturn(source);
    when(event.getResource()).thenReturn(resource);
    when(event.getProject()).thenReturn(project);
    return event;
  }

  private void verifyMarkersCreated() throws CoreException {
    verify(resource, times(1)).deleteMarkers(
        DartCore.DART_PARSING_PROBLEM_MARKER_TYPE,
        false,
        IResource.DEPTH_ZERO);
    verify(resource, times(1)).createMarker(DartCore.DART_PARSING_PROBLEM_MARKER_TYPE);
    verify(parseErrorMarker).setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
    verify(parseErrorMarker).setAttribute(IMarker.MESSAGE, TEST_ERROR_MESSAGE);
    verify(parseErrorMarker).setAttribute(IMarker.CHAR_START, 7);
    verify(parseErrorMarker).setAttribute(IMarker.CHAR_END, 102);
    verify(parseErrorMarker).setAttribute(IMarker.LINE_NUMBER, 1);
//  verify(parseErrorMarker).setAttribute("errorCode", 9);

    verify(resource, times(1)).deleteMarkers(
        DartCore.DART_RESOLUTION_PROBLEM_MARKER_TYPE,
        false,
        IResource.DEPTH_ZERO);
    verify(resource, times(1)).createMarker(DartCore.DART_RESOLUTION_PROBLEM_MARKER_TYPE);
    verify(resolutionErrorMarker).setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
    verify(resolutionErrorMarker).setAttribute(IMarker.MESSAGE, TEST_ERROR_MESSAGE);
    verify(resolutionErrorMarker).setAttribute(IMarker.CHAR_START, 120);
    verify(resolutionErrorMarker).setAttribute(IMarker.CHAR_END, 140);
    verify(resolutionErrorMarker).setAttribute(IMarker.LINE_NUMBER, 3);
//    verify(resolutionErrorMarker).setAttribute("errorCode", 9);
  }

  private void verifyNoMarkersCreated() throws CoreException {
    verify(resource, times(0)).deleteMarkers(
        DartCore.DART_PROBLEM_MARKER_TYPE,
        false,
        IResource.DEPTH_ZERO);
    verify(resource, times(0)).createMarker(DartCore.DART_PROBLEM_MARKER_TYPE);
  }
}
