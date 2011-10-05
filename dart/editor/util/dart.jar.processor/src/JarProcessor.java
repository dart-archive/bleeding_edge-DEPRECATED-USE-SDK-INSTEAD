// Copyright (c) 2011, the Dart project authors. All Rights Reserved.

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * The JarProcessor utility can create and manipulate jar files and osgi bundles. In particular, it
 * can:
 * <p>
 * <ul>
 * <li><b>-empty</b> Given an existing osgi bundle, create an identical but empty osgi bundle. This
 * new bundle has to dependencies on other bundles, and is good for pruning the bundle dependency
 * graph.
 * <li><b>-rename</b> Given an osgi bundle, copy and rename it. This changes the jar name and the id
 * in the manifest.
 * <li><b>-process</b> Given an osgi bundle and a file with class loading information, create a copy
 * of the jar file with the entries sorted in the order in which they'll be loaded. This converts
 * the random seeks in the jar file at load time into a sequential read. This decreases startup time
 * for the application.
 * <li><b>-processDir</b> Similar to the -process command, this command takes as input a directory
 * and processes every jar file in that directory which is listed in the class loading information
 * file.
 * </ul>
 */
public class JarProcessor {

  private static byte[] buffer = new byte[10240];

  public static void main(String[] args) throws IOException {
    if (args.length == 0) {
      usage();
    }

    String sourceJar = null;
    String sourceDir = null;
    String outDirectory = null;
    String pluginId = null;
    String classListFile = null;

    boolean createEmpty = false;
    boolean renamePlugin = false;
    boolean processPlugin = false;
    boolean processDir = false;

    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("-out")) {
        if (++i >= args.length) {
          usage();
        }

        outDirectory = args[i];
      }

      if (args[i].equals("-empty")) {
        createEmpty = true;

        if (++i >= args.length) {
          usage();
        }

        sourceJar = args[i];
      }

      if (args[i].equals("-rename")) {
        renamePlugin = true;

        if (++i >= args.length) {
          usage();
        }

        sourceJar = args[i];

        if (++i >= args.length) {
          usage();
        }

        pluginId = args[i];
      }

      if (args[i].equals("-process")) {
        processPlugin = true;

        if (++i >= args.length) {
          usage();
        }

        sourceJar = args[i];

        if (++i >= args.length) {
          usage();
        }

        classListFile = args[i];
      }

