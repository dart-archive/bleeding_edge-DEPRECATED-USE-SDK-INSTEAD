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
package com.google.dart.tools.core.test.util;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.indexer.DartIndexerResult;
import com.google.dart.tools.core.model.DartProject;

import junit.framework.Assert;
import junit.framework.ComparisonFailure;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * The class <code>TestUtilities</code> defines utility methods that can be used when writing tests.
 */
public class TestUtilities {
  /**
   * The name of the directory containing projects that can be loaded for testing purposes.
   */
  public static final String PROJECT_DIRECTORY_NAME = "test_data"; //$NON-NLS-1$

  /**
   * Assert that the actual array contains the same number of elements as the expected value and
   * that every corresponding pair of elements are equal.
   * 
   * @param expected an array containing the expected values
   * @param actual an array containing the actual values
   */
  public static void assertEqualElements(Object[] expected, Object[] actual) {
    int length = expected.length;
    Assert.assertEquals(length, actual.length);
    for (int i = 0; i < length; i++) {
      Assert.assertEquals(expected[i], actual[i]);
    }
  }

  /**
   * Assert that the array of actual values contains every value in the array of expected values,
   * allowing the values to occur in a different order in the two arrays. For example, the following
   * would not throw an exception: <blockquote><code>
   *    assertEqualsIgnoreOrder(new String[] {"a", "b", "c"}, new String[] {"c", "a", "b"});
   * </code></blockquote> while the following would: <blockquote><code>
   *    assertEqualsIgnoreOrder(new String[] {"a", "b", "a"}, new String[] {"a", "a", "a"});
   * </code></blockquote>
   * 
   * @param expected an array containing the expected values
   * @param actual an array containing the actual values
   */
  public static void assertEqualsIgnoreOrder(Object[] expected, Object[] actual) {
    int length = expected.length;
    if (actual.length != length) {
      Assert.fail("Expected length of " + length + " found length of " + actual.length);
    }
    boolean[] found = new boolean[length];
    for (int i = 0; i < length; i++) {
      int index = findUnfound(expected[i], actual, found);
      if (index < 0) {
        Assert.fail("Expected to find " + expected[i] + ", but did not");
      }
      found[index] = true;
    }
  }

  /**
   * Assert that two strings are equal if whitespace characters are ignored.
   * 
   * @param expected the expected value of the string
   * @param actual the actual value of the string
   */
  public static void assertEqualsIgnoreWhitespace(String expected, String actual) {
    assertEqualsIgnoreWhitespace(null, expected, actual);
  }

  /**
   * Assert that two strings are equal if whitespace characters are ignored.
   * 
   * @param message the message to be included if the strings are not equal
   * @param expected the expected value of the string
   * @param actual the actual value of the string
   */
  public static void assertEqualsIgnoreWhitespace(String message, String expected, String actual) {
    if (expected == null) {
      if (actual == null) {
        return;
      }
      throw new ComparisonFailure(message, expected, actual);
    } else if (actual == null) {
      throw new ComparisonFailure(message, expected, actual);
    }
    StringReader expectedReader = new StringReader(expected);
    StringReader actualReader = new StringReader(actual);
    int expectedChar = getNextNonWhitespace(expectedReader);
    int actualChar = getNextNonWhitespace(actualReader);
    while (expectedChar == actualChar) {
      if (expectedChar < 0) {
        return;
      }
      expectedChar = getNextNonWhitespace(expectedReader);
      actualChar = getNextNonWhitespace(actualReader);
    }
    throw new ComparisonFailure(message, expected, actual);
  }

  /**
   * Assert that the given object is an instance of the given class. This is equivalent to
   * <code>assertTrue(object instanceof type)</code> but with a more meaningful failure message.
   * 
   * @param object the object whose type is being tested
   * @param type the type that the object must be an instance of
   */
  public static void assertInstanceof(Object object, Class<?> type) {
    if (!type.isInstance(object)) {
      Assert.fail("Expected instanceof " + type.getName() + " but was instanceof "
          + object.getClass().getName());
    }
  }

  /**
   * Assert the the given indexer result does not contain any errors.
   * 
   * @param result the indexer result being tested
   */
  public static void assertNoErrors(DartIndexerResult result) {
    IPath[] errors = result.getErrors();
    if (errors != null && errors.length > 0) {
      PrintStringWriter writer = new PrintStringWriter();
      writer.println("Unexpected errors found:");
      for (IPath error : errors) {
        writer.print("  ");
        writer.println(error.toPortableString());
      }
      Assert.fail(writer.toString());
    }
  }

