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

import java.net.URI;

/**
 * The interface <code>Location</code> defines the behavior of objects that represent the location
 * in which a reference to a program element occurs.
 */
public interface Location {
  public static final Location[] EMPTY_ARRAY = new Location[0];

  @Override
  public boolean equals(Object obj);

  @Deprecated
  public IFile getContainingFile();

  /**
   * Return the URI of the file that contains this location, or <code>null</code> if this location
   * is not contained in a file.
   * 
   * @return the URI of the file that contains this location
   */
  public URI getContainingUri();

  public LocationType getLocationType();

  public String getSemiUniqueIdentifier();

  @Override
  public int hashCode();

  @Override
  public String toString();
}
