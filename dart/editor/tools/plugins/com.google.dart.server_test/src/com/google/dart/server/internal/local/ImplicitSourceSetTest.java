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

import com.google.dart.engine.source.Source;
import com.google.dart.server.SourceSet;
import com.google.dart.server.SourceSetKind;

import junit.framework.TestCase;

public class ImplicitSourceSetTest extends TestCase {
  public void test_ALL() throws Exception {
    SourceSet sourceSet = SourceSet.ALL;
    assertSame(SourceSetKind.ALL, sourceSet.getKind());
    assertSame(Source.EMPTY_ARRAY, sourceSet.getSources());
  }

  public void test_EXPLICITLY_ADDED() throws Exception {
    SourceSet sourceSet = SourceSet.EXPLICITLY_ADDED;
    assertSame(SourceSetKind.EXPLICITLY_ADDED, sourceSet.getKind());
    assertSame(Source.EMPTY_ARRAY, sourceSet.getSources());
  }

  public void test_NON_SDK() throws Exception {
    SourceSet sourceSet = SourceSet.NON_SDK;
    assertSame(SourceSetKind.NON_SDK, sourceSet.getKind());
    assertSame(Source.EMPTY_ARRAY, sourceSet.getSources());
  }
}
