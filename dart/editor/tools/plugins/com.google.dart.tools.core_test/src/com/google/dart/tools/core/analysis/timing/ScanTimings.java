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
package com.google.dart.tools.core.analysis.timing;

import junit.framework.TestCase;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import java.io.File;

/**
 * Evaluate time to scan a directory using {@link File} versus {@link IResourceProxyVisitor}
 */
public class ScanTimings extends TestCase {

  private int fileCount;

  public void test_large_scan() throws Exception {

    String key = getClass().getSimpleName() + ".dir";
    String dirPath = System.getProperty(key);
    assertNotNull("Must define system property " + key, dirPath);
    File dir = new File(dirPath);
    assertTrue("Must exist " + dirPath, dir.exists());
    System.out.println("Scanning files in " + dirPath);

    runScanFiles(dir);
    runScanFiles(dir);
    runScanFiles(dir);

    IProject project = createProject(dirPath);

    runScanResources(project);
    runScanResources(project);
    runScanResources(project);
  }

  private void checkName(String name) {
    fileCount++;
  }

  private IProject createProject(String dirPath) throws CoreException {
    long start = System.currentTimeMillis();
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IWorkspaceRoot root = workspace.getRoot();
    String projectName = "test";
    IProject project = root.getProject(projectName);
    IProjectDescription description = workspace.newProjectDescription(projectName);
    description.setLocation(new Path(dirPath));
    project.create(description, null);
    project.open(null);
    long delta = System.currentTimeMillis() - start;
    System.out.println("Open project in " + delta + " ms");
    return project;
  }

  private void runScanFiles(File dir) {
    fileCount = 0;
    long start = System.currentTimeMillis();
    scanFiles(dir);
    long delta = System.currentTimeMillis() - start;
    System.out.println("File: " + fileCount + " files in " + delta + " ms");
  }

  private void runScanResources(IProject project) throws CoreException {
    fileCount = 0;
    long start = System.currentTimeMillis();
    scanResources(project);
    long delta = System.currentTimeMillis() - start;
    System.out.println("IRes: " + fileCount + " files in " + delta + " ms");
  }

  private void scanFiles(File dir) {
    String[] names = dir.list();
    for (String name : names) {
      checkName(name);
      File file = new File(dir, name);
      if (file.isDirectory()) {
        scanFiles(file);
      }
    }
  }

  private void scanResources(IProject project) throws CoreException {
    project.accept(new IResourceProxyVisitor() {
      @Override
      public boolean visit(IResourceProxy proxy) throws CoreException {
        checkName(proxy.getName());
//        proxy.requestFullPath();
//        proxy.requestResource();
        return true;
      }
    }, 0);
  }
}
