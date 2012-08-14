/*
 * Copyright 2012 Dart project authors.
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
package com.google.dart.tools.core.internal.model;

import com.google.dart.compiler.SystemLibraryManager;

import java.io.File;

public class SystemLibraryManagerProviderAnyTest extends SystemLibraryManagerProviderTest {

  public void test_SystemLibraryManagerProvider_builtin() throws Exception {
    testLibrary("builtin", "builtin_runtime.dart");
  }

  public void test_SystemLibraryManagerProvider_core() throws Exception {
    testLibrary("core", "core_runtime.dart");
  }

  public void test_SystemLibraryManagerProvider_coreImpl() throws Exception {
    testLibrary("coreimpl", "coreimpl_runtime.dart");
  }

  public void test_SystemLibraryManagerProvider_html() throws Exception {
    testLibrary("html", "html_dartium.dart");
  }

  public void test_SystemLibraryManagerProvider_io() throws Exception {
    testLibrary("io", "io_runtime.dart");
  }

  public void test_SystemLibraryManagerProvider_isolate() throws Exception {
    testLibrary("isolate", "isolate_compiler.dart");
  }

  public void test_SystemLibraryManagerProvider_json() throws Exception {
    testLibrary("json", "json.dart");
  }

  public void test_SystemLibraryManagerProvider_package() throws Exception {
    String fileName = "ui" + File.separator + "lib.dart";
    testPackage(fileName, "package:" + fileName);
  }

  public void test_SystemLibraryManagerProvider_uri() throws Exception {
    testLibrary("uri", "uri.dart");
  }

  public void test_SystemLibraryManagerProvider_utf() throws Exception {
    testLibrary("utf", "utf.dart");
  }

  @Override
  protected SystemLibraryManager getLibraryManager() {
    return SystemLibraryManagerProvider.getAnyLibraryManager();
  }
}
