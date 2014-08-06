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
package com.google.dart.tools.ui.text.editor.tmp;

import com.google.dart.tools.core.internal.util.Util;
import com.google.dart.tools.ui.DartX;

/**
 * This class is defined to reduce the error count.
 * <p>
 * All references to it and its methods need to be removed, since Dart does not have signatures.
 */
public class Signature {

  public static final char[] ANY = new char[0];
  public static final int ARRAY_TYPE_SIGNATURE = 0;
  public static final int BASE_TYPE_SIGNATURE = 1;
  public static final int CLASS_TYPE_SIGNATURE = 2;
  public static final String SIG_VOID = "V";

  public static int getArrayCount(String x) {
    DartX.todo();
    return 0;
  }

  public static String getElementType(String x) {
    DartX.todo();
    return x;
  }

  public static int getParameterCount(char[] x) {
    DartX.todo();
    return 0;
  }

  public static char[][] getParameterTypes(char[] x) {
    DartX.todo();
    return null;
  }

  public static String[] getParameterTypes(String x) {
    DartX.todo();
    return null;
  }

  public static String getQualifier(String qualifiedTypeName) {
    DartX.todo();
    return null;
  }

  public static char[] getReturnType(char[] x) {
    DartX.todo();
    return x;
  }

  public static char[] getSignatureQualifier(char[] typeSignature) {
    if (typeSignature == null) {
      return Util.NO_CHAR;
    }
    int dotCount = 0;
    for (int i = 0; i < typeSignature.length; i++) {
      switch (typeSignature[i]) {
        case '.':
          dotCount++;
          break;
      }
    }
    if (dotCount > 0) {
      for (int i = 0; i < typeSignature.length; i++) {
        if (typeSignature[i] == '.') {
          dotCount--;
        }
        if (dotCount <= 0) {
          return subarray(typeSignature, 0, i);
        }
      }
    }
    return Util.NO_CHAR;
  }

  public static char[] getSignatureSimpleName(char[] typeSignature) {
    if (typeSignature == null) {
      return Util.NO_CHAR;
    }
    return typeSignature;
  }

  public static char[] getSimpleName(char[] x) {
    DartX.todo();
    return x;
  }

  public static String getSimpleName(String x) {
    DartX.todo();
    return x;
  }

  public static int getTypeSignatureKind(char[] x) {
    DartX.todo();
    return 0;
  }

  public static int getTypeSignatureKind(String x) {
    DartX.todo();
    return 0;
  }

  public static String getTypeVariable(String x) {
    DartX.todo();
    return x;
  }

  public static final char[] subarray(char[] array, int start, int end) {
    if (end == -1) {
      end = array.length;
    }
    if (start > end) {
      return null;
    }
    if (start < 0) {
      return null;
    }
    if (end > array.length) {
      return null;
    }

    char[] result = new char[end - start];
    System.arraycopy(array, start, result, 0, end - start);
    return result;
  }

  public static char[] toCharArray(char[] x) {
    DartX.todo();
    return x;
  }

  public static char[] toCharArray(char[] signature, Object object, Object object2, boolean b,
      boolean c) {
    DartX.todo();
    return new char[0];
  }

  public static String toString(String x) {
    DartX.todo();
    return x;
  }
}
