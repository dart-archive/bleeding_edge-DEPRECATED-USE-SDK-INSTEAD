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
import com.google.dart.tools.core.builder.BuildEvent;
import com.google.dart.tools.core.builder.BuildParticipant;
import com.google.dart.tools.core.builder.BuildVisitor;
import com.google.dart.tools.core.builder.CleanEvent;
import com.google.dart.tools.core.internal.builder.MarkerUtilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import java.io.IOException;
import java.util.List;

/**
 * A build participant to analyze html files.
 */
public class HtmlBuildParticipant implements BuildParticipant, BuildVisitor {

  @Override
  public void build(BuildEvent event, IProgressMonitor monitor) throws CoreException {
    event.traverse(this, false);
  }

  @Override
  public void clean(CleanEvent event, IProgressMonitor monitor) throws CoreException {
    MarkerUtilities.deleteMarkers(event.getProject());
  }

  @Override
  public boolean visit(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
    if (delta.getKind() == IResourceDelta.CHANGED) {
      IResource resource = delta.getResource();
      if (resource.getType() == IResource.FILE) {
        if (DartCore.isHTMLLikeFileName(resource.getName())) {
          processHtml((IFile) resource);
        }
      }
    }
    return true;
  }

  @Override
  public boolean visit(IResourceProxy proxy, IProgressMonitor monitor) throws CoreException {
    if (proxy.getType() == IResource.FILE) {
      if (DartCore.isHTMLLikeFileName(proxy.getName())) {
        processHtml((IFile) proxy.requestResource());
      }
    }
    return true;
  }

  protected void processHtml(IFile file) {
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

    // TODO (danrubel): reenable once build participants have been refactored
//    HtmlAnalyzeHelper.analyze(file);
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
