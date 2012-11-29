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

package com.google.dart.tools.core.html;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.builder.DartBuildParticipant;
import com.google.dart.tools.core.internal.builder.MarkerUtilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * A build participant to process html files.
 * 
 * @see DartBuildParticipant
 */
public class HtmlBuildParticipant implements DartBuildParticipant {

  public HtmlBuildParticipant() {

  }

  @Override
  public void build(int kind, Map<String, String> args, IResourceDelta delta,
      IProgressMonitor monitor) throws CoreException {
    if (IncrementalProjectBuilder.FULL_BUILD == kind) {
      // TODO: we're not passed in the project...
      //doFullBuild(project, monitor);
    } else if (IncrementalProjectBuilder.CLEAN_BUILD == kind) {
      // TODO: we're not passed in the project...
      //clean(project, monitor);
    } else if (delta != null) {
      // Perform an incremental build.
      processDelta(delta);
    } else {
      // TODO: we're not passed in the project...
      //doFullBuild(project, monitor);
    }

    monitor.done();
  }

  @Override
  public void clean(IProject project, IProgressMonitor monitor) throws CoreException {
    MarkerUtilities.deleteMarkers(project);
  }

  @SuppressWarnings("unused")
  private void doFullBuild(IProject project, IProgressMonitor monitor) throws CoreException {
    project.accept(new IResourceVisitor() {
      @Override
      public boolean visit(IResource resource) throws CoreException {
        if (resource.getType() == IResource.FILE) {
          processFile((IFile) resource);

          return true;
        } else {
          return true;
        }
      }
    });
  }

//  private int getLineNumber(String data, int index) {
//    char[] chars = data.toCharArray();
//
//    int lineCount = 0;
//
//    for (int i = 0; i < index; i++) {
//      if (chars[i] == '\n') {
//        lineCount++;
//      }
//    }
//
//    return lineCount + 1;
//  }

  private void processDelta(IResourceDelta delta) throws CoreException {
    delta.accept(new IResourceDeltaVisitor() {
      @Override
      public boolean visit(IResourceDelta delta) throws CoreException {
        IResource resource = delta.getResource();

        if (resource.getType() == IResource.FILE) {
          switch (delta.getKind()) {
            case IResourceDelta.ADDED:
              processFile((IFile) resource);
              break;
            case IResourceDelta.CHANGED:
              processFile((IFile) resource);
              break;
          }

          return true;
        } else if (resource.getType() == IResource.FOLDER) {
          if (resource.getName().startsWith(".")) {
            return false;
          }

          if (DartCore.isPackagesDirectory((IFolder) resource)) {
            return false;
          }

          return true;
        } else {
          return true;
        }
      }
    });
  }

  private void processFile(IFile file) {
    if (file.getName().toLowerCase().endsWith(".html")) {
      processHtml(file);
    }
  }

  private void processHtml(IFile file) {
    try {
      MarkerUtilities.deleteMarkers(file);

      XmlDocument document = new HtmlParser(Files.toString(
          file.getLocation().toFile(),
          Charsets.UTF_8)).parse();

      validate(document, file);
    } catch (CoreException e) {
      DartCore.logError(e);
    } catch (IOException ioe) {

    }

    HtmlAnalyzeHelper.analyze(file);
  }

  private void validate(XmlDocument document, IFile file) throws CoreException {
    for (XmlNode node : document.getChildren()) {
      if (node instanceof XmlElement) {
        validate((XmlElement) node, file);
      }
    }
  }

  private void validate(XmlElement node, IFile file) throws CoreException {
    // We need to be very smart about html attribute validation, as lots of html code
    // uses older attributes, or makes up their own custom attributes.
    // We should probably look for typos of the common attributes.
    if (DartCoreDebug.ENABLE_HTML_VALIDATION) {
      if (node.getAttributes().size() > 0) {
        List<String> validAttributes = HtmlKeywords.getAttributes(node.getLabel());

        if (!validAttributes.isEmpty()) {
          for (XmlAttribute attribute : node.getAttributes()) {
            String name = attribute.getName();

            if (!validAttributes.contains(name)) {
              if (!HtmlKeywords.isValidEventAttribute(name)) {
                int charStart = attribute.getStartToken().getLocation();
                String message = "\"" + name + "\" is not a valid attribute for the " + "<"
                    + node.getLabel() + "> element.";

                MarkerUtilities.createWarningMarker(
                    file,
                    message,
                    attribute.getStartToken().getLineNumber(),
                    charStart,
                    charStart + name.length());
              }
            }
          }
        }
      }
    }

    for (XmlNode child : node.getChildren()) {
      if (child instanceof XmlElement) {
        validate((XmlElement) child, file);
      }
    }
  }

}
