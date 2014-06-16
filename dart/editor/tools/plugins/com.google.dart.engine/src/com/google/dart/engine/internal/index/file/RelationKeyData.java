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
package com.google.dart.engine.internal.index.file;

import com.google.dart.engine.element.Element;
import com.google.dart.engine.index.Location;
import com.google.dart.engine.index.Relationship;

/**
 * An {@link Element} to {@link Location} relation key.
 * 
 * @coverage dart.engine.index
 */
public class RelationKeyData {
  final int elementId;
  final int relationshipId;

  public RelationKeyData(ElementCodec elementCodec, RelationshipCodec relationshipCodec,
      Element element, Relationship relationship) {
    this.elementId = elementCodec.encode(element);
    this.relationshipId = relationshipCodec.encode(relationship);
  }

  public RelationKeyData(int elementId, int relationshipId) {
    this.elementId = elementId;
    this.relationshipId = relationshipId;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof RelationKeyData)) {
      return false;
    }
    RelationKeyData other = (RelationKeyData) obj;
    return other.elementId == elementId && other.relationshipId == relationshipId;
  }

  @Override
  public int hashCode() {
    return 31 * elementId + relationshipId;
  }
}
