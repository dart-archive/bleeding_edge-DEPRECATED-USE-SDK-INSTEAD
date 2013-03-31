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
package com.google.dart.engine.internal.index;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.index.Location;
import com.google.dart.engine.utilities.collection.FastRemoveList;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ContributedLocationTest extends EngineTestCase {
  public void test_new() throws Exception {
    FastRemoveList<ContributedLocation> declarationOwner = FastRemoveList.newInstance();
    FastRemoveList<ContributedLocation> locationOwner = FastRemoveList.newInstance();
    Location location = mock(Location.class);
    ContributedLocation contributedLocation = new ContributedLocation(
        declarationOwner,
        locationOwner,
        location);
    assertSame(location, contributedLocation.getLocation());
    assertThat(declarationOwner).containsOnly(contributedLocation);
    assertThat(locationOwner).containsOnly(contributedLocation);
    // remove from "declaration"
    contributedLocation.removeFromDeclarationOwner();
    assertThat(declarationOwner).isEmpty();
    assertThat(locationOwner).containsOnly(contributedLocation);
    // remove from "location"
    contributedLocation.removeFromLocationOwner();
    assertThat(declarationOwner).isEmpty();
    assertThat(locationOwner).isEmpty();
  }
}
