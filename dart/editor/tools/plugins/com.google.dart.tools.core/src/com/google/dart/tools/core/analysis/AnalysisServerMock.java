package com.google.dart.tools.core.analysis;

import com.google.dart.compiler.ast.LibraryUnit;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * A mocked {@link AnalysisServer} for use when the {@link DartCoreDebug#ENABLE_NEW_ANALYSIS} flag
 * is set to true.
 * 
 * @see DartCore#createAnalysisServer()
 */
public class AnalysisServerMock implements AnalysisServer {

  private final class EditContextMock extends EditContext {
    EditContextMock() {
      super(AnalysisServerMock.this, null, null);
    }

    @Override
    public Task getIdleTask() {
      return null;
    }

    @Override
    public Map<URI, LibraryUnit> getResolvedLibraries(long millseconds) {
      return new HashMap<URI, LibraryUnit>();
    }

  }

  private final class SavedContextMock extends SavedContext {
    SavedContextMock() {
      super(AnalysisServerMock.this, null);
    }

    @Override
    public void addAnalysisListener(AnalysisListener listener) {
      // No-op
    }

    @Override
    public Task getIdleTask() {
      return null;
    }

    @Override
    public Map<URI, LibraryUnit> getResolvedLibraries(long millseconds) {
      return new HashMap<URI, LibraryUnit>();
    }

    @Override
    public Context getSuggestedContext(File libFileOrDir) {
      return this;
    }

    @Override
    public void removeAnalysisListener(AnalysisListener listener) {
      // No-op
    }

  }

  @Override
  public void addIdleListener(IdleListener listener) {
    // No-op
  }

  @Override
  public void analyze(File libraryFile) {
    // No-op
  }

  @Override
  public void changed(File file) {
    // No-op
  }

  @Override
  public void discard(File file) {
    // No-op
  }

  @Override
  public String getAnalysisStatus(String message) {
    return message;
  }

  @Override
  public EditContext getEditContext() {
    return new EditContextMock();
  }

  @Override
  public SavedContext getSavedContext() {
    return new SavedContextMock();
  }

  @Override
  public File[] getTrackedLibraryFiles() {
    return new File[0];
  }

  @Override
  public boolean isIdle() {
    // No-op
    return false;
  }

  @Override
  public boolean isLibraryCached(File file) {
    return false;
  }

  @Override
  public boolean isLibraryResolved(File file) {
    return false;
  }

  @Override
  public void queueAnalyzeContext() {
    // No-op
  }

  @Override
  public void queueAnalyzeSubTask(File libraryFile) {
    // No-op
  }

  @Override
  public void queueAnalyzeSubTaskIfNew(File libraryFile) {
    // No-op
  }

  @Override
  public void queueNewTask(Task task) {
    // No-op
  }

  @Override
  public void queueSubTask(Task subtask) {
    // No-op
  }

  @Override
  public boolean readCache() {
    return false;
  }

  @Override
  public void reanalyze() {
    // No-op
  }

  @Override
  public void removeBackgroundTasks(File discarded) {
    // No-op
  }

  @Override
  public void removeIdleListener(IdleListener listener) {
    // No-op
  }

  @Override
  public boolean scan(File file, long milliseconds) {
    return false;
  }

  @Override
  public void scan(File file, ScanCallback callback) {
    // No-op

  }

  @Override
  public void start() {
    // No-op
  }

  @Override
  public void startIdleTaskProcessing() {
    // No-op
  }

  @Override
  public void stop() {
    // No-op
  }

  @Override
  public boolean waitForIdle(long milliseconds) {
    return false;
  }

  @Override
  public boolean writeCache() {
    return false;
  }

}
