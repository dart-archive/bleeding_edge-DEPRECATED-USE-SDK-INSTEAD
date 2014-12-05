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
package com.google.dart.eclipse.ui.internal.navigator;

import com.google.dart.tools.core.pub.PubCacheManager_NEW;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IProject;

/**
 * A test to check if the project represents a package in the pub cache
 */
public class PubCacheProjectPropertyTester extends PropertyTester {

  private static final String IS_IN_PUB_CACHE = "isInPubCache";

  @Override
  public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {

    if (IS_IN_PUB_CACHE.equalsIgnoreCase(property)) {
      if (receiver instanceof IProject) {
        return PubCacheManager_NEW.isPubCacheProject((IProject) receiver);
      }
    }
    return false;
  }

}
