/*
 * Copyright 2011 Dart project authors.
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

import org.eclipse.core.runtime.Platform;

import java.io.File;

public class SystemLibraryManagerProviderVmTest extends SystemLibraryManagerProviderTest {

  public void test_SystemLibraryManagerProvider_builtin() throws Exception {
    testLibrary("builtin", "builtin_runtime.dart");
  }

  public void test_SystemLibraryManagerProvider_core() throws Exception {
    testLibrary("core", "core_runtime.dart");
  }

  public void test_SystemLibraryManagerProvider_coreImpl() throws Exception {
    testLibrary("coreimpl", "coreimpl_runtime.dart");
  }

  public void test_SystemLibraryManagerProvider_dom() throws Exception {
    testLibrary("dom", "dom.dart");
  }

  public void test_SystemLibraryManagerProvider_html() throws Exception {
    testLibrary("html", "html.dart");
  }

  public void test_SystemLibraryManagerProvider_htmlImpl() throws Exception {
    testLibrary("htmlimpl", "htmlimpl.dart");
  }

  @Override
  protected EditorLibraryManager getLibraryManager() {
    return SystemLibraryManagerProvider.getVmLibraryManager();
  }

  @Override
  protected void setUp() throws Exception {
    getLibDir().renameTo(getUnusedLibDir());
  }

  @Override
  protected void tearDown() throws Exception {
    getUnusedLibDir().renameTo(getLibDir());
  }

  private File getInstallDir() {
    return new File(Platform.getInstallLocation().getURL().getFile());
  }

  private File getLibDir() {
    return new File(getInstallDir(), "libraries");
  }

  private File getUnusedLibDir() {
    return new File(getInstallDir(), "libraries-unused");
  }
}
