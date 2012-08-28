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
package com.google.dart.tools.ui.text.folding;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartElementDelta;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.ElementChangedEvent;
import com.google.dart.tools.core.model.ParentElement;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.model.SourceReference;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.model.TypeMember;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartX;
import com.google.dart.tools.ui.PreferenceConstants;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;
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
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Updates the projection model of a class file or compilation unit.
 * <p>
 * Clients may instantiate or subclass. Subclasses must make sure to always call the superclass'
 * code when overriding methods that are marked with "subclasses may extend".
 * </p>
 * Provisional API: This class/interface is part of an interim API that is still under development
 * and expected to change significantly before reaching stability. It is being made available at
 * this early stage to solicit feedback from pioneering adopters on the understanding that any code
 * that uses this API will almost certainly be broken (repeatedly) as the API evolves.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class DefaultDartFoldingStructureProvider implements IDartFoldingStructureProvider,
    IDartFoldingStructureProviderExtension {
  /**
   * A {@link ProjectionAnnotation} for JavaScript code.
   */
  protected static final class DartProjectionAnnotation extends ProjectionAnnotation {

    private DartElement fJavaElement;
    private boolean fIsComment;

    /**
     * Creates a new projection annotation.
     * 
     * @param isCollapsed <code>true</code> to set the initial state to collapsed,
     *          <code>false</code> to set it to expanded
     * @param element the JavaScript element this annotation refers to
     * @param isComment <code>true</code> for a foldable comment, <code>false</code> for a foldable
     *          code element
     */
    public DartProjectionAnnotation(boolean isCollapsed, DartElement element, boolean isComment) {
      super(isCollapsed);
      fJavaElement = element;
      fIsComment = isComment;
    }

    /*
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
      return "JavaProjectionAnnotation:\n" + //$NON-NLS-1$
          "\telement: \t" + fJavaElement.toString() + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
          "\tcollapsed: \t" + isCollapsed() + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
          "\tcomment: \t" + isComment() + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    DartElement getElement() {
      return fJavaElement;
    }

    boolean isComment() {
      return fIsComment;
    }

    void setElement(DartElement element) {
      fJavaElement = element;
    }

    void setIsComment(boolean isComment) {
      fIsComment = isComment;
    }
  }

  /**
   * A context that contains the information needed to compute the folding structure of an
   * {@link org.eclipse.wst.jsdt.core.IJavaScriptUnit} or an
   * {@link org.eclipse.wst.jsdt.core.IClassFile}. Computed folding regions are collected via
   * {@linkplain #addProjectionRange(DefaultDartFoldingStructureProvider.DartProjectionAnnotation, Position)
   * addProjectionRange}.
   */
  protected final class FoldingStructureComputationContext {
    private final ProjectionAnnotationModel fModel;
    private final IDocument fDocument;

    private final boolean fAllowCollapsing;

    private Type fFirstType;
    private boolean fHasHeaderComment;
    private LinkedHashMap fMap = new LinkedHashMap();
    private TokenStream tokenStream;

    private FoldingStructureComputationContext(IDocument document, ProjectionAnnotationModel model,
        boolean allowCollapsing) {
      Assert.isNotNull(document);
      Assert.isNotNull(model);
      fDocument = document;
      fModel = model;
      fAllowCollapsing = allowCollapsing;
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
      fMap.put(annotation, position);
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
      return fAllowCollapsing;
    }

    /**
     * Returns <code>true</code> if header comments should be collapsed.
     * 
     * @return <code>true</code> if header comments should be collapsed
     */
    public boolean collapseHeaderComments() {
      return fAllowCollapsing && fCollapseHeaderComments;
    }

    /**
     * Returns <code>true</code> if import containers should be collapsed.
     * 
     * @return <code>true</code> if import containers should be collapsed
     */
    public boolean collapseImportContainer() {
      return fAllowCollapsing && fCollapseImportContainer;
    }

    /**
     * Returns <code>true</code> if inner types should be collapsed.
     * 
     * @return <code>true</code> if inner types should be collapsed
     */
    public boolean collapseInnerTypes() {
      return fAllowCollapsing && fCollapseInnerTypes;
    }

    /**
     * Returns <code>true</code> if javadoc comments should be collapsed.
     * 
     * @return <code>true</code> if javadoc comments should be collapsed
     */
    public boolean collapseJavadoc() {
      return fAllowCollapsing && fCollapseJavadoc;
    }

    /**
     * Returns <code>true</code> if methods should be collapsed.
     * 
     * @return <code>true</code> if methods should be collapsed
     */
    public boolean collapseMembers() {
      return fAllowCollapsing && fCollapseMembers;
    }

    boolean hasFirstType() {
      return fFirstType != null;
    }

    /**
     * Returns the document which contains the code being folded.
     * 
     * @return the document which contains the code being folded
     */
    private IDocument getDocument() {
      return fDocument;
    }

    private Type getFirstType() {
      return fFirstType;
    }

    private ProjectionAnnotationModel getModel() {
      return fModel;
    }

    private TokenStream getScanner() {
      return getScanner(0);
    }

    private TokenStream getScanner(int start) {
      tokenStream.begin(start);
      return tokenStream;
    }

    private boolean hasHeaderComment() {
      return fHasHeaderComment;
    }

    private void setFirstType(Type type) {
      if (hasFirstType()) {
        throw new IllegalStateException();
      }
      fFirstType = type;
    }

    private void setHasHeaderComment() {
      fHasHeaderComment = true;
    }

    private void setScannerSource(String source) {
      this.tokenStream = new TokenStream(source);
    }
  }

  /**
   * Matches comments.
   */
  private static final class CommentFilter implements Filter {
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
  private static final class CommentPosition extends Position implements IProjectionPosition {
    CommentPosition(int offset, int length) {
      super(offset, length);
    }

    /*
     * @see org.eclipse.jface.text.source.projection.IProjectionPosition#
     * computeCaptionOffset(org.eclipse.jface.text.IDocument)
     */
    @Override
    public int computeCaptionOffset(IDocument document) {
//			return 0;
      DocumentCharacterIterator sequence = new DocumentCharacterIterator(document, offset, offset
          + length);
      return findFirstContent(sequence, 0);
    }

    /*
     * @see org.eclipse.jface.text.source.projection.IProjectionPosition#
     * computeFoldingRegions(org.eclipse.jface.text.IDocument)
     */
    @Override
    public IRegion[] computeProjectionRegions(IDocument document) throws BadLocationException {
      DocumentCharacterIterator sequence = new DocumentCharacterIterator(document, offset, offset
          + length);
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

//		/**
//		 * Finds the offset of the first identifier part within <code>content</code>.
//		 * Returns 0 if none is found.
//		 *
//		 * @param content the content to search
//		 * @return the first index of a unicode identifier part, or zero if none can
//		 *         be found
//		 */
//		private int findPrefixEnd(final CharSequence content) {
//			// return the index after the leading '/*' or '/**'
//			int len= content.length();
//			int i= 0;
//			while (i < len && isWhiteSpace(content.charAt(i)))
//				i++;
//			if (len >= i + 2 && content.charAt(i) == '/' && content.charAt(i + 1) == '*')
//				if (len >= i + 3 && content.charAt(i + 2) == '*')
//					return i + 3;
//				else
//					return i + 2;
//			else
//				return i;
//		}
//
//		private boolean isWhiteSpace(char c) {
//			return c == ' ' || c == '\t';
//		}

    /**
     * Finds the offset of the first identifier part within <code>content</code> . Returns 0 if none
     * is found.
     * 
     * @param content the content to search
     * @param prefixEnd the end of the prefix
     * @return the first index of a unicode identifier part, or zero if none can be found
     */
    private int findFirstContent(final CharSequence content, int prefixEnd) {
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
   * the one containing the simple name of the JavaScript element, one folding away any lines after
   * the caption.
   */
  private static final class DartElementPosition extends Position implements IProjectionPosition {

    private TypeMember fMember;

    public DartElementPosition(int offset, int length, TypeMember member) {
      super(offset, length);
      Assert.isNotNull(member);
      fMember = member;
    }

    /*
     * @see org.eclipse.jface.text.source.projection.IProjectionPosition#
     * computeCaptionOffset(org.eclipse.jface.text.IDocument)
     */
    @Override
    public int computeCaptionOffset(IDocument document) throws BadLocationException {
      int nameStart = offset;
      try {
        // need a reconcile here?
        SourceRange nameRange = fMember.getNameRange();
        if (nameRange != null) {
          nameStart = nameRange.getOffset();
        }
      } catch (DartModelException e) {
        // ignore and use default
      }

      return nameStart - offset;
    }

    /*
     * @see org.eclipse.jface.text.source.projection.IProjectionPosition#
     * computeFoldingRegions(org.eclipse.jface.text.IDocument)
     */
    @Override
    public IRegion[] computeProjectionRegions(IDocument document) throws BadLocationException {
      int nameStart = offset;
      try {
        /*
         * The member's name range may not be correct. However, reconciling would trigger another
         * element delta which would lead to reentrant situations. Therefore, we optimistically
         * assume that the name range is correct, but double check the received lines below.
         */
        SourceRange nameRange = fMember.getNameRange();
        if (nameRange != null) {
          nameStart = nameRange.getOffset();
        }

      } catch (DartModelException e) {
        // ignore and use default
      }

      int firstLine = document.getLineOfOffset(offset);
      int captionLine = document.getLineOfOffset(nameStart);
      int lastLine = document.getLineOfOffset(offset + length);

      /*
       * see comment above - adjust the caption line to be inside the entire folded region, and rely
       * on later element deltas to correct the name range.
       */
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

    public void setMember(TypeMember member) {
      Assert.isNotNull(member);
      fMember = member;
    }

  }

  /**
   * Matches JavaScript elements contained in a certain set.
   */
  private static final class DartElementSetFilter implements Filter {
    private final Set<? extends DartElement> fSet;
    private final boolean fMatchCollapsed;

    private DartElementSetFilter(Set<? extends DartElement> set, boolean matchCollapsed) {
      fSet = set;
      fMatchCollapsed = matchCollapsed;
    }

    @Override
    public boolean match(DartProjectionAnnotation annotation) {
      boolean stateMatch = fMatchCollapsed == annotation.isCollapsed();
      if (stateMatch && !annotation.isComment() && !annotation.isMarkedDeleted()) {
        DartElement element = annotation.getElement();
        if (fSet.contains(element)) {
          return true;
        }
      }
      return false;
    }
  }

  private class ElementChangedListener implements
      com.google.dart.tools.core.model.ElementChangedListener {

    /*
     * @see org.eclipse.wst.jsdt.core.IElementChangedListener#elementChanged(org.
     * eclipse.wst.jsdt.core.ElementChangedEvent)
     */
    @Override
    public void elementChanged(ElementChangedEvent e) {
      DartElementDelta delta = findElement(fInput, e.getDelta());
      if (delta != null
          && (delta.getFlags() & (DartElementDelta.F_CONTENT | DartElementDelta.F_CHILDREN)) != 0) {

        DartX.todo();
//        if (shouldIgnoreDelta(e.getDelta().getCompilationUnitAST(), delta))
//          return;

//        fUpdatingCount++;
        try {
          update(createContext(false));
        } finally {
//          fUpdatingCount--;
        }
      }
    }

    private DartElementDelta findElement(DartElement target, DartElementDelta delta) {

      if (delta == null || target == null) {
        return null;
      }

      DartElement element = delta.getElement();

      if (target.equals(element)) {
        return delta;
      }

      DartElementDelta[] children = delta.getAffectedChildren();

      for (int i = 0; i < children.length; i++) {
        DartElementDelta d = findElement(target, children[i]);
        if (d != null) {
          return d;
        }
      }

      return null;
    }

    /**
     * Ignore the delta if there are errors on the caret line.
     * <p>
     * We don't ignore the delta if an import is added and the caret isn't inside the import
     * container.
     * </p>
     * 
     * @param ast the compilation unit AST
     * @param delta the JavaScript element delta for the given AST element
     * @return <code>true</code> if the delta should be ignored
     */
    @SuppressWarnings("unused")
    private boolean shouldIgnoreDelta(CompilationUnit ast, DartElementDelta delta) {
      if (ast == null) {
        return false; // can't compute
      }

      IDocument document = getDocument();
      if (document == null) {
        return false; // can't compute
      }

      DartEditor editor = fEditor;
      if (editor == null || editor.getCachedSelectedRange() == null) {
        return false; // can't compute
      }

//      try {
//        if (delta.getAffectedChildren().length == 1
//            && delta.getAffectedChildren()[0].getElement() instanceof DartImportContainer) {
//          DartElement elem = SelectionConverter.getElementAtOffset(
//              ast.getJavaElement(),
//              new TextSelection(editor.getCachedSelectedRange().x,
//                  editor.getCachedSelectedRange().y));
//          if (!(elem instanceof IImportDeclaration))
//            return false;
//
//        }
//      } catch (DartModelException e) {
//        return false; // can't compute
//      }

      int caretLine = 0;
      try {
        caretLine = document.getLineOfOffset(editor.getCachedSelectedRange().x) + 1;
      } catch (BadLocationException x) {
        return false; // can't compute
      }

      DartX.todo();
//      if (caretLine > 0 && ast != null) {
//        Problem[] problems = ast.getProblems();
//        for (int i = 0; i < problems.length; i++) {
//          if (problems[i].isError()
//              && caretLine == problems[i].getSourceLineNumber())
//            return true;
//        }
//      }

      return false;
    }
  }

  /**
   * Filter for annotations.
   */
  private static interface Filter {
    boolean match(DartProjectionAnnotation annotation);
  }

  /**
   * Matches members.
   */
  private static final class MemberFilter implements Filter {
    @Override
    public boolean match(DartProjectionAnnotation annotation) {
      if (!annotation.isComment() && !annotation.isMarkedDeleted()) {
        DartElement element = annotation.getElement();
        if (element instanceof TypeMember) {
          if (element.getElementType() != DartElement.TYPE
              || ((TypeMember) element).getDeclaringType() != null) {
            return true;
          }
        }
      }
      return false;
    }
  }

  /**
   * Internal projection listener.
   */
  private final class ProjectionListener implements IProjectionListener {
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

    /*
     * @see org.eclipse.jface.text.source.projection.IProjectionListener# projectionDisabled()
     */
    @Override
    public void projectionDisabled() {
      handleProjectionDisabled();
    }

    /*
     * @see org.eclipse.jface.text.source.projection.IProjectionListener# projectionEnabled()
     */
    @Override
    public void projectionEnabled() {
      handleProjectionEnabled();
    }
  }

  private static class TokenStream {
    com.google.dart.engine.scanner.StringScanner scanner;
    com.google.dart.engine.scanner.Token firstToken;
    com.google.dart.engine.scanner.Token currentToken;
    int begin;

    TokenStream(String source) {
      scanner = new com.google.dart.engine.scanner.StringScanner(null, source, null);
      firstToken = scanner.tokenize();
      currentToken = firstToken;
      begin = 0;
    }

    void begin(int start) {
      if (start == begin) {
        return;
      }
      if (start < begin) {
        begin = 0;
        currentToken = firstToken;
      }
      while (begin < start) {
        currentToken = currentToken.getNext();
        begin = currentToken.getOffset();
      }
    }

    com.google.dart.engine.scanner.Token next() {
      com.google.dart.engine.scanner.Token next = currentToken;
      currentToken = currentToken.getNext();
      return next;
    }
  }

  private static final class Tuple {
    DartProjectionAnnotation annotation;
    Position position;

    Tuple(DartProjectionAnnotation annotation, Position position) {
      this.annotation = annotation;
      this.position = position;
    }
  }

  static boolean isAvailable(SourceRange range) {
    return range != null && range.getOffset() != -1;
  }

  /* context and listeners */
  private DartEditor fEditor;
  private ProjectionListener fProjectionListener;
  private DartElement fInput;

  private ElementChangedListener fElementListener;
  /* preferences */
  private boolean fCollapseJavadoc = false;
  private boolean fCollapseImportContainer = true;
  private boolean fCollapseInnerTypes = true;
  private boolean fCollapseMembers = false;

  private boolean fCollapseHeaderComments = true;
  /* filters */
  /** Member filter, matches nested members (but not top-level types). */
  private final Filter fMemberFilter = new MemberFilter();

  /** Comment filter, matches comments. */
  private final Filter fCommentFilter = new CommentFilter();

  /**
   * Reusable scanner.
   */
//  private DartScanner fSharedScanner = ToolFactory.createScanner(true, false,
//      false, false);

//  private volatile int fUpdatingCount = 0;

  /**
   * Creates a new folding provider. It must be {@link #install(ITextEditor, ProjectionViewer)
   * installed} on an editor/viewer pair before it can be used, and {@link #uninstall() uninstalled}
   * when not used any longer.
   * <p>
   * The projection state may be reset by calling {@link #initialize()}.
   * </p>
   */
  public DefaultDartFoldingStructureProvider() {
  }

  /*
   * @see IJavaFoldingStructureProviderExtension#collapseComments()
   */
  @Override
  public final void collapseComments() {
    modifyFiltered(fCommentFilter, false);
  }

  /*
   * @see com.google.dart.tools.ui.text.folding.IJavaFoldingStructureProviderExtension
   * #collapseElements(org.eclipse.wst.jsdt.core.DartElement[])
   */
  @Override
  public final void collapseElements(DartElement[] elements) {
    Set set = new HashSet(Arrays.asList(elements));
    modifyFiltered(new DartElementSetFilter(set, false), false);
  }

  /*
   * @see IJavaFoldingStructureProviderExtension#collapseMembers()
   */
  @Override
  public final void collapseMembers() {
    modifyFiltered(fMemberFilter, false);
  }

  /*
   * @see com.google.dart.tools.ui.text.folding.IJavaFoldingStructureProviderExtension
   * #expandElements(org.eclipse.wst.jsdt.core.DartElement[])
   */
  @Override
  public final void expandElements(DartElement[] elements) {
    Set set = new HashSet(Arrays.asList(elements));
    modifyFiltered(new DartElementSetFilter(set, true), true);
  }

  /*
   * @see com.google.dart.tools.ui.text.folding.IJavaFoldingStructureProvider#initialize ()
   */
  @Override
  public final void initialize() {
//    fUpdatingCount++;
    try {
      update(createInitialContext());
    } finally {
//      fUpdatingCount--;
    }
  }

  /**
   * {@inheritDoc}
   * <p>
   * Subclasses may extend.
   * </p>
   * 
   * @param editor {@inheritDoc}
   * @param viewer {@inheritDoc}
   */
  @Override
  public void install(ITextEditor editor, ProjectionViewer viewer) {
    Assert.isLegal(editor != null);
    Assert.isLegal(viewer != null);

    internalUninstall();

    if (editor instanceof DartEditor) {
      fProjectionListener = new ProjectionListener(viewer);
      fEditor = (DartEditor) editor;
    }
  }

  /**
   * {@inheritDoc}
   * <p>
   * Subclasses may extend.
   * </p>
   */
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
  protected final IRegion alignRegion(IRegion region, FoldingStructureComputationContext ctx) {
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
        endOffset = document.getLineOffset(end + 1);
      } else {
        endOffset = document.getLineOffset(end) + document.getLineLength(end);
      }

      return new Region(offset, endOffset - offset);

    } catch (BadLocationException x) {
      // concurrent modification
      return null;
    }
  }

  /**
   * Computes the folding structure for a given {@link DartElement Dart element}. Computed
   * projection annotations are
   * {@link DefaultDartFoldingStructureProvider.FoldingStructureComputationContext#addProjectionRange(DefaultDartFoldingStructureProvider.DartProjectionAnnotation, Position)
   * added} to the computation context.
   * <p>
   * Subclasses may extend or replace. This implementation creates projection annotations for the
   * following elements:
   * <ul>
   * <li>top-level functions, fields, and typedefs
   * <li>members of types (not for top-level types)</li>
   * </ul>
   * </p>
   * 
   * @param element the Dart element to compute the folding structure for
   * @param ctx the computation context
   */
  protected void computeFoldingStructure(DartElement element, FoldingStructureComputationContext ctx) {

    boolean collapse = false;
    boolean collapseCode = true;
    switch (element.getElementType()) {

      case DartElement.IMPORT_CONTAINER:
        collapse = ctx.collapseImportContainer();
        break;
      case DartElement.TYPE:
        collapseCode = isInnerType((Type) element);
        collapse = ctx.collapseInnerTypes() && collapseCode;
        break;
      case DartElement.METHOD:
      case DartElement.FIELD:
      case DartElement.FUNCTION:
      case DartElement.FUNCTION_TYPE_ALIAS:
        collapse = ctx.collapseMembers();
        break;
      default:
        return;
    }

    IRegion[] regions = computeProjectionRanges((SourceReference) element, ctx);
    if (regions.length > 0) {
      // comments
      for (int i = 0; i < regions.length - 1; i++) {
        IRegion normalized = alignRegion(regions[i], ctx);
        if (normalized != null) {
          Position position = createCommentPosition(normalized);
          if (position != null) {
            boolean commentCollapse;
            if (i == 0 && (regions.length > 2 || ctx.hasHeaderComment())
                && element == ctx.getFirstType()) {
              commentCollapse = ctx.collapseHeaderComments();
            } else {
              commentCollapse = ctx.collapseJavadoc();
            }
            ctx.addProjectionRange(
                new DartProjectionAnnotation(commentCollapse, element, true),
                position);
          }
        }
      }
      // code
      if (collapseCode) {
        IRegion normalized = alignRegion(regions[regions.length - 1], ctx);
        if (normalized != null) {
          Position position = element instanceof TypeMember ? createMemberPosition(
              normalized,
              (TypeMember) element) : createCommentPosition(normalized);
          if (position != null) {
            ctx.addProjectionRange(new DartProjectionAnnotation(collapse, element, false), position);
          }
        }
      }
    }
  }

  /**
   * Computes the projection ranges for a given <code>SourceReference</code>. More than one range or
   * none at all may be returned. If there are no foldable regions, an empty array is returned.
   * <p>
   * The last region in the returned array (if not empty) describes the region for the java element
   * that implements the source reference. Any preceding regions describe javadoc comments of that
   * JavaScript element.
   * </p>
   * 
   * @param reference a JavaScript element that is a source reference
   * @param ctx the folding context
   * @return the regions to be folded
   */
  protected final IRegion[] computeProjectionRanges(SourceReference reference,
      FoldingStructureComputationContext ctx) {
    try {
      SourceRange range = reference.getSourceRange();
      if (!isAvailable(range)) {
        return new IRegion[0];
      }

      String contents = reference.getSource();
      if (contents == null) {
        return new IRegion[0];
      }

      List regions = new ArrayList();
      if (!ctx.hasFirstType() && reference instanceof Type) {
        ctx.setFirstType((Type) reference);
        IRegion headerComment = computeHeaderComment(ctx);
        if (headerComment != null) {
          regions.add(headerComment);
          ctx.setHasHeaderComment();
        }
      }

      final int shift = range.getOffset();
      TokenStream scanner = ctx.getScanner(shift);

      int start = shift;
      com.google.dart.engine.scanner.Token token = scanner.next();
      start = token.getOffset();
      com.google.dart.engine.scanner.Token comment = token.getPrecedingComments();
      while (comment != null) {
        int s = token.getOffset();
        int l = token.getLength();
        regions.add(new Region(s, l));
        comment = comment.getNext();
        if (comment == token) {
          comment = null;
        }
      }

      regions.add(new Region(start, shift + range.getLength() - start - 1));

      IRegion[] result = new IRegion[regions.size()];
      regions.toArray(result);
      return result;
    } catch (DartModelException e) {
    }

    return new IRegion[0];
  }

  /**
   * Creates a comment folding position from an
   * {@link #alignRegion(IRegion, DefaultDartFoldingStructureProvider.FoldingStructureComputationContext)
   * aligned} region.
   * 
   * @param aligned an aligned region
   * @return a folding position corresponding to <code>aligned</code>
   */
  protected final Position createCommentPosition(IRegion aligned) {
    return new CommentPosition(aligned.getOffset(), aligned.getLength());
  }

  /**
   * Creates a folding position that remembers its member from an
   * {@link #alignRegion(IRegion, DefaultDartFoldingStructureProvider.FoldingStructureComputationContext)
   * aligned} region.
   * 
   * @param aligned an aligned region
   * @param member the member to remember
   * @return a folding position corresponding to <code>aligned</code>
   */
  protected final Position createMemberPosition(IRegion aligned, TypeMember member) {
    return new DartElementPosition(aligned.getOffset(), aligned.getLength(), member);
  }

  /**
   * Called whenever projection is disabled, for example when the provider is {@link #uninstall()
   * uninstalled}, when the viewer issues a {@link IProjectionListener#projectionDisabled()
   * projectionDisabled} message and before {@link #handleProjectionEnabled() enabling} the
   * provider. Implementations must be prepared to handle multiple calls to this method even if the
   * provider is already disabled.
   * <p>
   * Subclasses may extend.
   * </p>
   */
  protected void handleProjectionDisabled() {
    if (fElementListener != null) {
      DartCore.removeElementChangedListener(fElementListener);
      fElementListener = null;
    }
  }

  /**
   * Called whenever projection is enabled, for example when the viewer issues a
   * {@link IProjectionListener#projectionEnabled() projectionEnabled} message. When the provider is
   * already enabled when this method is called, it is first {@link #handleProjectionDisabled()
   * disabled}.
   * <p>
   * Subclasses may extend.
   * </p>
   */
  protected void handleProjectionEnabled() {
    // http://home.ott.oti.com/teams/wswb/anon/out/vms/index.html
    // projectionEnabled messages are not always paired with projectionDisabled
    // i.e. multiple enabled messages may be sent out.
    // we have to make sure that we disable first when getting an enable
    // message.
    handleProjectionDisabled();

    if (isInstalled()) {
      initialize();
      fElementListener = new ElementChangedListener();
      try {
        DartCore.addElementChangedListener(fElementListener);
      } catch (Exception ex) {
        DartToolsPlugin.log(ex);
      }
    }
  }

  /**
   * Returns <code>true</code> if the provider is installed, <code>false</code> otherwise.
   * 
   * @return <code>true</code> if the provider is installed, <code>false</code> otherwise
   */
  protected final boolean isInstalled() {
    return fEditor != null;
  }

  private Map computeCurrentStructure(FoldingStructureComputationContext ctx) {
    Map map = new HashMap();
    ProjectionAnnotationModel model = ctx.getModel();
    Iterator e = model.getAnnotationIterator();
    while (e.hasNext()) {
      Object annotation = e.next();
      if (annotation instanceof DartProjectionAnnotation) {
        DartProjectionAnnotation java = (DartProjectionAnnotation) annotation;
        Position position = model.getPosition(java);
        Assert.isNotNull(position);
        List list = (List) map.get(java.getElement());
        if (list == null) {
          list = new ArrayList(2);
          map.put(java.getElement(), list);
        }
        list.add(new Tuple(java, position));
      }
    }

    Comparator comparator = new Comparator() {
      @Override
      public int compare(Object o1, Object o2) {
        return ((Tuple) o1).position.getOffset() - ((Tuple) o2).position.getOffset();
      }
    };
    for (Iterator it = map.values().iterator(); it.hasNext();) {
      List list = (List) it.next();
      Collections.sort(list, comparator);
    }
    return map;
  }

  private void computeFoldingStructure(DartElement[] elements,
      FoldingStructureComputationContext ctx) throws DartModelException {
    for (int i = 0; i < elements.length; i++) {
      DartElement element = elements[i];

      computeFoldingStructure(element, ctx);

      if (element instanceof ParentElement) {
        ParentElement parent = (ParentElement) element;
        computeFoldingStructure(parent.getChildren(), ctx);
      }
    }
  }

  private void computeFoldingStructure(FoldingStructureComputationContext ctx) {
    ParentElement parent = (ParentElement) fInput;
    try {
      if (!(fInput instanceof SourceReference)) {
        return;
      }
      String source = ((SourceReference) fInput).getSource();
      if (source == null) {
        return;
      }

      ctx.setScannerSource(source);
      computeFoldingStructure(parent.getChildren(), ctx);
    } catch (DartModelException x) {
    }
  }

  private IRegion computeHeaderComment(FoldingStructureComputationContext ctx)
      throws DartModelException {
    // search at most up to the first type
    SourceRange range = ctx.getFirstType().getSourceRange();
    if (range == null) {
      return null;
    }

    TokenStream scanner = ctx.getScanner();

    int headerStart = -1;
    int headerEnd = -1;
    boolean foundComment = false;
    com.google.dart.engine.scanner.Token terminal = scanner.next();
    com.google.dart.engine.scanner.Token comment = terminal.getPrecedingComments();
    while (comment != null) {
      if (!foundComment) {
        headerStart = comment.getOffset();
      }
      headerEnd = comment.getEnd();
      foundComment = true;
      comment = comment.getNext();
      if (comment == terminal) {
        comment = null;
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
   * <code>JavaProjectionAnnotation</code> instances in <code>annotations</code> can be found in the
   * passed <code>positionMap</code> or <code>fCachedModel</code> if <code>positionMap</code> is
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
   * @param annotations collection of <code>JavaProjectionAnnotation</code>
   * @param positionMap a <code>Map&lt;Annotation, Position&gt;</code> or <code>null</code>
   * @param ctx the context
   * @return a matching tuple or <code>null</code> for no match
   */
  private Tuple findMatch(Tuple tuple, Collection annotations, Map positionMap,
      FoldingStructureComputationContext ctx) {
    Iterator it = annotations.iterator();
    while (it.hasNext()) {
      DartProjectionAnnotation annotation = (DartProjectionAnnotation) it.next();
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
    DartEditor editor = fEditor;
    if (editor == null) {
      return null;
    }

    IDocumentProvider provider = editor.getDocumentProvider();
    if (provider == null) {
      return null;
    }

    return provider.getDocument(editor.getEditorInput());
  }

  private DartElement getInputElement() {
    if (fEditor == null) {
      return null;
    }
    return EditorUtility.getEditorInputDartElement(fEditor, false);
  }

  private ProjectionAnnotationModel getModel() {
    return (ProjectionAnnotationModel) fEditor.getAdapter(ProjectionAnnotationModel.class);
  }

  private void initializePreferences() {
    IPreferenceStore store = DartToolsPlugin.getDefault().getPreferenceStore();
    fCollapseInnerTypes = store.getBoolean(PreferenceConstants.EDITOR_FOLDING_INNERTYPES);
    fCollapseImportContainer = store.getBoolean(PreferenceConstants.EDITOR_FOLDING_IMPORTS);
    fCollapseJavadoc = store.getBoolean(PreferenceConstants.EDITOR_FOLDING_JAVADOC);
    fCollapseMembers = store.getBoolean(PreferenceConstants.EDITOR_FOLDING_METHODS);
    fCollapseHeaderComments = store.getBoolean(PreferenceConstants.EDITOR_FOLDING_HEADERS);
  }

  /**
   * Internal implementation of {@link #uninstall()}.
   */
  private void internalUninstall() {
    if (isInstalled()) {
      handleProjectionDisabled();
      fProjectionListener.dispose();
      fProjectionListener = null;
      fEditor = null;
    }
  }

  /**
   * Returns <code>true</code> if <code>type</code> is not a top-level type, <code>false</code> if
   * it is.
   * 
   * @param type the type to test
   * @return <code>true</code> if <code>type</code> is an inner type
   */
  private boolean isInnerType(Type type) {
//    return type.getDeclaringType() != null;
    return false;
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
  private void match(List deletions, Map additions, List changes,
      FoldingStructureComputationContext ctx) {
    if (deletions.isEmpty() || (additions.isEmpty() && changes.isEmpty())) {
      return;
    }

    List newDeletions = new ArrayList();
    List newChanges = new ArrayList();

    Iterator deletionIterator = deletions.iterator();
    while (deletionIterator.hasNext()) {
      DartProjectionAnnotation deleted = (DartProjectionAnnotation) deletionIterator.next();
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
        DartElement element = match.annotation.getElement();
        deleted.setElement(element);
        deletedPosition.setLength(match.position.getLength());
        if (deletedPosition instanceof DartElementPosition && element instanceof TypeMember) {
          DartElementPosition jep = (DartElementPosition) deletedPosition;
          jep.setMember((TypeMember) element);
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

    List modified = new ArrayList();
    Iterator iter = model.getAnnotationIterator();
    while (iter.hasNext()) {
      Object annotation = iter.next();
      if (annotation instanceof DartProjectionAnnotation) {
        DartProjectionAnnotation java = (DartProjectionAnnotation) annotation;

        if (expand == java.isCollapsed() && filter.match(java)) {
          if (expand) {
            java.markExpanded();
          } else {
            java.markCollapsed();
          }
          modified.add(java);
        }

      }
    }

    model.modifyAnnotations(
        null,
        null,
        (Annotation[]) modified.toArray(new Annotation[modified.size()]));
  }

  private void update(FoldingStructureComputationContext ctx) {
    if (ctx == null) {
      return;
    }
    if (!DartCoreDebug.ENABLE_FOLDING) {
      return;
    }
    Map additions = new HashMap();
    List deletions = new ArrayList();
    List updates = new ArrayList();

    computeFoldingStructure(ctx);
    Map newStructure = ctx.fMap;
    Map oldStructure = computeCurrentStructure(ctx);

    Iterator e = newStructure.keySet().iterator();
    while (e.hasNext()) {
      DartProjectionAnnotation newAnnotation = (DartProjectionAnnotation) e.next();
      Position newPosition = (Position) newStructure.get(newAnnotation);

      DartElement element = newAnnotation.getElement();
      /*
       * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=130472 and
       * https://bugs.eclipse.org/bugs/show_bug.cgi?id=127445 In the presence of syntax errors,
       * anonymous types may have a source range offset of 0. When such a situation is encountered,
       * we ignore the proposed folding range: if no corresponding folding range exists, it is
       * silently ignored; if there *is* a matching folding range, we ignore the position update and
       * keep the old range, in order to keep the folding structure stable.
       */
      boolean isMalformedAnonymousType = newPosition.getOffset() == 0
          && element.getElementType() == DartElement.TYPE && isInnerType((Type) element);
      List annotations = (List) oldStructure.get(element);
      if (annotations == null) {
        if (!isMalformedAnonymousType) {
          additions.put(newAnnotation, newPosition);
        }
      } else {
        Iterator x = annotations.iterator();
        boolean matched = false;
        while (x.hasNext()) {
          Tuple tuple = (Tuple) x.next();
          DartProjectionAnnotation existingAnnotation = tuple.annotation;
          Position existingPosition = tuple.position;
          if (newAnnotation.isComment() == existingAnnotation.isComment()) {
            boolean updateCollapsedState = ctx.allowCollapsing()
                && existingAnnotation.isCollapsed() != newAnnotation.isCollapsed();
            if (!isMalformedAnonymousType && existingPosition != null
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

    e = oldStructure.values().iterator();
    while (e.hasNext()) {
      List list = (List) e.next();
      int size = list.size();
      for (int i = 0; i < size; i++) {
        deletions.add(((Tuple) list.get(i)).annotation);
      }
    }

    match(deletions, additions, updates, ctx);

    Annotation[] deletedArray = (Annotation[]) deletions.toArray(new Annotation[deletions.size()]);
    Annotation[] changedArray = (Annotation[]) updates.toArray(new Annotation[updates.size()]);
    ctx.getModel().modifyAnnotations(deletedArray, additions, changedArray);
    ctx.setScannerSource(""); // clear token stream
  }
}
