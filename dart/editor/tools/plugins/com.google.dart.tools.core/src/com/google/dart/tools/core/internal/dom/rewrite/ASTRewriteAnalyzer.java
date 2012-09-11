/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.core.internal.dom.rewrite;

import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartArrayAccess;
import com.google.dart.compiler.ast.DartArrayLiteral;
import com.google.dart.compiler.ast.DartBinaryExpression;
import com.google.dart.compiler.ast.DartBlock;
import com.google.dart.compiler.ast.DartBooleanLiteral;
import com.google.dart.compiler.ast.DartBreakStatement;
import com.google.dart.compiler.ast.DartCase;
import com.google.dart.compiler.ast.DartCatchBlock;
import com.google.dart.compiler.ast.DartClass;
import com.google.dart.compiler.ast.DartComment;
import com.google.dart.compiler.ast.DartConditional;
import com.google.dart.compiler.ast.DartContinueStatement;
import com.google.dart.compiler.ast.DartDefault;
import com.google.dart.compiler.ast.DartDoWhileStatement;
import com.google.dart.compiler.ast.DartDoubleLiteral;
import com.google.dart.compiler.ast.DartEmptyStatement;
import com.google.dart.compiler.ast.DartExprStmt;
import com.google.dart.compiler.ast.DartField;
import com.google.dart.compiler.ast.DartFieldDefinition;
import com.google.dart.compiler.ast.DartForInStatement;
import com.google.dart.compiler.ast.DartForStatement;
import com.google.dart.compiler.ast.DartFunction;
import com.google.dart.compiler.ast.DartFunctionExpression;
import com.google.dart.compiler.ast.DartFunctionObjectInvocation;
import com.google.dart.compiler.ast.DartFunctionTypeAlias;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartIfStatement;
import com.google.dart.compiler.ast.DartImportDirective;
import com.google.dart.compiler.ast.DartInitializer;
import com.google.dart.compiler.ast.DartIntegerLiteral;
import com.google.dart.compiler.ast.DartLabel;
import com.google.dart.compiler.ast.DartLibraryDirective;
import com.google.dart.compiler.ast.DartMapLiteral;
import com.google.dart.compiler.ast.DartMapLiteralEntry;
import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.compiler.ast.DartMethodInvocation;
import com.google.dart.compiler.ast.DartNamedExpression;
import com.google.dart.compiler.ast.DartNativeBlock;
import com.google.dart.compiler.ast.DartNativeDirective;
import com.google.dart.compiler.ast.DartNewExpression;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartNullLiteral;
import com.google.dart.compiler.ast.DartParameter;
import com.google.dart.compiler.ast.DartParameterizedTypeNode;
import com.google.dart.compiler.ast.DartParenthesizedExpression;
import com.google.dart.compiler.ast.DartPropertyAccess;
import com.google.dart.compiler.ast.DartRedirectConstructorInvocation;
import com.google.dart.compiler.ast.DartReturnStatement;
import com.google.dart.compiler.ast.DartSourceDirective;
import com.google.dart.compiler.ast.DartStringInterpolation;
import com.google.dart.compiler.ast.DartStringLiteral;
import com.google.dart.compiler.ast.DartSuperConstructorInvocation;
import com.google.dart.compiler.ast.DartSuperExpression;
import com.google.dart.compiler.ast.DartSwitchMember;
import com.google.dart.compiler.ast.DartSwitchStatement;
import com.google.dart.compiler.ast.DartSyntheticErrorExpression;
import com.google.dart.compiler.ast.DartSyntheticErrorStatement;
import com.google.dart.compiler.ast.DartThisExpression;
import com.google.dart.compiler.ast.DartThrowExpression;
import com.google.dart.compiler.ast.DartTryStatement;
import com.google.dart.compiler.ast.DartTypeExpression;
import com.google.dart.compiler.ast.DartTypeNode;
import com.google.dart.compiler.ast.DartTypeParameter;
import com.google.dart.compiler.ast.DartUnaryExpression;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.ast.DartUnqualifiedInvocation;
import com.google.dart.compiler.ast.DartVariable;
import com.google.dart.compiler.ast.DartVariableStatement;
import com.google.dart.compiler.ast.DartWhileStatement;
import com.google.dart.compiler.parser.DartScanner;
import com.google.dart.compiler.parser.Token;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.dom.ChildListPropertyDescriptor;
import com.google.dart.tools.core.dom.ChildPropertyDescriptor;
import com.google.dart.tools.core.dom.PropertyDescriptorHelper;
import com.google.dart.tools.core.dom.StructuralPropertyDescriptor;
import com.google.dart.tools.core.dom.rewrite.TargetSourceRangeComputer;
import com.google.dart.tools.core.dom.rewrite.TargetSourceRangeComputer.SourceRange;
import com.google.dart.tools.core.dom.visitor.WrappedDartVisitor;
import com.google.dart.tools.core.dom.visitor.WrappedDartVisitorAdaptor;
import com.google.dart.tools.core.formatter.DefaultCodeFormatterConstants;
import com.google.dart.tools.core.formatter.IndentManipulation;
import com.google.dart.tools.core.internal.compiler.Util;
import com.google.dart.tools.core.internal.compiler.parser.RecoveryScannerData;
import com.google.dart.tools.core.internal.dom.rewrite.ASTRewriteFormatter.BlockContext;
import com.google.dart.tools.core.internal.dom.rewrite.ASTRewriteFormatter.Prefix;
import com.google.dart.tools.core.internal.dom.rewrite.RewriteEventStore.CopySourceInfo;
import com.google.dart.tools.core.internal.util.ScannerHelper;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.text.edits.CopySourceEdit;
import org.eclipse.text.edits.CopyTargetEdit;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MoveSourceEdit;
import org.eclipse.text.edits.MoveTargetEdit;
import org.eclipse.text.edits.RangeMarker;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.swing.text.html.parser.TagElement;

/**
 * Infrastructure to support code modifications. Existing code must stay untouched, new code added
 * with correct formatting, moved code left with the user's formatting / comments. Idea: - Get the
 * AST for existing code - Describe changes - This visitor analyzes the changes or annotations and
 * generates text edits (text manipulation API) that describe the required code changes.
 */
@SuppressWarnings("unused")
public final class ASTRewriteAnalyzer {
  class ListRewriter {
    protected String contantSeparator;
    protected int startPos;

    protected RewriteEvent[] list;

    public final int rewriteList(
        DartNode parent, StructuralPropertyDescriptor property, int offset, String keyword) {
      startPos = offset;
      list = getEvent(parent, property).getChildren();

      int total = list.length;
      if (total == 0) {
        return startPos;
      }

      int currPos = -1;

      int lastNonInsert = -1;
      int lastNonDelete = -1;

      for (int i = 0; i < total; i++) {
        int currMark = list[i].getChangeKind();

        if (currMark != RewriteEvent.INSERTED) {
          lastNonInsert = i;
          if (currPos == -1) {
            DartNode elem = (DartNode) list[i].getOriginalValue();
            currPos = getExtendedOffset(elem);
          }
        }
        if (currMark != RewriteEvent.REMOVED) {
          lastNonDelete = i;
        }
      }

      if (currPos == -1) { // only inserts
        if (keyword.length() > 0) { // creating a new list -> insert keyword
                                    // first (e.g. " throws ")
          TextEditGroup editGroup = getEditGroup(list[0]); // first node is
                                                           // insert
          doTextInsert(offset, keyword, editGroup);
        }
        currPos = offset;
      }
      if (lastNonDelete == -1) { // all removed, set back to start so the
                                 // keyword is removed as well
        currPos = offset;
      }

      int prevEnd = currPos;
      int prevMark = RewriteEvent.UNCHANGED;

      final int NONE = 0, NEW = 1, EXISTING = 2;
      int separatorState = NEW;

      for (int i = 0; i < total; i++) {
        RewriteEvent currEvent = list[i];
        int currMark = currEvent.getChangeKind();
        int nextIndex = i + 1;

        if (currMark == RewriteEvent.INSERTED) {
          TextEditGroup editGroup = getEditGroup(currEvent);
          DartNode node = (DartNode) currEvent.getNewValue();

          if (separatorState == NONE) { // element after last existing element
                                        // (but not first)
            doTextInsert(currPos, getSeparatorString(i - 1), editGroup); // insert
                                                                         // separator
            separatorState = NEW;
          }
          if (separatorState == NEW || insertAfterSeparator(node)) {
            if (separatorState == EXISTING) {
              updateIndent(prevMark, currPos, i, editGroup);
            }
            // insert node
            doTextInsert(currPos, node, getNodeIndent(i), true, editGroup);

            separatorState = NEW;
            if (i != lastNonDelete) {
              if (list[nextIndex].getChangeKind() != RewriteEvent.INSERTED) {
                // insert separator
                doTextInsert(currPos, getSeparatorString(i), editGroup);
              } else {
                separatorState = NONE;
              }
            }
          } else {
            // EXISTING && insert before separator
            doTextInsert(prevEnd, getSeparatorString(i - 1), editGroup);
            doTextInsert(prevEnd, node, getNodeIndent(i), true, editGroup);
          }
        } else if (currMark == RewriteEvent.REMOVED) {
          DartNode node = (DartNode) currEvent.getOriginalValue();
          TextEditGroup editGroup = getEditGroup(currEvent);
          int currEnd = getEndOfNode(node);
          // https://bugs.eclipse.org/bugs/show_bug.cgi?id=306524
          // Check for leading comments that are not part of extended range, and
          // prevent them
          // from getting removed.
          try {
            TokenScanner scanner = getScanner();
            int newOffset = prevEnd;
            int extendedOffset = getExtendedOffset(node);
            // Try to find the end of the last comment which is not part of
            // extended source
            // range of the node.
            while (TokenScanner.isComment(scanner.readNext(newOffset, false))) {
              int tempOffset = scanner.getNextEndOffset(newOffset, false);
              // check whether the comment is part of extended source range of
              // the node.
              // If it is then we need to stop.
              if (tempOffset < extendedOffset) {
                newOffset = tempOffset;
              } else {
                break;
              }
            }
            if (currPos < newOffset) {
              currPos = extendedOffset;
            }
            prevEnd = newOffset;
          } catch (CoreException e) {
            // ignore
          }
          if (i > lastNonDelete && separatorState == EXISTING) {
            // is last, remove previous separator: split delete to allow range
            // copies
            // remove separator
            doTextRemove(prevEnd, currPos - prevEnd, editGroup);
            // remove node
            doTextRemoveAndVisit(currPos, currEnd - currPos, node, editGroup);
            currPos = currEnd;
            prevEnd = currEnd;
          } else {
            if (i < lastNonDelete) {
              updateIndent(prevMark, currPos, i, editGroup);
            }

            // remove element and next separator
            // start of next
            int end = getStartOfNextNode(nextIndex, currEnd);
            // https://bugs.eclipse.org/bugs/show_bug.cgi?id=306524
            // Check for trailing comments that are not part of extended range,
            // and prevent them
            // from getting removed.
            try {
              TokenScanner scanner = getScanner();
              Token nextToken = scanner.readNext(currEnd, false);
              if (TokenScanner.isComment(nextToken)) {
                // the separator also has comments that are not part of extended
                // source range of this node or the next node. So dont remove
                // the separator
                if (end != scanner.getNextStartOffset(currEnd, false)) {
                  // If this condition were true, comments just found as part of
                  // the separator would've basically been
                  // part of the extended source range of the next node. So
                  // 'end' wud've safely been set to the correct position
                  // and no change is needed.
                  end = currEnd;
                }
              }
            } catch (CoreException e) {
              // ignore
            }
            doTextRemoveAndVisit(currPos, currEnd - currPos, node, getEditGroup(currEvent)); // remove node
            if (mustRemoveSeparator(currPos, i)) {
              doTextRemove(currEnd, end - currEnd, editGroup); // remove
                                                               // separator
            }
            currPos = end;
            prevEnd = currEnd;
            separatorState = NEW;
          }
        } else { // replaced or unchanged
          if (currMark == RewriteEvent.REPLACED) {
            DartNode node = (DartNode) currEvent.getOriginalValue();
            int currEnd = getEndOfNode(node);

            TextEditGroup editGroup = getEditGroup(currEvent);
            DartNode changed = (DartNode) currEvent.getNewValue();

            updateIndent(prevMark, currPos, i, editGroup);

            doTextRemoveAndVisit(currPos, currEnd - currPos, node, editGroup);
            doTextInsert(currPos, changed, getNodeIndent(i), true, editGroup);

            prevEnd = currEnd;
          } else { // is unchanged
            DartNode node = (DartNode) currEvent.getOriginalValue();
            voidVisit(node);
          }
          if (i == lastNonInsert) { // last node or next nodes are all inserts
            separatorState = NONE;
            if (currMark == RewriteEvent.UNCHANGED) {
              DartNode node = (DartNode) currEvent.getOriginalValue();
              prevEnd = getEndOfNode(node);
            }
            currPos = prevEnd;
          } else if (list[nextIndex].getChangeKind() != RewriteEvent.UNCHANGED) {
            // no updates needed while nodes are unchanged
            if (currMark == RewriteEvent.UNCHANGED) {
              DartNode node = (DartNode) currEvent.getOriginalValue();
              prevEnd = getEndOfNode(node);
            }
            currPos = getStartOfNextNode(nextIndex, prevEnd); // start of next
            separatorState = EXISTING;
          }
        }

        prevMark = currMark;
      }
      return currPos;
    }

