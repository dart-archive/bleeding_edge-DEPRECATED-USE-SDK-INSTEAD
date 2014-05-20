package com.google.dart.tools.core.internal.model;

import java.io.File;
import java.io.IOException;

public final class MockIgnoreFile extends DartIgnoreFile {

  public MockIgnoreFile() {
    super(new File("/tmp/does/not/exist/ignores.txt"));
  }

  @Override
  public void initFile() throws IOException {
    // do not write to disk
  }

  @Override
  public DartIgnoreFile load() throws IOException {
    // do not read from disk
    return this;
  }

  @Override
  public DartIgnoreFile store() throws IOException {
    // do not write to disk
    return this;
  }
}
