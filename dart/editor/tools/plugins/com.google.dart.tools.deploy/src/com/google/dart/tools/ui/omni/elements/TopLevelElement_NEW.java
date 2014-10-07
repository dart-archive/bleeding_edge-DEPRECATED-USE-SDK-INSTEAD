/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.tools.ui.omni.elements;

import com.google.dart.server.generated.types.Element;
import com.google.dart.server.generated.types.ElementKind;
import com.google.dart.server.generated.types.SearchResult;
import com.google.dart.tools.ui.DartElementLabels;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.text.editor.ElementLabelProvider_NEW;
import com.google.dart.tools.ui.omni.OmniElement;
import com.google.dart.tools.ui.omni.OmniEntry;
import com.google.dart.tools.ui.omni.OmniProposalProvider;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.resource.ImageDescriptor;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link OmniElement} for a top-level {@link Element}.
 */
public class TopLevelElement_NEW extends OmniElement {
  private static final String DEFAULT_PROJECT = "Dart SDK";
  private final Pattern pattern;
  private final Element element;
  private final Element library;

  public TopLevelElement_NEW(OmniProposalProvider provider, Pattern pattern,
      SearchResult searchResult) {
    super(provider);
    this.pattern = pattern;
    List<Element> path = searchResult.getPath();
    this.element = path.get(0);
    this.library = getLibraryElement(path);
  }

  @Override
  public String getId() {
    return element.getName();
  }

  @Override
  public ImageDescriptor getImageDescriptor() {
    return ElementLabelProvider_NEW.getImageDescriptor(element);
  }

  @Override
  public String getInfoLabel() {
    String info = "";
    String file = element.getLocation().getFile();
    IFile resource = DartUI.getSourceFile(file);
    if (resource != null) {
      info = resource.getProject().getName();
    } else {
      // TODO(scheglov) Analysis Server: implement
      info = DEFAULT_PROJECT;
//      IProject project = DartCore.getProjectManager().getProjectForContextId(element.getContextId());
//      if (project != null) {
//        info = project.getName();
//      } else {
//        info = DEFAULT_PROJECT;
//      }
    }
    return info;
  }

  @Override
  public String getLabel() {
    StringBuffer result = new StringBuffer();
    result.append(element.getName());

    //cache detail offset (used for styling detail area in OmniElement.paint(...))
    detailOffset = result.length();

    if (library != null) {
      result.append(DartElementLabels.CONCAT_STRING);
      result.append(library.getName());
    }

    return result.toString();
  }

  @Override
  public OmniEntry match(String filter, OmniProposalProvider providerForMatching) {
    String text = element.getName();
    Matcher matcher = pattern.matcher(text);
    if (matcher.matches()) {
      int groupCount = matcher.groupCount();
      int[][] inds = new int[groupCount][];
      for (int i = 0; i < groupCount; i++) {
        // OmniEntry expects inclusive start/end, so correct the end
        inds[i] = new int[] {matcher.start(1 + i), matcher.end(1 + i) - 1};
      }
      return new OmniEntry(this, providerForMatching, inds, EMPTY_INDICES);
    }
    return null;
  }

  @Override
  protected void doExecute(String text, UIInstrumentationBuilder instrumentation) {
    instrumentation.data("TypeElement.searchResultSelected", element.getName());
    try {
      DartUI.openInEditor(element, true);
    } catch (Throwable e) {
      DartToolsPlugin.log(e);
    }
  }

  private Element getLibraryElement(List<Element> elements) {
    for (Element element : elements) {
      if (element.getKind() == ElementKind.LIBRARY) {
        return element;
      }
    }
    return null;
  }
}
