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
package com.google.dart.server.timing;

import java.io.File;
import java.util.ArrayList;

/**
 * The abstract class {@code TimingTest} defines the behavior of objects that measure the time
 * required to perform some operation.
 */
public abstract class TimingTest {
  /**
   * The name of the test.
   */
  private String name;

  /**
   * The number of times the test will be performed in order to warm up the VM.
   */
  private static final int DEFAULT_WARMUP_COUNT = 10;

  /**
   * The number of times the test will be performed in order to compute a time.
   */
  private static final int DEFAULT_TIMING_COUNT = 10;

  /**
   * The file suffix used to identify Dart files.
   */
  private static final String DART_SUFFIX = ".dart";

  /**
   * The file suffix used to identify HTML files.
   */
  private static final String HTML_SUFFIX = ".html";

  /**
   * Initialize a newly created test to have the given name.
   */
  public TimingTest(String name) {
    this.name = name;
  }

  /**
   * Return the name of the test.
   */
  public String getName() {
    return name;
  }

  /**
   * Return the number of times the test will be performed in order to compute a time.
   */
  public int getTimingCount() {
    return DEFAULT_TIMING_COUNT;
  }

  /**
   * Return the number of times the test will be performed in order to warm up the VM.
   */
  public int getWarmupCount() {
    return DEFAULT_WARMUP_COUNT;
  }

  /**
   * Return the number of milliseconds required to perform the operation the specified number of
   * times.
   */
  public TimingResult run() throws Exception {
    long[] times;
    oneTimeSetUp();
    try {
      int warmupCount = getWarmupCount();
      for (int i = 0; i < warmupCount; i++) {
        setUp();
        try {
          perform();
        } finally {
          tearDown();
        }
      }
      int timingCount = getTimingCount();
      times = new long[timingCount];
      for (int i = 0; i < timingCount; i++) {
        setUp();
        long startTime;
        long endTime;
        try {
          startTime = System.nanoTime();
          perform();
        } finally {
          endTime = System.nanoTime();
          tearDown();
        }
        times[i] = endTime - startTime;
      }
    } finally {
      oneTimeTearDown();
    }
    return new TimingResult(times);
  }

  /**
   * Return the number of milliseconds required to perform the operation one time.
   */
  public TimingResult runOnce() throws Exception {
    long time;
    oneTimeSetUp();
    try {
      setUp();
      long startTime;
      long endTime;
      try {
        startTime = System.nanoTime();
        perform();
      } finally {
        endTime = System.nanoTime();
        tearDown();
      }
      time = endTime - startTime;
    } finally {
      oneTimeTearDown();
    }
    return new TimingResult(new long[] {time});
  }

  /**
   * Build a path relative to the given directory with the given components.
   * 
   * @param directory the directory that the components are relative to
   * @param components the components used to extend the path to the directory
   * @return the path that was constructed
   */
  protected String buildPath(File directory, String[] components) {
    for (int i = 0; i < components.length; i++) {
      directory = new File(directory, components[i]);
    }
    return directory.getAbsolutePath();
  }

  /**
   * Return a list of all of the Dart and HTML files in the given directory.
   * 
   * @param directory the directory being searched
   */
  protected ArrayList<File> computeFiles(File directory) {
    ArrayList<File> files = new ArrayList<File>();
    addAllFiles(files, directory);
    return files;
  }

  /**
   * Perform any operations that need to be performed once before any iterations.
   * 
   * @throws Exception if the set-up could not be performed
   */
  protected void oneTimeSetUp() throws Exception {
  }

  /**
   * Perform any operations that need to be performed once after all iterations.
   */
  protected void oneTimeTearDown() {
  }

  /**
   * Perform any operations that part of a single iteration. It is the execution of this method that
   * will be measured.
   */
  protected abstract void perform();

  /**
   * Perform any operations that need to be performed before each iteration.
   */
  protected void setUp() {
  }

  /**
   * Perform any operations that need to be performed after each iteration.
   */
  protected void tearDown() {
  }

  /**
   * Recursively add all of the Dart and HTML files in the given directory to the given list of
   * files.
   * 
   * @param files the list to which the files are to be added
   * @param directory the directory being searched
   */
  private void addAllFiles(ArrayList<File> files, File directory) {
    File[] children = directory.listFiles();
    int childCount = children.length;
    for (int i = 0; i < childCount; i++) {
      File child = children[i];
      String name = child.getName();
      if (child.isDirectory()) {
        if (!name.equals("packages")) {
          addAllFiles(files, child);
        }
      } else if (name.endsWith(DART_SUFFIX) || name.endsWith(HTML_SUFFIX)) {
        files.add(child);
      }
    }
  }
}