  /**
   * Assert that the Eclipse .log file does not exist.
   */
  public static void assertNoLogFile() {
    File logFile = getLogFile();
    if (logFile.exists()) {
      PrintStringWriter writer = new PrintStringWriter();
      try {
        String contents = FileUtilities.getContents(logFile);
        writer.println("Non-empty log file. Log file contents:");
        writer.println();
        writer.print(contents);
      } catch (IOException exception) {
        writer.println("Non-empty log file. Could not access contents of log file.");
        writer.println();
        exception.printStackTrace(writer);
      }
      Assert.fail(writer.toString());
    }
  }

  /**
   * Create a new directory that can safely be deleted after the test has completed.
   * 
   * @return the directory that was created
   */
  public static File createTempDirectory() {
    File tempDir = new File(System.getProperty("java.io.tmpdir")); //$NON-NLS-1$
    File subdirectory = new File(tempDir, "test"); //$NON-NLS-1$
    int index = 0;
    while (subdirectory.exists()) {
      index = index + 1;
      subdirectory = new File(tempDir, "test" + index); //$NON-NLS-1$
    }
    subdirectory.mkdirs();
    return subdirectory;
  }

  /**
   * Cause this thread to wait for at least the given number of milliseconds.
   * 
   * @param duration the number of milliseconds that this thread should wait
   */
  public static void delay(long duration) {
    // Display display = Display.getCurrent();
    // if (display == null) {
    try {
      Thread.sleep(duration);
    } catch (InterruptedException exception) {
      // Ignored
    }
    // } else {
    // long endTime = System.currentTimeMillis() + duration;
    // while (System.currentTimeMillis() < endTime) {
    // if (!display.readAndDispatch()) {
    // display.sleep();
    // }
    // display.update();
    // }
    // }
  }

  /**
   * Delete the Eclipse .log file if it exists.
   */
  public static void deleteLogFile() {
    File logFile = getLogFile();
    if (logFile.exists()) {
      logFile.delete();
    }
  }

  public static void deleteProject(final DartProject project) {
    try {
      project.getProject().getWorkspace().run(new IWorkspaceRunnable() {
        @Override
        public void run(IProgressMonitor monitor) throws CoreException {
          project.getProject().delete(true, true, null);
        }
      }, null);
    } catch (CoreException exception) {
      // DartCore.getLogger().logError(exception, "Could not delete the project " + project.getElementName()); //$NON-NLS-1$
    }
  }

  /**
   * Return the Eclipse .log file.
   * 
   * @return the Eclipse .log file
   */
  public static File getLogFile() {
    return Platform.getLogFileLocation().toFile();
  }

  /**
   * Return the absolute path to the resource within the specified plug-in with the given relative
   * path.
   * 
   * @param pluginId the id of the plug-in containing the resource
   * @param relativePath the relative path of the resource within the project
   * @return the absolute path to the resource
   * @throws IOException if some portion of the path is invalid
   */
  public static IPath getPluginRelativePath(String pluginId, IPath relativePath) throws IOException {
    IPath pluginPath = new Path(
        FileLocator.toFileURL(PluginUtilities.getInstallUrl(pluginId)).getPath());
    return pluginPath.append(relativePath);
  }

