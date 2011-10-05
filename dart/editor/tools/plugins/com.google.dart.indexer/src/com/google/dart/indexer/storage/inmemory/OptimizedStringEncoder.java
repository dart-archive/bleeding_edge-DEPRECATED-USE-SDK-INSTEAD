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

import com.google.dart.indexer.IndexerPlugin;

public class OptimizedStringEncoder extends AbstractStringEncoder {
  SimpleStringPool packagePool = new SimpleStringPool(1000);
  SimpleStringPool typePool = new SimpleStringPool(1000);
  PlainStringEncoder encoder = new PlainStringEncoder();

  @Override
  public String decode(ByteArray array, int position) {
    int int1 = array.getBuffer().getInt(position);
    StringBuilder builder = new StringBuilder();
    String string = packagePool.getString(int1);
    builder.append(string);
    int int2 = array.getBuffer().getInt(position + 4);
    builder.append(typePool.getString(int2));
    if (string.charAt(string.length() - 1) == '{') {
      builder.append(".java[");
    } else {
      builder.append(".class[");
    }
    String decode = encoder.decode(array, position + 8);
    builder.append(decode);
    return builder.toString();
  }

  @Override
  public void encode(String str, ByteArray array) {
    int lastIndexOf = str.indexOf('{');
    if (lastIndexOf != -1) {
      lastIndexOf++;
      try {
        String substring = str.substring(0, lastIndexOf);
        int add = packagePool.add(substring);
        array.addInt(add);
        int typeEnd = str.indexOf(".java[", lastIndexOf);
        if (typeEnd != -1) {
          String tName = str.substring(lastIndexOf, typeEnd);

          int add2 = typePool.add(tName);
          array.addInt(add2);
          typeEnd += 6;
          String remaining = str.substring(typeEnd);
          encoder.encode(remaining, array);
          return;
        }
      } catch (Exception exception) {
        IndexerPlugin.getLogger().logError(exception);
      }
    } else {
      lastIndexOf = str.indexOf('(');
      if (lastIndexOf != -1) {
        try {
          lastIndexOf++;
          String substring = str.substring(0, lastIndexOf);
          int add = packagePool.add(substring);
          array.addInt(add);

          int typeEnd = str.indexOf(".class[", lastIndexOf);
          if (typeEnd != -1) {
            String tName = str.substring(lastIndexOf, typeEnd);
            int add2 = typePool.add(tName);
            array.addInt(add2);
            typeEnd += 7;
            String remaining = str.substring(typeEnd);
            encoder.encode(remaining, array);
            return;
          }
        } catch (Exception exception) {
          IndexerPlugin.getLogger().logError(exception);
        }
      }
    }
    throw new IllegalArgumentException();
  }

  @Override
  public int memUsed() {
    return packagePool.memUsed() + typePool.memUsed();
  }
}
