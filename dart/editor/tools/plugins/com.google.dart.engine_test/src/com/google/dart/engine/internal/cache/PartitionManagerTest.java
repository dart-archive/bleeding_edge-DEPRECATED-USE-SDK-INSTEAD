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
import com.google.dart.engine.internal.sdk.MockDartSdk;
import com.google.dart.engine.sdk.DartSdk;

public class PartitionManagerTest extends EngineTestCase {
  public void test_clearCache() {
    PartitionManager manager = new PartitionManager();
    DartSdk sdk = new MockDartSdk();
    SdkCachePartition oldPartition = manager.forSdk(sdk);
    manager.clearCache();
    SdkCachePartition newPartition = manager.forSdk(sdk);
    assertNotSame(oldPartition, newPartition);
  }

  public void test_creation() {
    assertNotNull(new PartitionManager());
  }

  public void test_forSdk() {
    PartitionManager manager = new PartitionManager();

    DartSdk sdk1 = new MockDartSdk();
    SdkCachePartition partition1 = manager.forSdk(sdk1);
    assertNotNull(partition1);
    assertSame(partition1, manager.forSdk(sdk1));

    DartSdk sdk2 = new MockDartSdk();
    SdkCachePartition partition2 = manager.forSdk(sdk2);
    assertNotNull(partition2);
    assertSame(partition2, manager.forSdk(sdk2));
    assertNotSame(partition1, partition2);
  }
}