  /**
   * Return the name of the project at the given directory.
   * 
   * @param projectRootDirectory the root directory of the project
   * @return the name of the project at the given directory
   * @throws FileNotFoundException if a required file or directory does not exist
   * @throws IOException if a required file cannot be accessed
   * @throws SAXException if the .project file is not valid
   * @throws XMLException if the .project file is not valid
   */
  public static String getProjectName(IPath projectRootDirectory) throws FileNotFoundException,
      IOException, SAXException {
    File projectFile = projectRootDirectory.append(".project").toFile();
    try {
      Document projectDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
          projectFile);
      NodeList children = projectDocument.getFirstChild().getChildNodes();
      int count = children.getLength();
      for (int i = 0; i < count; i++) {
        Node node = children.item(i);
        if (node.getNodeName().equals("name")) {
          return node.getTextContent();
        }
      }
      throw new SAXException("Missing name element in project file");
    } catch (ParserConfigurationException exception) {
      throw new SAXException("Missing name element in project file");
    }
  }

  /**
   * Return the project with the given root directory, loading it if it is not already loaded. This
   * method assumes that there is no conflict with project names.
   * 
   * @param projectRootDirectory the root directory of the project
   * @return the project with the given root directory
   * @throws CoreException
   * @throws FileNotFoundException if a required file or directory does not exist
   * @throws IOException if a required file cannot be accessed
   * @throws SAXException if the .project file is not valid
   */
  public static DartProject loadDartProject(IPath projectRootDirectory) throws CoreException,
      FileNotFoundException, IOException, SAXException {
    String projectName = getProjectName(projectRootDirectory);
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IProject project = workspace.getRoot().getProject(projectName);
    if (!project.exists()) {
      IProjectDescription description = workspace.newProjectDescription(projectName);
      if (!workspace.getRoot().getLocation().isPrefixOf(projectRootDirectory)) {
        description.setLocation(projectRootDirectory);
      }
      project.create(description, null);
      project.open(null);
    }
    DartProject dartProject = DartCore.create(project);
    // waitForJobs();
    return dartProject;
  }

  /**
   * Return the project in the directory with the given name located in the <code>test_data</code>
   * directory in the plug-in with the given id, loading the project if it is not already loaded.
   * This method assumes that there is no conflict with project names.
   * 
   * @param pluginId the id of the plug-in containing the project
   * @param projectDirectory the name of the directory containing the project
   * @return the project in the specified directory
   * @throws CoreException
   * @throws FileNotFoundException if a required file or directory does not exist
   * @throws IOException if a required file cannot be accessed
   * @throws SAXException if the .project file is not valid
   */
  public static DartProject loadPluginRelativeProject(String pluginId, String projectDirectory)
      throws CoreException, FileNotFoundException, IOException, SAXException {

    IPath targetDirectory = ResourcesPlugin.getWorkspace().getRoot().getLocation();
    targetDirectory = targetDirectory.append(projectDirectory);

    if (!targetDirectory.append(".project").toFile().exists()) {

      IPath sourceDirectory = new Path(FileLocator.toFileURL(
          PluginUtilities.getInstallUrl(pluginId)).getPath());
      sourceDirectory = sourceDirectory.append(PROJECT_DIRECTORY_NAME).append(projectDirectory);

      FileUtilities.copyDirectoryContents(sourceDirectory.toFile(), targetDirectory.toFile());
    }
    DartProject project = loadDartProject(targetDirectory);
    refreshWorkspace();

    return project;
  }

  /**
   * Return <code>true</code> if the Eclipse .log file does not exist.
   * 
   * @return <code>true</code> if the Eclipse .log file does not exist
   */
  public static boolean logFileIsEmpty() {
    return !getLogFile().exists();
  }

  /**
   * Refresh the content of the workspace to be consistent with what's on disk.
   */
  public static void refreshWorkspace() {
    try {
      ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, null);
    } catch (CoreException exception) {
      // DartCore.getLogger().logError(exception, "Could not refresh workspace"); //$NON-NLS-1$
    }
  }

  /**
   * Run the given operation, passing in a temporary directory that is newly created (implying that
   * it exists and is empty), and delete the directory after the operation has completed.
   * 
   * @param operation the operation to be run
   * @throws Exception if the operation throws an Exception or if the directory either cannot be
   *           created or cannot be deleted
   */
  public static void runWithTempDirectory(FileOperation operation) throws Exception {
    File tempDir = createTempDirectory();
    try {
      operation.run(tempDir);
    } finally {
      FileUtilities.delete(tempDir);
    }
  }

  private static int findUnfound(Object object, Object[] actual, boolean[] found) {
    if (object == null) {
      for (int i = 0; i < actual.length; i++) {
        if (!found[i] && actual[i] == null) {
          return i;
        }
      }
    } else {
      for (int i = 0; i < actual.length; i++) {
        if (!found[i] && object.equals(actual[i])) {
          return i;
        }
      }
    }
    return -1;
  }

  private static int getNextNonWhitespace(StringReader reader) {
    try {
      int nextChar = reader.read();
      while (nextChar >= 0 && Character.isWhitespace((char) nextChar)) {
        nextChar = reader.read();
      }
      return nextChar;
    } catch (IOException exception) {
      // This cannot happen because we're reading from a StringReader.
      return -1;
    }
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private TestUtilities() {
    super();
  }
}
