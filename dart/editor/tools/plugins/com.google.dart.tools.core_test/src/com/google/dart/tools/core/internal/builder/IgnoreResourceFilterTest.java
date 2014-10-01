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
package com.google.dart.tools.core.internal.builder;

import com.google.dart.tools.core.internal.model.DartIgnoreManager;
import com.google.dart.tools.core.internal.model.MockIgnoreFile;
import com.google.dart.tools.core.mock.MockProject;

import junit.framework.TestCase;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.mockito.Mockito;

public class IgnoreResourceFilterTest extends TestCase {

  private MockProject project;
  private DartIgnoreManager ignoreManager;
  private DeltaListener listener;
  private AnalysisMarkerManager markerManager;

  public void testPackageSourceAdded() throws Exception {
    SourceDeltaEvent event = newSourceDeltaEvent();

    ignoreManager.addToIgnores(event.getResource().getLocation());
    newTarget().packageSourceAdded(event);
    Mockito.verifyNoMoreInteractions(listener);
    Mockito.verify(markerManager).clearMarkers(event.getResource());
    Mockito.verifyNoMoreInteractions(markerManager);

    ignoreManager.removeFromIgnores(event.getResource().getLocation());
    newTarget().packageSourceAdded(event);
    Mockito.verify(listener).packageSourceAdded(event);
    Mockito.verifyNoMoreInteractions(listener);
    Mockito.verifyNoMoreInteractions(markerManager);

    ignoreManager.addToIgnores(event.getResource().getParent().getLocation());
    newTarget().packageSourceAdded(event);
    Mockito.verifyNoMoreInteractions(listener);
    Mockito.verify(markerManager, Mockito.times(2)).clearMarkers(event.getResource());
    Mockito.verifyNoMoreInteractions(markerManager);
  }

  public void testPackageSourceChanged() throws Exception {
    SourceDeltaEvent event = newSourceDeltaEvent();

    ignoreManager.addToIgnores(event.getResource().getLocation());
    newTarget().packageSourceChanged(event);
    Mockito.verifyNoMoreInteractions(listener);

    ignoreManager.removeFromIgnores(event.getResource().getLocation());
    newTarget().packageSourceChanged(event);
    Mockito.verify(listener).packageSourceChanged(event);
    Mockito.verifyNoMoreInteractions(listener);

    ignoreManager.addToIgnores(event.getResource().getParent().getLocation());
    newTarget().packageSourceChanged(event);
    Mockito.verifyNoMoreInteractions(listener);
    Mockito.verifyNoMoreInteractions(markerManager);
  }

  public void testPackageSourceContainerRemoved() throws Exception {
    SourceContainerDeltaEvent event = newSourceContainerDeltaEvent();
    newTarget().packageSourceContainerRemoved(event);
    Mockito.verify(listener).packageSourceContainerRemoved(event);
    Mockito.verifyNoMoreInteractions(listener);
    Mockito.verifyNoMoreInteractions(markerManager);
  }

  public void testPackageSourceContainerRemoved_ignored() throws Exception {
    SourceContainerDeltaEvent event = newSourceContainerDeltaEvent();
    ignoreManager.addToIgnores(event.getResource().getLocation());
    newTarget().packageSourceContainerRemoved(event);
    Mockito.verifyNoMoreInteractions(listener);
    Mockito.verifyNoMoreInteractions(markerManager);
  }

  public void testPackageSourceContainerRemoved_unignored() throws Exception {
    SourceContainerDeltaEvent event = newSourceContainerDeltaEvent();
    ignoreManager.addToIgnores(event.getResource().getLocation());
    ignoreManager.removeFromIgnores(event.getResource().getLocation());
    newTarget().packageSourceContainerRemoved(event);
    Mockito.verify(listener).packageSourceContainerRemoved(event);
    Mockito.verifyNoMoreInteractions(listener);
    Mockito.verifyNoMoreInteractions(markerManager);
  }

