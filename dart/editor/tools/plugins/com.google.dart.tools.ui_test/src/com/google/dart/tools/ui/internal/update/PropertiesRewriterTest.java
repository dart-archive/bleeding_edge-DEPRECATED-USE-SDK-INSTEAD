/*
 * Copyright (c) 2015, the Dart project authors.
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

import com.google.dart.tools.update.core.internal.jobs.PropertiesRewriter;

import junit.framework.TestCase;

import java.util.Properties;

public class PropertiesRewriterTest extends TestCase {

  public void testMergeNoOverrides() {
    Properties orig = new Properties();
    orig.put("update.core.url", "/editor/update/channels/be/");

    Properties latest = new Properties();
    latest.put("dart.dartium", "/dart/dartium/");

    Properties merged = PropertiesRewriter.merge(orig, latest);
    assertEquals(merged.size(), 2);
    assertEquals("/editor/update/channels/be/", merged.getProperty("update.core.url"));
    assertEquals("/dart/dartium/", merged.getProperty("dart.dartium"));
  }

  public void testMergeWithOverrides() {
    Properties orig = new Properties();
    orig.put("update.core.url", "/editor/update/channels/be/");
    orig.put("dart.dartium", "/dart/dartium/");

    Properties latest = new Properties();
    latest.put("dart.dartium", "/my/dartium/");

    Properties merged = PropertiesRewriter.merge(orig, latest);
    assertEquals(merged.size(), 2);
    assertEquals("/editor/update/channels/be/", merged.getProperty("update.core.url"));
    assertEquals("/dart/dartium/", merged.getProperty("dart.dartium"));
  }

  public void testNoMerges() {
    Properties orig = new Properties();
    orig.put("update.core.url", "/editor/update/channels/be/");
    orig.put("dart.dartium", "/dart/dartium/");

    Properties latest = new Properties();

    Properties merged = PropertiesRewriter.merge(orig, latest);
    assertEquals(merged.size(), 2);
    assertEquals("/editor/update/channels/be/", merged.getProperty("update.core.url"));
    assertEquals("/dart/dartium/", merged.getProperty("dart.dartium"));
  }

}
