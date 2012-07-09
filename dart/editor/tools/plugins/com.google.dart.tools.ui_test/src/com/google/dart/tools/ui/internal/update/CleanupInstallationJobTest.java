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
package com.google.dart.tools.ui.internal.update;

import com.google.dart.tools.update.core.internal.jobs.CleanupInstallationJob;

import junit.framework.TestCase;

public class CleanupInstallationJobTest extends TestCase {

  public void testSorting() {

    assertLessThan(
        "com.acme.bundle_0.9.0.v200803061810.jar",
        "com.acme.bundle_0.9.0.v200803061811.jar");

    assertLessThan("com.acme.bundle_0.9.0.v200803061810", "com.acme.bundle_0.9.0.v200803061811");

    assertLessThan(
        "com.acme.bundle_0.8.0.v200803061812.jar",
        "com.acme.bundle_0.9.0.v200803061811.jar");

    assertLessThan("com.acme.bundle_0.8.0.v200803061812", "com.acme.bundle_0.9.0.v200803061811");

    assertLessThan(
        "com.acme.bundle_0.1.2.201206291310.jar",
        "com.acme.bundle_0.1.11.201206291310.jar");

    assertLessThan("com.acme.bundle_0.1.2.201206291310", "com.acme.bundle_0.1.11.201206291310");

    assertEqualTo("com.acme.bundle_1.8.2.v20120109-1030", "com.acme.bundle_1.8.2.v20120109-1030");
    assertEqualTo("com.acme.bundle", "com.acme.bundle");
  }

  private void assertEqualTo(String bundle1, String bundle2) {
    assertEquals(0, CleanupInstallationJob.lexicalCompareBundleFileNames(bundle1, bundle2));
  }

  private void assertLessThan(String bundle1, String bundle2) {
    assertTrue(CleanupInstallationJob.lexicalCompareBundleFileNames(bundle1, bundle2) < 0);
  }

}