  public void testPackageSourceRemoved() throws Exception {
    SourceDeltaEvent event = newSourceDeltaEvent();
    newTarget().packageSourceRemoved(event);
    Mockito.verify(listener).packageSourceRemoved(event);
    Mockito.verifyNoMoreInteractions(listener);
    Mockito.verifyNoMoreInteractions(markerManager);
  }

  public void testPackageSourceRemoved_ignored() throws Exception {
    SourceDeltaEvent event = newSourceDeltaEvent();
    ignoreManager.addToIgnores(event.getResource().getLocation());
    newTarget().packageSourceRemoved(event);
    Mockito.verifyNoMoreInteractions(listener);
    Mockito.verifyNoMoreInteractions(markerManager);
  }

  public void testPackageSourceRemoved_unignored() throws Exception {
    SourceDeltaEvent event = newSourceDeltaEvent();
    ignoreManager.addToIgnores(event.getResource().getLocation());
    ignoreManager.removeFromIgnores(event.getResource().getLocation());
    newTarget().packageSourceRemoved(event);
    Mockito.verify(listener).packageSourceRemoved(event);
    Mockito.verifyNoMoreInteractions(listener);
    Mockito.verifyNoMoreInteractions(markerManager);
  }

  public void testPubspecAdded() throws Exception {
    SourceDeltaEvent event = newSourceDeltaEvent();

    ignoreManager.addToIgnores(event.getResource().getLocation());
    newTarget().pubspecAdded(event);
    Mockito.verifyNoMoreInteractions(listener);

    ignoreManager.removeFromIgnores(event.getResource().getLocation());
    newTarget().pubspecAdded(event);
    Mockito.verify(listener).pubspecAdded(event);
    Mockito.verifyNoMoreInteractions(listener);

    ignoreManager.addToIgnores(event.getResource().getParent().getLocation());
    newTarget().pubspecAdded(event);
    Mockito.verifyNoMoreInteractions(listener);
    Mockito.verifyNoMoreInteractions(markerManager);
  }

  public void testPubspecChanged() throws Exception {
    SourceDeltaEvent event = newSourceDeltaEvent();

    ignoreManager.addToIgnores(event.getResource().getLocation());
    newTarget().pubspecChanged(event);
    Mockito.verifyNoMoreInteractions(listener);

    ignoreManager.removeFromIgnores(event.getResource().getLocation());
    newTarget().pubspecChanged(event);
    Mockito.verify(listener).pubspecChanged(event);
    Mockito.verifyNoMoreInteractions(listener);

    ignoreManager.addToIgnores(event.getResource().getParent().getLocation());
    newTarget().pubspecChanged(event);
    Mockito.verifyNoMoreInteractions(listener);
    Mockito.verifyNoMoreInteractions(markerManager);
  }

  public void testPubspecRemoved() throws Exception {
    SourceDeltaEvent event = newSourceDeltaEvent();

    ignoreManager.addToIgnores(event.getResource().getLocation());
    newTarget().pubspecRemoved(event);
    Mockito.verifyNoMoreInteractions(listener);

    ignoreManager.removeFromIgnores(event.getResource().getLocation());
    newTarget().pubspecRemoved(event);
    Mockito.verify(listener).pubspecRemoved(event);
    Mockito.verifyNoMoreInteractions(listener);

    ignoreManager.addToIgnores(event.getResource().getParent().getLocation());
    newTarget().pubspecRemoved(event);
    Mockito.verifyNoMoreInteractions(listener);
    Mockito.verifyNoMoreInteractions(markerManager);
  }

