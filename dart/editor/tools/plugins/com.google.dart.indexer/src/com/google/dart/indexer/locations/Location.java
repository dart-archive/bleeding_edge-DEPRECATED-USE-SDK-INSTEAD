/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.indexer.locations;

import org.eclipse.core.resources.IFile;

/**
 * The interface <code>Location</code> defines the behavior of objects that represent the location
 * in which a reference to a program element occurs.
 */
public interface Location {
  public static final Location[] EMPTY_ARRAY = new Location[0];

  @Override
  boolean equals(Object obj);

  IFile getContainingFile();

  LocationType getLocationType();

  String getSemiUniqueIdentifier();

  @Override
  int hashCode();

  @Override
  String toString();
}
