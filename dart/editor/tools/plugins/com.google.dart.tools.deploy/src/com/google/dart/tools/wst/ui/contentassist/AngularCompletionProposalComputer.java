package com.google.dart.tools.wst.ui.contentassist;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.HtmlElement;
import com.google.dart.engine.element.angular.AngularComponentElement;
import com.google.dart.engine.element.angular.AngularElement;
import com.google.dart.engine.element.angular.AngularPropertyElement;
import com.google.dart.engine.element.angular.AngularSelectorElement;
import com.google.dart.engine.element.angular.AngularTagSelectorElement;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.html.ast.HtmlUnitUtils;
import com.google.dart.engine.html.ast.XmlTagNode;
import com.google.dart.engine.index.Index;
import com.google.dart.engine.internal.element.angular.AngularApplication;
import com.google.dart.engine.search.SearchEngineFactory;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.deploy.Activator;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.text.dart.DartCompletionProposalComputer;
import com.google.dart.tools.ui.text.dart.ContentAssistInvocationContext;
import com.google.dart.tools.ui.text.dart.DartContentAssistInvocationContext;
import com.google.dart.tools.wst.ui.HtmlReconcilerHook;
import com.google.dart.tools.wst.ui.HtmlReconcilerManager;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext;
import org.eclipse.wst.sse.ui.contentassist.ICompletionProposalComputer;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;

import static org.eclipse.wst.sse.ui.internal.contentassist.ContentAssistUtils.getStructuredDocumentRegion;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * {@link ICompletionProposalComputer} for Angular HTML.
 */
@SuppressWarnings("restriction")
public class AngularCompletionProposalComputer implements ICompletionProposalComputer {
  private static final List<ICompletionProposal> EMPTY_PROPOSALS = ImmutableList.of();

  private static List<AngularPropertyElement> getSortedProperties(AngularComponentElement component) {
    List<AngularPropertyElement> properties = Lists.newArrayList(component.getProperties());
    Collections.sort(properties, new Comparator<AngularPropertyElement>() {
      @Override
      public int compare(AngularPropertyElement o1, AngularPropertyElement o2) {
        String name1 = o1.getName();
        String name2 = o2.getName();
        return name1.compareTo(name2);
      }
    });
    return properties;
  }

  private final DartCompletionProposalComputer proposalComputer = new DartCompletionProposalComputer();
  private ITextViewer viewer;
  private IStructuredDocument document;
  private int offset;
  private AnalysisContext analysisContext;
  private Source source;
  private HtmlUnit htmlUnit;
  private HtmlElement htmlElement;
  private AngularApplication application;
  private List<ICompletionProposal> proposals;

  @Override
  public List<ICompletionProposal> computeCompletionProposals(
      CompletionProposalInvocationContext completionContext, IProgressMonitor monitor) {
    try {
      viewer = completionContext.getViewer();
      document = (IStructuredDocument) viewer.getDocument();
      offset = completionContext.getInvocationOffset();
      // prepare resolved HtmlUnit
      htmlUnit = getResolvedHtmlUnit();
      if (htmlUnit == null) {
        return EMPTY_PROPOSALS;
      }
      // prepare HtmlElement
      htmlElement = htmlUnit.getElement();
      if (htmlElement == null) {
        return EMPTY_PROPOSALS;
      }
      analysisContext = htmlElement.getContext();
      source = htmlElement.getSource();
      // prepare AngularApplication
      application = analysisContext.getAngularApplicationWithHtml(source);
      if (application == null) {
        return EMPTY_PROPOSALS;
      }
      // try to complete as Dart
      {
        List<ICompletionProposal> result = completeExpression(monitor);
        if (result != null) {
          return result;
        }
      }
      // try to complete as Angular specific HTML entity
      proposals = Lists.newArrayList();
      completeAttribute();
      completeTag();
      return proposals;
    } catch (Throwable e) {
      DartToolsPlugin.log(e);
    } finally {
      viewer = null;
      document = null;
      offset = 0;
    }
    return EMPTY_PROPOSALS;
  }

  @Override
  public List<ICompletionProposal> computeContextInformation(
      CompletionProposalInvocationContext context, IProgressMonitor monitor) {
    List<ICompletionProposal> proposals = Lists.newArrayList();
    return proposals;
  }

  @Override
  public String getErrorMessage() {
    return null;
  }

  @Override
  public void sessionEnded() {
  }

  @Override
  public void sessionStarted() {
  }

  private void addEmptyAttributeProposal(int offset, int length, String name) {
    String replacement = name + "=" + '"';
    int cursorPosition = replacement.length();
    replacement += '"';
    proposals.add(new CompletionProposal(
        replacement,
        offset,
        length,
        cursorPosition,
        Activator.getImage("icons/full/dart16/angular_16_blue.png"),
        null,
        null,
        null));
  }

