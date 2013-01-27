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
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.parser.ParserErrorCode;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.DartCore;

import junit.framework.TestCase;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ProjectAnalyzerTest extends TestCase {

  private static final String TEST_ERROR_MESSAGE = "test error message";

  private ProjectAnalyzer analyzer;
  private IResource resource;
  private SourceDeltaEvent event;
  private IMarker marker;

  public void testSourceAdded() throws Exception {
    analyzer.sourceAdded(event);
    verifyZeroInteractions(resource);
    analyzer.updateMarkers();
    verifyMarkerCreated();
  }

  public void testSourceChanged() throws Exception {
    analyzer.sourceChanged(event);
    verifyZeroInteractions(resource);
    analyzer.updateMarkers();
    verifyMarkerCreated();
  }

  @Override
  protected void setUp() throws Exception {
    resource = mock(IResource.class);
    marker = mock(IMarker.class);
    when(resource.createMarker(DartCore.DART_PROBLEM_MARKER_TYPE)).thenReturn(marker);
    Source source = mock(Source.class);

    CompilationUnit unit = mock(CompilationUnit.class);
    AnalysisError error = mock(AnalysisError.class);
    when(error.getErrorCode()).thenReturn(ParserErrorCode.DUPLICATE_LABEL_IN_SWITCH_STATEMENT);
    when(error.getSource()).thenReturn(source);
    when(error.getMessage()).thenReturn(TEST_ERROR_MESSAGE);
    when(error.getOffset()).thenReturn(7);
    when(error.getLength()).thenReturn(95);
    when(unit.getSyntacticErrors()).thenReturn(new AnalysisError[] {error});

    AnalysisContext context = mock(AnalysisContext.class);
    when(context.parse(source)).thenReturn(unit);

    event = mock(SourceDeltaEvent.class);
    when(event.getContext()).thenReturn(context);
    when(event.getSource()).thenReturn(source);
    when(event.getResource()).thenReturn(resource);

    analyzer = new ProjectAnalyzer();
  }

  private void verifyMarkerCreated() throws CoreException {
    verify(resource).deleteMarkers(DartCore.DART_PROBLEM_MARKER_TYPE, false, IResource.DEPTH_ZERO);
    verify(resource).createMarker(DartCore.DART_PROBLEM_MARKER_TYPE);
    verifyNoMoreInteractions(resource);
    verify(marker).setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
    verify(marker).setAttribute(IMarker.MESSAGE, TEST_ERROR_MESSAGE);
    verify(marker).setAttribute(IMarker.CHAR_START, 7);
    verify(marker).setAttribute(IMarker.CHAR_END, 102);
    verifyNoMoreInteractions(marker);
  }
}
