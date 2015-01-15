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

import com.google.common.util.concurrent.Uninterruptibles;
import com.google.dart.engine.services.util.DartDocUtilities;
import com.google.dart.engine.utilities.instrumentation.Instrumentation;
import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;
import com.google.dart.server.AnalysisServer;
import com.google.dart.server.GetHoverConsumer;
import com.google.dart.server.generated.types.CompletionSuggestion;
import com.google.dart.server.generated.types.CompletionSuggestionKind;
import com.google.dart.server.generated.types.Element;
import com.google.dart.server.generated.types.HoverInformation;
import com.google.dart.server.generated.types.Location;
import com.google.dart.server.generated.types.RequestError;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.utilities.general.CharOperation;
import com.google.dart.tools.ui.DartElementImageDescriptor;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.PreferenceConstants;
import com.google.dart.tools.ui.internal.text.completion.DartServerProposalCollector;
import com.google.dart.tools.ui.internal.text.editor.DartTextHover;
import com.google.dart.tools.ui.internal.text.editor.ElementLabelProvider_NEW;
import com.google.dart.tools.ui.internal.text.html.HTMLPrinter;
import com.google.dart.tools.ui.internal.viewsupport.DartElementImageProvider;
import com.google.dart.tools.ui.internal.viewsupport.ImageDescriptorRegistry;
import com.google.dart.tools.ui.text.dart.IDartCompletionProposal;

import static com.google.dart.server.generated.types.CompletionSuggestionKind.IMPORT;
import static com.google.dart.server.generated.types.CompletionSuggestionKind.KEYWORD;
import static com.google.dart.server.generated.types.ElementKind.FIELD;
import static com.google.dart.server.generated.types.ElementKind.FUNCTION;
import static com.google.dart.server.generated.types.ElementKind.GETTER;
import static com.google.dart.server.generated.types.ElementKind.LOCAL_VARIABLE;
import static com.google.dart.server.generated.types.ElementKind.METHOD;
import static com.google.dart.server.generated.types.ElementKind.PARAMETER;
import static com.google.dart.server.generated.types.ElementKind.SETTER;
import static com.google.dart.server.generated.types.ElementKind.TOP_LEVEL_VARIABLE;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.AbstractReusableInformationControlCreator;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
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
import org.eclipse.jface.text.link.ILinkedModeListener;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedModeUI.ExitFlags;
import org.eclipse.jface.text.link.LinkedModeUI.IExitPolicy;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.osgi.util.TextProcessor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;
import org.osgi.framework.Bundle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * {@link DartServerProposal} represents a code completion suggestion returned by
 * {@link AnalysisServer}.
 */
