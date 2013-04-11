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

import com.google.dart.engine.utilities.io.PrintStringWriter;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.analysis.AnalysisServer;
import com.google.dart.tools.core.internal.index.impl.InMemoryIndex;
import com.google.dart.tools.core.internal.model.PackageLibraryManagerProvider;
import com.google.dart.tools.core.utilities.net.NetUtils;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swt.internal.Library;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PlatformUI;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.zip.CRC32;

/**
 * Provides utility methods for feedback submission.
 */
@SuppressWarnings("restriction")
public class FeedbackUtils {

  /**
   * Contains information about the current session
   */
  public static class Stats {
    public final int numProjects = ResourcesPlugin.getWorkspace().getRoot().getProjects().length;
    public final int numEditors = getNumberOfOpenDartEditors();
    public final int numThreads = getNumberOfThreads();
    public final long maxMem = getMaxMem();
    public final long totalMem = Runtime.getRuntime().totalMemory();
    public final long freeMem = Runtime.getRuntime().freeMemory();
    public final String indexStats;
    public final boolean autoRunPubEnabled = DartCore.getPlugin().isAutoRunPubEnabled();

    public Stats() {
      if (DartCoreDebug.ENABLE_NEW_ANALYSIS) {
        indexStats = "index: " + DartCore.getProjectManager().getIndex().getStatistics();
      } else {
        indexStats = InMemoryIndex.getInstance().getIndexStatus("index");
      }
    }

    @Override
    public String toString() {
      PrintStringWriter writer = new PrintStringWriter();

      writer.print("# projects: ");
      writer.println(numProjects);

      writer.print("# open dart files: ");
      writer.println(countString(numEditors));

      writer.println("auto-run pub: " + autoRunPubEnabled);

      writer.println("localhost resolves to: " + cleanLocalHost(NetUtils.getLoopbackAddress()));

      writer.print("mem max/total/free: ");
      writer.print(convertToMeg(maxMem));
      writer.print(" / ");
      writer.print(convertToMeg(totalMem));
      writer.print(" / ");
      writer.print(convertToMeg(freeMem));
      writer.println(" MB");

      writer.println("thread count: " + countString(numThreads));

      if (!DartCoreDebug.ENABLE_NEW_ANALYSIS) {
        AnalysisServer server = PackageLibraryManagerProvider.getDefaultAnalysisServer();
        String analysisStats = server.getAnalysisStatus("analysis");
        writer.println(analysisStats);
      }

      writer.println(indexStats);

      return writer.toString();
    }

    /**
     * Converts the given number of bytes to the corresponding number of megabytes (rounded up).
     */
    private long convertToMeg(long numBytes) {
      return (numBytes + (512 * 1024)) / (1024 * 1024);
    }

    private String countString(int count) {
      return count == -1 ? "<unknown>" : Integer.toString(count);
    }

  }

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
    return DartToolsPlugin.getVersionString() + " (" + getBuildDate() + ") " + binaryDetails;
  }

  // Copied from org.eclipse.ui.internal.HeapStatus
  public static long getMaxMem() {
    long max = Long.MAX_VALUE;
    try {
      // Must use reflect to allow compilation against JCL/Foundation
      Method maxMemMethod = Runtime.class.getMethod("maxMemory", new Class[0]); //$NON-NLS-1$
      Object o = maxMemMethod.invoke(Runtime.getRuntime(), new Object[0]);
      if (o instanceof Long) {
        max = ((Long) o).longValue();
      }
    } catch (Exception e) {
      // ignore if method missing or if there are other failures trying to determine the max
    }
    return max;
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
   * Answer information about the current session
   */
  public static Stats getStats() {
    try {
      return new Stats();
    } catch (Exception e) {
      DartCore.logError("Exception obtaining information about the current session", e);
      return null;
    }
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

  /**
   * If localhost doesn't resolve to something we know to be generic, just return "other". This is
   * used to make sure that we don't accidentally send user identifying information.
   * 
   * @param host
   * @return
   */
  private static String cleanLocalHost(String host) {
    if (host.startsWith("local") || host.startsWith("127.") || host.startsWith("::")) {
      return host;
    } else {
      return "other";
    }
  }

  private static String getBinaryString() throws Exception {
    return "*" + (is64bitBinary() ? "64" : "32") + " bit binary*";
  }

  private static String getBuildDate() {
    return DartCore.getBuildDate();
  }

  private static int getNumberOfOpenDartEditors() {

    final Integer[] projects = new Integer[] {-1};

    Display.getDefault().syncExec(new Runnable() {
      @Override
      public void run() {
        try {
          int count = 0;
          for (IEditorReference ref : PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences()) {
            if (DartCore.isDartLikeFileName(ref.getPartName())) {
              ++count;
            }
          }
          projects[0] = count;
        } catch (Throwable e) {
          //ignore --- -1 value indicates a failure
        }
      }
    });

    return projects[0];
  }

  private static int getNumberOfThreads() {
    try {
      return Thread.getAllStackTraces().keySet().size();
    } catch (Throwable e) {
      return -1;
    }
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
