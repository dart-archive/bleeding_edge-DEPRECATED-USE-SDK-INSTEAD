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
package com.google.dart.engine.internal.cache;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.source.TestSource;

public class SdkCachePartitionTest extends EngineTestCase {
  public void test_contains_false() {
    SdkCachePartition partition = new SdkCachePartition(null, 8);
    Source source = new TestSource();
    assertFalse(partition.contains(source));
  }

  public void test_contains_true() {
    SdkCachePartition partition = new SdkCachePartition(null, 8);
    SourceFactory factory = new SourceFactory(new DartUriResolver(
        DirectoryBasedDartSdk.getDefaultSdk()));
    Source source = factory.forUri("dart:core");
    assertTrue(partition.contains(source));
  }

  public void test_creation() {
    assertNotNull(new SdkCachePartition(null, 8));
  }
}
