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
import com.google.dart.engine.parser.ParserErrorCode;
import com.google.dart.engine.resolver.ResolverErrorCode;
import com.google.dart.engine.source.Source;
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

  private ProjectAnalyzer analyzer;
  private IResource resource;
  private SourceDeltaEvent event;
  private IMarker marker;

  public void testSourceAdded() throws Exception {
    analyzer.sourceAdded(event);
    verifyZeroInteractions(resource);
    analyzer.analyze(new NullProgressMonitor());
    verifyMarkersCreated();
  }

  public void testSourceChanged() throws Exception {
    analyzer.sourceChanged(event);
    verifyZeroInteractions(resource);
    analyzer.analyze(new NullProgressMonitor());
    verifyMarkersCreated();
  }

  @Override
  protected void setUp() throws Exception {
    resource = mock(IResource.class);
    marker = mock(IMarker.class);
    when(resource.createMarker(DartCore.DART_PROBLEM_MARKER_TYPE)).thenReturn(marker).thenReturn(
        mock(IMarker.class));
    when(resource.getLocation()).thenReturn(new Path("does_not_exist.dart"));
    Source source = mock(Source.class);
    ArrayList<Source> sources = new ArrayList<Source>();
    sources.add(source);

    CompilationUnit unit = mock(CompilationUnit.class);
    AnalysisError parseError = mock(AnalysisError.class);
    when(parseError.getErrorCode()).thenReturn(ParserErrorCode.DUPLICATE_LABEL_IN_SWITCH_STATEMENT);
    when(parseError.getSource()).thenReturn(source);
    when(parseError.getMessage()).thenReturn(TEST_ERROR_MESSAGE);
    when(parseError.getOffset()).thenReturn(7);
    when(parseError.getLength()).thenReturn(95);
    when(unit.getSyntacticErrors()).thenReturn(new AnalysisError[] {parseError});
    AnalysisError resolutionError = mock(AnalysisError.class);
    when(resolutionError.getErrorCode()).thenReturn(ResolverErrorCode.LABEL_IN_OUTER_SCOPE);
    when(resolutionError.getSource()).thenReturn(source);
    when(resolutionError.getMessage()).thenReturn(TEST_ERROR_MESSAGE);
    when(resolutionError.getOffset()).thenReturn(7);
    when(resolutionError.getLength()).thenReturn(95);
    when(unit.getSemanticErrors()).thenReturn(new AnalysisError[] {resolutionError});

    AnalysisContext context = mock(AnalysisContext.class);
    LibraryElement library = mock(LibraryElement.class);
    when(context.parse(source)).thenReturn(unit);
    when(context.sourcesToResolve(sources.toArray(new Source[sources.size()]))).thenReturn(sources);
    when(context.getLibraryElement(source)).thenReturn(library);
    when(context.resolve(source, library)).thenReturn(unit);

    Project project = mock(Project.class);
    when(project.getResourceFor(source)).thenReturn(resource);

    event = mock(SourceDeltaEvent.class);
    when(event.getContext()).thenReturn(context);
    when(event.getSource()).thenReturn(source);
    when(event.getResource()).thenReturn(resource);
    when(event.getProject()).thenReturn(project);

    analyzer = new ProjectAnalyzer(new DartIgnoreManager());
  }

  private void verifyMarkersCreated() throws CoreException {
    verify(resource).deleteMarkers(DartCore.DART_PROBLEM_MARKER_TYPE, false, IResource.DEPTH_ZERO);
    verify(resource, times(2)).createMarker(DartCore.DART_PROBLEM_MARKER_TYPE); // resolution error
    verify(marker).setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
    verify(marker).setAttribute(IMarker.MESSAGE, TEST_ERROR_MESSAGE);
    verify(marker).setAttribute(IMarker.CHAR_START, 7);
    verify(marker).setAttribute(IMarker.CHAR_END, 102);
//    verify(marker).setAttribute(IMarker.LINE_NUMBER, 22);
//    verify(marker).setAttribute("errorCode", 9);
  }
}