    public final int rewriteList(DartNode parent, StructuralPropertyDescriptor property, int offset,
        String keyword, String separator) {
      contantSeparator = separator;
      return rewriteList(parent, property, offset, keyword);
    }

    protected int getEndOfNode(DartNode node) {
      return getExtendedEnd(node);
    }

    protected int getInitialIndent() {
      return getIndent(startPos);
    }

    protected final DartNode getNewNode(int index) {
      return (DartNode) list[index].getNewValue();
    }

    protected int getNodeIndent(int nodeIndex) {
      DartNode node = getOriginalNode(nodeIndex);
      if (node == null) {
        for (int i = nodeIndex - 1; i >= 0; i--) {
          DartNode curr = getOriginalNode(i);
          if (curr != null) {
            return getIndent(curr.getSourceInfo().getOffset());
          }
        }
        return getInitialIndent();
      }
      return getIndent(node.getSourceInfo().getOffset());
    }

    protected final DartNode getOriginalNode(int index) {
      return (DartNode) list[index].getOriginalValue();
    }

    protected String getSeparatorString(int nodeIndex) {
      return contantSeparator;
    }

    protected int getStartOfNextNode(int nextIndex, int defaultPos) {
      for (int i = nextIndex; i < list.length; i++) {
        RewriteEvent elem = list[i];
        if (elem.getChangeKind() != RewriteEvent.INSERTED) {
          DartNode node = (DartNode) elem.getOriginalValue();
          return getExtendedOffset(node);
        }
      }
      return defaultPos;
    }

    protected boolean mustRemoveSeparator(int originalOffset, int nodeIndex) {
      return true;
    }

    protected void updateIndent(
        int prevMark, int originalOffset, int nodeIndex, TextEditGroup editGroup) {
      // Do nothing.
    }

    private boolean insertAfterSeparator(DartNode node) {
      return !isInsertBoundToPrevious(node);
    }
  }
  class ModifierRewriter extends ListRewriter {

    private final Prefix annotationSeparation;

    public ModifierRewriter(Prefix annotationSeparation) {
      this.annotationSeparation = annotationSeparation;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jdt.internal.core.dom.rewrite.ASTRewriteAnalyzer.ListRewriter
     * #getSeparatorString(int)
     */
    @Override
    protected String getSeparatorString(int nodeIndex) {
      // DartNode curr = getNewNode(nodeIndex);
      // if (curr instanceof Annotation) {
      // return annotationSeparation.getPrefix(getNodeIndent(nodeIndex + 1));
      // }
      return super.getSeparatorString(nodeIndex);
    }
  }

  class ParagraphListRewriter extends ListRewriter {

    public final static int DEFAULT_SPACING = 1;

    private int initialIndent;
    private int separatorLines;

    public ParagraphListRewriter(int initialIndent, int separator) {
      this.initialIndent = initialIndent;
      this.separatorLines = separator;
    }

    @Override
    protected int getInitialIndent() {
      return initialIndent;
    }

    @Override
    protected String getSeparatorString(int nodeIndex) {
      return getSeparatorString(nodeIndex, nodeIndex + 1);
    }

    protected String getSeparatorString(int nodeIndex, int nextNodeIndex) {
      int newLines = separatorLines == -1 ? getNewLines(nodeIndex) : separatorLines;

      String lineDelim = getLineDelimiter();
      StringBuffer buf = new StringBuffer(lineDelim);
      for (int i = 0; i < newLines; i++) {
        buf.append(lineDelim);
      }
      buf.append(createIndentString(getNodeIndent(nextNodeIndex)));
      return buf.toString();
    }

    @Override
    protected boolean mustRemoveSeparator(int originalOffset, int nodeIndex) {
      // Do not remove separator if the previous non removed node is on the same
      // line and the next node is on another line
      int previousNonRemovedNodeIndex = nodeIndex - 1;
      while (previousNonRemovedNodeIndex >= 0
          && list[previousNonRemovedNodeIndex].getChangeKind() == RewriteEvent.REMOVED) {
        previousNonRemovedNodeIndex--;
      }

      if (previousNonRemovedNodeIndex > -1) {
        LineInformation lineInformation = getLineInformation();

        RewriteEvent prevEvent = list[previousNonRemovedNodeIndex];
        int prevKind = prevEvent.getChangeKind();
        if (prevKind == RewriteEvent.UNCHANGED || prevKind == RewriteEvent.REPLACED) {
          DartNode prevNode = (DartNode) list[previousNonRemovedNodeIndex].getOriginalValue();
          int prevEndPosition = prevNode.getSourceInfo().getOffset()
              + prevNode.getSourceInfo().getLength();
          int prevLine = lineInformation.getLineOfOffset(prevEndPosition);
          int line = lineInformation.getLineOfOffset(originalOffset);

          if (prevLine == line && nodeIndex + 1 < list.length) {
            RewriteEvent nextEvent = list[nodeIndex + 1];
            int nextKind = nextEvent.getChangeKind();

            if (nextKind == RewriteEvent.UNCHANGED || prevKind == RewriteEvent.REPLACED) {
              DartNode nextNode = (DartNode) nextEvent.getOriginalValue();
              int nextStartPosition = nextNode.getSourceInfo().getOffset();
              int nextLine = lineInformation.getLineOfOffset(nextStartPosition);

              return nextLine == line;
            }
            return false;
          }
        }
      }

      return true;
    }

    private int countEmptyLines(DartNode last) {
      LineInformation lineInformation = getLineInformation();
      int lastLine = lineInformation.getLineOfOffset(getExtendedEnd(last));
      if (lastLine >= 0) {
        int startLine = lastLine + 1;
        int start = lineInformation.getLineOffset(startLine);
        if (start < 0) {
          return 0;
        }
        char[] cont = getContent();
        int i = start;
        while (i < cont.length && ScannerHelper.isWhitespace(cont[i])) {
          i++;
        }
        if (i > start) {
          lastLine = lineInformation.getLineOfOffset(i);
          if (lastLine > startLine) {
            return lastLine - startLine;
          }
        }
      }
      return 0;
    }

    private int getNewLines(int nodeIndex) {
      DartCore.notYetImplemented();
      return 0;
      // DartNode curr = getNode(nodeIndex);
      // DartNode next = getNode(nodeIndex + 1);
      //
      // int currKind = curr.getNodeType();
      // int nextKind = next.getNodeType();
      //
      // DartNode last = null;
      // DartNode secondLast = null;
      // for (int i = 0; i < list.length; i++) {
      // DartNode elem = (DartNode) list[i].getOriginalValue();
      // if (elem != null) {
      // if (last != null) {
      // if (elem.getNodeType() == nextKind
      // && last.getNodeType() == currKind) {
      // return countEmptyLines(last);
      // }
      // secondLast = last;
      // }
      // last = elem;
      // }
      // }
      // if (curr instanceof DartFieldDefinition
      // && next instanceof DartFieldDefinition) {
      // return 0;
      // }
      // if (secondLast != null) {
      // return countEmptyLines(secondLast);
      // }
      // return DEFAULT_SPACING;
    }

    private DartNode getNode(int nodeIndex) {
      DartNode elem = (DartNode) list[nodeIndex].getOriginalValue();
      if (elem == null) {
        elem = (DartNode) list[nodeIndex].getNewValue();
      }
      return elem;
    }
  }

  class SwitchListRewriter extends ParagraphListRewriter {

    private boolean indentSwitchStatementsCompareToCases;

    public SwitchListRewriter(int initialIndent) {
      super(initialIndent, 0);
      indentSwitchStatementsCompareToCases = DefaultCodeFormatterConstants.TRUE.equals(
          ASTRewriteAnalyzer.this.options.get(
              DefaultCodeFormatterConstants.FORMATTER_INDENT_SWITCHSTATEMENTS_COMPARE_TO_CASES));
    }

    @Override
    protected int getNodeIndent(int nodeIndex) {
      int indent = getInitialIndent();

      if (indentSwitchStatementsCompareToCases) {
        RewriteEvent event = list[nodeIndex];
        int changeKind = event.getChangeKind();

        DartNode node;
        if (changeKind == RewriteEvent.INSERTED || changeKind == RewriteEvent.REPLACED) {
          node = (DartNode) event.getNewValue();
        } else {
          node = (DartNode) event.getOriginalValue();
        }

        if (!(node instanceof DartSwitchMember)) {
          indent++;
        }
      }
      return indent;
    }

    @Override
    protected String getSeparatorString(int nodeIndex) {
      int total = list.length;

      int nextNodeIndex = nodeIndex + 1;
      while (nextNodeIndex < total && list[nextNodeIndex].getChangeKind() == RewriteEvent.REMOVED) {
        nextNodeIndex++;
      }
      if (nextNodeIndex == total) {
        return super.getSeparatorString(nodeIndex);
      }
      return getSeparatorString(nodeIndex, nextNodeIndex);
    }

    @Override
    protected void updateIndent(
        int prevMark, int originalOffset, int nodeIndex, TextEditGroup editGroup) {
      if (prevMark != RewriteEvent.UNCHANGED && prevMark != RewriteEvent.REPLACED) {
        return;
      }

      // Do not change indent if the previous non removed node is on the same
      // line
      int previousNonRemovedNodeIndex = nodeIndex - 1;
      while (previousNonRemovedNodeIndex >= 0
          && list[previousNonRemovedNodeIndex].getChangeKind() == RewriteEvent.REMOVED) {
        previousNonRemovedNodeIndex--;
      }

      if (previousNonRemovedNodeIndex > -1) {
        LineInformation lineInformation = getLineInformation();

        RewriteEvent prevEvent = list[previousNonRemovedNodeIndex];
        int prevKind = prevEvent.getChangeKind();
        if (prevKind == RewriteEvent.UNCHANGED || prevKind == RewriteEvent.REPLACED) {
          DartNode prevNode = (DartNode) list[previousNonRemovedNodeIndex].getOriginalValue();
          int prevEndPosition = prevNode.getSourceInfo().getOffset()
              + prevNode.getSourceInfo().getLength();
          int prevLine = lineInformation.getLineOfOffset(prevEndPosition);
          int line = lineInformation.getLineOfOffset(originalOffset);

          if (prevLine == line) {
            return;
          }
        }
      }

      int total = list.length;
      while (nodeIndex < total && list[nodeIndex].getChangeKind() == RewriteEvent.REMOVED) {
        nodeIndex++;
      }

      int originalIndent = getIndent(originalOffset);
      int newIndent = getNodeIndent(nodeIndex);

      if (originalIndent != newIndent) {
        int line = getLineInformation().getLineOfOffset(originalOffset);
        if (line >= 0) {
          int lineStart = getLineInformation().getLineOffset(line);
          // remove previous indentation
          doTextRemove(lineStart, originalOffset - lineStart, editGroup);
          // add new indentation
          doTextInsert(lineStart, createIndentString(newIndent), editGroup);
        }
      }
    }
  }
  private class RewriteVisitor extends WrappedDartVisitor<Object> {
    @Override
    public void postVisit(DartNode node) {
      TextEditGroup editGroup = eventStore.getTrackedNodeData(node);
      if (editGroup != null) {
        currentEdit = currentEdit.getParent();
      }
      // remove copy source edits
      doCopySourcePostVisit(node, sourceCopyEndNodes);
    }

