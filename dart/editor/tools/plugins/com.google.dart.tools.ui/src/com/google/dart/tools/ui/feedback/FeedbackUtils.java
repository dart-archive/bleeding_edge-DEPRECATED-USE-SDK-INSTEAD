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
package com.google.dart.tools.ui.feedback;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.swt.internal.Library;

import java.lang.reflect.Field;
import java.util.zip.CRC32;

/**
 * Provides utility methods for feedback submission.
 */
@SuppressWarnings("restriction")
public class FeedbackUtils {

  /**
   * Calculate a CRC-32 checksum for a given array of bytes.
   * 
   * @param data the array of bytes
   * @return a CRC-32 checksum
   */
  public static String calculateCRC32(byte[] data) {
    CRC32 crc = new CRC32();

    crc.update(data);

    long val = crc.getValue();

    StringBuffer buf = new StringBuffer();

    buf.append(toHex((int) (0xFFL & (val >> 24))));
    buf.append(toHex((int) (0xFFL & (val >> 16))));
    buf.append(toHex((int) (0xFFL & (val >> 8))));
    buf.append(toHex((int) (0xFFL & (val))));

    return buf.toString().toUpperCase();
  }

  /**
   * Get a String representation of editor version details. Minimally, this will include the build
   * id. If a binary mismatch is detected (e.g., editor binary is 32 bit while the OS arch is 64
   * bit) the current binary bit info will be appended as well.
   * 
   * @return a String representation of editor version details
   */
  public static String getEditorVersionDetails() {
    String binaryDetails = null;
    try {
      binaryDetails = binaryMismatch() ? " - " + getBinaryString() : "";
    } catch (Exception e) {
      binaryDetails = "- <unable to detect binary type>";
    }
    return DartToolsPlugin.getBuildId() + " (" + getBuildDate() + ") " + binaryDetails;
  }

  /**
   * Get a String representation of the current OS.
   * 
   * @return a String representation of the current OS
   */
  public static String getOSName() {
    return System.getProperty("os.name") + " - " + getOSArch();
  }

  /**
   * Return a list of the substrings in the given string that are separated by the given separator.
   * If the given flag is <code>true</code>, the substrings will have leading and trailing
   * whitespace removed.
   * 
   * @param string the string to be split
   * @param separator the separator that delimits substrings
   * @param trimSubstrings <code>true</code> if substrings should be trimmed
   * @return the list of substrings that were found
   */
  public static String[] splitString(String string, String separator, boolean trimSubstrings) {
    String[] results = string.split(separator);

    if (trimSubstrings) {
      for (int i = 0; i < results.length; i++) {
        results[i] = results[i].trim();
      }
    }

    return results;
  }

  private static boolean binaryMismatch() throws Exception {
    return is64bitOS() != is64bitBinary();
  }

  private static String getBinaryString() throws Exception {
    return "*" + (is64bitBinary() ? "64" : "32") + " bit binary*";
  }

  private static String getBuildDate() {
    return DartCore.getBuildDate();
  }

  private static String getOSArch() {
    return System.getProperty("os.arch") + " (" + System.getProperty("os.version") + ")";
  }

  private static boolean is64bitBinary() throws Exception {
    Class<Library> swtLibraryClass = Library.class;
    Field is64 = swtLibraryClass.getDeclaredField("IS_64");
    is64.setAccessible(true);
    return is64.getBoolean(swtLibraryClass);
  }

  private static boolean is64bitOS() {
    return getOSArch().contains("64");
  }

  /**
   * Convert an integer to a hex string.
   * 
   * @param i an integer
   * @return a hex representation of the given integer
   */
  private static String toHex(int i) {
    String str = Integer.toHexString(0xFF & i);

    if (str.length() < 2) {
      str = "0" + str;
    }

    return str;
  }

}
