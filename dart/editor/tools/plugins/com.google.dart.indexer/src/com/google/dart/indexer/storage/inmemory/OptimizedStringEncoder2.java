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

public class OptimizedStringEncoder2 extends AbstractStringEncoder {
  SimpleStringPool packagePool = new SimpleStringPool(1000);
  PlainStringEncoder encoder = new PlainStringEncoder();

  public OptimizedStringEncoder2() {

  }

  public OptimizedStringEncoder2(SimpleStringPool str) {
    this.packagePool = str;
  }

  @Override
  public String decode(ByteArray array, int position) {
    int int1 = array.getBuffer().getInt(position);
    if (int1 == -2) {
      String decode = encoder.decode(array, position + 4);
      return decode;
    }
    StringBuilder builder = new StringBuilder();
    String string = packagePool.getString(int1);
    builder.append(string);
    String decode = encoder.decode(array, position + 4);
    builder.append(decode);
    return builder.toString();
  }

  @Override
  public void encode(String str, ByteArray array) {
    String JAVA = ".java[";
    String CLASS = ".class[";
    int lastIndexOf = str.indexOf(JAVA);
    if (lastIndexOf != -1) {
      lastIndexOf += JAVA.length();
      try {
        String substring = str.substring(0, lastIndexOf);
        int add = packagePool.add(substring);
        array.addInt(add);
        String remaining = str.substring(lastIndexOf);
        encoder.encode(remaining, array);
        return;
      } catch (Exception exception) {
        IndexerPlugin.getLogger().logError(exception);
      }
    } else {
      lastIndexOf = str.indexOf(CLASS);
      if (lastIndexOf != -1) {
        lastIndexOf += CLASS.length();
        try {
          String substring = str.substring(0, lastIndexOf);
          int add = packagePool.add(substring);
          array.addInt(add);
          String remaining = str.substring(lastIndexOf);
          encoder.encode(remaining, array);
          return;
        } catch (Exception exception) {
          IndexerPlugin.getLogger().logError(exception);
        }
      } else {
        array.addInt(-2);
        String remaining = str;
        encoder.encode(remaining, array);
        return;
      }

    }
    throw new IllegalArgumentException();
  }

  @Override
  public int memUsed() {
    return packagePool.memUsed();
  }
}