    @Override
    public void preVisit(DartNode node) {
      // copies, then range marker

      CopySourceInfo[] infos = eventStore.getNodeCopySources(node);
      doCopySourcePreVisit(infos, sourceCopyEndNodes);

      TextEditGroup editGroup = eventStore.getTrackedNodeData(node);
      if (editGroup != null) {
        SourceRange range = getExtendedRange(node);
        int offset = range.getStartPosition();
        int length = range.getLength();
        TextEdit edit = new RangeMarker(offset, length);
        addEditGroup(editGroup, edit);
        addEdit(edit);
        currentEdit = edit;
      }

      ensureSpaceBeforeReplace(node);
    }

    @Override
    public void visit(List<? extends DartNode> nodes) {
      if (nodes != null) {
        for (DartNode node : nodes) {
          node.accept(rewriteVisitor);
        }
      }
    }

    @Override
    public Object visitArrayAccess(DartArrayAccess node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      rewriteRequiredNode(node, PropertyDescriptorHelper.DART_ARRAY_ACCESS_TARGET);
      rewriteRequiredNode(node, PropertyDescriptorHelper.DART_ARRAY_ACCESS_KEY);
      return null;
    }

    @Override
    public Object visitArrayLiteral(DartArrayLiteral node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      int startPos = getPosAfterLeftBrace(node.getSourceInfo().getOffset());
      rewriteNodeList(
          node,
          PropertyDescriptorHelper.DART_ARRAY_LITERAL_EXPRESSIONS,
          startPos,
          "", ", "); //$NON-NLS-1$ //$NON-NLS-2$
      return null;
    }

    @Override
    public Object visitBinaryExpression(DartBinaryExpression node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      Token operator = node.getOperator();
      if (operator.isAssignmentOperator()) {
        int pos = rewriteRequiredNode(
            node,
            PropertyDescriptorHelper.DART_BINARY_EXPRESSION_LEFT_OPERAND);
        rewriteOperation(node, PropertyDescriptorHelper.DART_BINARY_EXPRESSION_OPERATOR, pos);
        rewriteRequiredNode(node, PropertyDescriptorHelper.DART_BINARY_EXPRESSION_RIGHT_OPERAND);
      } else {
        int pos = rewriteRequiredNode(
            node,
            PropertyDescriptorHelper.DART_BINARY_EXPRESSION_LEFT_OPERAND);
        boolean needsNewOperation = isChanged(
            node,
            PropertyDescriptorHelper.DART_BINARY_EXPRESSION_OPERATOR);
        String operation = getNewValue(
            node,
            PropertyDescriptorHelper.DART_BINARY_EXPRESSION_OPERATOR).toString();
        if (needsNewOperation) {
          replaceOperation(
              pos,
              operation,
              getEditGroup(node, PropertyDescriptorHelper.DART_BINARY_EXPRESSION_OPERATOR));
        }
        rewriteRequiredNode(node, PropertyDescriptorHelper.DART_BINARY_EXPRESSION_RIGHT_OPERAND);
      }
      return null;
    }

    @Override
    public Object visitBlock(DartBlock node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      int startPos;
      if (isCollapsed(node)) {
        startPos = node.getSourceInfo().getOffset();
      } else {
        startPos = getPosAfterLeftBrace(node.getSourceInfo().getOffset());
      }
      int startIndent = getIndent(node.getSourceInfo().getOffset()) + 1;
      rewriteParagraphList(
          node,
          PropertyDescriptorHelper.DART_BLOCK_STATEMENTS,
          startPos,
          startIndent,
          0,
          1);
      return null;
    }

    @Override
    public Object visitBooleanLiteral(DartBooleanLiteral node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      Boolean newLiteral = (Boolean) getNewValue(
          node,
          PropertyDescriptorHelper.DART_BOOLEAN_LITERAL_VALUE);
      TextEditGroup group = getEditGroup(node, PropertyDescriptorHelper.DART_BOOLEAN_LITERAL_VALUE);
      doTextReplace(
          node.getSourceInfo().getOffset(),
          node.getSourceInfo().getLength(),
          newLiteral.toString(),
          group);
      return null;
    }

    @Override
    public Object visitBreakStatement(DartBreakStatement node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      // try {
      DartCore.notYetImplemented();
      int offset = 0; // getScanner().getTokenEndOffset(TerminalTokens.TokenNamebreak,
                      // node.getSourceInfo().getSourceStart());
      rewriteNode(
          node,
          PropertyDescriptorHelper.DART_GOTO_STATEMENT_LABEL,
          offset,
          ASTRewriteFormatter.SPACE); // space between break and label
      // } catch (CoreException e) {
      // handleException(e);
      // }
      return null;
    }

