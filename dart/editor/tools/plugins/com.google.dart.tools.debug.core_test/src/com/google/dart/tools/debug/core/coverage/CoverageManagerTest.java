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

package com.google.dart.tools.debug.core.coverage;

import com.google.dart.tools.core.utilities.io.FileUtilities;

import junit.framework.TestCase;

import org.fest.assertions.MapAssert;
import org.json.JSONObject;

import static org.fest.assertions.Assertions.assertThat;

import java.io.File;
import java.util.TreeMap;

public class CoverageManagerTest extends TestCase {
  public void test_createTempDir() throws Exception {
    String tempDir = CoverageManager.createTempDir();
    File tempDirFile = new File(tempDir);
    FileUtilities.delete(tempDirFile);
  }

  public void test_parseHitMap() throws Exception {
    JSONObject jsonObject = new JSONObject("{'hits': [1, 10, 2, 20, 3, 5, 3, 30]}");
    TreeMap<Integer, Integer> hitMap = CoverageManager.parseHitMap(jsonObject);
    assertThat(hitMap).hasSize(3);
    assertThat(hitMap).includes(MapAssert.entry(1, 10));
    assertThat(hitMap).includes(MapAssert.entry(2, 20));
    assertThat(hitMap).includes(MapAssert.entry(3, 35));
  }
}
