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

package com.google.dart.server.internal.asserts;

import com.google.dart.engine.source.Source;
import com.google.dart.server.generated.types.NavigationRegion;

import junit.framework.Assert;

import org.apache.commons.lang3.StringUtils;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * A helper for validating {@link NavigationRegion}s.
 */
public class NavigationRegionsAssert {
  static int findOffset(Source source, String search) throws Exception {
    String content = source.getContents().getData().toString();
    int offset = content.indexOf(search);
    assertTrue("Cannot find '" + search + "' in " + source, offset >= 0);
    return offset;
  }

  private final NavigationRegion[] regions;

  public NavigationRegionsAssert(NavigationRegion[] regions) {
    this.regions = regions;
  }

  /**
   * Returns the {@link NavigationRegion} that starts with the given "search" and has the given
   * length.
   */
  public NavigationRegion findRegion(Source source, String search, int length) throws Exception {
    int offset = findOffset(source, search);
    return findRegion(offset, length);
  }

  /**
   * Verifies that there are no {@link NavigationRegion} that starts with the given "search" and has
   * the given length.
   */
  public void hasNoRegion(Source source, String search, int length) throws Exception {
    NavigationRegion region = findRegion(source, search, length);
    if (region != null) {
      Assert.fail("Unexpected region\n'" + search + "' with length=" + length + " in\n"
          + StringUtils.join(regions, "\n"));
    }
  }

//  /**
//   * Finds the {@link NavigationRegion} that for the given "search", validates that it exists and
//   * returns the corresponding {@link ElementAssert}.
//   */
//  public ElementAssert hasRegion(Source source, String search) throws Exception {
//    return hasRegion(source, search, search.length());
//  }
//
//  /**
//   * Finds the {@link NavigationRegion} that starts with the given "search" and has the given
//   * length, validates that it exists and returns the corresponding {@link ElementAssert}.
//   */
//  public ElementAssert hasRegion(Source source, String search, int length) throws Exception {
//    NavigationRegion region = findRegion(source, search, length);
//    if (region == null) {
//      Assert.fail("Cannot find\n'" + search + "' with length=" + length + " in\n"
//          + StringUtils.join(regions, "\n"));
//    }
//    Element target = region.getTargets()[0];
//    return new ElementAssert(target);
//  }

  public void isEmpty() {
    assertThat(regions).describedAs("Navigation regions").isNullOrEmpty();
  }

  public void isNotEmpty() {
    assertThat(regions).describedAs("Navigation regions").isNotEmpty();
  }

  /**
   * Asserts that there are no {@link NavigationRegion} containing the offset of "search".
   */
  public void noRegionAt(Source source, String search) throws Exception {
    int offset = findOffset(source, search);
    NavigationRegion region = findRegionContaining(offset);
    if (region != null) {
      Assert.fail("Unexpected\n" + region + "\nat \n'" + search + "'\nin\n"
          + StringUtils.join(regions, "\n"));
    }
  }

  private NavigationRegion findRegion(int offset, int length) {
    for (NavigationRegion region : regions) {
      if (region.getOffset() == offset && region.getLength() == length) {
        return region;
      }
    }
    return null;
  }

  private NavigationRegion findRegionContaining(int offset) {
    for (NavigationRegion region : regions) {
      if (region.containsInclusive(offset)) {
        return region;
      }
    }
    return null;
  }
}