public class DartServerProposal implements ICompletionProposal, ICompletionProposalExtension,
    ICompletionProposalExtension2, ICompletionProposalExtension3, ICompletionProposalExtension4,
    ICompletionProposalExtension5, ICompletionProposalExtension6, IDartCompletionProposal {

  /**
   * The control creator.
   */
  private static final class ControlCreator extends AbstractReusableInformationControlCreator {
    @Override
    @SuppressWarnings("restriction")
    public IInformationControl doCreateInformationControl(Shell parent) {
      String font = PreferenceConstants.APPEARANCE_JAVADOC_FONT;
      return new org.eclipse.jface.internal.text.html.BrowserInformationControl(
          parent,
          font,
          DartToolsPlugin.getAdditionalInfoAffordanceString()) {
        @Override
        public IInformationControlCreator getInformationPresenterControlCreator() {
          return new PresenterControlCreator();
        }
      };
    }
  }

  /**
   * Presenter control creator.
   */
  private static final class PresenterControlCreator extends
      AbstractReusableInformationControlCreator {
    @Override
    @SuppressWarnings("restriction")
    public IInformationControl doCreateInformationControl(Shell parent) {
      String font = PreferenceConstants.APPEARANCE_JAVADOC_FONT;
      return new org.eclipse.jface.internal.text.html.BrowserInformationControl(parent, font, true);
    }
  }

  private final class ProposalContextInformation implements IContextInformation {
    String informationDisplayString;

    public ProposalContextInformation(String informationDisplayString) {
      this.informationDisplayString = informationDisplayString;
    }

    @Override
    public String getContextDisplayString() {
      // TODO(paulberry): apparently not used?
      return null;
    }

    @Override
    public Image getImage() {
      // TODO(paulberry): apparently not used?
      return null;
    }

    @Override
    public String getInformationDisplayString() {
      return informationDisplayString;
    }

  }

  private static final ElementLabelProvider_NEW ELEMENT_LABEL_PROVIDER = new ElementLabelProvider_NEW();

  /**
   * The CSS used to format DartDoc information.
   */
  private static String CSS_STYLES;
  private final static char[] TRIGGERS = new char[] {
      ' ', '\t', '.', ',', ';', '(', ')', '[', ']', '{', '}', '=', '!', '#'};
  private final DartServerProposalCollector collector;
  private final CompletionSuggestion suggestion;
  private final int relevance;

  private final StyledString styledCompletion;

  private Image image;

  /**
   * The offset into {@link replacementString} where the cursor should be placed if the proposal is
   * accepted. Computed by {@see computeCompletion}.
   */
  private int selectionOffset = 0;

  /**
   * The length of text that should be selected if the proposal is accepted. Computed by {@see
   * computeCompletion}.
   */
  private int selectionLength = 0;

  /**
   * The {@link IInformationControlCreator} for documentation.
   */
  private IInformationControlCreator informationControlCreator;

  /**
   * The replacement string, or null if it has not yet been computed. Computed by {@see
   * computeCompletion}.
   */
  private String replacementString = null;

  /**
   * If the completion is a method call with at least one argument, offsets within replacementString
   * of the arguments. Otherwise null. Computed by {@see computeCompletion}.
   */
  private int[] argumentOffsets = null;

  /**
   * If the completion is a method call with at least one argument, the lengths of the arguments.
   * Otherwise null. Computed by {@see computeCompletion}.
   */
  private int[] argumentLengths = null;

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
    String completion = getCompletion();
    InstrumentationBuilder instrumentation = Instrumentation.builder("ServerProposal-Apply");
    instrumentation.metric("Trigger", trigger);
    instrumentation.data("Completion", completion);
    int replacementOffset = collector.getReplacementOffset();
    int replacementLength = offset - replacementOffset;
    try {
      IDocument doc = viewer.getDocument();
      /*
       * If no characters have been typed and the trigger character is a '.'
       * then then skip the suggestion and just insert the trigger character
       * to prevent suggestion from being inserted between .. in a cascade.
       * This also re-triggers code completion on the cascade.
       */
      if (replacementLength == 0 && trigger == '.') {
        selectionOffset = 1;
        selectionLength = 0;
        doc.replace(offset, 0, Character.toString(trigger));
        return;
      }
      /*
       * Simplistic argument list completion... strip parens and continue
       */
      // TODO (danrubel) improve argument list completion
      if (completion.startsWith("(")) {
        completion = completion.substring(1, completion.length() - 1);
      }
      /*
       * Insert the suggestion
       */
      doc.replace(replacementOffset, replacementLength, completion);
      /*
       * If the suggestion has parameters, initiate entering parameters
       */
      if (argumentLengths != null) {
        // Set up linked position groups for the arguments.
        LinkedModeModel model = new LinkedModeModel();
        buildLinkedModeModel(model, doc, replacementOffset);
        model.forceInstall();

        LinkedModeUI ui = new EditorLinkedModeUI(model, viewer);
        ui.setExitPolicy(new ExitPolicy(')', doc, viewer));
        ui.setExitPosition(viewer, replacementOffset + completion.length(), 0, Integer.MAX_VALUE);
        ui.setCyclingMode(LinkedModeUI.CYCLE_WHEN_NO_PARENT);
        ui.enter();
        return;
      }
      /*
       * Insert the trigger character typed if it is not enter or null
       */
      if (trigger != '\0' && trigger != '\n') {
        doc.replace(
            replacementOffset + selectionOffset,
            selectionLength,
            Character.toString(trigger));
        ++selectionOffset;
        selectionLength = 0;
        return;
      }
    } catch (BadLocationException e) {
      DartCore.logInformation("Failed to replace offset:" + replacementOffset + " length:"
          + replacementLength + " with:" + completion, e);
      instrumentation.metric("Problem", "BadLocationException");
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public String getAdditionalProposalInfo() {
    // getAdditionalProposalInfo(IProgressMonitor monitor) is called instead of this method.
    return null;
  }

  @Override
  public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
    final String[] text = {null};
    Element element = suggestion.getElement();
    if (element != null) {
      Location location = element.getLocation();
      if (location != null) {
        final CountDownLatch latch = new CountDownLatch(1);
        DartCore.getAnalysisServer().analysis_getHover(
            location.getFile(),
            location.getOffset(),
            new GetHoverConsumer() {
              @Override
              public void computedHovers(HoverInformation[] hovers) {
                if (hovers.length != 0) {
                  HoverInformation hover = hovers[0];
                  // prepare HTML content
                  String dartdocText = hover.getDartdoc();
                  String dartdocHtml = DartDocUtilities.getDartDocAsHtml2(dartdocText);
                  String info = DartTextHover.getElementDocumentationHtml(
                      hover.getElementDescription(),
                      dartdocHtml);
                  // wrap into HTML page
                  StringBuffer buffer = new StringBuffer();
                  HTMLPrinter.insertPageProlog(buffer, 0, getCssStyles());
                  buffer.append(info);
                  HTMLPrinter.addPageEpilog(buffer);
                  // done
                  text[0] = buffer.toString();
                  latch.countDown();
                }
              }

              @Override
              public void onError(RequestError requestError) {
                latch.countDown();
              }
            });
        Uninterruptibles.awaitUninterruptibly(latch, 1000, TimeUnit.MILLISECONDS);
      }
    }
    return text[0];
  }

  @Override
  public IContextInformation getContextInformation() {
    String s = getParamString();
    if (s != null) {
      return new ProposalContextInformation(s);
    } else {
      return null;
    }
  }

  @Override
  public int getContextInformationPosition() {
    // The context information position is the position where the arguments begin (this is used by
    // DartParameterListValidator as the starting point for counting commas, to figure out which
    // parameter the user is currently typing).  So it's the same as the start of the text which
    // should be selected when the completion is accepted.
    computeCompletion();
    return collector.getReplacementOffset() + selectionOffset;
  }

  @Override
  public String getDisplayString() {
    // this method is used for alphabetic sorting,
    // while getStyledDisplayString() is displayed to the user.
    return suggestion.getCompletion();
  }

  @Override
  public Image getImage() {
    if (image == null) {
      image = computeImage();
    }
    return image;
  }

  @Override
  @SuppressWarnings("restriction")
  public IInformationControlCreator getInformationControlCreator() {
    // TODO(scheglov) Linux is known to crash sometimes when we create Browser.
    // https://code.google.com/p/dart/issues/detail?id=12903
    // It always was like this.
    if (DartCore.isLinux()) {
      return null;
    }
    // For luckier OSes.
    Shell shell = DartToolsPlugin.getActiveWorkbenchShell();
    if (shell == null
        || !org.eclipse.jface.internal.text.html.BrowserInformationControl.isAvailable(shell)) {
      return null;
    }

    if (informationControlCreator == null) {
      informationControlCreator = new ControlCreator();
    }
    return informationControlCreator;
  }

  @Override
  public int getPrefixCompletionStart(IDocument document, int completionOffset) {
    return collector.getReplacementOffset();
  }

  @Override
  public CharSequence getPrefixCompletionText(IDocument document, int completionOffset) {
    int length = Math.max(0, completionOffset - collector.getReplacementOffset());
    return suggestion.getCompletion().substring(0, length);
  }

  @Override
  public int getRelevance() {
    return relevance;
  }

  @Override
  public Point getSelection(IDocument document) {
    computeCompletion();
    return new Point(collector.getReplacementOffset() + selectionOffset, selectionLength);
  }

  @Override
  public StyledString getStyledDisplayString() {
    return styledCompletion;
  }

  @Override
  public char[] getTriggerCharacters() {
    return TRIGGERS;
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

  protected void buildLinkedModeModel(LinkedModeModel model, IDocument document, int baseOffset)
      throws BadLocationException {
    // TODO(paulberry): consider extending to support optional arguments, as
    // FilledArgumentNamesMethodProposal does.
    for (int i = 0; i != argumentOffsets.length; i++) {
      LinkedPositionGroup group = new LinkedPositionGroup();
      LinkedPosition pos = new LinkedPosition(
          document,
          baseOffset + argumentOffsets[i],
          argumentLengths[i],
          LinkedPositionGroup.NO_STOP);
      group.addPosition(pos);
      model.addGroup(group);
    }
  }

  /**
   * Compute {@link replacementString}, {@link selectionOffset}, {@link selectionLength},
   * {@link argumentOffsets}, and {@link argumentLengths}, if they haven't been computed already.
   */
  private void computeCompletion() {
    if (replacementString != null) {
      // Already computed.
      return;
    }
    List<String> parameterNames = suggestion.getParameterNames();
    if (parameterNames == null) {
      // Just complete a single identifier.
      replacementString = suggestion.getCompletion();
      selectionOffset = replacementString.length();
      selectionLength = 0;
    } else {
      // Complete with the identifier, parens, and arguments.
      StringBuffer buffer = new StringBuffer(suggestion.getCompletion());
      buffer.append('(');
      int requiredParameterCount = suggestion.getRequiredParameterCount();
      if (requiredParameterCount > 0) {
        argumentOffsets = new int[requiredParameterCount];
        argumentLengths = new int[requiredParameterCount];
        for (int i = 0; i < requiredParameterCount; i++) {
          if (i != 0) {
            buffer.append(", ");
          }
          argumentOffsets[i] = buffer.length();
          buffer.append(parameterNames.get(i));
          argumentLengths[i] = buffer.length() - argumentOffsets[i];
        }
        selectionOffset = argumentOffsets[0];
        selectionLength = argumentLengths[0];
      } else {
        selectionOffset = buffer.length();
        selectionLength = 0;
        // If this method does not take any parameters, then position the cursor after the ')'
        if (parameterNames.isEmpty()) {
          ++selectionOffset;
        }
      }
      buffer.append(')');
      replacementString = buffer.toString();
    }
  }

  private Image computeImage() {
    ImageDescriptorRegistry fRegistry = DartToolsPlugin.getImageDescriptorRegistry();
    ImageDescriptor descriptor = null;
    int overlay = 0;

    String kind;
    Element element = suggestion.getElement();
    if (element != null) {
      return ELEMENT_LABEL_PROVIDER.getImage(element);
    } else {
      kind = suggestion.getKind();
      if (!IMPORT.equals(kind) && !KEYWORD.equals(kind)) {
        DartCore.logError("Expected element for suggestion kind: " + kind);
      }
    }

    if (IMPORT.equals(kind)) {
      descriptor = DartPluginImages.DESC_OBJS_LIBRARY;
    }

    else if (KEYWORD.equals(kind)) {
      descriptor = DartPluginImages.DESC_DART_KEYWORD;
    }

    else {
      descriptor = DartPluginImages.DESC_BLANK;
    }

    if (descriptor != null) {
      if (suggestion.isDeprecated()) {
        overlay |= DartElementImageDescriptor.DEPRECATED;
      }
      if (overlay != 0) {
        descriptor = new DartElementImageDescriptor(
            descriptor,
            overlay,
            DartElementImageProvider.BIG_SIZE);
      }
    }

    return fRegistry.get(descriptor);
  }

  private int computeRelevance() {
    int relevance = suggestion.getRelevance();
    if (relevance != 1000) {
      return relevance;
    }
    // TODO (danrubel) move this code to the server
    { // DEFAULT
      Element element = suggestion.getElement();
      if (element != null) {
        String kind = element.getKind();
        if (LOCAL_VARIABLE.equals(kind) || PARAMETER.equals(kind)) {
          return 1059;
        } else if (FIELD.equals(kind)) {
          return 1058;
        } else if (METHOD.equals(kind) || GETTER.equals(kind) || SETTER.equals(kind)
            || FUNCTION.equals(kind)) {
          return 1057;
        } else if (TOP_LEVEL_VARIABLE.equals(kind)) {
          return 1056;
        } else if (KEYWORD.equals(kind)) {
          return 1055;
        }
      }
      return 1000;
    }
  }

  private StyledString computeStyledDisplayString() {
    // element
    Element element = suggestion.getElement();
    if (element != null) {
      return ELEMENT_LABEL_PROVIDER.getStyledText(element);
    }

    // not element
    StyledString buf = new StyledString();
    buf.append(suggestion.getCompletion());
    return buf;
  }

  private String getCompletion() {
    computeCompletion();
    return replacementString;
  }

  /**
   * Returns the style information for displaying HTML content.
   */
  private String getCssStyles() {
    if (CSS_STYLES == null) {
      Bundle bundle = Platform.getBundle(DartToolsPlugin.getPluginId());
      URL url = bundle.getEntry("/DartdocHoverStyleSheet.css"); //$NON-NLS-1$
      if (url != null) {
        BufferedReader reader = null;
        try {
          url = FileLocator.toFileURL(url);
          reader = new BufferedReader(new InputStreamReader(url.openStream()));
          StringBuffer buffer = new StringBuffer(200);
          String line = reader.readLine();
          while (line != null) {
            buffer.append(line);
            buffer.append('\n');
            line = reader.readLine();
          }
          CSS_STYLES = buffer.toString();
        } catch (IOException ex) {
          DartToolsPlugin.log(ex);
        } finally {
          try {
            if (reader != null) {
              reader.close();
            }
          } catch (IOException e) {
          }
        }

      }
    }
    String css = CSS_STYLES;
    if (css != null) {
      FontData fontData = JFaceResources.getFontRegistry().getFontData(
          PreferenceConstants.APPEARANCE_JAVADOC_FONT)[0];
      css = HTMLPrinter.convertTopLevelFont(css, fontData);
    }
    return css;
  }

  /**
   * @return A string representing the parameters or {@code null} if no parameters for completion
   */
  private String getParamString() {
    Element element = suggestion.getElement();
    if (element != null && !CompletionSuggestionKind.IDENTIFIER.equals(suggestion.getKind())) {
      String kind = element.getKind();
      if (!GETTER.equals(kind) && !SETTER.equals(kind)) {
        if (element != null) {
          return element.getParameters();
        }
      }
    }
    return null;
  }
}

/**
 * Allow the linked mode editor to continue running even when the exit character is typed as part of
 * a function argument. Using shift operators in a context that expects balanced angle brackets is
 * not legal syntax and will confuse the linked mode editor.
 */
class ExitPolicy implements IExitPolicy {

  private int parenCount = 0;
  private int braceCount = 0;
  private int bracketCount = 0;
  private int angleBracketCount = 0;
  private char lastChar = (char) 0;

  final char exitChar;
  private final IDocument document;
  private final ITextViewer viewer;

  public ExitPolicy(char exitChar, IDocument document, ITextViewer viewer) {
    this.exitChar = exitChar;
    this.document = document;
    this.viewer = viewer;
  }

  @Override
  public ExitFlags doExit(LinkedModeModel environment, VerifyEvent event, int offset, int length) {
    countGroupChars(event);
    if (event.character == exitChar && isBalanced(exitChar)) {
      if (environment.anyPositionContains(offset)) {
        return new ExitFlags(ILinkedModeListener.UPDATE_CARET, false);
      } else {
        return new ExitFlags(ILinkedModeListener.UPDATE_CARET, true);
      }
    }

    switch (event.character) {
      case ';':
        return new ExitFlags(ILinkedModeListener.EXTERNAL_MODIFICATION
            | ILinkedModeListener.UPDATE_CARET | ILinkedModeListener.EXIT_ALL, true);
      case '\b':
        if (viewer.getSelectedRange().y > 0) {
          return new ExitFlags(ILinkedModeListener.EXTERNAL_MODIFICATION, true);
        }
        return null;
      case SWT.CR:
        // when entering a function as a parameter, we don't want
        // to jump after the parenthesis when return is pressed
        if (offset > 0) {
          try {
            if (document.getChar(offset - 1) == '{') {
              return new ExitFlags(ILinkedModeListener.EXIT_ALL, true);
            }
          } catch (BadLocationException e) {
          }
        }
        return null;
//      case ',':
//        // Making comma act like tab seems like a good idea
        // but it requires auto-insert of matching group chars to work.
//        if (offset > 0) {
//          try {
//            if (fDocument.getChar(offset) == ',') {
//              event.character = 0x09;
//              return null;
//            }
//          } catch (BadLocationException e) {
//          }
//        }
      default:
        return null;
    }
  }

  private void countGroupChar(char ch, int inc) {
    switch (ch) {
      case '(':
        parenCount += inc;
        break;
      case ')':
        parenCount -= inc;
        break;
      case '{':
        braceCount += inc;
        break;
      case '}':
        braceCount -= inc;
        break;
      case '[':
        bracketCount += inc;
        break;
      case ']':
        bracketCount -= inc;
        break;
      case '<':
        angleBracketCount += inc;
        break;
      case '>':
        if (lastChar != '=') {
          // only decrement when not part of =>
          angleBracketCount -= inc;
        }
        break;
      case '=':
        if (lastChar == '>') {
          // deleting => should not change angleBracketCount
          angleBracketCount += inc;
        }
        break;
      default:
        break;
    }
    lastChar = ch;
  }

  private void countGroupChars(VerifyEvent event) {
    char ch = event.character;
    int inc = 1;
    if (ch == '\b') { // TODO Find correct delete chars for Linux & Windows
      inc = -1;
      if (!(event.widget instanceof StyledText)) {
        return;
      }
      Point sel = ((StyledText) event.widget).getSelection();
      try {
        if (sel.x == sel.y) {
          ch = document.getChar(sel.x);
          countGroupChar(ch, inc);
        } else {
          for (int x = sel.y - 1; x >= sel.x; x--) {
            ch = document.getChar(x);
            countGroupChar(ch, inc);
          }
        }
      } catch (BadLocationException ex) {
        return;
      }
    } else {
      countGroupChar(ch, inc);
    }
  }

  private boolean isBalanced(char ch) {
    switch (ch) {
      case ')':
        return parenCount == -1;
      case '}':
        return braceCount == -1;
      case ']':
        return bracketCount == -1;
      case '>':
        return angleBracketCount == -1;
      default:
        return true; // never unbalanced
    }
  }
}
