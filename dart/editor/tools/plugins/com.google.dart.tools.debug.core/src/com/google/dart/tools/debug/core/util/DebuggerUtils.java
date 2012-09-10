/*
 * Copyright 2012 Dart project authors.
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

package com.google.dart.tools.debug.core.util;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IStackFrame;

import java.util.Arrays;
import java.util.List;

/**
 * This class contains static utility methods for use by the debugger.
 */
public class DebuggerUtils {

  public static final String LIBRARY_NAME = "library";

  public static final String TOP_LEVEL_NAME = "top-level";

  /**
   * Returns whether the given frame needs some additional disambiguating information from its two
   * surrounding frames.
   * 
   * @param frame
   * @return
   * @throws DebugException
   */
  public static boolean areSiblingNamesUnique(IExceptionStackFrame frame) throws DebugException {
    List<IStackFrame> frames = Arrays.asList(frame.getThread().getStackFrames());

    int index = frames.indexOf(frame);

    if (index == -1) {
      return true;
    }

    if (index > 0) {
      IExceptionStackFrame other = (IExceptionStackFrame) frames.get(index - 1);

      if (needsDisambiguating(other, frame)) {
        return false;
      }
    }

    if ((index + 1) < frames.size()) {
      IExceptionStackFrame other = (IExceptionStackFrame) frames.get(index + 1);

      if (needsDisambiguating(frame, other)) {
        return false;
      }
    }

    return true;
  }

  /**
   * The names of private fields are mangled by the VM.
   * <p>
   * _foo@652376 ==> _foo
   * 
   * @param name
   * @return
   */
  public static String demanglePrivateName(String name) {
    if (name == null) {
      return null;
    }

    int atIndex = name.indexOf('@');

    while (atIndex != -1) {
      // check for _foo@76876.bar (or _Process@14117cc4._reportError@14117cc4)
      int endIndex = name.indexOf('.', atIndex);

      if (endIndex == -1) {
        name = name.substring(0, atIndex);
      } else {
        name = name.substring(0, atIndex) + name.substring(endIndex);
      }

      atIndex = name.indexOf('@');
    }

    // Also remove the trailing '.' for default constructors.
    if (name.endsWith(".")) {
      name = name.substring(0, name.length() - 1);
    }

    return name;
  }

  /**
   * @return whether the given debugger symbol name represents a private symbol
   */
  public static boolean isPrivateName(String name) {
    if (name.indexOf('.') != -1) {
      return name.indexOf("._") != -1;
    } else {
      return name.startsWith("_");
    }
  }

  public static String printString(String str) {
    if (str == null) {
      return null;
    }

    if (str.indexOf('\n') != -1) {
      str = str.replace("\n", "\\n");
    }

    if (str.indexOf('\r') != -1) {
      str = str.replace("\r", "\\r");
    }

    if (str.indexOf('\t') != -1) {
      str = str.replace("\t", "\\t");
    }

    return "\"" + str + "\"";
  }

  private static boolean needsDisambiguating(IExceptionStackFrame frame1,
      IExceptionStackFrame frame2) {
    if (frame1.getShortName().equals(frame2.getShortName())) {
      // These will need disambiguating if the long names are different.

      return !frame1.getLongName().equals(frame2.getLongName());
    }

    return false;
  }

  private DebuggerUtils() {

  }

}
