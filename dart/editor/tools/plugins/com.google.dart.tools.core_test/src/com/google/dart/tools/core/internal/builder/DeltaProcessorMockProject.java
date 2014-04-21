package com.google.dart.tools.core.internal.builder;

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.ChangeSet;
import com.google.dart.engine.index.Index;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.tools.core.CallList;
import com.google.dart.tools.core.analysis.model.PubFolder;
import com.google.dart.tools.core.internal.analysis.model.ProjectImpl;
import com.google.dart.tools.core.mock.MockContainer;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import static org.mockito.Mockito.mock;

import java.io.File;

/**
 * Specialized {@link ProjectImpl} that returns a mock context for recording what analysis is
 * requested rather than a context that would actually analyze the source.
 */
class DeltaProcessorMockProject extends ProjectImpl {

  private static final String DISCARD_CONTEXTS_IN = "discardContextsIn";
  private static final String PUBSPEC_ADDED = "pubspecAdded";
  private static final String PUBSPEC_REMOVED = "pubspecRemoved";

  private final CallList calls = new CallList();

  DeltaProcessorMockProject(IProject resource) {
    super(resource, mock(DartSdk.class), "sdk-id", mock(Index.class), new AnalysisContextFactory() {
      @Override
      public AnalysisContext createContext() {
        return new MockContext();
      }
    });
  }

  @Override
  public void discardContextsIn(IContainer container) {
    calls.add(this, DISCARD_CONTEXTS_IN, container);
  }

  @Override
  public void pubspecAdded(IContainer container) {
    calls.add(this, PUBSPEC_ADDED, container);
  }

  @Override
  public void pubspecRemoved(IContainer container) {
    calls.add(this, PUBSPEC_REMOVED, container);
  }

  void assertChanged(IContainer pubFolderResource, ChangeSet expected) {
    getContextFor(pubFolderResource).assertChanged(expected);
  }

  void assertChanged(IContainer pubFolderResource, File[] added, File[] changed,
      File[] removedFiles, File[] removedDirs) {
    getContextFor(pubFolderResource).assertChanged(added, changed, removedFiles, removedDirs);
  }

  void assertChanged(IContainer pubFolderResource, IResource[] added, IResource[] changed,
      IResource[] removed) {
    getContextFor(pubFolderResource).assertChanged(added, changed, removed);
  }

  void assertDiscardContextsIn(IContainer... expected) {
    for (IContainer container : expected) {
      calls.assertCall(this, DISCARD_CONTEXTS_IN, container);
    }
  }

  void assertNoCalls() {
    calls.assertNoCalls();
    ((MockContext) getDefaultContext()).assertNoCalls();
    for (PubFolder pubFolder : getPubFolders()) {
      ((MockContext) pubFolder.getContext()).assertNoCalls();
    }
  }

  void assertPackagesRemoved(MockContainer pubContainer) {
    PubFolder pubFolder = getPubFolder(pubContainer);
    ChangeSet expected = new ChangeSet();
    expected.removedContainer(pubFolder.getInvertedSourceContainer());
    assertChanged(pubContainer, expected);
  }

  void assertPubspecAdded(IContainer... expected) {
    for (IContainer container : expected) {
      calls.assertCall(this, PUBSPEC_ADDED, container);
    }
  }

  void assertPubspecRemoved(IContainer... expected) {
    for (IContainer container : expected) {
      calls.assertCall(this, PUBSPEC_REMOVED, container);
    }
  }

  private MockContext getContextFor(IContainer pubFolderResource) {
    MockContext context;
    if (pubFolderResource != null) {
      PubFolder pubFolder = getPubFolder(pubFolderResource);
      DeltaProcessorTest.assertNotNull(pubFolder);
      DeltaProcessorTest.assertSame(pubFolderResource, pubFolder.getResource());
      context = (MockContext) pubFolder.getContext();
    } else {
      PubFolder pubFolder = getPubFolder(getResource());
      DeltaProcessorTest.assertNull(pubFolder);
      context = (MockContext) getDefaultContext();
    }
    return context;
  }
}
