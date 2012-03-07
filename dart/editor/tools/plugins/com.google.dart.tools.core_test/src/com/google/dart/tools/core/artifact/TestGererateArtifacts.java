// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.dart.tools.core.artifact;

import junit.framework.TestCase;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author mrrussell@google.com (Mark Russell)
 */
public class TestGererateArtifacts extends TestCase {

  public void test_write_artifacts() throws Exception {
    String extraDirName = System.getProperty("build.extra.artifacts", "");
    if (extraDirName.length() > 0) {
      System.out.println("extraDirName is " + extraDirName);
      File baseDir = new File(extraDirName);
      baseDir.mkdirs();
      File baseFile = new File(baseDir, "base.txt");
      File stagedDir = new File(baseDir, "stage");
      File stageFile = new File(stagedDir, "stage.txt");
      File samplesDir = new File(baseDir, "samples");
      File sampleFile = new File(samplesDir, "sample.txt");
      stagedDir.mkdirs();
      samplesDir.mkdirs();
      stageFile.createNewFile();
      sampleFile.createNewFile();
      baseFile.createNewFile();
      createFile(baseFile, "name");
      createFile(sampleFile, "name");
      createFile(stageFile, "name");
    } else {
      System.out.println("extraDirName is zero length");
    }
  }

  /**
   * @param baseFile
   * @throws IOException
   */
  private void createFile(File baseFile, String name) throws IOException {
    BufferedWriter w = null;
    try {
      w = new BufferedWriter(new FileWriter(baseFile));
      w.write("test string for " + name);
    } finally {
      if (w != null) {
        try {
          w.close();
        } catch (IOException e) {
          //intentionally ignored
        }
      }
    }
  }
}
