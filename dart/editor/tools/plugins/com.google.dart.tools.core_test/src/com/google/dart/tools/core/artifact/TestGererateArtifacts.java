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