      if (args[i].equals("-processDir")) {
        processDir = true;

        if (++i >= args.length) {
          usage();
        }

        sourceDir = args[i];

        if (++i >= args.length) {
          usage();
        }

        classListFile = args[i];
      }
    }

    if (createEmpty) {
      createEmptyJar(sourceJar, outDirectory);
    } else if (renamePlugin) {
      renamePlugin(sourceJar, pluginId, outDirectory);
    } else if (processPlugin) {
      processPlugin(sourceJar, classListFile, outDirectory);
    } else if (processDir) {
      processDir(sourceDir, classListFile, outDirectory);
    } else {
      usage();
    }
  }

  protected static void usage() {
    System.out.println("usage: java -jar JarProcessor.jar -empty <jar> -out <dir>");
    System.out.println("usage: java -jar JarProcessor.jar -rename <jar> <new_id> -out <dir>");
    System.out.println("usage: java -jar JarProcessor.jar -process <jar> <classesFile> -out <dir>");
    System.out.println("usage: java -jar JarProcessor.jar -processDir <dir> <classesFile> -out <dir>");

    System.exit(0);
  }

  /**
   * Create an empty copy of the given osgi bundle.
   * 
   * @param sourceJarPath
   * @param outDirectory
   * @throws IOException
   */
  static void createEmptyJar(String sourceJarPath, String outDirectory) throws IOException {
    File sourceFile = new File(sourceJarPath);

    JarFile sourceJar = new JarFile(sourceFile);

    File tempFile = File.createTempFile(sourceFile.getName(), "jar");

    createEmptyPlugin(sourceJar, tempFile);

    moveOrReplace(sourceFile, tempFile, outDirectory);
  }

  /**
   * Create copies of the plugins in the given directory with the jar entries sorted into load
   * order.
   * 
   * @param sourceDir
   * @param classListFile
   * @param outDirectory
   * @throws IOException
   */
  static void processDir(String sourceDir, String classListFile, String outDirectory)
      throws IOException {
    File dir = new File(sourceDir);

    Map<String, List<String>> jarToClassMap = createJarToClassMap(new File(classListFile));

    for (File file : dir.listFiles()) {
      if (file.isFile() && file.getName().endsWith(".jar")) {
        JarFile sourceJar = new JarFile(file);

        File tempFile = File.createTempFile(file.getName(), "jar");

        if (processPlugin(sourceJar, tempFile, jarToClassMap)) {
          moveOrReplace(file, tempFile, outDirectory);
        }
      }
    }
  }

  /**
   * Create a copy of the given osgi plugin with the jar entries sorted into load order.
   * 
   * @param sourceJarPath
   * @param classListFile
   * @param outDirectory
   * @throws IOException
   */
  static void processPlugin(String sourceJarPath, String classListFile, String outDirectory)
      throws IOException {
    File sourceFile = new File(sourceJarPath);

    JarFile sourceJar = new JarFile(sourceFile);

    File tempFile = File.createTempFile(sourceFile.getName(), "jar");

    Map<String, List<String>> jarToClassMap = createJarToClassMap(new File(classListFile));

    if (processPlugin(sourceJar, tempFile, jarToClassMap)) {
      moveOrReplace(sourceFile, tempFile, outDirectory);
    }
  }

  /**
   * Create a renamed copy of the given osgi plugin.
   * 
   * @param sourceJarPath
   * @param newPluginId
   * @param outDirectory
   * @throws IOException
   */
  static void renamePlugin(String sourceJarPath, String newPluginId, String outDirectory)
      throws IOException {
    File sourceFile = new File(sourceJarPath);

    JarFile sourceJar = new JarFile(sourceFile);

    File tempFile = File.createTempFile(sourceFile.getName(), "jar");

    String newFileName = renamePlugin(sourceJar, tempFile, newPluginId);

    moveOrReplace(sourceFile, tempFile, outDirectory, newFileName);
  }

  private static void copyManifestAttribute(String id, Manifest sourceManifest,
      Manifest destManifest) {
    destManifest.getMainAttributes().putValue(id, sourceManifest.getMainAttributes().getValue(id));
  }

  private static void copyStream(InputStream in, OutputStream out) throws IOException {
    int count = in.read(buffer);

    while (count != -1) {
      out.write(buffer, 0, count);
      count = in.read(buffer);
    }

    in.close();
  }

  private static void createEmptyPlugin(JarFile sourceJar, File tempFile) throws IOException {
    Manifest oldManifest = sourceJar.getManifest();
    Manifest manifest = new Manifest();

//    Manifest-Version: 1.0
//    Bundle-ActivationPolicy: lazy
//    Bundle-Localization: plugin
//    Bundle-ManifestVersion: 2
//    Bundle-Name: %pluginName
//    Bundle-RequiredExecutionEnvironment: JavaSE-1.6
//    Bundle-SymbolicName: com.google.dash.tools.core;singleton:=true
//    Bundle-Version: 0.1.0.qualifier
//    Bundle-Vendor: %providerName

    copyManifestAttribute("Manifest-Version", oldManifest, manifest);
    copyManifestAttribute("Bundle-ActivationPolicy", oldManifest, manifest);
    copyManifestAttribute("Bundle-ManifestVersion", oldManifest, manifest);
    copyManifestAttribute("Bundle-Name", oldManifest, manifest);
    copyManifestAttribute("Bundle-RequiredExecutionEnvironment", oldManifest, manifest);
    copyManifestAttribute("Bundle-SymbolicName", oldManifest, manifest);
    copyManifestAttribute("Bundle-Version", oldManifest, manifest);
    copyManifestAttribute("Bundle-Vendor", oldManifest, manifest);

    JarOutputStream outFile = new JarOutputStream(new FileOutputStream(tempFile), manifest);

    outFile.finish();
    outFile.close();

    System.out.println("[created empty plugin " + sourceJar.getName() + "]");
  }

