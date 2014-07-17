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

import com.google.dart.server.Location;
import com.google.dart.server.RefactoringProblem;
import com.google.dart.server.RefactoringProblemSeverity;
import com.google.dart.server.utilities.general.ObjectUtilities;

/**
 * A concrete implementation of {@link RefactoringProblem}.
 * 
 * @coverage dart.server
 */
public class RefactoringProblemImpl implements RefactoringProblem {

  private final RefactoringProblemSeverity severity;
  private final String message;
  private final Location location;

  public RefactoringProblemImpl(RefactoringProblemSeverity severity, String message,
      Location location) {
    this.severity = severity;
    this.message = message;
    this.location = location;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof RefactoringProblemImpl) {
      RefactoringProblemImpl other = (RefactoringProblemImpl) obj;
      return other.severity == severity && ObjectUtilities.equals(other.message, message)
          && ObjectUtilities.equals(other.location, location);
    }
    return false;
  }

  @Override
  public Location getLocation() {
    return location;
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public RefactoringProblemSeverity getSeverity() {
    return severity;
  }

  @Override
  public int hashCode() {
    int hash = message.hashCode();
    hash = hash * 31 + location.hashCode();
    hash = hash * 31 + severity.ordinal();
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[severity=");
    builder.append(severity.name());
    builder.append(", message=");
    builder.append(message);
    builder.append(", location=");
    builder.append(location);
    builder.append("]");
    return builder.toString();
  }

}
