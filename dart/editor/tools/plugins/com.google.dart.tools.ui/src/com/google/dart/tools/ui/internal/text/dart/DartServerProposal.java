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
package com.google.dart.tools.ui.internal.text.dart;

import com.google.dart.server.AnalysisServer;
import com.google.dart.server.generated.types.CompletionSuggestion;
import com.google.dart.server.generated.types.Element;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.utilities.general.CharOperation;
import com.google.dart.tools.ui.DartElementImageDescriptor;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.text.completion.DartServerProposalCollector;
import com.google.dart.tools.ui.internal.viewsupport.DartElementImageProvider;
import com.google.dart.tools.ui.internal.viewsupport.ImageDescriptorRegistry;
import com.google.dart.tools.ui.text.dart.IDartCompletionProposal;

import static com.google.dart.server.generated.types.CompletionSuggestionKind.IMPORT;
import static com.google.dart.server.generated.types.CompletionSuggestionKind.KEYWORD;
import static com.google.dart.server.generated.types.ElementKind.CLASS;
import static com.google.dart.server.generated.types.ElementKind.CLASS_TYPE_ALIAS;
import static com.google.dart.server.generated.types.ElementKind.CONSTRUCTOR;
import static com.google.dart.server.generated.types.ElementKind.FIELD;
import static com.google.dart.server.generated.types.ElementKind.FUNCTION;
import static com.google.dart.server.generated.types.ElementKind.FUNCTION_TYPE_ALIAS;
import static com.google.dart.server.generated.types.ElementKind.GETTER;
import static com.google.dart.server.generated.types.ElementKind.LOCAL_VARIABLE;
import static com.google.dart.server.generated.types.ElementKind.METHOD;
import static com.google.dart.server.generated.types.ElementKind.PREFIX;
import static com.google.dart.server.generated.types.ElementKind.SETTER;
import static com.google.dart.server.generated.types.ElementKind.TOP_LEVEL_VARIABLE;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension3;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension4;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension5;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension6;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.osgi.util.TextProcessor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

/**
 * {@link DartServerProposal} represents a code completion suggestion returned by
 * {@link AnalysisServer}.
 */