//[Loaded org.eclipse.equinox.launcher.JNIBridge from file:/Users/devoncarew/build-dart/out/dash/plugins/org.eclipse.equinox.launcher_1.2.0.v20110502.jar]
//[Loaded org.eclipse.equinox.launcher.Main from file:/Users/devoncarew/build-dart/out/dash/plugins/org.eclipse.equinox.launcher_1.2.0.v20110502.jar]
//[Loaded org.eclipse.equinox.launcher.Main$EclipsePolicy from file:/Users/devoncarew/build-dart/out/dash/plugins/org.eclipse.equinox.launcher_1.2.0.v20110502.jar]
//[Loaded org.eclipse.equinox.launcher.Main$StartupClassLoader from file:/Users/devoncarew/build-dart/out/dash/plugins/org.eclipse.equinox.launcher_1.2.0.v20110502.jar]
//[Loaded org.eclipse.equinox.launcher.Main$SplashHandler from file:/Users/devoncarew/build-dart/out/dash/plugins/org.eclipse.equinox.launcher_1.2.0.v20110502.jar]
//[Loaded org.osgi.framework.BundleContext from file:/Users/devoncarew/build-dart/out/dash/plugins/org.eclipse.osgi_3.7.0.v20110613.jar]
//[Loaded org.eclipse.osgi.internal.profile.Profile from file:/Users/devoncarew/build-dart/out/dash/plugins/org.eclipse.osgi_3.7.0.v20110613.jar]

  private static Map<String, List<String>> createJarToClassMap(File file) throws IOException {
    Map<String, List<String>> jarMap = new HashMap<String, List<String>>();

    BufferedReader in = new BufferedReader(new FileReader(file), 102400);

    String line = in.readLine();

    int classCount = 0;

    while (line != null) {
      if (!line.isEmpty()) {
        if (line.startsWith("[Loaded ") && line.endsWith(".jar]")) {
          line = line.substring(0, line.length() - 1);

          String[] strs = line.split(" ");

          // [Loaded
          // org.eclipse.osgi.internal.profile.Profile
          // from
          // file:/Users/devoncarew/build-dart/out/dash/plugins/org.eclipse.osgi_3.7.0.v20110613.jar

          String className = strs[1];

          String pluginName = strs[3];

          if (pluginName.indexOf('/') != -1) {
            pluginName = pluginName.substring(pluginName.lastIndexOf('/') + 1);
          }

          if (pluginName.indexOf('_') != -1) {
            pluginName = pluginName.substring(0, pluginName.indexOf('_'));
          }

          List<String> classList = jarMap.get(pluginName);

          if (classList == null) {
            classList = new ArrayList<String>();
            jarMap.put(pluginName, classList);
          }

          classCount++;

          classList.add(className);
        }
      }

      line = in.readLine();
    }

    in.close();

    System.out.println(jarMap.keySet().size() + " jar files, " + classCount + " classes.");

    return jarMap;
  }

  private static void moveOrReplace(File sourceJar, File tempFile, String outDirectory)
      throws IOException {
    moveOrReplace(sourceJar, tempFile, outDirectory, null);
  }

  private static void moveOrReplace(File sourceJar, File tempFile, String outDirectory,
      String newFileName) throws IOException {
    File outFile = sourceJar;

    if (outDirectory != null) {
      File dir = new File(outDirectory);

      outFile = new File(dir, sourceJar.getName());
    }

    if (newFileName != null) {
      outFile = new File(outFile.getParentFile(), newFileName);
    }

    byte[] buffer = new byte[10480];

    InputStream in = new FileInputStream(tempFile);
    OutputStream out = new FileOutputStream(outFile);

    int count = in.read(buffer);

    while (count != -1) {
      out.write(buffer, 0, count);
      count = in.read(buffer);
    }

    out.close();
    in.close();
  }

  private static boolean processPlugin(JarFile sourceJar, File tempFile,
      Map<String, List<String>> jarToClassMap) throws IOException {

    String bundleId = sourceJar.getName();

    if (bundleId.indexOf('/') != -1) {
      bundleId = bundleId.substring(bundleId.lastIndexOf('/') + 1);
    }

    if (bundleId.indexOf('_') != -1) {
      bundleId = bundleId.substring(0, bundleId.indexOf('_'));
    }

    List<String> classes = jarToClassMap.get(bundleId);

    if (classes == null) {
      return false;
    }

    Manifest manifest = sourceJar.getManifest();

    // Remove the SHA digest info for signed jar files.
    Manifest newManifest = new Manifest();

    for (Object key : manifest.getMainAttributes().keySet()) {
      newManifest.getMainAttributes().put(key, manifest.getMainAttributes().get(key));
    }

    JarOutputStream outFile = new JarOutputStream(new FileOutputStream(tempFile), newManifest);

    Set<String> writtenEntries = new HashSet<String>();

    int reorderCount = 0;
    int entryCount = 0;

    for (String className : classes) {
      JarEntry entry = sourceJar.getJarEntry(className.replace('.', '/') + ".class");

      if (entry != null) {
        InputStream in = sourceJar.getInputStream(entry);

        JarEntry newEntry = new JarEntry(entry);

        // Storing the entries does not improve our launch times.
        //newEntry.setMethod(ZipEntry.STORED);
        //newEntry.setCompressedSize(entry.getSize());

        outFile.putNextEntry(newEntry);

        copyStream(in, outFile);

        outFile.closeEntry();

        writtenEntries.add(entry.getName());

        reorderCount++;
      }
    }

    Enumeration<JarEntry> entries = sourceJar.entries();

    while (entries.hasMoreElements()) {
      JarEntry entry = entries.nextElement();

      String entryName = entry.getName();

      if (!entryName.startsWith("META-INF/")) {

        entryCount++;

        if (!writtenEntries.contains(entryName)) {
          InputStream in = sourceJar.getInputStream(entry);

          outFile.putNextEntry(entry);

          copyStream(in, outFile);

          outFile.closeEntry();

          writtenEntries.add(entry.getName());
        }
      }
    }

    outFile.finish();
    outFile.close();

    System.out.println("[" + sourceJar.getName() + " rewritten, " + reorderCount + " of "
        + entryCount + " entries re-ordered]");

    return true;
  }

  private static String renamePlugin(JarFile sourceJar, File tempFile, String newPluginId)
      throws IOException {
    String bundleVersion = sourceJar.getName();

    if (bundleVersion.indexOf('/') != -1) {
      bundleVersion = bundleVersion.substring(bundleVersion.lastIndexOf('/') + 1);
    }

    if (bundleVersion.indexOf('_') != -1) {
      bundleVersion = bundleVersion.substring(bundleVersion.lastIndexOf('_'));
    }

    Manifest manifest = sourceJar.getManifest();

    // Remove the SHA digest info for signed jar files.
    Manifest newManifest = new Manifest();

    for (Object key : manifest.getMainAttributes().keySet()) {
      newManifest.getMainAttributes().put(key, manifest.getMainAttributes().get(key));
    }

    // rename plugin -
    String oldSymName = manifest.getMainAttributes().getValue("Bundle-SymbolicName");

    if (oldSymName != null) {
      String newSymName;

      if (oldSymName.indexOf(';') != -1) {
        newSymName = newPluginId + oldSymName.substring(oldSymName.indexOf(';'));
      } else {
        newSymName = newPluginId;
      }

      newManifest.getMainAttributes().putValue("Bundle-SymbolicName", newSymName);
    }

    JarOutputStream outFile = new JarOutputStream(new FileOutputStream(tempFile), newManifest);

    Enumeration<JarEntry> entries = sourceJar.entries();

    while (entries.hasMoreElements()) {
      JarEntry entry = entries.nextElement();

      String entryName = entry.getName();

      if (!entryName.startsWith("META-INF/")) {
        InputStream in = sourceJar.getInputStream(entry);

        outFile.putNextEntry(entry);

        copyStream(in, outFile);

        outFile.closeEntry();
      }
    }

    outFile.finish();
    outFile.close();

    System.out.println("[renamed " + sourceJar.getName() + " to " + newPluginId + "]");

    return newPluginId + bundleVersion;
  }

}
