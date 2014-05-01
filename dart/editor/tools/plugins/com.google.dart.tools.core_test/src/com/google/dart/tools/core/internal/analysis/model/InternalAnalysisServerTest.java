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
package com.google.dart.tools.core.internal.analysis.model;

import com.google.dart.server.AnalysisServer;
import com.google.dart.server.InternalAnalysisServer;
import com.google.dart.tools.core.DartCore;

import junit.framework.TestCase;

/**
 * Test the temporary interface used to connect {@link AnalysisServer} to older functionality that
 * has yet to be ported.
 */
public class InternalAnalysisServerTest extends TestCase {

  final InternalAnalysisServer server = ((InternalAnalysisServer) DartCore.getAnalysisServer());

  public void test_getContextMap() {
    assertNotNull(server.getContextMap());
  }

  public void test_getIndex() throws Exception {
    assertNotNull(server.getIndex());
    assertSame(DartCore.getProjectManager().getIndex(), server.getIndex());
  }
}
