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

package com.google.dart.server.internal.local;

import com.google.common.collect.ImmutableMap;

import junit.framework.TestCase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Map;

public class CreateContextOperationTest extends TestCase {
  private LocalAnalysisServerImpl server = mock(LocalAnalysisServerImpl.class);

  public void test_perform() throws Exception {
    Map<String, String> packageMap = ImmutableMap.of("pkg-1", "dir-1");
    CreateContextOperation operation = new CreateContextOperation("id", "my-dir", packageMap);
    assertEquals("id", operation.getContextId());
    assertSame(ServerOperationPriority.SERVER, operation.getPriority());
    // perform
    operation.performOperation(server);
    verify(server, times(1)).internalCreateContext("id", "my-dir", packageMap);
  }
}