  public void testSourceAdded() throws Exception {
    SourceDeltaEvent event = newSourceDeltaEvent();

    ignoreManager.addToIgnores(event.getResource().getLocation());
    newTarget().sourceAdded(event);
    Mockito.verifyNoMoreInteractions(listener);
    Mockito.verify(markerManager).clearMarkers(event.getResource());
    Mockito.verifyNoMoreInteractions(markerManager);

    ignoreManager.removeFromIgnores(event.getResource().getLocation());
    newTarget().sourceAdded(event);
    Mockito.verify(listener).sourceAdded(event);
    Mockito.verifyNoMoreInteractions(listener);
    Mockito.verifyNoMoreInteractions(markerManager);

    ignoreManager.addToIgnores(event.getResource().getParent().getLocation());
    newTarget().sourceAdded(event);
    Mockito.verifyNoMoreInteractions(listener);
    Mockito.verify(markerManager, Mockito.times(2)).clearMarkers(event.getResource());
    Mockito.verifyNoMoreInteractions(markerManager);
  }

  public void testSourceChanged() throws Exception {
    SourceDeltaEvent event = newSourceDeltaEvent();

    ignoreManager.addToIgnores(event.getResource().getLocation());
    newTarget().sourceChanged(event);
    Mockito.verifyNoMoreInteractions(listener);

    ignoreManager.removeFromIgnores(event.getResource().getLocation());
    newTarget().sourceChanged(event);
    Mockito.verify(listener).sourceChanged(event);
    Mockito.verifyNoMoreInteractions(listener);

    ignoreManager.addToIgnores(event.getResource().getParent().getLocation());
    newTarget().sourceChanged(event);
    Mockito.verifyNoMoreInteractions(listener);
    Mockito.verifyNoMoreInteractions(markerManager);
  }

  public void testSourceContainerRemoved() throws Exception {
    SourceContainerDeltaEvent event = newSourceContainerDeltaEvent();
    newTarget().sourceContainerRemoved(event);
    Mockito.verify(listener).sourceContainerRemoved(event);
    Mockito.verifyNoMoreInteractions(listener);
    Mockito.verifyNoMoreInteractions(markerManager);
  }

  public void testSourceContainerRemoved_ignored() throws Exception {
    SourceContainerDeltaEvent event = newSourceContainerDeltaEvent();
    ignoreManager.addToIgnores(event.getResource().getLocation());
    newTarget().sourceContainerRemoved(event);
    Mockito.verifyNoMoreInteractions(listener);
    Mockito.verifyNoMoreInteractions(markerManager);
  }

  public void testSourceContainerRemoved_null() throws Exception {
    SourceContainerDeltaEvent event = newSourceContainerDeltaEvent_nullResource();
    ignoreManager.addToIgnores(project.getFolder("web"));
    newTarget().sourceContainerRemoved(event);
    Mockito.verifyNoMoreInteractions(listener);
    Mockito.verifyNoMoreInteractions(markerManager);
  }

  public void testSourceContainerRemoved_unignored() throws Exception {
    SourceContainerDeltaEvent event = newSourceContainerDeltaEvent();
    ignoreManager.addToIgnores(event.getResource().getLocation());
    ignoreManager.removeFromIgnores(event.getResource().getLocation());
    newTarget().sourceContainerRemoved(event);
    Mockito.verify(listener).sourceContainerRemoved(event);
    Mockito.verifyNoMoreInteractions(listener);
    Mockito.verifyNoMoreInteractions(markerManager);
  }

  public void testSourceRemoved() throws Exception {
    SourceDeltaEvent event = newSourceDeltaEvent();
    newTarget().sourceRemoved(event);
    Mockito.verify(listener).sourceRemoved(event);
    Mockito.verifyNoMoreInteractions(listener);
    Mockito.verifyNoMoreInteractions(markerManager);
  }

  public void testSourceRemoved_ignored() throws Exception {
    SourceDeltaEvent event = newSourceDeltaEvent();
    ignoreManager.addToIgnores(event.getResource().getLocation());
    newTarget().sourceRemoved(event);
    Mockito.verifyNoMoreInteractions(listener);
    Mockito.verifyNoMoreInteractions(markerManager);
  }

