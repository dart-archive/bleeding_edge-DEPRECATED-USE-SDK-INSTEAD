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

import com.google.dart.engine.index.Relationship;

/**
 * A helper that encodes/decodes {@link Relationship}s to/from integers.
 * 
 * @coverage dart.engine.index
 */
public class RelationshipCodec {
  private final StringCodec stringCodec;

  public RelationshipCodec(StringCodec stringCodec) {
    this.stringCodec = stringCodec;
  }

  public Relationship decode(int idIndex) {
    String id = stringCodec.decode(idIndex);
    return Relationship.getRelationship(id);
  }

  public int encode(Relationship relationship) {
    String id = relationship.getIdentifier();
    return stringCodec.encode(id);
  }
}
