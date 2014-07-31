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
package com.google.dart.server.internal;

import com.google.dart.server.Parameter;
import com.google.dart.server.utilities.general.ObjectUtilities;

/**
 * An implementation of {@link Parameter}.
 * 
 * @coverage dart.server
 */
public class ParameterImpl implements Parameter {
  private final String type;
  private final String name;

  public ParameterImpl(String type, String name) {
    this.type = type;
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || !(o instanceof ParameterImpl)) {
      return false;
    }
    ParameterImpl other = (ParameterImpl) o;
    return ObjectUtilities.equals(other.type, type) && ObjectUtilities.equals(other.name, name);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[type=");
    builder.append(type);
    builder.append(", name=");
    builder.append(name);
    builder.append("]");
    return builder.toString();
  }
}