public class DartServerProposal implements ICompletionProposal, ICompletionProposalExtension,
    ICompletionProposalExtension2, ICompletionProposalExtension3, ICompletionProposalExtension4,
    ICompletionProposalExtension5, ICompletionProposalExtension6, IDartCompletionProposal {

  private static final String RIGHT_ARROW = " \u2192 "; //$NON-NLS-1$

  private final DartServerProposalCollector collector;
  private final CompletionSuggestion suggestion;
  private final int relevance;
  private final StyledString styledCompletion;
  private Image image;

  public DartServerProposal(DartServerProposalCollector collector, CompletionSuggestion suggestion) {
    this.collector = collector;
    this.suggestion = suggestion;
    this.relevance = computeRelevance();
    this.styledCompletion = computeStyledDisplayString();
  }

  @Override
  public void apply(IDocument document) {
    // not used
  }

  @Override
  public void apply(IDocument document, char trigger, int offset) {
    // not used
  }

  @Override
  public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
    int length = offset - collector.getReplacementOffset();
    try {
      viewer.getDocument().replace(collector.getReplacementOffset(), length, getCompletion());
    } catch (BadLocationException e) {
      DartCore.logInformation("Failed to replace offset:" + collector.getReplacementOffset()
          + " length:" + length + " with:" + getCompletion(), e);
    }
  }

  @Override
  public String getAdditionalProposalInfo() {
    // getAdditionalProposalInfo(IProgressMonitor monitor) is called instead of this method.
    return null;
  }

  @Override
  public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
    //TODO (danrubel): determine if additional information is needed and supply it
    return null;
  }

  @Override
  public IContextInformation getContextInformation() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int getContextInformationPosition() {
    return collector.getReplacementOffset() + collector.getReplacementLength();
  }

  @Override
  public String getDisplayString() {
    // this method is used for alphabetic sorting,
    // while getStyledDisplayString() is displayed to the user.
    return getCompletion();
  }

  @Override
  public Image getImage() {
    if (image == null) {
      image = computeImage();
    }
    return image;
  }

  @Override
  public IInformationControlCreator getInformationControlCreator() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int getPrefixCompletionStart(IDocument document, int completionOffset) {
    return collector.getReplacementOffset();
  }

  @Override
  public CharSequence getPrefixCompletionText(IDocument document, int completionOffset) {
    int length = Math.max(0, completionOffset - collector.getReplacementOffset());
    return getCompletion().substring(0, length);
  }

  @Override
  public int getRelevance() {
    return relevance;
  }

  @Override
  public Point getSelection(IDocument document) {
    return new Point(collector.getReplacementOffset() + getCompletion().length(), 0);
  }

  @Override
  public StyledString getStyledDisplayString() {
    return styledCompletion;
  }

  @Override
  public char[] getTriggerCharacters() {
    return null;
  }

  @Override
  public boolean isAutoInsertable() {
    return false;
  }

  @Override
  public boolean isValidFor(IDocument document, int offset) {
    // replaced by validate(IDocument, int, event)
    return true;
  }

  @Override
  public void selected(ITextViewer viewer, boolean smartToggle) {
    // called when the proposal is selected
  }

  @Override
  public void unselected(ITextViewer viewer) {
    // called when the proposal is unselected
  }

  @Override
  public boolean validate(IDocument document, int offset, DocumentEvent event) {
    int replacementOffset = collector.getReplacementOffset();
    if (offset < replacementOffset) {
      return false;
    }
    String prefix;
    try {
      prefix = document.get(replacementOffset, offset - replacementOffset);
    } catch (BadLocationException x) {
      return false;
    }
    String string = TextProcessor.deprocess(getDisplayString());
    if (string.length() < prefix.length()) {
      return false;
    }
    String start = string.substring(0, prefix.length());
    char[] pattern = prefix.toCharArray();
    char[] name = string.toCharArray();
    return start.equalsIgnoreCase(prefix)
        || CharOperation.camelCaseMatch(pattern, 0, pattern.length, name, 0, name.length, false);
  }

  private Image computeImage() {
    ImageDescriptorRegistry fRegistry = DartToolsPlugin.getImageDescriptorRegistry();
    ImageDescriptor descriptor = null;
    int overlay = 0;

    String kind;
    boolean isPrivate;
    boolean isTopLevelOrStatic;
    boolean isAbstract;
    Element element = suggestion.getElement();
    if (element != null) {
      kind = element.getKind();
      isPrivate = element.isPrivate();
      isTopLevelOrStatic = element.isTopLevelOrStatic();
      isAbstract = element.isAbstract();
    } else {
      kind = suggestion.getKind();
      isPrivate = false;
      isTopLevelOrStatic = false;
      isAbstract = false;
      if (!IMPORT.equals(kind) && !KEYWORD.equals(kind)) {
        DartCore.logError("Expected element for suggestion kind: " + kind);
      }
    }

    // TODO (danrubel) additional info needed from suggestion
    boolean isInInterfaceOrAnnotation = false;

    if (CLASS.equals(kind)) {
      if (isPrivate && !isInInterfaceOrAnnotation) {
        descriptor = DartPluginImages.DESC_DART_CLASS_PRIVATE;
      } else {
        descriptor = DartPluginImages.DESC_DART_CLASS_PUBLIC;
      }
      if (isAbstract) {
        overlay = DartElementImageDescriptor.ABSTRACT;
      }
    }

    else if (CLASS_TYPE_ALIAS.equals(kind)) {
      descriptor = DartPluginImages.DESC_DART_CLASS_TYPE_ALIAS;
    }

    else if (CONSTRUCTOR.equals(kind)) {
      if (isPrivate && !isInInterfaceOrAnnotation) {
        descriptor = DartPluginImages.DESC_DART_METHOD_PRIVATE;
      } else {
        descriptor = DartPluginImages.DESC_DART_METHOD_PUBLIC;
      }
      overlay = DartElementImageDescriptor.CONSTRUCTOR;
    }

    else if (FUNCTION_TYPE_ALIAS.equals(kind)) {
      if (isPrivate && !isInInterfaceOrAnnotation) {
        descriptor = DartPluginImages.DESC_DART_FUNCTIONTYPE_PRIVATE;
      } else {
        descriptor = DartPluginImages.DESC_DART_FUNCTIONTYPE_PUBLIC;
      }
    }

    else if (FIELD.equals(kind)) {
      if (isPrivate && !isInInterfaceOrAnnotation) {
        descriptor = DartPluginImages.DESC_DART_FIELD_PRIVATE;
      } else {
        descriptor = DartPluginImages.DESC_DART_FIELD_PUBLIC;
      }
      if (isTopLevelOrStatic) {
        overlay = DartElementImageDescriptor.STATIC;
      }
    }

    else if (FUNCTION.equals(kind)) {
      if (isPrivate && !isInInterfaceOrAnnotation) {
        descriptor = DartPluginImages.DESC_DART_METHOD_PRIVATE;
      } else {
        descriptor = DartPluginImages.DESC_DART_METHOD_PUBLIC;
      }
      if (isTopLevelOrStatic) {
        overlay = DartElementImageDescriptor.STATIC;
      }
    }

    else if (GETTER.equals(kind)) {
      if (isPrivate && !isInInterfaceOrAnnotation) {
        descriptor = DartPluginImages.DESC_DART_METHOD_PRIVATE;
      } else {
        descriptor = DartPluginImages.DESC_DART_METHOD_PUBLIC;
      }
      overlay = DartElementImageDescriptor.GETTER;
    }

    else if (IMPORT.equals(kind)) {
      descriptor = DartPluginImages.DESC_OBJS_LIBRARY;
    }

    else if (KEYWORD.equals(kind)) {
      descriptor = DartPluginImages.DESC_DART_KEYWORD;
    }

    else if (PREFIX.equals(kind)) {
      descriptor = DartPluginImages.DESC_OBJS_LIBRARY;
    }

    else if (LOCAL_VARIABLE.equals(kind)) {
      descriptor = DartPluginImages.DESC_OBJS_LOCAL_VARIABLE;
    }

    else if (METHOD.equals(kind)) {
      System.out.println("method: " + element.getName() + "(" + element.getParameters() + ")");
      if (isPrivate && !isInInterfaceOrAnnotation) {
        descriptor = DartPluginImages.DESC_DART_METHOD_PRIVATE;
      } else {
        descriptor = DartPluginImages.DESC_DART_METHOD_PUBLIC;
      }
      if (isTopLevelOrStatic) {
        overlay = DartElementImageDescriptor.STATIC;
      }
    }

    else if (SETTER.equals(kind)) {
      if (isPrivate && !isInInterfaceOrAnnotation) {
        descriptor = DartPluginImages.DESC_DART_METHOD_PRIVATE;
      } else {
        descriptor = DartPluginImages.DESC_DART_METHOD_PUBLIC;
      }
      overlay = DartElementImageDescriptor.SETTER;
    }

    else if (TOP_LEVEL_VARIABLE.equals(kind)) {
      descriptor = DartPluginImages.DESC_OBJS_TOP_LEVEL_VARIABLE;
    }

    if (descriptor != null) {
      if (suggestion.isDeprecated()) {
        overlay |= DartElementImageDescriptor.DEPRECATED;
      }
      if (overlay != 0) {
        descriptor = new DartElementImageDescriptor(
            descriptor,
            overlay,
            DartElementImageProvider.SMALL_SIZE);
      }
    } else {
      descriptor = DartPluginImages.DESC_BLANK;
    }
    return fRegistry.get(descriptor);
  }

  private int computeRelevance() {
    String relevance = suggestion.getRelevance();
    if (relevance == "HIGH") {
      return 0;
    } else if (relevance == "LOW") {
      return 2;
    } else { // DEFAULT
      return 1;
    }
  }

  private StyledString computeStyledDisplayString() {
    StyledString buf = new StyledString();
    buf.append(getCompletion());

    String returnType = suggestion.getReturnType();
    if (returnType != null && returnType.length() > 0) {
      buf.append(RIGHT_ARROW, StyledString.QUALIFIER_STYLER);
      buf.append(returnType, StyledString.QUALIFIER_STYLER);
    }

    return buf;
  }

  private String getCompletion() {
    return suggestion.getCompletion();
  }
}
