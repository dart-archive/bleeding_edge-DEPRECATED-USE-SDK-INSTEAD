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

package com.google.dart.engine.services.internal.correction;

/**
 * Test for {@link URIUtils}.
 */
public class URIUtilsTest extends AbstractDartTest {
  public void test_computeRelativePath_same() throws Exception {
    assertEquals(null, URIUtils.computeRelativePath("", ""));
    assertEquals(null, URIUtils.computeRelativePath("/", "/"));
    assertEquals(null, URIUtils.computeRelativePath("/the/same/path", "/the/same/path"));
  }

  public void test_computeRelativePath_targetInSameFolder() throws Exception {
    assertEquals("file.txt", URIUtils.computeRelativePath("/same/folder", "/same/folder/file.txt"));
  }

  public void test_computeRelativePath_targetInSubFolder() throws Exception {
    assertEquals(
        "sub/file.txt",
        URIUtils.computeRelativePath("/common/root/", "/common/root/sub/file.txt"));
  }

  public void test_computeRelativePath_targetInSuperFolder() throws Exception {
    assertEquals(
        "../file.txt",
        URIUtils.computeRelativePath("/common/root/sub", "/common/root/file.txt"));
  }

  public void test_computeRelativePath_targetInSuperSubFolder() throws Exception {
    assertEquals(
        "../bbb/ccc/file.txt",
        URIUtils.computeRelativePath("/common/root/aaa", "/common/root/bbb/ccc/file.txt"));
  }
}
