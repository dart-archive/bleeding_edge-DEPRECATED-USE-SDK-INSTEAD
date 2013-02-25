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
package com.google.dart.indexer.storage.inmemory;

public abstract class AbstractStringEncoder {
  public abstract void encode(String str, ByteArray array);

  public boolean equals(ByteArray array, int position, String value) {
    String decode = decode(array, position);
    return decode.equals(value);
  }

  public int memUsed() {
    return 0;
  }

  abstract String decode(ByteArray array, int position);
}
