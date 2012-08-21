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

import com.google.dart.compiler.PackageLibraryManager;

public class PackageLibraryManagerProviderDartCTest extends PackageLibraryManagerProviderTest {

  public void test_PackageLibraryManagerProvider_expandCore() throws Exception {
    testLibrary("core", "corelib.dart");
  }

  public void test_PackageLibraryManagerProvider_expandCoreImpl() throws Exception {
    testLibrary("coreimpl", "corelib_impl.dart");
  }

  public void test_PackageLibraryManagerProvider_expandDom() throws Exception {
    testLibrary("dom", "dom.dart");
  }

  public void test_PackageLibraryManagerProvider_expandHtml() throws Exception {
    testLibrary("html", "html.dart");
  }

  @Override
  protected PackageLibraryManager getLibraryManager() {
    return PackageLibraryManagerProvider.getAnyLibraryManager();
  }
}
