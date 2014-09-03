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
import com.google.dart.engine.element.angular.AngularDecoratorElement;
import com.google.dart.engine.element.angular.AngularElement;
import com.google.dart.engine.element.angular.AngularHasAttributeSelectorElement;
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
  /**
   * Container with information about an attribute completion.
   */
  private static class AttributeCompletion {
    final String name;

    public AttributeCompletion(String name) {
      this.name = name;
    }
  }

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
  private HtmlUnit htmlUnit;
  private HtmlElement htmlElement;
  private AngularApplication application;
  private List<ICompletionProposal> proposals;

  private boolean doExpressionCompletion;

  @Override
  public List<ICompletionProposal> computeCompletionProposals(
      CompletionProposalInvocationContext completionContext, IProgressMonitor monitor) {
    try {
      viewer = completionContext.getViewer();
      document = (IStructuredDocument) viewer.getDocument();
      offset = completionContext.getInvocationOffset();
      proposals = Lists.newArrayList();
      // wait for Angular resolution
      if (!waitForResolution()) {
        return proposals;
      }
      // try to complete as Angular specific HTML entity
      doExpressionCompletion = true;
      completeAttribute();
      completeTag();
      // maybe complete as Dart
      if (doExpressionCompletion) {
        completeExpression(monitor);
      }
      // done
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
    // prepare tag region in document
    IStructuredDocumentRegion tagRegion = getStructuredDocumentRegion(viewer, offset);
    if (tagRegion == null) {
      return;
    }
    if (!tagRegion.isEnded()) {
      return;
    }
    int tagOffset = tagRegion.getStartOffset();
    // prepare possible attribute completions
    List<AttributeCompletion> attributeCompletions = Lists.newArrayList();
    // add component properties
    {
      AngularComponentElement component = getAngularComponent();
      if (component != null) {
        List<AngularPropertyElement> componentProperties = getSortedProperties(component);
        for (AngularPropertyElement property : componentProperties) {
          String propertyName = property.getName();
          AttributeCompletion completion = new AttributeCompletion(propertyName);
          attributeCompletions.add(completion);
        }
      }
    }
    // add directives
    for (AngularElement angularElement : application.getElements()) {
      if (angularElement instanceof AngularDecoratorElement) {
        AngularDecoratorElement directive = (AngularDecoratorElement) angularElement;
        AngularSelectorElement selector = directive.getSelector();
        if (selector instanceof AngularHasAttributeSelectorElement) {
          AngularHasAttributeSelectorElement attributeSelector = (AngularHasAttributeSelectorElement) selector;
          attributeCompletions.add(new AttributeCompletion(attributeSelector.getName()));
        }
      }
    }
    // prepare text region
    ITextRegion textRegion = tagRegion.getRegionAtCharacterOffset(offset);
    if (textRegion == null) {
      return;
    }
    String textType = textRegion.getType();
    // "<tag attrNamePrefix!>"
    if (DOMRegionContext.XML_TAG_CLOSE.equals(textType)) {
      ITextRegion prevTextRegion = tagRegion.getRegionAtCharacterOffset(offset - 1);
      String prevTextType = prevTextRegion.getType();
      if (DOMRegionContext.XML_TAG_ATTRIBUTE_NAME.equals(prevTextType)) {
        textRegion = prevTextRegion;
        textType = prevTextType;
      }
    }
    // "<tag attrNamePrefix! otherAttr='bar'>"
    if (DOMRegionContext.XML_TAG_ATTRIBUTE_NAME.equals(textType)) {
      int attrOffset = tagOffset + textRegion.getStart();
      int attrLength = textRegion.getTextLength();
      String attrPrefix = document.get(attrOffset, attrLength);
      // propose component properties
      for (AttributeCompletion completion : attributeCompletions) {
        String propertyName = completion.name;
        if (propertyName.startsWith(attrPrefix)) {
          addEmptyAttributeProposal(attrOffset, attrLength, propertyName);
        }
      }
      // done
      doExpressionCompletion = false;
      return;
    }
    // after tag name or other attribute value
    if (DOMRegionContext.XML_TAG_NAME.equals(textType)) {
      if (textRegion.getLength() > 1 + textRegion.getTextLength()) {
        for (AttributeCompletion completion : attributeCompletions) {
          String propertyName = completion.name;
          addEmptyAttributeProposal(offset, 0, propertyName);
        }
        doExpressionCompletion = false;
      }
      // done
      return;
    }
    // "<tag !>" or "<tag foo='value'! >"
    if (DOMRegionContext.XML_TAG_CLOSE.equals(textType)
        || DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE.equals(textType)) {
      if (hasWhitespaceBetweenLastAndOffset(tagRegion)) {
        for (AttributeCompletion completion : attributeCompletions) {
          String propertyName = completion.name;
          addEmptyAttributeProposal(offset, 0, propertyName);
        }
        doExpressionCompletion = false;
      }
      // done
      return;
    }
  }

  private void completeExpression(IProgressMonitor monitor) {
    // find Expression
    final Expression expression = HtmlUnitUtils.getExpression(htmlUnit, offset);
    if (expression == null) {
      return;
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
          null,
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
        null,
        assistContext,
        null);
    List<ICompletionProposal> dartProposals = proposalComputer.computeCompletionProposals(
        completionContext,
        monitor);
    proposals.addAll(dartProposals);
  }

  private void completeTag() throws Exception {
    int nameOffset = 0;
    int nameLength = 0;
    String namePrefix = null;
    // prepare region in document
    IStructuredDocumentRegion tagRegion = getStructuredDocumentRegion(viewer, offset);
    if (tagRegion == null) {
      return;
    }
    String tagType = tagRegion.getType();
    //  "<prefix!" completion
    if (DOMRegionContext.XML_TAG_NAME.equals(tagType)) {
      int tagOffset = tagRegion.getStartOffset();
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
      // prepare name information
      nameOffset = tagOffset + textRegion.getStart();
      nameLength = textRegion.getTextLength();
      namePrefix = document.get(nameOffset, nameLength);
    }
    //  "<!" completion
    if (DOMRegionContext.XML_CONTENT.equals(tagType)) {
      if (offset != 0 && document.get(offset - 1, 1).equals("<")) {
        nameOffset = offset;
        nameLength = 0;
        namePrefix = "";
      }
    }
    // check if completion is activated at a supported position
    if (namePrefix == null) {
      return;
    }
    // cannot be a Dart expression
    doExpressionCompletion = false;
    // complete the tag name
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
                tagName,
                null,
                null));
          }
        }
      }
    }
  }

  /**
   * Return an {@link AngularComponentElement} that encloses given offset.
   */
  private AngularComponentElement getAngularComponent() {
    // prepare enclosing tag
    XmlTagNode tagNode = HtmlUnitUtils.getEnclosingTagNode(htmlUnit, offset);
    if (tagNode == null) {
      return null;
    }
    // prepare Angular selector
    Element tagElement = tagNode.getElement();
    if (!(tagElement instanceof AngularTagSelectorElement)) {
      return null;
    }
    AngularTagSelectorElement selector = (AngularTagSelectorElement) tagElement;
    // prepare Angular component
    if (!(selector.getEnclosingElement() instanceof AngularComponentElement)) {
      return null;
    }
    return (AngularComponentElement) selector.getEnclosingElement();
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

  private void prepareResolution() {
    HtmlReconcilerHook reconciler = HtmlReconcilerManager.getInstance().reconcilerFor(document);
    htmlUnit = reconciler.getResolvedUnit();
    application = reconciler.getApplication();
    if (htmlUnit != null) {
      htmlElement = htmlUnit.getElement();
    }
  }

  private boolean waitForResolution() {
    long start = System.currentTimeMillis();
    while (System.currentTimeMillis() - start < 300) {
      prepareResolution();
      if (htmlUnit != null && htmlElement != null && application != null) {
        return true;
      }
      Thread.yield();
    }
    return false;
  }
}
