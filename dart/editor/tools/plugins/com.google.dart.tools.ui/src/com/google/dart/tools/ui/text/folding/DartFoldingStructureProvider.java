/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.tools.ui.text.folding;

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.ClassMember;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.Directive;
import com.google.dart.engine.ast.NamespaceDirective;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.ast.visitor.BreadthFirstVisitor;
import com.google.dart.engine.ast.visitor.GeneralizingAstVisitor;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.error.BooleanErrorListener;
import com.google.dart.engine.scanner.CharSequenceReader;
import com.google.dart.engine.scanner.Scanner;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.utilities.source.SourceRange;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.PreferenceConstants;
import com.google.dart.tools.ui.internal.text.dart.IDartReconcilingListener;
import com.google.dart.tools.ui.internal.text.editor.CompilationUnitEditor;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.functions.DocumentCharacterIterator;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.projection.IProjectionListener;
import org.eclipse.jface.text.source.projection.IProjectionPosition;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Updates the projection model of a compilation unit.
 */
public class DartFoldingStructureProvider implements IDartFoldingStructureProvider,
    IDartFoldingStructureProviderExtension {

  /**
   * A {@link ProjectionAnnotation} for Dart code.
   */
  protected static class DartProjectionAnnotation extends ProjectionAnnotation {

    private AstNode node;
    private boolean isComment;

    /**
     * Creates a new projection annotation.
     * 
     * @param isCollapsed <code>true</code> to set the initial state to collapsed,
     *          <code>false</code> to set it to expanded
     * @param node the Dart AST node this annotation refers to
     * @param isComment <code>true</code> for a foldable comment, <code>false</code> for a foldable
     *          code element
     */
    public DartProjectionAnnotation(boolean isCollapsed, AstNode node, boolean isComment) {
      super(isCollapsed);
      this.node = node;
      this.isComment = isComment;
    }

    @Override
    public String toString() {
      return "DartProjectionAnnotation:\n" + //$NON-NLS-1$
          "\tnode: \t" + node.toString() + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
          "\tcollapsed: \t" + isCollapsed() + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
          "\tcomment: \t" + isComment() + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    AstNode getElement() {
      return node;
    }

    boolean isComment() {
      return isComment;
    }

    void setElement(AstNode element) {
      node = element;
    }

    void setIsComment(boolean isComment) {
      this.isComment = isComment;
    }
  }

  /**
   * A context that contains the information needed to compute the folding structure of a Dart
   * compilation unit. Computed folding regions are collected via
   * {@linkplain #addProjectionRange(DartFoldingStructureProvider.DartProjectionAnnotation, Position)
   * addProjectionRange}.
   */
  protected class FoldingStructureComputationContext {

    private ProjectionAnnotationModel model;
    private IDocument document;
    private boolean allowCollapsing;
    private AstNode firstRef;
    private boolean hasHeaderComment;
    private Map<DartProjectionAnnotation, Position> map = new LinkedHashMap<DartProjectionAnnotation, Position>();
    private TokenStream tokenStream;

    private FoldingStructureComputationContext(IDocument document, ProjectionAnnotationModel model,
        boolean allowCollapsing) {
      Assert.isNotNull(document);
      Assert.isNotNull(model);
      this.document = document;
      this.model = model;
      this.allowCollapsing = allowCollapsing;
    }

    /**
     * Adds a projection (folding) region to this context. The created annotation / position pair
     * will be added to the {@link ProjectionAnnotationModel} of the {@link ProjectionViewer} of the
     * editor.
     * 
     * @param annotation the annotation to add
     * @param position the corresponding position
     */
    public void addProjectionRange(DartProjectionAnnotation annotation, Position position) {
      map.put(annotation, position);
    }

    /**
     * Returns <code>true</code> if newly created folding regions may be collapsed,
     * <code>false</code> if not. This is usually <code>false</code> when updating the folding
     * structure while typing; it may be <code>true</code> when computing or restoring the initial
     * folding structure.
     * 
     * @return <code>true</code> if newly created folding regions may be collapsed,
     *         <code>false</code> if not
     */
    public boolean allowCollapsing() {
      return allowCollapsing;
    }

    /**
     * Returns <code>true</code> if classes should be collapsed.
     * 
     * @return <code>true</code> if classes should be collapsed
     */
    public boolean collapseClasses() {
      return allowCollapsing && collapseClasses;
    }

    /**
     * Returns <code>true</code> if Dart doc comments should be collapsed.
     * 
     * @return <code>true</code> if Dart doc comments should be collapsed
     */
    public boolean collapseDartDoc() {
      return allowCollapsing && collapseDartDoc;
    }

    /**
     * Returns <code>true</code> if top-level functions should be collapsed.
     * 
     * @return <code>true</code> if functions should be collapsed
     */
    public boolean collapseFunctions() {
      return allowCollapsing && collapseFunctions;
    }

    /**
     * Returns <code>true</code> if header comments should be collapsed.
     * 
     * @return <code>true</code> if header comments should be collapsed
     */
    public boolean collapseHeaderComments() {
      return allowCollapsing && collapseHeaderComments;
    }

    /**
     * Returns <code>true</code> if import containers should be collapsed.
     * 
     * @return <code>true</code> if import containers should be collapsed
     */
    public boolean collapseImportContainer() {
      return allowCollapsing && collapseImportContainer;
    }

    /**
     * Returns <code>true</code> if methods should be collapsed.
     * 
     * @return <code>true</code> if methods should be collapsed
     */
    public boolean collapseMembers() {
      return allowCollapsing && collapseMembers;
    }

    public boolean collapseStatements() {
      return false; // TODO(messick) Implement statement folding.
    }

    boolean hasFirstRef() {
      return firstRef != null;
    }

    /**
     * Returns the document which contains the code being folded.
     * 
     * @return the document which contains the code being folded
     */
    private IDocument getDocument() {
      return document;
    }

    private AstNode getFirstRef() {
      return firstRef;
    }

    private ProjectionAnnotationModel getModel() {
      return model;
    }

    private TokenStream getScanner(int start) throws InvalidSourceException {
      tokenStream.begin(start);
      return tokenStream;
    }

    private boolean hasHeaderComment() {
      return hasHeaderComment;
    }

    private void setFirstRef(AstNode type) {
      if (hasFirstRef()) {
        throw new IllegalStateException();
      }
      firstRef = type;
    }

    private void setHasHeaderComment() {
      hasHeaderComment = true;
    }

    private void setScannerSource(String source) throws InvalidSourceException {
      this.tokenStream = new TokenStream(source);
    }
  }

  private static class CollapsibleNodeClassifier extends
      GeneralizingAstVisitor<CollapsibleNodeType> {

    @Override
    public CollapsibleNodeType visitClassMember(ClassMember node) {
      return CollapsibleNodeType.CLASS_MEMEBER;
    }

    @Override
    public CollapsibleNodeType visitCompilationUnitMember(CompilationUnitMember node) {
      return CollapsibleNodeType.TOP_LEVEL_DECL;
    }

    @Override
    public CollapsibleNodeType visitDirective(Directive node) {
      return CollapsibleNodeType.DIRECTIVE;
    }

    @Override
    public CollapsibleNodeType visitNamespaceDirective(NamespaceDirective node) {
      return CollapsibleNodeType.NAMESPACE_DIRECTIVE;
    }

    @Override
    public CollapsibleNodeType visitNode(AstNode node) {
      return CollapsibleNodeType.NONE;
    }

    @Override
    public CollapsibleNodeType visitStatement(Statement node) {
      return CollapsibleNodeType.STATEMENT;
    }
  }

  private static enum CollapsibleNodeType {
    CLASS_MEMEBER,
    TOP_LEVEL_DECL,
    STATEMENT,
    DIRECTIVE,
    NAMESPACE_DIRECTIVE,
    NONE;
  }

  /**
   * Matches comments.
   */
  private static class CommentFilter implements Filter {
    @Override
    public boolean match(DartProjectionAnnotation annotation) {
      if (annotation.isComment() && !annotation.isMarkedDeleted()) {
        return true;
      }
      return false;
    }
  }

  /**
   * Projection position that will return two foldable regions: one folding away the region from
   * after the '/**' to the beginning of the content, the other from after the first content line
   * until after the comment.
   */
  private static class CommentPosition extends Position implements IProjectionPosition {

    CommentPosition(int offset, int length) {
      super(offset, length);
    }

    @Override
    public int computeCaptionOffset(IDocument document) {
      DocumentCharacterIterator sequence;
      sequence = new DocumentCharacterIterator(document, offset, offset + length);
      return findFirstContent(sequence, 0);
    }

    @Override
    public IRegion[] computeProjectionRegions(IDocument document) throws BadLocationException {
      DocumentCharacterIterator sequence;
      sequence = new DocumentCharacterIterator(document, offset, offset + length);
      int prefixEnd = 0;
      int contentStart = findFirstContent(sequence, prefixEnd);

      int firstLine = document.getLineOfOffset(offset + prefixEnd);
      int captionLine = document.getLineOfOffset(offset + contentStart);
      int lastLine = document.getLineOfOffset(offset + length);

      Assert.isTrue(firstLine <= captionLine, "first folded line is greater than the caption line"); //$NON-NLS-1$
      Assert.isTrue(captionLine <= lastLine, "caption line is greater than the last folded line"); //$NON-NLS-1$

      IRegion preRegion;
      if (firstLine < captionLine) {
//				preRegion= new Region(offset + prefixEnd, contentStart - prefixEnd);
        int preOffset = document.getLineOffset(firstLine);
        IRegion preEndLineInfo = document.getLineInformation(captionLine);
        int preEnd = preEndLineInfo.getOffset();
        preRegion = new Region(preOffset, preEnd - preOffset);
      } else {
        preRegion = null;
      }

      if (captionLine < lastLine) {
        int postOffset = document.getLineOffset(captionLine + 1);
        IRegion postRegion = new Region(postOffset, offset + length - postOffset);
        if (preRegion == null) {
          return new IRegion[] {postRegion};
        }
        return new IRegion[] {preRegion, postRegion};
      }
      if (preRegion != null) {
        return new IRegion[] {preRegion};
      }
      return null;
    }

    /**
     * Finds the offset of the first identifier part within <code>content</code> . Returns 0 if none
     * is found.
     * 
     * @param content the content to search
     * @param prefixEnd the end of the prefix
     * @return the first index of a unicode identifier part, or zero if none can be found
     */
    private int findFirstContent(CharSequence content, int prefixEnd) {
      int lenght = content.length();
      for (int i = prefixEnd; i < lenght; i++) {
        if (Character.isUnicodeIdentifierPart(content.charAt(i))) {
          return i;
        }
      }
      return 0;
    }
  }

  /**
   * Projection position that will return two foldable regions: one folding away the lines before
   * the one containing the simple name of the Dart element, one folding away any lines after the
   * caption.
   */
  private static class DartElementPosition extends Position implements IProjectionPosition {

    private AstNode fMember;

    public DartElementPosition(int offset, int length, AstNode member) {
      super(offset, length);
      Assert.isNotNull(member);
      fMember = member;
    }

    @Override
    public int computeCaptionOffset(IDocument document) throws BadLocationException {
      int nameStart = offset;
      SourceRange nameRange = new SourceRange(fMember.getOffset(), fMember.getLength());
      if (nameRange != null) {
        nameStart = nameRange.getOffset();
      }
      return nameStart - offset;
    }

    @Override
    public IRegion[] computeProjectionRegions(IDocument document) throws BadLocationException {
      int nameStart = offset;
      SourceRange nameRange = new SourceRange(fMember.getOffset(), fMember.getLength());
      if (nameRange != null) {
        nameStart = nameRange.getOffset();
      }
      int firstLine = document.getLineOfOffset(offset);
      int captionLine = document.getLineOfOffset(nameStart);
      int lastLine = document.getLineOfOffset(offset + length);
      if (captionLine < firstLine) {
        captionLine = firstLine;
      }
      if (captionLine > lastLine) {
        captionLine = lastLine;
      }
      IRegion preRegion;
      if (firstLine < captionLine) {
        int preOffset = document.getLineOffset(firstLine);
        IRegion preEndLineInfo = document.getLineInformation(captionLine);
        int preEnd = preEndLineInfo.getOffset();
        preRegion = new Region(preOffset, preEnd - preOffset);
      } else {
        preRegion = null;
      }
      if (captionLine < lastLine) {
        int postOffset = document.getLineOffset(captionLine + 1);
        IRegion postRegion = new Region(postOffset, offset + length - postOffset);

        if (preRegion == null) {
          return new IRegion[] {postRegion};
        }
        return new IRegion[] {preRegion, postRegion};
      }
      if (preRegion != null) {
        return new IRegion[] {preRegion};
      }
      return null;
    }

    public void setMember(AstNode member) {
      Assert.isNotNull(member);
      fMember = member;
    }

  }

  /**
   * Filter for annotations.
   */
  private static interface Filter {
    boolean match(DartProjectionAnnotation annotation);
  }

  private static class InvalidSourceException extends Exception {
  }

  /**
   * Matches members.
   */
  private static class MemberFilter extends CollapsibleNodeClassifier implements Filter {

    @Override
    public boolean match(DartProjectionAnnotation annotation) {
      if (!annotation.isComment() && !annotation.isMarkedDeleted()) {
        AstNode element = annotation.getElement();
        return element.accept(this) != CollapsibleNodeType.NONE;
      }
      return false;
    }

  }

  /**
   * Internal projection listener.
   */
  private class ProjectionListener implements IProjectionListener {
    private ProjectionViewer fViewer;

    /**
     * Registers the listener with the viewer.
     * 
     * @param viewer the viewer to register a listener with
     */
    public ProjectionListener(ProjectionViewer viewer) {
      Assert.isLegal(viewer != null);
      fViewer = viewer;
      fViewer.addProjectionListener(this);
    }

    /**
     * Disposes of this listener and removes the projection listener from the viewer.
     */
    public void dispose() {
      if (fViewer != null) {
        fViewer.removeProjectionListener(this);
        fViewer = null;
      }
    }

    @Override
    public void projectionDisabled() {
      handleProjectionDisabled();
    }

    @Override
    public void projectionEnabled() {
      handleProjectionEnabled();
    }
  }

  private static class TokenStream {
    Token firstToken;
    Token currentToken;
    int begin;

    TokenStream(String source) throws InvalidSourceException {
      BooleanErrorListener listener = new BooleanErrorListener();
      Scanner scanner = new Scanner(null, new CharSequenceReader(source), listener);
      if (listener.getErrorReported()) {
        throw new InvalidSourceException();
      } else {
        firstToken = scanner.tokenize();
        currentToken = firstToken;
        begin = 0;
      }
    }

    void begin(int start) throws InvalidSourceException {
      if (start == begin) {
        return;
      }
      if (start < begin) {
        begin = 0;
        currentToken = firstToken;
      }
      Token prev = currentToken;
      while (begin < start) {
        currentToken = currentToken.getNext();
        if (currentToken == prev) {
          throw new InvalidSourceException();
        }
        prev = currentToken;
        begin = currentToken.getOffset();
      }
    }

    Token next() {
      Token next = currentToken;
      currentToken = currentToken.getNext();
      return next;
    }
  }

  private static class Tuple {
    DartProjectionAnnotation annotation;
    Position position;

    Tuple(DartProjectionAnnotation annotation, Position position) {
      this.annotation = annotation;
      this.position = position;
    }
  }

  private static boolean isAvailable(SourceRange range) {
    return range != null && range.getOffset() != -1;
  }

  /* context and listeners */
  private CompilationUnitEditor dartEditor;
  private ProjectionListener projectionListener;
  private AstNode fInput;

  /* preferences */
  private boolean collapseDartDoc = false;
  private boolean collapseImportContainer = false;
  private boolean collapseMembers = false;
  private boolean collapseHeaderComments = true;
  private boolean collapseClasses = false;
  private boolean collapseFunctions = false;

  /* filters */
  /** Member filter, matches collapse-able AST nodes. */
  private Filter memberFilter = new MemberFilter();
  /** Comment filter, matches comments. */
  private Filter commentFilter = new CommentFilter();

  private CollapsibleNodeClassifier classifier = new CollapsibleNodeClassifier();
  private IDartReconcilingListener reconcileListener = new IDartReconcilingListener() {
    @Override
    public void reconciled(CompilationUnit unit) {
      refresh();
    }
  };

  /**
   * Creates a new folding provider. It must be {@link #install(ITextEditor, ProjectionViewer)
   * installed} on an editor/viewer pair before it can be used, and {@link #uninstall() uninstalled}
   * when not used any longer.
   * <p>
   * The projection state may be reset by calling {@link #initialize()}.
   * </p>
   */
  public DartFoldingStructureProvider() {
  }

  @Override
  public void collapseComments() {
    modifyFiltered(commentFilter, false);
  }

  @Override
  public void collapseMembers() {
    modifyFiltered(memberFilter, false);
  }

  @Override
  public void initialize() {
    update(createInitialContext());
  }

  @Override
  public void install(ITextEditor editor, ProjectionViewer viewer) {
    Assert.isLegal(editor != null);
    Assert.isLegal(viewer != null);
    internalUninstall();
    if (editor instanceof CompilationUnitEditor) {
      projectionListener = new ProjectionListener(viewer);
      dartEditor = (CompilationUnitEditor) editor;
      dartEditor.addReconcileListener(reconcileListener);
    }
  }

  public void refresh() {
    final FoldingStructureComputationContext context = createContext(false);

    Display.getDefault().asyncExec(new Runnable() {
      @Override
      public void run() {
        update(context);
      }
    });
  }

  @Override
  public void uninstall() {
    internalUninstall();
  }

  /**
   * Aligns <code>region</code> to start and end at a line offset. The region's start is decreased
   * to the next line offset, and the end offset increased to the next line start or the end of the
   * document. <code>null</code> is returned if <code>region</code> is <code>null</code> itself or
   * does not comprise at least one line delimiter, as a single line cannot be folded.
   * 
   * @param region the region to align, may be <code>null</code>
   * @param ctx the folding context
   * @return a region equal or greater than <code>region</code> that is aligned with line offsets,
   *         <code>null</code> if the region is too small to be foldable (e.g. covers only one line)
   */
  protected IRegion alignRegion(IRegion region, FoldingStructureComputationContext ctx) {
    if (region == null) {
      return null;
    }
    IDocument document = ctx.getDocument();
    try {
      int start = document.getLineOfOffset(region.getOffset());
      int end = document.getLineOfOffset(region.getOffset() + region.getLength());
      if (start >= end) {
        return null;
      }
      int offset = document.getLineOffset(start);
      int endOffset;
      if (document.getNumberOfLines() > end + 1) {
        endOffset = document.getLineOffset(end);
//        if (endOffset != region.getOffset() + region.getLength()) {
        endOffset = document.getLineOffset(end + 1);
//        }
      } else {
        endOffset = document.getLineOffset(end);
        if (endOffset != end) {
          endOffset += document.getLineLength(end);
        }
      }
      return new Region(offset, endOffset - offset);
    } catch (BadLocationException x) {
      // concurrent modification
      return null;
    }
  }

  /**
   * Computes the folding structure for a given {@link AstNode Dart node}. Computed projection
   * annotations are
   * {@link DartFoldingStructureProvider.FoldingStructureComputationContext#addProjectionRange(DartFoldingStructureProvider.DartProjectionAnnotation, Position)
   * added} to the computation context.
   * <p>
   * This implementation creates projection annotations for the following elements:
   * <ul>
   * <li>top-level functions, variables, and typedefs</li>
   * <li>classes</li>
   * <li>fields and methods</li>
   * <li>local functions</li>
   * </ul>
   * </p>
   * 
   * @param node the Dart AST node to compute the folding structure for
   * @param ctx the computation context
   */
  protected void computeFoldingStructure(AstNode node, FoldingStructureComputationContext ctx) {

    boolean collapse = false;
    boolean collapseCode = true;
    switch (node.accept(classifier)) {
      case DIRECTIVE:
        collapse = ctx.collapseImportContainer();
        break;
      case TOP_LEVEL_DECL:
        collapse = ctx.collapseClasses();
        break;
      case CLASS_MEMEBER:
        collapse = ctx.collapseMembers();
        break;
      case STATEMENT:
        collapse = ctx.collapseStatements();
      default:
        return;
    }

    IRegion[] regions = computeProjectionRanges(node, ctx);
    if (regions.length > 0) {
      // comments
      for (int i = 0; i < regions.length - 1; i++) {
        IRegion normalized = alignRegion(regions[i], ctx);
        if (normalized != null) {
          Position position = createCommentPosition(normalized);
          if (position != null) {
            boolean commentCollapse;
            if (i == 0 && (regions.length > 2 || ctx.hasHeaderComment())
                && node == ctx.getFirstRef()) {
              commentCollapse = ctx.collapseHeaderComments();
            } else {
              commentCollapse = ctx.collapseDartDoc();
            }
            ctx.addProjectionRange(
                new DartProjectionAnnotation(commentCollapse, node, true),
                position);
          }
        }
      }
      // code
      if (collapseCode) {
        IRegion normalized = alignRegion(regions[regions.length - 1], ctx);
        if (normalized != null) {
          Position position = node instanceof ExecutableElement ? createMemberPosition(
              normalized,
              node) : createCommentPosition(normalized);
          if (position != null) {
            ctx.addProjectionRange(new DartProjectionAnnotation(collapse, node, false), position);
          }
        }
      }
    }
  }

  /**
   * Computes the projection ranges for a given <code>ASTNode</code>. More than one range or none at
   * all may be returned. If there are no fold-able regions, an empty array is returned.
   * <p>
   * The last region in the returned array (if not empty) describes the region for the Dart node
   * that implements the source reference. Any preceding regions describe Dart doc comments of that
   * element.
   * </p>
   * 
   * @param reference a Dart element that is a source reference
   * @param ctx the folding context
   * @return the regions to be folded
   */
  protected IRegion[] computeProjectionRanges(AstNode reference,
      FoldingStructureComputationContext ctx) {
    SourceRange range = new SourceRange(reference.getOffset(), reference.getLength());
    if (!isAvailable(range)) {
      return new IRegion[0];
    }
    List<IRegion> regions = new ArrayList<IRegion>();
    if (!ctx.hasFirstRef()) {
      ctx.setFirstRef(reference);
      IRegion headerComment = computeHeaderComment(ctx);
      if (headerComment != null) {
        regions.add(headerComment);
        ctx.setHasHeaderComment();
      }
    }
    int shift = range.getOffset();
    TokenStream scanner;
    try {
      scanner = ctx.getScanner(shift);
    } catch (InvalidSourceException ex) {
      return new IRegion[0];
    }
    int start = shift;
    Token token = scanner.next();
    start = token.getOffset();
    Token comment = token.getPrecedingComments();
    if (dartEditor == null) {
      return new IRegion[0];
    }
    IEditorInput editorInput = dartEditor.getEditorInput();
    IDocumentProvider documentProvider = dartEditor.getDocumentProvider();
    IDocument doc = documentProvider.getDocument(editorInput);
    while (comment != null) {
      int s = comment.getOffset();
      int l = comment.getLength();
      if (comment.getLexeme().startsWith("//")) {
        Token nextComment = comment.getNext();
        while (nextComment != null && nextComment != token
            && nextComment.getLexeme().startsWith("//")) {
          l = nextComment.getEnd() - s;
          nextComment = nextComment.getNext();
        }
        comment = nextComment;
      } else {
        comment = comment.getNext();
      }
      if (isSpanningMultipleLines(doc, s, l)) {
        regions.add(new Region(s, l));
      }
      if (comment == token) {
        comment = null;
      }
    }
    int len = shift + range.getLength() - start - 1;
    regions.add(new Region(start, len));
    IRegion[] result = new IRegion[regions.size()];
    regions.toArray(result);
    return result;
  }

  /**
   * Creates a comment folding position from an
   * {@link #alignRegion(IRegion, DartFoldingStructureProvider.FoldingStructureComputationContext)
   * aligned} region.
   * 
   * @param aligned an aligned region
   * @return a folding position corresponding to <code>aligned</code>
   */
  protected Position createCommentPosition(IRegion aligned) {
    return new CommentPosition(aligned.getOffset(), aligned.getLength());
  }

  /**
   * Creates a folding position that remembers its member from an
   * {@link #alignRegion(IRegion, DartFoldingStructureProvider.FoldingStructureComputationContext)
   * aligned} region.
   * 
   * @param aligned an aligned region
   * @param node the AST node to remember
   * @return a folding position corresponding to <code>aligned</code>
   */
  protected Position createMemberPosition(IRegion aligned, AstNode node) {
    return new DartElementPosition(aligned.getOffset(), aligned.getLength(), node);
  }

  /**
   * Called whenever projection is disabled, for example when the provider is {@link #uninstall()
   * uninstalled}, when the viewer issues a {@link IProjectionListener#projectionDisabled()
   * projectionDisabled} message and before {@link #handleProjectionEnabled() enabling} the
   * provider. Implementations must be prepared to handle multiple calls to this method even if the
   * provider is already disabled.
   */
  protected void handleProjectionDisabled() {
    dartEditor.removeReconcileListener(reconcileListener);
  }

  /**
   * Called whenever projection is enabled, for example when the viewer issues a
   * {@link IProjectionListener#projectionEnabled() projectionEnabled} message. When the provider is
   * already enabled when this method is called, it is first {@link #handleProjectionDisabled()
   * disabled}.
   */
  protected void handleProjectionEnabled() {
    // projectionEnabled messages are not always paired with projectionDisabled
    // i.e. multiple enabled messages may be sent out.
    // we have to make sure that we disable first when getting an enable message.
    handleProjectionDisabled();
    // TODO (danrubel) fix for use with analysis server
    if (isInstalled() && !DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      initialize();
      dartEditor.addReconcileListener(reconcileListener);
    }
  }

  /**
   * Returns <code>true</code> if the provider is installed, <code>false</code> otherwise.
   * 
   * @return <code>true</code> if the provider is installed, <code>false</code> otherwise
   */
  protected boolean isInstalled() {
    return dartEditor != null;
  }

  private Map<AstNode, List<Tuple>> computeCurrentStructure(FoldingStructureComputationContext ctx) {
    Map<AstNode, List<Tuple>> map = new HashMap<AstNode, List<Tuple>>();
    ProjectionAnnotationModel model = ctx.getModel();
    @SuppressWarnings("unchecked")
    Iterator<Object> e = model.getAnnotationIterator();
    while (e.hasNext()) {
      Object annotation = e.next();
      if (annotation instanceof DartProjectionAnnotation) {
        DartProjectionAnnotation projection = (DartProjectionAnnotation) annotation;
        Position position = model.getPosition(projection);
        Assert.isNotNull(position);
        List<Tuple> list = map.get(projection.getElement());
        if (list == null) {
          list = new ArrayList<Tuple>(2);
          map.put(projection.getElement(), list);
        }
        list.add(new Tuple(projection, position));
      }
    }
    Comparator<Tuple> comparator = new Comparator<Tuple>() {
      @Override
      public int compare(Tuple o1, Tuple o2) {
        return o1.position.getOffset() - o2.position.getOffset();
      }
    };
    for (Iterator<List<Tuple>> it = map.values().iterator(); it.hasNext();) {
      List<Tuple> list = it.next();
      Collections.sort(list, comparator);
    }
    return map;
  }

  private void computeFoldingStructure(final FoldingStructureComputationContext ctx) {
    CompilationUnit parent = getInputElement();
    try {
      if (parent == null) {
        return;
      }
      String source = ctx.getDocument().get();
      if (source == null) {
        return;
      }
      ctx.setScannerSource(source);
      BreadthFirstVisitor<Void> v = new BreadthFirstVisitor<Void>() {
        @Override
        public Void visitNode(AstNode node) {
          computeFoldingStructure(node, ctx);
          super.visitNode(node);
          return null;
        }
      };
      v.visitAllNodes(parent);
    } catch (InvalidSourceException x) {
      DartToolsPlugin.log(x);
    }
  }

  private IRegion computeHeaderComment(FoldingStructureComputationContext ctx) {
    // search at most up to the first element
    TokenStream scanner;
    try {
      scanner = ctx.getScanner(0);
    } catch (InvalidSourceException ex) {
      return null;
    }
    int headerStart = -1;
    int headerEnd = -1;
    boolean foundComment = false;
    Token terminal = scanner.next();
    Token comment = terminal.getPrecedingComments();
    while (comment != null) {
      if (!foundComment) {
        headerStart = comment.getOffset();
      }
      headerEnd = comment.getEnd();
      foundComment = true;
      Token nextComment = comment.getNext();
      if (nextComment == terminal || nextComment == null) {
        comment = null;
      } else if (nextComment.getOffset() != comment.getEnd()) {
        comment = null;
      } else {
        comment = nextComment;
      }
    }
    if (headerEnd != -1) {
      return new Region(headerStart, headerEnd - headerStart);
    }
    return null;
  }

  private FoldingStructureComputationContext createContext(boolean allowCollapse) {
    if (!isInstalled()) {
      return null;
    }
    ProjectionAnnotationModel model = getModel();
    if (model == null) {
      return null;
    }
    IDocument doc = getDocument();
    if (doc == null) {
      return null;
    }
    return new FoldingStructureComputationContext(doc, model, allowCollapse);
  }

  private FoldingStructureComputationContext createInitialContext() {
    initializePreferences();
    fInput = getInputElement();
    if (fInput == null) {
      return null;
    }
    return createContext(true);
  }

  /**
   * Finds a match for <code>tuple</code> in a collection of annotations. The positions for the
   * <code>DartProjectionAnnotation</code> instances in <code>annotations</code> can be found in the
   * passed <code>positionMap</code> or <code>cachedModel</code> if <code>positionMap</code> is
   * <code>null</code>.
   * <p>
   * A tuple is said to match another if their annotations have the same comment flag and their
   * position offsets are equal.
   * </p>
   * <p>
   * If a match is found, the annotation gets removed from <code>annotations</code>.
   * </p>
   * 
   * @param tuple the tuple for which we want to find a match
   * @param annotations collection of <code>DartProjectionAnnotation</code>
   * @param positionMap a <code>Map&lt;Annotation, Position&gt;</code> or <code>null</code>
   * @param ctx the context
   * @return a matching tuple or <code>null</code> for no match
   */
  private Tuple findMatch(Tuple tuple, Collection<DartProjectionAnnotation> annotations,
      Map<DartProjectionAnnotation, Position> positionMap, FoldingStructureComputationContext ctx) {
    Iterator<DartProjectionAnnotation> it = annotations.iterator();
    while (it.hasNext()) {
      DartProjectionAnnotation annotation = it.next();
      if (tuple.annotation.isComment() == annotation.isComment()) {
        Position position = positionMap == null ? ctx.getModel().getPosition(annotation)
            : (Position) positionMap.get(annotation);
        if (position == null) {
          continue;
        }
        if (tuple.position.getOffset() == position.getOffset()) {
          it.remove();
          return new Tuple(annotation, position);
        }
      }
    }
    return null;
  }

  private IDocument getDocument() {
    DartEditor editor = dartEditor;
    if (editor == null) {
      return null;
    }
    IDocumentProvider provider = editor.getDocumentProvider();
    if (provider == null) {
      return null;
    }
    return provider.getDocument(editor.getEditorInput());
  }

  private CompilationUnit getInputElement() {
    if (dartEditor == null) {
      return null;
    }
    return dartEditor.getInputUnit();
  }

  private ProjectionAnnotationModel getModel() {
    return (ProjectionAnnotationModel) dartEditor.getAdapter(ProjectionAnnotationModel.class);
  }

  private void initializePreferences() {
    IPreferenceStore store = DartToolsPlugin.getDefault().getPreferenceStore();//dartEditor.getPreferences()
    collapseImportContainer = store.getBoolean(PreferenceConstants.EDITOR_FOLDING_IMPORTS);
    collapseDartDoc = store.getBoolean(PreferenceConstants.EDITOR_FOLDING_JAVADOC);
    collapseMembers = store.getBoolean(PreferenceConstants.EDITOR_FOLDING_METHODS);
    collapseHeaderComments = store.getBoolean(PreferenceConstants.EDITOR_FOLDING_HEADERS);
    collapseClasses = store.getBoolean(PreferenceConstants.EDITOR_FOLDING_CLASSES);
    collapseFunctions = store.getBoolean(PreferenceConstants.EDITOR_FOLDING_FUNCTIONS);
  }

  /**
   * Internal implementation of {@link #uninstall()}.
   */
  private void internalUninstall() {
    if (isInstalled()) {
      handleProjectionDisabled();
      projectionListener.dispose();
      projectionListener = null;
      dartEditor = null;
    }
  }

  private boolean isSpanningMultipleLines(IDocument doc, int offset, int length) {
    try {
      return doc.getLineOfOffset(offset + length) - doc.getLineOfOffset(offset) > 1;
    } catch (BadLocationException ex) {
      return false;
    }
  }

  /**
   * Matches deleted annotations to changed or added ones. A deleted annotation/position tuple that
   * has a matching addition / change is updated and marked as changed. The matching tuple is not
   * added (for additions) or marked as deletion instead (for changes). The result is that more
   * annotations are changed and fewer get deleted/re-added.
   * 
   * @param deletions list with deleted annotations
   * @param additions map with position to annotation mappings
   * @param changes list with changed annotations
   * @param ctx the context
   */
  private void match(List<DartProjectionAnnotation> deletions,
      Map<DartProjectionAnnotation, Position> additions, List<DartProjectionAnnotation> changes,
      FoldingStructureComputationContext ctx) {
    if (deletions.isEmpty() || (additions.isEmpty() && changes.isEmpty())) {
      return;
    }
    List<DartProjectionAnnotation> newDeletions = new ArrayList<DartProjectionAnnotation>();
    List<DartProjectionAnnotation> newChanges = new ArrayList<DartProjectionAnnotation>();
    Iterator<DartProjectionAnnotation> deletionIterator = deletions.iterator();
    while (deletionIterator.hasNext()) {
      DartProjectionAnnotation deleted = deletionIterator.next();
      Position deletedPosition = ctx.getModel().getPosition(deleted);
      if (deletedPosition == null) {
        continue;
      }
      Tuple deletedTuple = new Tuple(deleted, deletedPosition);
      Tuple match = findMatch(deletedTuple, changes, null, ctx);
      boolean addToDeletions = true;
      if (match == null) {
        match = findMatch(deletedTuple, additions.keySet(), additions, ctx);
        addToDeletions = false;
      }
      if (match != null) {
        AstNode element = match.annotation.getElement();
        deleted.setElement(element);
        deletedPosition.setLength(match.position.getLength());
        if (deletedPosition instanceof DartElementPosition && element instanceof AstNode) {
          DartElementPosition jep = (DartElementPosition) deletedPosition;
          jep.setMember(element);
        }
        deletionIterator.remove();
        newChanges.add(deleted);
        if (addToDeletions) {
          newDeletions.add(match.annotation);
        }
      }
    }
    deletions.addAll(newDeletions);
    changes.addAll(newChanges);
  }

  /**
   * Collapses or expands all annotations matched by the passed filter.
   * 
   * @param filter the filter to use to select which annotations to collapse
   * @param expand <code>true</code> to expand the matched annotations, <code>false</code> to
   *          collapse them
   */
  private void modifyFiltered(Filter filter, boolean expand) {
    if (!isInstalled()) {
      return;
    }
    ProjectionAnnotationModel model = getModel();
    if (model == null) {
      return;
    }
    List<DartProjectionAnnotation> modified = new ArrayList<DartProjectionAnnotation>();
    @SuppressWarnings("unchecked")
    Iterator<Object> iter = model.getAnnotationIterator();
    while (iter.hasNext()) {
      Object annotation = iter.next();
      if (annotation instanceof DartProjectionAnnotation) {
        DartProjectionAnnotation projection = (DartProjectionAnnotation) annotation;
        if (expand == projection.isCollapsed() && filter.match(projection)) {
          if (expand) {
            projection.markExpanded();
          } else {
            projection.markCollapsed();
          }
          modified.add(projection);
        }
      }
    }
    model.modifyAnnotations(null, null, modified.toArray(new Annotation[modified.size()]));
  }

  private void update(FoldingStructureComputationContext ctx) {
    if (ctx == null) {
      return;
    }
    if (DartUI.isTooComplexDartDocument(ctx.getDocument())) {
      return;
    }
    Map<DartProjectionAnnotation, Position> additions = new HashMap<DartProjectionAnnotation, Position>();
    List<DartProjectionAnnotation> deletions = new ArrayList<DartProjectionAnnotation>();
    List<DartProjectionAnnotation> updates = new ArrayList<DartProjectionAnnotation>();
    computeFoldingStructure(ctx);
    Map<DartProjectionAnnotation, Position> newStructure = ctx.map;
    Map<AstNode, List<Tuple>> oldStructure = computeCurrentStructure(ctx);
    Iterator<DartProjectionAnnotation> e = newStructure.keySet().iterator();
    while (e.hasNext()) {
      DartProjectionAnnotation newAnnotation = e.next();
      Position newPosition = newStructure.get(newAnnotation);
      AstNode element = newAnnotation.getElement();
      List<Tuple> annotations = oldStructure.get(element);
      if (annotations == null) {
        additions.put(newAnnotation, newPosition);
      } else {
        Iterator<Tuple> x = annotations.iterator();
        boolean matched = false;
        while (x.hasNext()) {
          Tuple tuple = x.next();
          DartProjectionAnnotation existingAnnotation = tuple.annotation;
          Position existingPosition = tuple.position;
          if (newAnnotation.isComment() == existingAnnotation.isComment()) {
            boolean updateCollapsedState = ctx.allowCollapsing()
                && existingAnnotation.isCollapsed() != newAnnotation.isCollapsed();
            if (existingPosition != null
                && (!newPosition.equals(existingPosition) || updateCollapsedState)) {
              existingPosition.setOffset(newPosition.getOffset());
              existingPosition.setLength(newPosition.getLength());
              if (updateCollapsedState) {
                if (newAnnotation.isCollapsed()) {
                  existingAnnotation.markCollapsed();
                } else {
                  existingAnnotation.markExpanded();
                }
              }
              updates.add(existingAnnotation);
            }
            matched = true;
            x.remove();
            break;
          }
        }
        if (!matched) {
          additions.put(newAnnotation, newPosition);
        }
        if (annotations.isEmpty()) {
          oldStructure.remove(element);
        }
      }
    }
    Iterator<List<Tuple>> e2 = oldStructure.values().iterator();
    while (e2.hasNext()) {
      List<Tuple> list = e2.next();
      int size = list.size();
      for (int i = 0; i < size; i++) {
        deletions.add(list.get(i).annotation);
      }
    }
    match(deletions, additions, updates, ctx);
    Annotation[] deletedArray = deletions.toArray(new Annotation[deletions.size()]);
    Annotation[] changedArray = updates.toArray(new Annotation[updates.size()]);
    ctx.getModel().modifyAnnotations(deletedArray, additions, changedArray);
    try {
      ctx.setScannerSource(""); // clear token stream
    } catch (InvalidSourceException e1) {
    }
  }
}
