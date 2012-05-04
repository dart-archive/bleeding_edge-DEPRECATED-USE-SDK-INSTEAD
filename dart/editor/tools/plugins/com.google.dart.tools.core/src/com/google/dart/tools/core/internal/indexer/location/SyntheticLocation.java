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
package com.google.dart.tools.core.internal.indexer.location;

import com.google.dart.indexer.locations.Location;
import com.google.dart.indexer.locations.LocationType;

import org.eclipse.core.resources.IFile;

import java.net.URI;
import java.util.HashMap;

public class SyntheticLocation implements Location {
  /**
   * A table mapping identifiers to the instances associated with those identifiers.
   */
  private static final HashMap<String, SyntheticLocation> InstanceMap = new HashMap<String, SyntheticLocation>();

  /**
   * The synthetic location representing the list of all Dart classes.
   */
  public static final SyntheticLocation ALL_CLASSES = new SyntheticLocation("allClasses");

  /**
   * The synthetic location representing the list of all Dart function type aliases.
   */
  public static final SyntheticLocation ALL_FUNCTION_TYPE_ALIASES = new SyntheticLocation(
      "allFunctionTypeAliases");

  /**
   * The synthetic location representing the list of all Dart interfaces.
   */
  public static final SyntheticLocation ALL_INTERFACES = new SyntheticLocation("allInterfaces");

  /**
   * Return the instance of this class with the given identifier, or <code>null</code> if there is
   * no such instance.
   * 
   * @param identifier the identifier used to uniquely identify the instance to be returned
   * @return the instance of this class with the given identifier
   */
  public static Location getInstance(String identifier) {
    return InstanceMap.get(identifier);
  }

  /**
   * The identifier used to uniquely identify an instance of this class.
   */
  private String identifier;

  /**
   * Initialize a newly created synthetic location to have the given identifier.
   * 
   * @param identifier the identifier used to uniquely identify an instance of this class
   */
  private SyntheticLocation(String identifier) {
    this.identifier = identifier;
    InstanceMap.put(identifier, this);
  }

  @Override
  @Deprecated
  public IFile getContainingFile() {
    return null;
  }

  @Override
  public URI getContainingUri() {
    return null;
  }

  @Override
  public LocationType getLocationType() {
    return SyntheticLocationType.getInstance();
  }

  @Override
  public String getSemiUniqueIdentifier() {
    return identifier;
  }
}