  private void completeAttribute() throws Exception {
    // prepare region in document
    IStructuredDocumentRegion tagRegion = getStructuredDocumentRegion(viewer, offset);
    if (tagRegion == null) {
      return;
    }
    String tagType = tagRegion.getType();
    int tagOffset = tagRegion.getStartOffset();
    // prepare Angular component
    AngularComponentElement component = null;
    if (DOMRegionContext.XML_TAG_NAME.equals(tagType)) {
      // prepare enclosing tag
      XmlTagNode tagNode = HtmlUnitUtils.getEnclosingTagNode(htmlUnit, tagOffset);
      if (tagNode == null) {
        return;
      }
      // prepare Angular selector
      Element tagElement = tagNode.getElement();
      if (!(tagElement instanceof AngularTagSelectorElement)) {
        return;
      }
      AngularTagSelectorElement selector = (AngularTagSelectorElement) tagElement;
      // prepare Angular component
      if (!(selector.getEnclosingElement() instanceof AngularComponentElement)) {
        return;
      }
      component = (AngularComponentElement) selector.getEnclosingElement();
    }
    if (component == null) {
      return;
    }
    // prepare text region
    ITextRegion textRegion = tagRegion.getRegionAtCharacterOffset(offset);
    if (textRegion == null) {
      return;
    }
    String textType = textRegion.getType();
    // attribute name prefix
    if (DOMRegionContext.XML_TAG_ATTRIBUTE_NAME.equals(textType)) {
      int attrOffset = tagOffset + textRegion.getStart();
      int attrLength = textRegion.getTextLength();
      String attrPrefix = document.get(attrOffset, attrLength);
      // propose component properties
      for (AngularPropertyElement property : getSortedProperties(component)) {
        String propertyName = property.getName();
        if (propertyName.startsWith(attrPrefix)) {
          addEmptyAttributeProposal(attrOffset, attrLength, propertyName);
        }
      }
      // done
      return;
    }
    // after tag name or other attribute value
    if (DOMRegionContext.XML_TAG_NAME.equals(textType)) {
      if (textRegion.getLength() > 1 + textRegion.getTextLength()) {
        for (AngularPropertyElement property : getSortedProperties(component)) {
          String propertyName = property.getName();
          addEmptyAttributeProposal(offset, 0, propertyName);
        }
      }
      // done
      return;
    }
    // "<tag !>" or "<tag foo='value'! >"
    if (DOMRegionContext.XML_TAG_CLOSE.equals(textType)
        || DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE.equals(textType)) {
      if (hasWhitespaceBetweenLastAndOffset(tagRegion)) {
        for (AngularPropertyElement property : getSortedProperties(component)) {
          String propertyName = property.getName();
          addEmptyAttributeProposal(offset, 0, propertyName);
        }
      }
      // done
      return;
    }
  }

  private List<ICompletionProposal> completeExpression(IProgressMonitor monitor) {
    // find Expression
    final Expression expression = HtmlUnitUtils.getExpression(htmlUnit, offset);
    if (expression == null) {
      return null;
    }
    // prepare AssistContext
    final AssistContext assistContext;
    {
      AnalysisContext analysisContext = htmlElement.getContext();
      Index index = DartCore.getProjectManager().getIndex();
      assistContext = new AssistContext(
          SearchEngineFactory.createSearchEngine(index),
          analysisContext,
          null,
          offset,
          0) {
        @Override
        public CompilationUnitElement getCompilationUnitElement() {
          return htmlElement.getAngularCompilationUnit();
        }

        @Override
        public AstNode getCoveredNode() {
          return expression;
        }
      };
    }
    // use Dart completion
    ContentAssistInvocationContext completionContext = new DartContentAssistInvocationContext(
        viewer,
        offset,
        null) {
      @Override
      public AssistContext getAssistContext() {
        return assistContext;
      }
    };
    return proposalComputer.computeCompletionProposals(completionContext, monitor);
  }

  private void completeTag() throws Exception {
    // prepare region in document
    IStructuredDocumentRegion tagRegion = getStructuredDocumentRegion(viewer, offset);
    if (tagRegion == null) {
      return;
    }
    String tagType = tagRegion.getType();
    int tagOffset = tagRegion.getStartOffset();
    // should be XML_TAG_NAME completion
    if (!DOMRegionContext.XML_TAG_NAME.equals(tagType)) {
      return;
    }
    // we want a tag completion for "<prefix! <some-other-tag>" but not for "<closed-tag !>"
    if (tagRegion.isEnded()) {
      return;
    }
    // prepare text region
    ITextRegion textRegion = tagRegion.getRegionAtCharacterOffset(offset);
    if (textRegion == null) {
      return;
    }
    String textType = textRegion.getType();
    // should be XML_TAG_NAME completion
    if (!DOMRegionContext.XML_TAG_NAME.equals(textType)) {
      return;
    }
    // complete the tag name
    int nameOffset = tagOffset + textRegion.getStart();
    int nameLength = textRegion.getTextLength();
    String namePrefix = document.get(nameOffset, nameLength);
    for (AngularElement element : application.getElements()) {
      if (element instanceof AngularComponentElement) {
        AngularComponentElement component = (AngularComponentElement) element;
        AngularSelectorElement selector = component.getSelector();
        if (selector instanceof AngularTagSelectorElement) {
          AngularTagSelectorElement tagSelector = (AngularTagSelectorElement) selector;
          String tagName = tagSelector.getName();
          if (tagName.startsWith(namePrefix)) {
            String replacement = tagName + ">";
            int cursorPosition = replacement.length();
            replacement += "</" + tagName + ">";
            proposals.add(new CompletionProposal(
                replacement,
                nameOffset,
                nameLength,
                cursorPosition,
                Activator.getImage("icons/full/dart16/angular_16_blue.png"),
                null,
                null,
                null));
          }
        }
      }
    }
  }

  private HtmlUnit getResolvedHtmlUnit() {
    HtmlReconcilerHook reconciler = HtmlReconcilerManager.getInstance().reconcilerFor(document);
    return reconciler.getResolvedUnit();
  }

  private boolean hasWhitespaceBetweenLastAndOffset(IStructuredDocumentRegion tagRegion) {
    ITextRegion textRegion = tagRegion.getRegionAtCharacterOffset(offset);
    // tag closing is not interesting, we need to know what is before it: "<tag !>" or "<tag!>"
    if (DOMRegionContext.XML_TAG_CLOSE.equals(textRegion.getType())) {
      textRegion = tagRegion.getRegionAtCharacterOffset(offset - 1);
    }
    // we need a whitespace: "<tag !" or "<tag foo='bar' !"
    return offset > tagRegion.getStartOffset() + textRegion.getTextEnd();
  }
}