    @Override
    public Object visitCase(DartCase node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Object visitCatchBlock(DartCatchBlock node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      rewriteRequiredNode(node, PropertyDescriptorHelper.DART_CATCH_BLOCK_EXCEPTION);
      DartCore.notYetImplemented();
      // rewriteNode(node,
      // PropertyDescriptorHelper.DART_CATCH_BLOCK_STACK_TRACE);
      rewriteRequiredNode(node, PropertyDescriptorHelper.DART_CATCH_BLOCK_BODY);
      return null;
    }

    @Override
    public Object visitClass(DartClass node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Object visitConditional(DartConditional node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      rewriteRequiredNode(node, PropertyDescriptorHelper.DART_CONDITIONAL_CONDITION);
      rewriteRequiredNode(node, PropertyDescriptorHelper.DART_CONDITIONAL_THEN);
      rewriteRequiredNode(node, PropertyDescriptorHelper.DART_CONDITIONAL_ELSE);
      return null;
    }

    @Override
    public Object visitContinueStatement(DartContinueStatement node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      // try {
      DartCore.notYetImplemented();
      int offset = 0; // getScanner().getTokenEndOffset(TerminalTokens.TokenNamecontinue,
                      // node.getSourceInfo().getSourceStart());
      rewriteNode(
          node,
          PropertyDescriptorHelper.DART_GOTO_STATEMENT_LABEL,
          offset,
          ASTRewriteFormatter.SPACE); // space between continue and
                                      // label
                                      // } catch (CoreException e) {
      // handleException(e);
      // }
      return null;
    }

    @Override
    public Object visitDefault(DartDefault node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Object visitDoubleLiteral(DartDoubleLiteral node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      String newLiteral = (String) getNewValue(
          node,
          PropertyDescriptorHelper.DART_DOUBLE_LITERAL_VALUE);
      TextEditGroup group = getEditGroup(node, PropertyDescriptorHelper.DART_DOUBLE_LITERAL_VALUE);
      doTextReplace(
          node.getSourceInfo().getOffset(),
          node.getSourceInfo().getLength(),
          newLiteral,
          group);
      return null;
    }

    @Override
    public Object visitDoWhileStatement(DartDoWhileStatement node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      int pos = node.getSourceInfo().getOffset();
      // try {
      RewriteEvent event = getEvent(node, PropertyDescriptorHelper.DART_DO_WHILE_STATEMENT_BODY);
      if (event != null && event.getChangeKind() == RewriteEvent.REPLACED) {
        DartCore.notYetImplemented();
        int startOffset = 0; // getScanner().getTokenEndOffset(TerminalTokens.TokenNamedo,
                             // pos);
        DartNode body = (DartNode) event.getOriginalValue();
        int bodyEnd = body.getSourceInfo().getOffset() + body.getSourceInfo().getLength();
        DartCore.notYetImplemented();
        int endPos = 0; // getScanner().getTokenStartOffset(TerminalTokens.TokenNamewhile,
                        // bodyEnd);
        rewriteBodyNode(
            node,
            PropertyDescriptorHelper.DART_DO_WHILE_STATEMENT_BODY,
            startOffset,
            endPos,
            getIndent(node.getSourceInfo().getOffset()),
            formatter.DO_BLOCK); // body
      } else {
        voidVisit(node, PropertyDescriptorHelper.DART_DO_WHILE_STATEMENT_BODY);
      }
      // } catch (CoreException e) {
      // handleException(e);
      // }

      rewriteRequiredNode(node, PropertyDescriptorHelper.DART_DO_WHILE_STATEMENT_CONDITION);
      return null;
    }

    @Override
    public Object visitEmptyStatement(DartEmptyStatement node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      changeNotSupported(node); // no modification possible
      return null;
    }

    @Override
    public Object visitExprStmt(DartExprStmt node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      rewriteRequiredNode(node, PropertyDescriptorHelper.DART_EXPRESSION_STATEMENT_EXPRESSION);
      return null;
    }

    @Override
    public Object visitField(DartField node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      // TODO revisit this when implementing refactoring
      DartCore.notYetImplemented();
      return null;
    }

    @Override
    public Object visitFieldDefinition(DartFieldDefinition node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      // TODO revisit this when implementing refactoring
      DartCore.notYetImplemented();
      return null;
    }

    @Override
    public Object visitForInStatement(DartForInStatement node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      // TODO Auto-generated method stub
      DartCore.notYetImplemented();
      return null;
    }

    @Override
    public Object visitForStatement(DartForStatement node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      // try {
      int pos = node.getSourceInfo().getOffset();
      if (isChanged(node, PropertyDescriptorHelper.DART_FOR_STATEMENT_INIT)) {
        // position after opening parent
        DartCore.notYetImplemented();
        int startOffset = 0; // getScanner().getTokenEndOffset(TerminalTokens.TokenNameLPAREN,
                             // pos);
        pos = rewriteNodeList(
            node,
            PropertyDescriptorHelper.DART_FOR_STATEMENT_INIT,
            startOffset,
            "", ", "); //$NON-NLS-1$ //$NON-NLS-2$
      } else {
        pos = doVisit(node, PropertyDescriptorHelper.DART_FOR_STATEMENT_INIT, pos);
      }
      // position after first semicolon
      DartCore.notYetImplemented();
      pos = 0; // getScanner().getTokenEndOffset(TerminalTokens.TokenNameSEMICOLON,
               // pos);
      pos = rewriteNode(
          node,
          PropertyDescriptorHelper.DART_FOR_STATEMENT_CONDITION,
          pos,
          ASTRewriteFormatter.NONE);
      if (isChanged(node, PropertyDescriptorHelper.DART_FOR_STATEMENT_INCREMENT)) {
        DartCore.notYetImplemented();
        int startOffset = 0; // getScanner().getTokenEndOffset(TerminalTokens.TokenNameSEMICOLON,
                             // pos);
        pos = rewriteNodeList(
            node,
            PropertyDescriptorHelper.DART_FOR_STATEMENT_INCREMENT,
            startOffset,
            "", ", "); //$NON-NLS-1$ //$NON-NLS-2$
      } else {
        pos = doVisit(node, PropertyDescriptorHelper.DART_FOR_STATEMENT_INCREMENT, pos);
      }
      RewriteEvent bodyEvent = getEvent(node, PropertyDescriptorHelper.DART_FOR_STATEMENT_BODY);
      if (bodyEvent != null && bodyEvent.getChangeKind() == RewriteEvent.REPLACED) {
        DartCore.notYetImplemented();
        int startOffset = 0; // getScanner().getTokenEndOffset(TerminalTokens.TokenNameRPAREN,
                             // pos);
        rewriteBodyNode(
            node,
            PropertyDescriptorHelper.DART_FOR_STATEMENT_BODY,
            startOffset,
            -1,
            getIndent(node.getSourceInfo().getOffset()),
            formatter.FOR_BLOCK); // body
      } else {
        voidVisit(node, PropertyDescriptorHelper.DART_FOR_STATEMENT_BODY);
      }
      // } catch (CoreException e) {
      // handleException(e);
      // }
      return null;
    }

    @Override
    public Object visitFunction(DartFunction node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Object visitFunctionExpression(DartFunctionExpression node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Object visitFunctionObjectInvocation(DartFunctionObjectInvocation node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Object visitFunctionTypeAlias(DartFunctionTypeAlias node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Object visitIdentifier(DartIdentifier node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      String newString = (String) getNewValue(
          node,
          PropertyDescriptorHelper.DART_IDENTIFIER_TARGET_NAME);
      TextEditGroup group = getEditGroup(
          node,
          PropertyDescriptorHelper.DART_IDENTIFIER_TARGET_NAME);
      doTextReplace(
          node.getSourceInfo().getOffset(),
          node.getSourceInfo().getLength(),
          newString,
          group);
      return null;
    }

    @Override
    public Object visitIfStatement(DartIfStatement node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      int pos = rewriteRequiredNode(node, PropertyDescriptorHelper.DART_IF_STATEMENT_CONDITION); // statement

      RewriteEvent thenEvent = getEvent(node, PropertyDescriptorHelper.DART_IF_STATEMENT_THEN);
      int elseChange = getChangeKind(node, PropertyDescriptorHelper.DART_IF_STATEMENT_ELSE);

      if (thenEvent != null && thenEvent.getChangeKind() != RewriteEvent.UNCHANGED) {
        // try {
        DartCore.notYetImplemented();
        // after the closing parent
        int tok = 0; // getScanner().readNext(pos, true);
        // pos = (tok == TerminalTokens.TokenNameRPAREN)
        // ? getScanner().getCurrentEndOffset()
        // : getScanner().getCurrentStartOffset();

        int indent = getIndent(node.getSourceInfo().getOffset());

        int endPos = -1;
        Object elseStatement = getOriginalValue(
            node,
            PropertyDescriptorHelper.DART_IF_STATEMENT_ELSE);
        if (elseStatement != null) {
          DartNode thenStatement = (DartNode) thenEvent.getOriginalValue();
          DartCore.notYetImplemented();
          // else keyword
          endPos = 0; // getScanner().getTokenStartOffset(TerminalTokens.TokenNameelse,
                      // thenStatement.getSourceInfo().getSourceStart() +
                      // thenStatement.getSourceInfo().getSourceLength());
        }
        if (elseStatement == null || elseChange != RewriteEvent.UNCHANGED) {
          pos = rewriteBodyNode(
              node,
              PropertyDescriptorHelper.DART_IF_STATEMENT_THEN,
              pos,
              endPos,
              indent,
              formatter.IF_BLOCK_NO_ELSE);
        } else {
          pos = rewriteBodyNode(
              node,
              PropertyDescriptorHelper.DART_IF_STATEMENT_THEN,
              pos,
              endPos,
              indent,
              formatter.IF_BLOCK_WITH_ELSE);
        }
        // } catch (CoreException e) {
        // handleException(e);
        // }
      } else {
        pos = doVisit(node, PropertyDescriptorHelper.DART_IF_STATEMENT_THEN, pos);
      }

      if (elseChange != RewriteEvent.UNCHANGED) {
        int indent = getIndent(node.getSourceInfo().getOffset());
        Object newThen = getNewValue(node, PropertyDescriptorHelper.DART_IF_STATEMENT_THEN);
        if (newThen instanceof DartBlock) {
          rewriteBodyNode(
              node,
              PropertyDescriptorHelper.DART_IF_STATEMENT_ELSE,
              pos,
              -1,
              indent,
              formatter.ELSE_AFTER_BLOCK);
        } else {
          rewriteBodyNode(
              node,
              PropertyDescriptorHelper.DART_IF_STATEMENT_ELSE,
              pos,
              -1,
              indent,
              formatter.ELSE_AFTER_STATEMENT);
        }
      } else {
        pos = doVisit(node, PropertyDescriptorHelper.DART_IF_STATEMENT_ELSE, pos);
      }
      return null;
    }

    @Override
    public Object visitImportDirective(DartImportDirective node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      rewriteRequiredNode(node, PropertyDescriptorHelper.DART_IMPORT_DIRECTIVE_URI);
      if (node.getPrefix() != null) {
        // TODO (brianwilkerson) This probably isn't correct because the prefix is not required.
        rewriteRequiredNode(node, PropertyDescriptorHelper.DART_IMPORT_DIRECTIVE_PREFIX);
      }
      return null;
    }

    @Override
    public Object visitInitializer(DartInitializer node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Object visitIntegerLiteral(DartIntegerLiteral node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      String newLiteral = (String) getNewValue(
          node,
          PropertyDescriptorHelper.DART_INTEGER_LITERAL_VALUE);
      TextEditGroup group = getEditGroup(node, PropertyDescriptorHelper.DART_INTEGER_LITERAL_VALUE);
      doTextReplace(
          node.getSourceInfo().getOffset(),
          node.getSourceInfo().getLength(),
          newLiteral,
          group);
      return null;
    }

    @Override
    public Object visitLabel(DartLabel node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      rewriteRequiredNode(node, PropertyDescriptorHelper.DART_LABELED_STATEMENT_LABEL);
      rewriteRequiredNode(node, PropertyDescriptorHelper.DART_LABELED_STATEMENT_STATEMENT);
      return null;
    }

    @Override
    public Object visitLibraryDirective(DartLibraryDirective node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      rewriteRequiredNode(node, PropertyDescriptorHelper.DART_LIBRARY_DIRECTIVE_NAME);
      return null;
    }

    @Override
    public Object visitMapLiteral(DartMapLiteral node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Object visitMapLiteralEntry(DartMapLiteralEntry node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Object visitMethodDefinition(DartMethodDefinition node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Object visitMethodInvocation(DartMethodInvocation node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      int pos = rewriteOptionalQualifier(
          node,
          PropertyDescriptorHelper.DART_METHOD_INVOCATION_TARGET,
          node.getSourceInfo().getOffset());
      pos = rewriteRequiredNode(
          node,
          PropertyDescriptorHelper.DART_METHOD_INVOCATION_FUNCTION_NAME);

      if (isChanged(node, PropertyDescriptorHelper.DART_INVOCATION_ARGS)) {
        // eval position after opening parent
        // try {
        DartCore.notYetImplemented();
        int startOffset = 0; // getScanner().getTokenEndOffset(TerminalTokens.TokenNameLPAREN,
                             // pos);
        rewriteNodeList(node, PropertyDescriptorHelper.DART_INVOCATION_ARGS, startOffset, "", ", "); //$NON-NLS-1$ //$NON-NLS-2$
        // } catch (CoreException e) {
        // handleException(e);
        // }
      } else {
        voidVisit(node, PropertyDescriptorHelper.DART_INVOCATION_ARGS);
      }
      return null;
    }

    @Override
    public Object visitNamedExpression(DartNamedExpression node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      rewriteRequiredNode(node, PropertyDescriptorHelper.DART_NAMED_EXPRESSION_NAME);
      rewriteRequiredNode(node, PropertyDescriptorHelper.DART_NAMED_EXPRESSION_EXPRESSION);
      return null;
    }

    @Override
    public Object visitNativeBlock(DartNativeBlock node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Object visitNativeDirective(DartNativeDirective node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      rewriteRequiredNode(node, PropertyDescriptorHelper.DART_NATIVE_DIRECTIVE_URI);
      return null;
    }

    @Override
    public Object visitNewExpression(DartNewExpression node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Object visitNullLiteral(DartNullLiteral node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      changeNotSupported(node); // no modification possible
      return null;
    }

    @Override
    public Object visitParameter(DartParameter node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Object visitParameterizedTypeNode(DartParameterizedTypeNode node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Object visitParenthesizedExpression(DartParenthesizedExpression node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      rewriteRequiredNode(node, PropertyDescriptorHelper.DART_PARENTHESIZED_EXPRESSION_EXPRESSION);
      return null;
    }

    @Override
    public Object visitPropertyAccess(DartPropertyAccess node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      rewriteRequiredNode(node, PropertyDescriptorHelper.DART_PROPERTY_ACCESS_QUALIFIER);
      rewriteRequiredNode(node, PropertyDescriptorHelper.DART_PROPERTY_ACCESS_NAME);
      return null;
    }

    @Override
    public Object visitRedirectConstructorInvocation(DartRedirectConstructorInvocation node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Object visitReturnStatement(DartReturnStatement node) {
      // try {
      DartCore.notYetImplemented();
      beforeRequiredSpaceIndex = 0; // getScanner().getTokenEndOffset(TerminalTokens.TokenNamereturn,
                                    // node.getSourceInfo().getSourceStart());
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      ensureSpaceBeforeReplace(node);
      rewriteNode(
          node,
          PropertyDescriptorHelper.DART_RETURN_STATEMENT_VALUE,
          beforeRequiredSpaceIndex,
          ASTRewriteFormatter.SPACE);
      // } catch (CoreException e) {
      // handleException(e);
      // }
      return null;
    }

    @Override
    public Object visitSourceDirective(DartSourceDirective node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      rewriteRequiredNode(node, PropertyDescriptorHelper.DART_SOURCE_DIRECTIVE_URI);
      return null;
    }

    @Override
    public Object visitStringInterpolation(DartStringInterpolation node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Object visitStringLiteral(DartStringLiteral node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      String escapedSeq = (String) getNewValue(
          node,
          PropertyDescriptorHelper.DART_STRING_LITERAL_VALUE);
      TextEditGroup group = getEditGroup(node, PropertyDescriptorHelper.DART_STRING_LITERAL_VALUE);
      doTextReplace(
          node.getSourceInfo().getOffset(),
          node.getSourceInfo().getLength(),
          escapedSeq,
          group);
      return null;
    }

    @Override
    public Object visitSuperConstructorInvocation(DartSuperConstructorInvocation node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      int pos = rewriteRequiredNode(
          node,
          PropertyDescriptorHelper.DART_SUPER_CONSTRUCTOR_INVOCATION_NAME);
      if (isChanged(node, PropertyDescriptorHelper.DART_INVOCATION_ARGS)) {
        // eval position after opening parent
        // try {
        DartCore.notYetImplemented();
        pos = 0; // getScanner().getTokenEndOffset(TerminalTokens.TokenNameLPAREN,
                 // pos);
        rewriteNodeList(node, PropertyDescriptorHelper.DART_INVOCATION_ARGS, pos, "", ", "); //$NON-NLS-1$ //$NON-NLS-2$
        // } catch (CoreException e) {
        // handleException(e);
        // }
      } else {
        voidVisit(node, PropertyDescriptorHelper.DART_INVOCATION_ARGS);
      }
      return null;
    }

    @Override
    public Object visitSuperExpression(DartSuperExpression node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      changeNotSupported(node); // no modification possible
      return null;
    }

    @Override
    public Object visitSwitchStatement(DartSwitchStatement node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      int pos = rewriteRequiredNode(
          node,
          PropertyDescriptorHelper.DART_SWITCH_STATEMENT_EXPRESSION);

      StructuralPropertyDescriptor property = PropertyDescriptorHelper.DART_SWITCH_STATEMENT_MEMBERS;
      if (getChangeKind(node, property) != RewriteEvent.UNCHANGED) {
        // try {
        DartCore.notYetImplemented();
        pos = 0; // getScanner().getTokenEndOffset(TerminalTokens.TokenNameLBRACE,
                 // pos);
        int insertIndent = getIndent(node.getSourceInfo().getOffset());
        if (DefaultCodeFormatterConstants.TRUE.equals(
            options.get(
                DefaultCodeFormatterConstants.FORMATTER_INDENT_SWITCHSTATEMENTS_COMPARE_TO_SWITCH))) {
          insertIndent++;
        }

        ParagraphListRewriter listRewriter = new SwitchListRewriter(insertIndent);
        StringBuffer leadString = new StringBuffer();
        leadString.append(getLineDelimiter());
        leadString.append(createIndentString(insertIndent));
        listRewriter.rewriteList(node, property, pos, leadString.toString());
        // } catch (CoreException e) {
        // handleException(e);
        // }
      } else {
        voidVisit(node, property);
      }
      return null;
    }

    @Override
    public Object visitSyntheticErrorExpression(DartSyntheticErrorExpression node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      String newString = (String) getNewValue(
          node,
          PropertyDescriptorHelper.DART_SYNTHETIC_ERROR_EXPRESSION_TOKEN_STRING);
      TextEditGroup group = getEditGroup(
          node,
          PropertyDescriptorHelper.DART_SYNTHETIC_ERROR_EXPRESSION_TOKEN_STRING);
      doTextReplace(
          node.getSourceInfo().getOffset(),
          node.getSourceInfo().getLength(),
          newString,
          group);
      return null;
    }

    @Override
    public Object visitSyntheticErrorStatement(DartSyntheticErrorStatement node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      String newString = (String) getNewValue(
          node,
          PropertyDescriptorHelper.DART_SYNTHETIC_ERROR_STATEMENT_TOKEN_STRING);
      TextEditGroup group = getEditGroup(
          node,
          PropertyDescriptorHelper.DART_SYNTHETIC_ERROR_STATEMENT_TOKEN_STRING);
      doTextReplace(
          node.getSourceInfo().getOffset(),
          node.getSourceInfo().getLength(),
          newString,
          group);
      return null;
    }

    @Override
    public Object visitThisExpression(DartThisExpression node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      changeNotSupported(node); // no modification possible
      return null;
    }

    @Override
    public Object visitThrowExpression(DartThrowExpression node) {
      // try {
      DartCore.notYetImplemented();
      beforeRequiredSpaceIndex = 0; // getScanner().getTokenEndOffset(TerminalTokens.TokenNamethrow,
                                    // node.getSourceInfo().getSourceStart());
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      ensureSpaceBeforeReplace(node);
      rewriteRequiredNode(node, PropertyDescriptorHelper.DART_THROW_STATEMENT_EXCEPTION);
      // } catch (CoreException e) {
      // handleException(e);
      // }
      return null;
    }

    @Override
    public Object visitTryStatement(DartTryStatement node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      int pos = rewriteRequiredNode(node, PropertyDescriptorHelper.DART_TRY_STATEMENT_TRY_BLOCK);
      if (isChanged(node, PropertyDescriptorHelper.DART_TRY_STATEMENT_CATCH_BLOCKS)) {
        int indent = getIndent(node.getSourceInfo().getOffset());
        String prefix = formatter.CATCH_BLOCK.getPrefix(indent);
        pos = rewriteNodeList(
            node,
            PropertyDescriptorHelper.DART_TRY_STATEMENT_CATCH_BLOCKS,
            pos,
            prefix,
            prefix);
      } else {
        pos = doVisit(node, PropertyDescriptorHelper.DART_TRY_STATEMENT_CATCH_BLOCKS, pos);
      }
      rewriteNode(
          node,
          PropertyDescriptorHelper.DART_TRY_STATEMENT_FINALY_BLOCK,
          pos,
          formatter.FINALLY_BLOCK);
      return null;
    }

    @Override
    public Object visitTypeExpression(DartTypeExpression node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Object visitTypeNode(DartTypeNode node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Object visitTypeParameter(DartTypeParameter node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Object visitUnaryExpression(DartUnaryExpression node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      if (node.isPrefix()) {
        rewriteOperation(
            node,
            PropertyDescriptorHelper.DART_UNARY_EXPRESSION_OPERATOR,
            node.getSourceInfo().getOffset());
        rewriteRequiredNode(node, PropertyDescriptorHelper.DART_UNARY_EXPRESSION_OPERAND);
      } else {
        int pos = rewriteRequiredNode(node, PropertyDescriptorHelper.DART_UNARY_EXPRESSION_OPERAND);
        rewriteOperation(node, PropertyDescriptorHelper.DART_UNARY_EXPRESSION_OPERATOR, pos);
      }
      return null;
    }

    @Override
    public Object visitUnit(DartUnit node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      rewriteParagraphList(node, PropertyDescriptorHelper.DART_UNIT_MEMBERS, 0, 0, -1, 2);
      return null;
    }

    @Override
    public Object visitUnqualifiedInvocation(DartUnqualifiedInvocation node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Object visitVariable(DartVariable node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Object visitVariableStatement(DartVariableStatement node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      // TODO This is a list, not a single node!
      rewriteRequiredNode(node, PropertyDescriptorHelper.DART_VARIABLE_STATEMENT_VARIABLES);
      return null;
    }

    @Override
    public Object visitWhileStatement(DartWhileStatement node) {
      if (!hasChildrenChanges(node)) {
        return doVisitUnchangedChildren(node);
      }
      int pos = rewriteRequiredNode(node, PropertyDescriptorHelper.DART_WHILE_STATEMENT_CONDITION);
      // try {
      if (isChanged(node, PropertyDescriptorHelper.DART_WHILE_STATEMENT_BODY)) {
        DartCore.notYetImplemented();
        int startOffset = 0; // getScanner().getTokenEndOffset(TerminalTokens.TokenNameRPAREN,
                             // pos);
        rewriteBodyNode(
            node,
            PropertyDescriptorHelper.DART_WHILE_STATEMENT_BODY,
            startOffset,
            -1,
            getIndent(node.getSourceInfo().getOffset()),
            formatter.WHILE_BLOCK);
      } else {
        voidVisit(node, PropertyDescriptorHelper.DART_WHILE_STATEMENT_BODY);
      }
      // } catch (CoreException e) {
      // handleException(e);
      // }
      return null;
    }
  }

  TextEdit currentEdit;
  final RewriteEventStore eventStore; // used from inner classes
  private TokenScanner tokenScanner; // shared scanner
  private final Map<CopySourceInfo, TextEdit> sourceCopyInfoToEdit;
  private final Stack<DartNode> sourceCopyEndNodes;
  private final char[] content;

  private final LineInformation lineInfo;

  private final ASTRewriteFormatter formatter;

  private final NodeInfoStore nodeInfos;

  private final TargetSourceRangeComputer extendedSourceRangeComputer;

  private final LineCommentEndOffsets lineCommentEndOffsets;

  private int beforeRequiredSpaceIndex = -1;

  Map<String, String> options;

  private RecoveryScannerData recoveryScannerData;

  private ASTVisitor<Object> rewriteVisitor;

  /**
   * Constructor for ASTRewriteAnalyzer.
   * <p>
   * The given options cannot be null.
   * </p>
   * 
   * @param content the content of the compilation unit to rewrite.
   * @param lineInfo line information for the content of the compilation unit to rewrite.
   * @param rootEdit the edit to add all generated edits to
   * @param eventStore the event store containing the description of changes
   * @param nodeInfos annotations to nodes, such as if a node is a string placeholder or a copy
   *          target
   * @param comments list of comments of the compilation unit to rewrite (elements of type <code>Comment
   *          </code>) or <code>null</code>.
   * @param options the current jdt.core options (formatting/compliance)
   * @param extendedSourceRangeComputer the source range computer to use
   * @param recoveryScannerData internal data used by {@link RecoveryScanner}
   */
  public ASTRewriteAnalyzer(char[] content, LineInformation lineInfo, String lineDelim,
      TextEdit rootEdit, RewriteEventStore eventStore, NodeInfoStore nodeInfos,
      List<DartComment> comments, Map<String, String> options,
      TargetSourceRangeComputer extendedSourceRangeComputer,
      RecoveryScannerData recoveryScannerData) {
    this.eventStore = eventStore;
    this.content = content;
    this.lineInfo = lineInfo;
    this.nodeInfos = nodeInfos;
    this.tokenScanner = null;
    this.currentEdit = rootEdit;
    this.sourceCopyInfoToEdit = new IdentityHashMap<CopySourceInfo, TextEdit>();
    this.sourceCopyEndNodes = new Stack<DartNode>();

    this.formatter = new ASTRewriteFormatter(nodeInfos, eventStore, options, lineDelim);

    this.extendedSourceRangeComputer = extendedSourceRangeComputer;
    this.lineCommentEndOffsets = new LineCommentEndOffsets(comments);

    this.options = options;

    this.recoveryScannerData = recoveryScannerData;

    this.rewriteVisitor = new WrappedDartVisitorAdaptor<Object>(new RewriteVisitor());
  }

  /**
   * Analyze the given node.
   * 
   * @param node the node to be analyzed
   */
  public void analyze(DartNode node) {
    node.accept(rewriteVisitor);
  }

  public void ensureSpaceAfterReplace(DartNode node, ChildPropertyDescriptor desc) {
    if (getChangeKind(node, desc) == RewriteEvent.REPLACED) {
      int leftOperandEnd = getExtendedEnd((DartNode) getOriginalValue(node, desc));
      try {
        // instanceof
        int offset = getScanner().getNextStartOffset(leftOperandEnd, true);
        if (offset == leftOperandEnd) {
          doTextInsert(offset, String.valueOf(' '), getEditGroup(node, desc));
        }
      } catch (CoreException e) {
        handleException(e);
      }
    }
  }

  public void ensureSpaceBeforeReplace(DartNode node) {
    if (beforeRequiredSpaceIndex == -1) {
      return;
    }

    List<RewriteEvent> events = eventStore.getChangedPropertieEvents(node);

    for (Iterator<RewriteEvent> iterator = events.iterator(); iterator.hasNext();) {
      RewriteEvent event = iterator.next();
      if (event.getChangeKind() == RewriteEvent.REPLACED
          && event.getOriginalValue() instanceof DartNode) {
        if (beforeRequiredSpaceIndex == getExtendedOffset((DartNode) event.getOriginalValue())) {
          doTextInsert(beforeRequiredSpaceIndex, String.valueOf(' '), getEditGroup(event));
          beforeRequiredSpaceIndex = -1;
          return;
        }
      }
    }

    if (beforeRequiredSpaceIndex < getExtendedOffset(node)) {
      beforeRequiredSpaceIndex = -1;
    }
  }

  final void addEdit(TextEdit edit) {
    currentEdit.addChild(edit);
  }

  final void addEditGroup(TextEditGroup editGroup, TextEdit edit) {
    editGroup.addTextEdit(edit);
  }

  final String createIndentString(int indent) {
    DartCore.notYetImplemented();
    return ""; // formatter.createIndentString(indent);
  }

  final void doCopySourcePostVisit(DartNode node, Stack<DartNode> nodeEndStack) {
    while (!nodeEndStack.isEmpty() && nodeEndStack.peek() == node) {
      nodeEndStack.pop();
      currentEdit = currentEdit.getParent();
    }
  }

  final void doCopySourcePreVisit(CopySourceInfo[] infos, Stack<DartNode> nodeEndStack) {
    if (infos != null) {
      for (int i = 0; i < infos.length; i++) {
        CopySourceInfo curr = infos[i];
        TextEdit edit = getCopySourceEdit(curr);
        addEdit(edit);
        currentEdit = edit;
        nodeEndStack.push(curr.getNode());
      }
    }
  }

  final void doTextInsert(int insertOffset, DartNode node, int initialIndentLevel,
      boolean removeLeadingIndent, TextEditGroup editGroup) {
//    ArrayList markers = new ArrayList();
    DartCore.notYetImplemented();
    String formatted = ""; // formatter.getFormattedResult(node,
                           // initialIndentLevel, markers);

    int currPos = 0;
    if (removeLeadingIndent) {
      while (currPos < formatted.length()
          && ScannerHelper.isWhitespace(formatted.charAt(currPos))) {
        currPos++;
      }
    }
    DartCore.notYetImplemented();
    // for (int i = 0; i < markers.size(); i++) { // markers.size can change!
    // NodeMarker curr = (NodeMarker) markers.get(i);
    //
    // int offset = curr.offset;
    // if (offset >= currPos) {
    // String insertStr = formatted.substring(currPos, offset);
    // doTextInsert(insertOffset, insertStr, editGroup); // insert until the
    // marker's begin
    // } else {
    // // already processed
    // continue;
    // }
    //
    // Object data = curr.data;
    // if (data instanceof TextEditGroup) { // tracking a node
    // // need to split and create 2 edits as tracking node can surround
    // replaced node.
    // TextEdit edit = new RangeMarker(insertOffset, 0);
    // addEditGroup((TextEditGroup) data, edit);
    // addEdit(edit);
    // if (curr.length != 0) {
    // int end = offset + curr.length;
    // int k = i + 1;
    // while (k < markers.size()
    // && ((NodeMarker) markers.get(k)).offset < end) {
    // k++;
    // }
    // curr.offset = end;
    // curr.length = 0;
    // markers.add(k, curr); // add again for end position
    // }
    // currPos = offset;
    // } else {
    // String destIndentString = formatter.getIndentString(getCurrentLine(
    // formatted, offset));
    // if (data instanceof CopyPlaceholderData) { // replace with a copy/move
    // target
    // CopySourceInfo copySource = ((CopyPlaceholderData) data).copySource;
    // int srcIndentLevel = getIndent(copySource.getNode().getSourceInfo().getSourceStart());
    // TextEdit sourceEdit = getCopySourceEdit(copySource);
    // doTextCopy(sourceEdit, insertOffset, srcIndentLevel,
    // destIndentString, editGroup);
    // currPos = offset + curr.length; // continue to insert after the replaced
    // string
    // if (needsNewLineForLineComment(copySource.getNode(), formatted,
    // currPos)) {
    // doTextInsert(insertOffset, getLineDelimiter(), editGroup);
    // }
    // } else if (data instanceof StringPlaceholderData) { // replace with a
    // placeholder
    // String code = ((StringPlaceholderData) data).code;
    // String str = formatter.changeIndent(code, 0, destIndentString);
    // doTextInsert(insertOffset, str, editGroup);
    // currPos = offset + curr.length; // continue to insert after the replaced
    // string
    // }
    // }
    //
    // }
    if (currPos < formatted.length()) {
      String insertStr = formatted.substring(currPos);
      doTextInsert(insertOffset, insertStr, editGroup);
    }
  }

  final void doTextInsert(int offset, String insertString, TextEditGroup editGroup) {
    if (insertString.length() > 0) {
      // bug fix for 95839: problem with inserting at the end of a line comment
      if (lineCommentEndOffsets.isEndOfLineComment(offset, content)) {
        if (!insertString.startsWith(getLineDelimiter())) {
          TextEdit edit = new InsertEdit(offset, getLineDelimiter()); // add a
                                                                      // line
                                                                      // delimiter
          addEdit(edit);
          if (editGroup != null) {
            addEditGroup(editGroup, edit);
          }
        }
        lineCommentEndOffsets.remove(offset); // only one line delimiter per
                                              // line comment required
      }
      TextEdit edit = new InsertEdit(offset, insertString);
      addEdit(edit);
      if (editGroup != null) {
        addEditGroup(editGroup, edit);
      }
    }
  }

  final TextEdit doTextRemove(int offset, int len, TextEditGroup editGroup) {
    if (len == 0) {
      return null;
    }
    TextEdit edit = new DeleteEdit(offset, len);
    addEdit(edit);
    if (editGroup != null) {
      addEditGroup(editGroup, edit);
    }
    return edit;
  }

  final void doTextRemoveAndVisit(int offset, int len, DartNode node, TextEditGroup editGroup) {
    TextEdit edit = doTextRemove(offset, len, editGroup);
    if (edit != null) {
      currentEdit = edit;
      voidVisit(node);
      currentEdit = edit.getParent();
    } else {
      voidVisit(node);
    }
  }

  final int doVisit(DartNode node) {
    node.accept(rewriteVisitor);
    return getExtendedEnd(node);
  }

  final char[] getContent() {
    return content;
  }

  final TextEdit getCopySourceEdit(CopySourceInfo info) {
    TextEdit edit = sourceCopyInfoToEdit.get(info);
    if (edit == null) {
      SourceRange range = getExtendedRange(info.getNode());
      int start = range.getStartPosition();
      int end = start + range.getLength();
      if (info.isMove) {
        MoveSourceEdit moveSourceEdit = new MoveSourceEdit(start, end - start);
        moveSourceEdit.setTargetEdit(new MoveTargetEdit(0));
        edit = moveSourceEdit;
      } else {
        CopySourceEdit copySourceEdit = new CopySourceEdit(start, end - start);
        copySourceEdit.setTargetEdit(new CopyTargetEdit(0));
        edit = copySourceEdit;
      }
      sourceCopyInfoToEdit.put(info, edit);
    }
    return edit;
  }

  final TextEditGroup getEditGroup(RewriteEvent change) {
    return eventStore.getEventEditGroup(change);
  }

  final RewriteEvent getEvent(DartNode parent, StructuralPropertyDescriptor property) {
    return eventStore.getEvent(parent, property);
  }

  final int getExtendedEnd(DartNode node) {
    TargetSourceRangeComputer.SourceRange range = getExtendedRange(node);
    return range.getStartPosition() + range.getLength();
  }

  final int getExtendedOffset(DartNode node) {
    return getExtendedRange(node).getStartPosition();
  }

  /**
   * Return the extended source range for a node.
   * 
   * @return an extended source range (never null)
   */
  final SourceRange getExtendedRange(DartNode node) {
    if (eventStore.isRangeCopyPlaceholder(node)) {
      return new SourceRange(node.getSourceInfo().getOffset(), node.getSourceInfo().getLength());
    }
    return extendedSourceRangeComputer.computeSourceRange(node);
  }

  final int getIndent(int offset) {
    DartCore.notYetImplemented();
    return 0; // formatter.computeIndentUnits(getIndentOfLine(offset));
  }

  final String getIndentAtOffset(int pos) {
    DartCore.notYetImplemented();
    return ""; // formatter.getIndentString(getIndentOfLine(pos));
  }

  final String getLineDelimiter() {
    DartCore.notYetImplemented();
    return ""; // formatter.getLineDelimiter();
  }

  final LineInformation getLineInformation() {
    return lineInfo;
  }

  final TokenScanner getScanner() {
    if (tokenScanner == null) {
      DartCore.notYetImplemented();
      // CompilerOptions compilerOptions = new CompilerOptions(options);
      DartScanner scanner = new DartScanner(new String(content));
      // if (recoveryScannerData == null) {
      // scanner = new Scanner(true, false, false, compilerOptions.sourceLevel,
      // compilerOptions.complianceLevel, null, null, true);
      // } else {
      // scanner = new RecoveryScanner(false, false,
      // compilerOptions.sourceLevel, compilerOptions.complianceLevel, null,
      // null, true, recoveryScannerData);
      // }
      // scanner.setSource(content);
      tokenScanner = new TokenScanner(scanner);
    }
    return tokenScanner;
  }

  final void handleException(Throwable e) {
    IllegalArgumentException runtimeException = new IllegalArgumentException(
        "Document does not match the AST"); //$NON-NLS-1$
    runtimeException.initCause(e);
    throw runtimeException;
  }

  final boolean isInsertBoundToPrevious(DartNode node) {
    return eventStore.isInsertBoundToPrevious(node);
  }

  final void voidVisit(DartNode node) {
    node.accept(rewriteVisitor);
  }

  private void changeNotSupported(DartNode node) {
    Assert.isTrue(false, "Change not supported in " + node.getClass().getName()); //$NON-NLS-1$
  }

  private final TextEdit doTextCopy(TextEdit sourceEdit, int destOffset, int sourceIndentLevel,
      String destIndentString, TextEditGroup editGroup) {
    TextEdit targetEdit;
    DartCore.notYetImplemented();
    SourceModifier modifier = new SourceModifier(sourceIndentLevel, destIndentString, 2 /*
                                                                                         * formatter.
                                                                                         * getTabWidth
                                                                                         * ()
                                                                                         */, 2 /*
                                                                                                * formatter
                                                                                                * .
                                                                                                * getIndentWidth
                                                                                                * ()
                                                                                                */);

    if (sourceEdit instanceof MoveSourceEdit) {
      MoveSourceEdit moveEdit = (MoveSourceEdit) sourceEdit;
      moveEdit.setSourceModifier(modifier);

      targetEdit = new MoveTargetEdit(destOffset, moveEdit);
      addEdit(targetEdit);
    } else {
      CopySourceEdit copyEdit = (CopySourceEdit) sourceEdit;
      copyEdit.setSourceModifier(modifier);

      targetEdit = new CopyTargetEdit(destOffset, copyEdit);
      addEdit(targetEdit);
    }

    if (editGroup != null) {
      addEditGroup(editGroup, sourceEdit);
      addEditGroup(editGroup, targetEdit);
    }
    return targetEdit;

  }

  private final void doTextReplace(
      int offset, int len, String insertString, TextEditGroup editGroup) {
    if (len > 0 || insertString.length() > 0) {
      TextEdit edit = new ReplaceEdit(offset, len, insertString);
      addEdit(edit);
      if (editGroup != null) {
        addEditGroup(editGroup, edit);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private final int doVisit(DartNode parent, StructuralPropertyDescriptor property, int offset) {
    Object node = getOriginalValue(parent, property);
    if (property.isChildProperty() && node != null) {
      return doVisit((DartNode) node);
    } else if (property.isChildListProperty()) {
      return doVisitList((List<DartNode>) node, offset);
    }
    return offset;
  }

  private int doVisitList(List<DartNode> list, int offset) {
    int endPos = offset;
    for (Iterator<DartNode> iter = list.iterator(); iter.hasNext();) {
      DartNode curr = iter.next();
      endPos = doVisit(curr);
    }
    return endPos;
  }

  private final boolean doVisitUnchangedChildren(DartNode parent) {
    List<StructuralPropertyDescriptor> properties = PropertyDescriptorHelper
      .getStructuralPropertiesForType(parent);
    for (int i = 0; i < properties.size(); i++) {
      voidVisit(parent, properties.get(i));
    }
    return false;
  }

  private int findTagNameEnd(TagElement tagNode) {
    DartCore.notYetImplemented();
    return 0;
    // if (tagNode.getTagName() != null) {
    // char[] cont = getContent();
    // int len = cont.length;
    // int i = tagNode.getStartPosition();
    // while (i < len && !IndentManipulation.isIndentChar(cont[i])) {
    // i++;
    // }
    // return i;
    // }
    // return tagNode.getStartPosition();
  }

  private final int getChangeKind(DartNode node, StructuralPropertyDescriptor property) {
    RewriteEvent event = getEvent(node, property);
    if (event != null) {
      return event.getChangeKind();
    }
    return RewriteEvent.UNCHANGED;
  }

  private String getCurrentLine(String str, int pos) {
    for (int i = pos - 1; i >= 0; i--) {
      char ch = str.charAt(i);
      if (IndentManipulation.isLineDelimiterChar(ch)) {
        return str.substring(i + 1, pos);
      }
    }
    return str.substring(0, pos);
  }

  // private int getDimensions(ArrayType parent) {
  // Type t = (Type) getOriginalValue(parent,
  // ArrayType.COMPONENT_TYPE_PROPERTY);
  // int dimensions = 1; // always include this array type
  // while (t.isArrayType()) {
  // dimensions++;
  // t = (Type) getOriginalValue(t, ArrayType.COMPONENT_TYPE_PROPERTY);
  // }
  // return dimensions;
  // }

  private final TextEditGroup getEditGroup(DartNode parent, StructuralPropertyDescriptor property) {
    RewriteEvent event = getEvent(parent, property);
    if (event != null) {
      return getEditGroup(event);
    }
    return null;
  }

  // private Type getElementType(ArrayType parent) {
  // Type t = (Type) getOriginalValue(parent,
  // ArrayType.COMPONENT_TYPE_PROPERTY);
  // while (t.isArrayType()) {
  // t = (Type) getOriginalValue(t, ArrayType.COMPONENT_TYPE_PROPERTY);
  // }
  // return t;
  // }

  final private String getIndentOfLine(int pos) {
    int line = getLineInformation().getLineOfOffset(pos);
    if (line >= 0) {
      char[] cont = getContent();
      int lineStart = getLineInformation().getLineOffset(line);
      int i = lineStart;
      while (i < cont.length && IndentManipulation.isIndentChar(content[i])) {
        i++;
      }
      return new String(cont, lineStart, i - lineStart);
    }
    return Util.EMPTY_STRING;
  }

  private final Object getNewValue(DartNode parent, StructuralPropertyDescriptor property) {
    return eventStore.getNewValue(parent, property);
  }

  private final Object getOriginalValue(DartNode parent, StructuralPropertyDescriptor property) {
    return eventStore.getOriginalValue(parent, property);
  }

  /*
   * Next token is a left brace. Returns the offset after the brace. For incomplete code, return the
   * start offset.
   */
  private int getPosAfterLeftBrace(int pos) {
    DartCore.notYetImplemented();
    // try {
    // int nextToken = getScanner().readNext(pos, true);
    // if (nextToken == TerminalTokens.TokenNameLBRACE) {
    // return getScanner().getCurrentEndOffset();
    // }
    // } catch (CoreException e) {
    // handleException(e);
    // }
    return pos;
  }

  private final boolean hasChildrenChanges(DartNode node) {
    return eventStore.hasChangedProperties(node);
  }

  private boolean isAllOfKind(RewriteEvent[] children, int kind) {
    for (int i = 0; i < children.length; i++) {
      if (children[i].getChangeKind() != kind) {
        return false;
      }
    }
    return true;
  }

  private final boolean isChanged(DartNode node, StructuralPropertyDescriptor property) {
    RewriteEvent event = getEvent(node, property);
    if (event != null) {
      return event.getChangeKind() != RewriteEvent.UNCHANGED;
    }
    return false;
  }

  private final boolean isCollapsed(DartNode node) {
    return nodeInfos.isCollapsed(node);
  }

  private boolean needsNewLineForLineComment(DartNode node, String formatted, int offset) {
    if (!lineCommentEndOffsets.isEndOfLineComment(getExtendedEnd(node), content)) {
      return false;
    }
    // copied code ends with a line comment, but doesn't contain the new line
    return offset < formatted.length()
        && !IndentManipulation.isLineDelimiterChar(formatted.charAt(offset));
  }

  private void replaceOperation(
      int posBeforeOperation, String newOperation, TextEditGroup editGroup) {
    try {
      getScanner().readNext(posBeforeOperation, true);
      doTextReplace(
          getScanner().getCurrentStartOffset(),
          getScanner().getCurrentLength(),
          newOperation,
          editGroup);
    } catch (CoreException e) {
      handleException(e);
    }
  }

  /*
   * endpos can be -1 -> use the end pos of the body
   */
  private int rewriteBodyNode(DartNode parent, StructuralPropertyDescriptor property, int offset,
      int endPos, int indent, BlockContext context) {
    RewriteEvent event = getEvent(parent, property);
    if (event != null) {
      switch (event.getChangeKind()) {
        case RewriteEvent.INSERTED: {
        DartNode node = (DartNode) event.getNewValue();
        TextEditGroup editGroup = getEditGroup(event);

        String[] strings = context.getPrefixAndSuffix(indent, node, eventStore);

        doTextInsert(offset, strings[0], editGroup);
        doTextInsert(offset, node, indent, true, editGroup);
        doTextInsert(offset, strings[1], editGroup);
        return offset;
      }
        case RewriteEvent.REMOVED: {
        DartNode node = (DartNode) event.getOriginalValue();
        if (endPos == -1) {
          endPos = getExtendedEnd(node);
        }

        TextEditGroup editGroup = getEditGroup(event);
        // if there is a prefix, remove the prefix as well
        int len = endPos - offset;
        doTextRemoveAndVisit(offset, len, node, editGroup);
        return endPos;
      }
        case RewriteEvent.REPLACED: {
        DartNode node = (DartNode) event.getOriginalValue();
        boolean insertNewLine = false;
        if (endPos == -1) {
          int previousEnd = node.getSourceInfo().getOffset() + node.getSourceInfo().getLength();
          endPos = getExtendedEnd(node);
          if (endPos != previousEnd) {
            // check if the end is a comment
            Token token = Token.EOS;
            try {
              token = getScanner().readNext(previousEnd, false);
            } catch (CoreException e) {
              // ignore
            }
            if (token == Token.COMMENT) {
              insertNewLine = true;
            }
          }
        }
        TextEditGroup editGroup = getEditGroup(event);
        int nodeLen = endPos - offset;

        DartNode replacingNode = (DartNode) event.getNewValue();
        String[] strings = context.getPrefixAndSuffix(indent, replacingNode, eventStore);
        doTextRemoveAndVisit(offset, nodeLen, node, editGroup);

        String prefix = strings[0];
        String insertedPrefix = prefix;
        if (insertNewLine) {
          DartCore.notYetImplemented();
          insertedPrefix = getLineDelimiter() + "" /*
                                                    * formatter.createIndentString (indent)
                                                    */
          + insertedPrefix.trim() + ' ';
        }
        doTextInsert(offset, insertedPrefix, editGroup);
        String lineInPrefix = getCurrentLine(prefix, prefix.length());
        if (prefix.length() != lineInPrefix.length()) {
          // prefix contains a new line: update the indent to the one used in
          // the prefix
          DartCore.notYetImplemented();
          indent = 0 /* formatter.computeIndentUnits(lineInPrefix) */;
        }
        doTextInsert(offset, replacingNode, indent, true, editGroup);
        doTextInsert(offset, strings[1], editGroup);
        return endPos;
      }
      }
    }
    int pos = doVisit(parent, property, offset);
    if (endPos != -1) {
      return endPos;
    }
    return pos;
  }

  private int rewriteExtraDimensions(
      DartNode parent, StructuralPropertyDescriptor property, int pos) {
    RewriteEvent event = getEvent(parent, property);
    if (event == null || event.getChangeKind() == RewriteEvent.UNCHANGED) {
      return ((Integer) getOriginalValue(parent, property)).intValue();
    }
    int oldDim = ((Integer) event.getOriginalValue()).intValue();
    int newDim = ((Integer) event.getNewValue()).intValue();

    if (oldDim != newDim) {
      TextEditGroup editGroup = getEditGroup(event);
      rewriteExtraDimensions(oldDim, newDim, pos, editGroup);
    }
    return oldDim;
  }

  private void rewriteExtraDimensions(int oldDim, int newDim, int pos, TextEditGroup editGroup) {

    if (oldDim < newDim) {
      for (int i = oldDim; i < newDim; i++) {
        doTextInsert(pos, "[]", editGroup); //$NON-NLS-1$
      }
    } else if (newDim < oldDim) {
      // try {
      getScanner().setOffset(pos);
      DartCore.notYetImplemented();
      // for (int i = newDim; i < oldDim; i++) {
      // getScanner().readToToken(TerminalTokens.TokenNameRBRACKET);
      // }
      doTextRemove(pos, getScanner().getCurrentEndOffset() - pos, editGroup);
      // } catch (CoreException e) {
      // handleException(e);
      // }
    }
  }

  private int rewriteJavadoc(DartNode node, StructuralPropertyDescriptor property) {
    int pos = rewriteNode(
        node,
        property,
        node.getSourceInfo().getOffset(),
        ASTRewriteFormatter.NONE);
    int changeKind = getChangeKind(node, property);
    if (changeKind == RewriteEvent.INSERTED) {
      String indent = getLineDelimiter() + getIndentAtOffset(pos);
      doTextInsert(pos, indent, getEditGroup(node, property));
    } else if (changeKind == RewriteEvent.REMOVED) {
      try {
        getScanner().readNext(pos, false);
        doTextRemove(pos, getScanner().getCurrentStartOffset() - pos, getEditGroup(node, property));
        pos = getScanner().getCurrentStartOffset();
      } catch (CoreException e) {
        handleException(e);
      }
    }
    return pos;
  }

  private void rewriteMethodBody(DartMethodDefinition parent, int startPos) {
    // TODO(brianwilkerson) This used to reference the method body property. I'm
    // not sure whether function body is the equivalent. See last statement as
    // well.
    DartCore.notYetImplemented();
    RewriteEvent event = getEvent(parent, PropertyDescriptorHelper.DART_FUNCTION_BODY);
    if (event != null) {
      switch (event.getChangeKind()) {
        case RewriteEvent.INSERTED: {
        int endPos = parent.getSourceInfo().getOffset() + parent.getSourceInfo().getLength();
        TextEditGroup editGroup = getEditGroup(event);
        DartNode body = (DartNode) event.getNewValue();
        doTextRemove(startPos, endPos - startPos, editGroup);
        int indent = getIndent(parent.getSourceInfo().getOffset());
        String prefix = formatter.METHOD_BODY.getPrefix(indent);
        doTextInsert(startPos, prefix, editGroup);
        doTextInsert(startPos, body, indent, true, editGroup);
        return;
      }
        case RewriteEvent.REMOVED: {
        TextEditGroup editGroup = getEditGroup(event);
        DartNode body = (DartNode) event.getOriginalValue();
        int endPos = parent.getSourceInfo().getOffset() + parent.getSourceInfo().getLength();
        doTextRemoveAndVisit(startPos, endPos - startPos, body, editGroup);
        doTextInsert(startPos, ";", editGroup); //$NON-NLS-1$
        return;
      }
        case RewriteEvent.REPLACED: {
        TextEditGroup editGroup = getEditGroup(event);
        DartNode body = (DartNode) event.getOriginalValue();
        doTextRemoveAndVisit(
            body.getSourceInfo().getOffset(),
            body.getSourceInfo().getLength(),
            body,
            editGroup);
        doTextInsert(
            body.getSourceInfo().getOffset(),
            (DartNode) event.getNewValue(),
            getIndent(body.getSourceInfo().getOffset()),
            true,
            editGroup);
        return;
      }
      }
    }
    voidVisit(parent, PropertyDescriptorHelper.DART_FUNCTION_BODY);
  }

  private void rewriteModifiers(
      DartNode parent, StructuralPropertyDescriptor property, int offset) {
    DartCore.notYetImplemented();
    // RewriteEvent event = getEvent(parent, property);
    // if (event == null || event.getChangeKind() != RewriteEvent.REPLACED) {
    // return;
    // }
    // try {
    // int oldModifiers = ((Integer) event.getOriginalValue()).intValue();
    // int newModifiers = ((Integer) event.getNewValue()).intValue();
    // TextEditGroup editGroup = getEditGroup(event);
    //
    // TokenScanner scanner = getScanner();
    //
    // Token tok = scanner.readNext(offset, false);
    // int startPos = scanner.getCurrentStartOffset();
    // int nextStart = startPos;
    // loop : while (true) {
    // if (TokenScanner.isComment(tok)) {
    // tok = scanner.readNext(true); // next non-comment token
    // }
    // boolean keep = true;
    // switch (tok) {
    // case TerminalTokens.TokenNamepublic:
    // keep = Modifier.isPublic(newModifiers);
    // break;
    // case TerminalTokens.TokenNameprotected:
    // keep = Modifier.isProtected(newModifiers);
    // break;
    // case TerminalTokens.TokenNameprivate:
    // keep = Modifier.isPrivate(newModifiers);
    // break;
    // case TerminalTokens.TokenNamestatic:
    // keep = Modifier.isStatic(newModifiers);
    // break;
    // case TerminalTokens.TokenNamefinal:
    // keep = Modifier.isFinal(newModifiers);
    // break;
    // case TerminalTokens.TokenNameabstract:
    // keep = Modifier.isAbstract(newModifiers);
    // break;
    // case TerminalTokens.TokenNamenative:
    // keep = Modifier.isNative(newModifiers);
    // break;
    // case TerminalTokens.TokenNamevolatile:
    // keep = Modifier.isVolatile(newModifiers);
    // break;
    // case TerminalTokens.TokenNamestrictfp:
    // keep = Modifier.isStrictfp(newModifiers);
    // break;
    // case TerminalTokens.TokenNametransient:
    // keep = Modifier.isTransient(newModifiers);
    // break;
    // case TerminalTokens.TokenNamesynchronized:
    // keep = Modifier.isSynchronized(newModifiers);
    // break;
    // default:
    // break loop;
    // }
    // tok = getScanner().readNext(false); // include comments
    // int currPos = nextStart;
    // nextStart = getScanner().getCurrentStartOffset();
    // if (!keep) {
    // doTextRemove(currPos, nextStart - currPos, editGroup);
    // }
    // }
    // int addedModifiers = newModifiers & ~oldModifiers;
    // if (addedModifiers != 0) {
    // if (startPos != nextStart) {
    // int visibilityModifiers = addedModifiers
    // & (Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED);
    // if (visibilityModifiers != 0) {
    // StringBuffer buf = new StringBuffer();
    // ASTRewriteFlattener.printModifiers(visibilityModifiers, buf);
    // doTextInsert(startPos, buf.toString(), editGroup);
    // addedModifiers &= ~visibilityModifiers;
    // }
    // }
    // StringBuffer buf = new StringBuffer();
    // ASTRewriteFlattener.printModifiers(addedModifiers, buf);
    // doTextInsert(nextStart, buf.toString(), editGroup);
    // }
    // } catch (CoreException e) {
    // handleException(e);
    // }
  }

  private int rewriteModifiers2(DartNode node, ChildListPropertyDescriptor property, int pos) {
    DartCore.notYetImplemented();
    return pos;
    // RewriteEvent event = getEvent(node, property);
    // if (event == null || event.getChangeKind() == RewriteEvent.UNCHANGED) {
    // return doVisit(node, property, pos);
    // }
    // RewriteEvent[] children = event.getChildren();
    // boolean isAllInsert = isAllOfKind(children, RewriteEvent.INSERTED);
    // boolean isAllRemove = isAllOfKind(children, RewriteEvent.REMOVED);
    // if (isAllInsert || isAllRemove) {
    // // update pos
    // try {
    // pos = getScanner().getNextStartOffset(pos, false);
    // } catch (CoreException e) {
    // handleException(e);
    // }
    // }
    //
    // Prefix formatterPrefix;
    // if (property == SingleVariableDeclaration.MODIFIERS2_PROPERTY)
    // formatterPrefix = formatter.PARAM_ANNOTATION_SEPARATION;
    // else
    // formatterPrefix = formatter.ANNOTATION_SEPARATION;
    //
    // int endPos = new ModifierRewriter(formatterPrefix).rewriteList(node,
    //        property, pos, "", " "); //$NON-NLS-1$ //$NON-NLS-2$
    //
    // try {
    // int nextPos = getScanner().getNextStartOffset(endPos, false);
    //
    // boolean lastUnchanged = children[children.length - 1].getChangeKind() !=
    // RewriteEvent.UNCHANGED;
    //
    // if (isAllRemove) {
    // doTextRemove(endPos, nextPos - endPos,
    // getEditGroup(children[children.length - 1]));
    // return nextPos;
    // } else if (isAllInsert || (nextPos == endPos && lastUnchanged)) { // see
    // // bug
    // // 165654
    // RewriteEvent lastChild = children[children.length - 1];
    // String separator;
    // if (lastChild.getNewValue() instanceof Annotation) {
    // separator = formatterPrefix.getPrefix(getIndent(pos));
    // } else {
    // separator = String.valueOf(' ');
    // }
    // doTextInsert(endPos, separator, getEditGroup(lastChild));
    // }
    // } catch (CoreException e) {
    // handleException(e);
    // }
    // return endPos;
  }

  private int rewriteNode(
      DartNode parent, StructuralPropertyDescriptor property, int offset, Prefix prefix) {
    RewriteEvent event = getEvent(parent, property);
    if (event != null) {
      switch (event.getChangeKind()) {
        case RewriteEvent.INSERTED: {
        DartNode node = (DartNode) event.getNewValue();
        TextEditGroup editGroup = getEditGroup(event);
        int indent = getIndent(offset);
        doTextInsert(offset, prefix.getPrefix(indent), editGroup);
        doTextInsert(offset, node, indent, true, editGroup);
        return offset;
      }
        case RewriteEvent.REMOVED: {
        DartNode node = (DartNode) event.getOriginalValue();
        TextEditGroup editGroup = getEditGroup(event);

        // if there is a prefix, remove the prefix as well
        int nodeEnd;
        int len;
        if (offset == 0) {
          SourceRange range = getExtendedRange(node);
          offset = range.getStartPosition();
          len = range.getLength();
          nodeEnd = offset + len;
        } else {
          nodeEnd = getExtendedEnd(node);
          len = nodeEnd - offset;
        }
        doTextRemoveAndVisit(offset, len, node, editGroup);
        return nodeEnd;
      }
        case RewriteEvent.REPLACED: {
        DartNode node = (DartNode) event.getOriginalValue();
        TextEditGroup editGroup = getEditGroup(event);
        SourceRange range = getExtendedRange(node);
        int nodeOffset = range.getStartPosition();
        int nodeLen = range.getLength();
        doTextRemoveAndVisit(nodeOffset, nodeLen, node, editGroup);
        doTextInsert(
            nodeOffset,
            (DartNode) event.getNewValue(),
            getIndent(offset),
            true,
            editGroup);
        return nodeOffset + nodeLen;
      }
      }
    }
    return doVisit(parent, property, offset);
  }

  private int rewriteNodeList(DartNode parent, StructuralPropertyDescriptor property, int pos,
      String keyword, String separator) {
    RewriteEvent event = getEvent(parent, property);
    if (event != null && event.getChangeKind() != RewriteEvent.UNCHANGED) {
      return new ListRewriter().rewriteList(parent, property, pos, keyword, separator);
    }
    return doVisit(parent, property, pos);
  }

  private void rewriteOperation(
      DartNode parent, StructuralPropertyDescriptor property, int posBeforeOperation) {
    RewriteEvent event = getEvent(parent, property);
    if (event != null && event.getChangeKind() != RewriteEvent.UNCHANGED) {
      try {
        String newOperation = event.getNewValue().toString();
        TextEditGroup editGroup = getEditGroup(event);
        getScanner().readNext(posBeforeOperation, true);
        doTextReplace(
            getScanner().getCurrentStartOffset(),
            getScanner().getCurrentLength(),
            newOperation,
            editGroup);
      } catch (CoreException e) {
        handleException(e);
      }
    }
  }

  private int rewriteOptionalQualifier(
      DartNode parent, StructuralPropertyDescriptor property, int startPos) {
    RewriteEvent event = getEvent(parent, property);
    if (event != null) {
      switch (event.getChangeKind()) {
        case RewriteEvent.INSERTED: {
        DartNode node = (DartNode) event.getNewValue();
        TextEditGroup editGroup = getEditGroup(event);
        doTextInsert(startPos, node, getIndent(startPos), true, editGroup);
        doTextInsert(startPos, ".", editGroup); //$NON-NLS-1$
        return startPos;
      }
        case RewriteEvent.REMOVED: {
        // try {
        DartNode node = (DartNode) event.getOriginalValue();
        TextEditGroup editGroup = getEditGroup(event);
        DartCore.notYetImplemented();
        int dotEnd = 0; // getScanner().getTokenEndOffset(TerminalTokens.TokenNameDOT,
                        // node.getSourceInfo().getSourceStart() + node.getSourceInfo().getSourceLength());
        doTextRemoveAndVisit(startPos, dotEnd - startPos, node, editGroup);
        return dotEnd;
        // } catch (CoreException e) {
        // handleException(e);
        // }
        // break;
      }
        case RewriteEvent.REPLACED: {
        DartNode node = (DartNode) event.getOriginalValue();
        TextEditGroup editGroup = getEditGroup(event);
        SourceRange range = getExtendedRange(node);
        int offset = range.getStartPosition();
        int length = range.getLength();

        doTextRemoveAndVisit(offset, length, node, editGroup);
        doTextInsert(offset, (DartNode) event.getNewValue(), getIndent(startPos), true, editGroup);
        DartCore.notYetImplemented();
        // try {
        // return getScanner().getTokenEndOffset(TerminalTokens.TokenNameDOT,
        // offset + length);
        // } catch (CoreException e) {
        // handleException(e);
        // }
        // break;
        return 0;
      }
      }
    }
    Object node = getOriginalValue(parent, property);
    if (node == null) {
      return startPos;
    }
    int pos = doVisit((DartNode) node);
    DartCore.notYetImplemented();
    // try {
    // return getScanner().getTokenEndOffset(TerminalTokens.TokenNameDOT, pos);
    // } catch (CoreException e) {
    // handleException(e);
    // }
    return pos;
  }

  private int rewriteOptionalTypeParameters(DartNode parent, StructuralPropertyDescriptor property,
      int offset, String keyword, boolean adjustOnNext, boolean needsSpaceOnRemoveAll) {
    int pos = offset;
    RewriteEvent event = getEvent(parent, property);
    if (event != null && event.getChangeKind() != RewriteEvent.UNCHANGED) {
      RewriteEvent[] children = event.getChildren();
      try {
        boolean isAllInserted = isAllOfKind(children, RewriteEvent.INSERTED);
        if (isAllInserted && adjustOnNext) {
          pos = getScanner().getNextStartOffset(pos, false); // adjust on next
                                                             // element
        }
        boolean isAllRemoved = !isAllInserted && isAllOfKind(children, RewriteEvent.REMOVED);
        if (isAllRemoved) { // all removed: set start to left bracket
          DartCore.notYetImplemented();
          int posBeforeOpenBracket = 0; // getScanner().getTokenStartOffset(TerminalTokens.TokenNameLESS,
                                        // pos);
          if (posBeforeOpenBracket != pos) {
            needsSpaceOnRemoveAll = false;
          }
          pos = posBeforeOpenBracket;
        }
        pos = new ListRewriter().rewriteList(parent, property, pos, String.valueOf('<'), ", "); //$NON-NLS-1$
        if (isAllRemoved) { // all removed: remove right and space up to next
                            // element
          DartCore.notYetImplemented();
          int endPos = 0; // getScanner().getTokenEndOffset(TerminalTokens.TokenNameGREATER,
                          // pos); // set pos to '>'
          endPos = getScanner().getNextStartOffset(endPos, false);
          String replacement = needsSpaceOnRemoveAll ? String.valueOf(' ') : Util.EMPTY_STRING;
          doTextReplace(
              pos,
              endPos - pos,
              replacement,
              getEditGroup(children[children.length - 1]));
          return endPos;
        } else if (isAllInserted) {
          doTextInsert(
              pos,
              String.valueOf('>' + keyword),
              getEditGroup(children[children.length - 1]));
          return pos;
        }
      } catch (CoreException e) {
        handleException(e);
      }
    } else {
      pos = doVisit(parent, property, pos);
    }
    if (pos != offset) { // list contained some type -> parse after closing
                         // bracket
      DartCore.notYetImplemented();
      // try {
      // return getScanner().getTokenEndOffset(TerminalTokens.TokenNameGREATER,
      // pos);
      // } catch (CoreException e) {
      // handleException(e);
      // }
    }
    return pos;
  }

  private int rewriteParagraphList(DartNode parent, StructuralPropertyDescriptor property,
      int insertPos, int insertIndent, int separator, int lead) {
    RewriteEvent event = getEvent(parent, property);
    if (event == null || event.getChangeKind() == RewriteEvent.UNCHANGED) {
      return doVisit(parent, property, insertPos);
    }

    RewriteEvent[] events = event.getChildren();
    ParagraphListRewriter listRewriter = new ParagraphListRewriter(insertIndent, separator);
    StringBuffer leadString = new StringBuffer();
    if (isAllOfKind(events, RewriteEvent.INSERTED)) {
      for (int i = 0; i < lead; i++) {
        leadString.append(getLineDelimiter());
      }
      leadString.append(createIndentString(insertIndent));
    }
    return listRewriter.rewriteList(parent, property, insertPos, leadString.toString());
  }

  private int rewriteRequiredNode(DartNode parent, StructuralPropertyDescriptor property) {
    RewriteEvent event = getEvent(parent, property);
    if (event != null && event.getChangeKind() == RewriteEvent.REPLACED) {
      DartNode node = (DartNode) event.getOriginalValue();
      TextEditGroup editGroup = getEditGroup(event);
      SourceRange range = getExtendedRange(node);
      int offset = range.getStartPosition();
      int length = range.getLength();
      doTextRemoveAndVisit(offset, length, node, editGroup);
      doTextInsert(offset, (DartNode) event.getNewValue(), getIndent(offset), true, editGroup);
      return offset + length;
    }
    return doVisit(parent, property, 0);
  }

  private void rewriteReturnType(
      DartMethodDefinition node, boolean isConstructor, boolean isConstructorChange) {
    DartCore.notYetImplemented();
    ChildPropertyDescriptor property = (ChildPropertyDescriptor) PropertyDescriptorHelper.DART_FUNCTION_RETURN_TYPE; // DartMethodDefinition.RETURN_TYPE_PROPERTY;

    // weakness in the AST: return type can exist, even if missing in source
    DartNode originalReturnType = (DartNode) getOriginalValue(node, property);
    boolean returnTypeExists = originalReturnType != null
        && originalReturnType.getSourceInfo().getOffset() != -1;
    if (!isConstructorChange && returnTypeExists) {
      rewriteRequiredNode(node, property);
      ensureSpaceAfterReplace(node, property);
      return;
    }
    // difficult cases: return type insert or remove
    DartNode newReturnType = (DartNode) getNewValue(node, property);
    if (isConstructorChange || !returnTypeExists && newReturnType != originalReturnType) {
      // use the start offset of the method name to insert
      DartCore.notYetImplemented();
      DartNode originalMethodName = (DartNode) getOriginalValue(
          node,
          PropertyDescriptorHelper.DART_CLASS_MEMBER_NAME); // DartMethodDefinition.NAME_PROPERTY);
      // see bug 84049: can't use extended offset
      int nextStart = originalMethodName.getSourceInfo().getOffset();
      TextEditGroup editGroup = getEditGroup(node, property);
      if (isConstructor || !returnTypeExists) { // insert
        doTextInsert(nextStart, newReturnType, getIndent(nextStart), true, editGroup);
        doTextInsert(nextStart, " ", editGroup); //$NON-NLS-1$
      } else { // remove up to the method name
        int offset = getExtendedOffset(originalReturnType);
        doTextRemoveAndVisit(offset, nextStart - offset, originalReturnType, editGroup);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private final void voidVisit(DartNode parent, StructuralPropertyDescriptor property) {
    Object node = getOriginalValue(parent, property);
    // TODO(scheglov)
    if (property.isChildProperty() && node instanceof DartNode) {
      voidVisit((DartNode) node);
    } else if (property.isChildListProperty()) {
      voidVisitList((List<DartNode>) node);
    }
//    if (property.isChildProperty() && node != null) {
//      voidVisit((DartNode) node);
//    } else if (property.isChildListProperty()) {
//      voidVisitList((List<DartNode>) node);
//    }
  }

  private void voidVisitList(List<DartNode> list) {
    for (Iterator<DartNode> iter = list.iterator(); iter.hasNext();) {
      doVisit(iter.next());
    }
  }
}
