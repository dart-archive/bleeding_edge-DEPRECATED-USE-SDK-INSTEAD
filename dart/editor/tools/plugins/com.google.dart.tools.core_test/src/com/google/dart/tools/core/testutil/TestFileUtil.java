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
package com.google.dart.tools.core.testutil;

import com.google.dart.tools.core.generator.DartProjectGenerator;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * File based utility methods for use during test execution.
 */
public class TestFileUtil {

  /**
   * Answer the specified workspace project, creating it if necessary
   * 
   * @param projectName the name of the project
   * @return the project (not <code>null</code>)
   */
  public static IProject getOrCreateDartProject(String projectName) throws CoreException {
    final IProject project = getRoot().getProject(projectName);
    if (project.exists()) {
      return project;
    }
    final DartProjectGenerator generator = new DartProjectGenerator();
    generator.setName(projectName);
    run(new IWorkspaceRunnable() {
      @Override
      public void run(IProgressMonitor monitor) throws CoreException {
        generator.execute(new NullProgressMonitor());
      }
    });
    return project;
  }

  /**
   * Get the specified folder, creating it if necessary
   * 
   * @param project the project containing the folder (not <code>null</code>)
   * @param name the folder name (not <code>null</code>, not empty)
   * @return the folder (not <code>null</code>)
   */
  public static IContainer getOrCreateFolder(IProject project, String name) throws CoreException {
    final IFolder folder = project.getFolder(name);
    if (folder.exists()) {
      return folder;
    }
    run(new IWorkspaceRunnable() {
      @Override
      public void run(IProgressMonitor monitor) throws CoreException {
        folder.create(false, true, new NullProgressMonitor());
      }
    });
    return folder;
  }

  public static IWorkspaceRoot getRoot() {
    return getWorkspace().getRoot();
  }

  public static IWorkspace getWorkspace() {
    return ResourcesPlugin.getWorkspace();
  }

  /**
   * Read the content from the specified workspace file
   * 
   * @param file the workspace file (not <code>null</code>)
   * @return the content (not <code>null</code>)
   */
  public static String readFile(IFile file) throws CoreException, IOException {
    if (!file.exists()) {
      throw new IOException("File does not exist: " + file);
    }
    return readStream(file.getContents());
  }

  /**
   * Read the content from the specified resource
   * 
   * @param base the class relative to which the resource is stored
   * @param relPath the path to the resource relative to the base
   * @return the resource content
   * @throws IOException if the content is not found or there is a problem reading the content
   */
  public static String readResource(Class<?> base, String relPath) throws IOException {
    InputStream stream = base.getResourceAsStream(relPath);
    if (stream == null) {
      throw new IOException("Failed to find '" + relPath + "' relative to " + base.getName());
    }
    return readStream(stream);
  }

  /**
   * Read the content in the specified stream and close the stream
   * 
   * @param stream the stream to read (not <code>null</code>)
   * @return the content (not <code>null</code>)
   */
  public static String readStream(InputStream stream) throws IOException {
    StringBuilder result = new StringBuilder(2000);
    char[] buf = new char[2000];
    Reader contents = new InputStreamReader(stream);
    try {
      while (true) {
        int count = contents.read(buf);
        if (count == -1) {
          break;
        }
        result.append(buf, 0, count);
      }
    } finally {
      stream.close();
    }
    return result.toString();
  }

  /**
   * Perform any workspace modifications by wrapping them in a {@link IWorkspaceRunnable} and
   * calling this method.
   * 
   * @param runnable the runnable containing the workspace modification code
   */
  public static void run(IWorkspaceRunnable runnable) throws CoreException {
    getWorkspace().run(runnable, new NullProgressMonitor());
  }
}
