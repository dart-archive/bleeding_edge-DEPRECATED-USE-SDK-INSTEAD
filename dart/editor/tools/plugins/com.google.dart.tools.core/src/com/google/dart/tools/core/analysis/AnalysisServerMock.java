package com.google.dart.tools.core.analysis;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;

import java.io.File;

/**
 * A mocked {@link AnalysisServer} for use when the {@link DartCoreDebug#ENABLE_NEW_ANALYSIS} flag
 * is set to true.
 * 
 * @see DartCore#createAnalysisServer()
 */
public class AnalysisServerMock implements AnalysisServer {

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
  public void reanalyze() {
    // No-op
  }

  @Override
  public boolean scan(File file, long milliseconds) {
    return false;
  }
}
