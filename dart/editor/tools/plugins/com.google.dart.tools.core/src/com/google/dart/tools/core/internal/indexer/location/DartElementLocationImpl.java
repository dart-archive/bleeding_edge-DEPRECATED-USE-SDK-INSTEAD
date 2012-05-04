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

import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.SourceRange;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import java.net.URI;

public abstract class DartElementLocationImpl implements DartElementLocation {
  /**
   * The kind of reference represented by this location, or <code>null</code> if this location
   * doesn't represent a reference to an element.
   */
  private ReferenceKind referenceKind;

  /**
   * The source range associated with this location.
   */
  private SourceRange sourceRange;

  /**
   * Initialize a newly created location to have the given source range.
   * 
   * @param sourceRange the source range associated with this location
   */
  public DartElementLocationImpl(SourceRange sourceRange) {
    this.sourceRange = sourceRange;
  }

  @Override
  @Deprecated
  public IFile getContainingFile() {
    try {
      IResource resource = getDartElement().getUnderlyingResource();
      if (resource != null && resource.getType() == IResource.FILE) {
        return (IFile) resource;
      }
    } catch (DartModelException exception) {
      // Could not get the underlying resource for some reason
    }
    return null;
  }

  @Override
  public URI getContainingUri() {
    try {
      IResource resource = getDartElement().getUnderlyingResource();
      if (resource != null && resource.getType() == IResource.FILE) {
        return resource.getLocationURI();
      }
    } catch (DartModelException exception) {
      // Could not get the underlying resource for some reason
    }
    return null;
  }

  @Override
  public ReferenceKind getReferenceKind() {
    return referenceKind;
  }

  @Override
  public String getSemiUniqueIdentifier() {
    StringBuilder builder = new StringBuilder();
    writeUniqueIdentifier(builder);
    return builder.toString();
  }

  @Override
  public SourceRange getSourceRange() {
    return sourceRange;
  }

  /**
   * Set the kind of reference represented by this location to the given kind.
   * 
   * @param referenceKind the kind of reference represented by this location
   */
  public void setReferenceKind(ReferenceKind referenceKind) {
    this.referenceKind = referenceKind;
  }

  /**
   * Use the given string builder to create the unique identifier for this location. The unique
   * identifier has the following format:
   * 
   * <pre>
   *    <i>uniqueIdentifier</i> ::= <i>elementMemento</i> ':' <i>additionalData</i>
   *    <i>elementMemento</i> ::= String
   *    <i>additionalData</i> ::= <i>sourceInfo</i>  [ ';' <i>referenceKind</i> ]
   *    <i>sourceInfo</i> ::= <i>sourceOffset</i> ',' <i>sourceLength</i>
   *    <i>sourceOffset</i> ::= int
   *    <i>sourceLength</i> ::= int
   *    <i>referenceKind</i> ::= char
   * </pre>
   * 
   * @param builder the string builder to be used to create the unique identifier
   */
  private void writeUniqueIdentifier(StringBuilder builder) {
    builder.append(getDartElement().getHandleIdentifier());
    builder.append(':');
    SourceRange range = getSourceRange();
    if (range == null) {
      builder.append("0,0");
    } else {
      builder.append(range.getOffset());
      builder.append(',');
      builder.append(range.getLength());
    }
    ReferenceKind kind = getReferenceKind();
    if (kind != null) {
      builder.append(';');
      builder.append(kind.getCode());
    }
  }
}