  public void testSourceRemoved_null() throws Exception {
    SourceDeltaEvent event = newSourceDeltaEvent_nullResource();
    ignoreManager.addToIgnores(project.getFolder("web").getFile("other.dart"));
    newTarget().sourceRemoved(event);
    Mockito.verifyNoMoreInteractions(listener);
    Mockito.verifyNoMoreInteractions(markerManager);
  }

  public void testSourceRemoved_unignored() throws Exception {
    SourceDeltaEvent event = newSourceDeltaEvent();
    ignoreManager.addToIgnores(event.getResource().getLocation());
    ignoreManager.removeFromIgnores(event.getResource().getLocation());
    newTarget().sourceRemoved(event);
    Mockito.verify(listener).sourceRemoved(event);
    Mockito.verifyNoMoreInteractions(listener);
    Mockito.verifyNoMoreInteractions(markerManager);
  }

  public void testVisitContext() throws Exception {
    SourceContainerDeltaEvent event = newSourceContainerDeltaEvent();
    newTarget().visitContext(event);
    Mockito.verify(listener).visitContext(event);
    Mockito.verifyNoMoreInteractions(listener);
    Mockito.verifyNoMoreInteractions(markerManager);
  }

  public void testVisitContext_ignored() throws Exception {
    SourceContainerDeltaEvent event = newSourceContainerDeltaEvent();
    // Pass along visit context... even if it is ignored
    ignoreManager.addToIgnores(event.getResource().getLocation());
    newTarget().visitContext(event);
    Mockito.verify(listener).visitContext(event);
    Mockito.verifyNoMoreInteractions(listener);
    Mockito.verifyNoMoreInteractions(markerManager);
  }

  public void testVisitContext_unignored() throws Exception {
    SourceContainerDeltaEvent event = newSourceContainerDeltaEvent();
    ignoreManager.addToIgnores(event.getResource().getLocation());
    ignoreManager.removeFromIgnores(event.getResource().getLocation());
    newTarget().visitContext(event);
    Mockito.verify(listener).visitContext(event);
    Mockito.verifyNoMoreInteractions(listener);
    Mockito.verifyNoMoreInteractions(markerManager);
  }

  @Override
  protected void setUp() throws Exception {
    project = TestProjects.newPubProject3();
    markerManager = Mockito.mock(AnalysisMarkerManager.class);
    ignoreManager = new DartIgnoreManager(new MockIgnoreFile());
    listener = Mockito.mock(DeltaListener.class);
  }

  private SourceContainerDeltaEvent newSourceContainerDeltaEvent() {
    IFolder folder = project.getFolder("web");
    SourceContainerDeltaEvent event = Mockito.mock(SourceContainerDeltaEvent.class);
    Mockito.when(event.getResource()).thenReturn(folder);
    return event;
  }

  private SourceContainerDeltaEvent newSourceContainerDeltaEvent_nullResource() {
    SourceContainerDeltaEvent event = Mockito.mock(SourceContainerDeltaEvent.class);
    Mockito.when(event.getResource()).thenReturn(null);
    return event;
  }

  private SourceDeltaEvent newSourceDeltaEvent() {
    IFile file = project.getFolder("web").getFile("other.dart");
    SourceDeltaEvent event = Mockito.mock(SourceDeltaEvent.class);
    Mockito.when(event.getResource()).thenReturn(file);
    return event;
  }

  private SourceDeltaEvent newSourceDeltaEvent_nullResource() {
    SourceDeltaEvent event = Mockito.mock(SourceDeltaEvent.class);
    Mockito.when(event.getResource()).thenReturn(null);
    return event;
  }

  private IgnoreResourceFilter newTarget() {
    IgnoreResourceFilter filter = new IgnoreResourceFilter(ignoreManager, markerManager);
    filter.addDeltaListener(listener);
    return filter;
  }
}
