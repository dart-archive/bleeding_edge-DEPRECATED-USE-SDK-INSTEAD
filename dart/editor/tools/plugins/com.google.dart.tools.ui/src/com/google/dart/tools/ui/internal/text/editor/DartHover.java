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

package com.google.dart.tools.ui.internal.text.editor;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.visitor.ElementLocator;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.services.util.DartDocUtilities;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.utilities.general.StringUtilities;
import com.google.dart.engine.utilities.source.SourceRange;
import com.google.dart.server.GetHoverConsumer;
import com.google.dart.server.generated.types.HoverInformation;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.ui.internal.actions.NewSelectionConverter;
import com.google.dart.tools.ui.internal.problemsview.ProblemsView;
import com.google.dart.tools.ui.internal.util.GridDataFactory;
import com.google.dart.tools.ui.internal.util.GridLayoutFactory;
import com.google.dart.tools.ui.text.DartSourceViewerConfiguration;

import org.apache.commons.lang3.text.WordUtils;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.AbstractInformationControl;
import org.eclipse.jface.text.AbstractReusableInformationControlCreator;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IInformationControlExtension2;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextHoverExtension2;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.ISourceViewerExtension2;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.MarkerAnnotation;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class DartHover implements ITextHover, ITextHoverExtension, ITextHoverExtension2 {

  private static class AnnotationsSection {
    private final FormToolkit toolkit;
    private final Section section;
    private final Composite container;

    public AnnotationsSection(Composite parent, String title) {
      toolkit = createToolkit(parent.getDisplay());
      this.section = toolkit.createSection(parent, Section.TITLE_BAR);
      GridDataFactory.create(section).grabHorizontal().fill();
      section.setText(title);
      container = toolkit.createComposite(section);
      GridLayoutFactory.create(container).columns(2).spacingHorizontal(0);
      section.setClient(container);
    }

    public void setAnnotations(List<Annotation> annotations) {
      for (Control child : container.getChildren()) {
        child.dispose();
      }
      annotations = getSortedAnnotations(annotations);
      for (Annotation annotation : annotations) {
        Label imageLabel = new Label(container, SWT.NONE);
        if (annotation instanceof MarkerAnnotation) {
          IMarker marker = ((MarkerAnnotation) annotation).getMarker();
          imageLabel.setImage(ProblemsView.LABEL_PROVIDER.getImage(marker));
        }
        toolkit.createLabel(container, annotation.getText());
      }
    }
  }

  private static class DartInformationControl extends AbstractInformationControl implements
      IInformationControlExtension2 {
    private static final Point SIZE_CONSTRAINTS = new Point(10000, 10000);

    private static boolean isGridVisible(AnnotationsSection section) {
      return section.section.getVisible();
    }

    private static boolean isGridVisible(DocSection section) {
      return section.section.getVisible();
    }

    private static boolean isGridVisible(TextSection section) {
      return section.section.getVisible();
    }

    private static void setGridVisible(AnnotationsSection section, boolean visible) {
      setGridVisible(section.section, visible);
    }

    private static void setGridVisible(Control control, boolean visible) {
      GridDataFactory.modify(control).exclude(!visible);
      control.setVisible(visible);
      control.getParent().layout();
    }

    private static void setGridVisible(DocSection section, boolean visible) {
      setGridVisible(section.section, visible);
    }

    private static void setGridVisible(TextSection section, boolean visible) {
      setGridVisible(section.section, visible);
    }

    private boolean hasContents;

    private Composite container;
    private TextSection elementSection;
    private TextSection librarySection;
    private AnnotationsSection problemsSection;
    private DocSection docSection;
    private TextSection staticTypeSection;
    private TextSection propagatedTypeSection;
    private TextSection parameterSection;

    public DartInformationControl(Shell parentShell) {
      super(parentShell, false);
      toolkit = createToolkit(parentShell.getDisplay());
      create();
    }

    @Override
    public Point computeSizeConstraints(int widthInChars, int heightInChars) {
      return SIZE_CONSTRAINTS;
    }

    @Override
    public Point computeSizeHint() {
      // Shell was already packed and has the required size.
      return getShell().getSize();
    }

    @Override
    public IInformationControlCreator getInformationPresenterControlCreator() {
      return new DartInformationControlCreator();
    }

    @Override
    public boolean hasContents() {
      return hasContents;
    }

    @Override
    public void setInput(Object input) {
      hasContents = false;
      // Hide all sections.
      setGridVisible(elementSection, false);
      setGridVisible(librarySection, false);
      setGridVisible(problemsSection, false);
      setGridVisible(docSection, false);
      setGridVisible(staticTypeSection, false);
      setGridVisible(propagatedTypeSection, false);
      setGridVisible(parameterSection, false);
      if (input instanceof HoverInfo_NEW) {
        //
        // Display hover based on Analysis Server response
        //
        HoverInformation hover = ((HoverInfo_NEW) input).hover;
        if (hover != null) {
          // Element
          if (hover.getElementKind() != null) {
            // show Element
            {
              String description = hover.getElementDescription();
              if (description != null) {
                String text = WordUtils.wrap(description, 100);
                setGridVisible(elementSection, true);
                elementSection.setTitle(WordUtils.capitalize(hover.getElementKind()));
                elementSection.setText(text);
              }
            }
            // show Library
            {
              String unitName = hover.getContainingLibraryPath();
              String libraryName = hover.getContainingLibraryName();
              if (unitName != null && libraryName != null) {
                String text = StringUtilities.abbreviateLeft(libraryName, 25) + " | "
                    + StringUtilities.abbreviateLeft(unitName, 35);
                setGridVisible(librarySection, true);
                librarySection.setText(text);
              }
            }
            // Dart Doc
            {
              String dartDoc = hover.getDartdoc();
              if (dartDoc != null) {
                setGridVisible(docSection, true);
                docSection.setDoc(dartDoc);
              }
            }
          }
          // parameter
          {
            String parameter = hover.getParameter();
            if (parameter != null) {
              setGridVisible(parameterSection, true);
              parameterSection.setText(parameter);
            }
          }
          // static type
          {
            String staticType = hover.getStaticType();
            if (staticType != null) {
              setGridVisible(staticTypeSection, true);
              staticTypeSection.setText(staticType);
            }
          }
          // propagated type
          {
            String propagatedType = hover.getPropagatedType();
            if (propagatedType != null) {
              setGridVisible(propagatedTypeSection, true);
              propagatedTypeSection.setText(propagatedType);
            }
          }
        }
        // Annotations.
        {
          List<Annotation> annotations = ((HoverInfo_NEW) input).annotations;
          int size = annotations.size();
          if (size != 0) {
            setGridVisible(problemsSection, true);
            problemsSection.setAnnotations(annotations);
          }
        }
      } else if (input instanceof HoverInfo_OLD) {
        //
        // Display hover based upon java base Analysis Engine information
        //
        HoverInfo_OLD hoverInfo = (HoverInfo_OLD) input;
        AstNode node = hoverInfo.node;
        Element element = hoverInfo.element;
        // Element
        if (element != null) {
          // show variable, if synthetic accessor
          if (element instanceof PropertyAccessorElement) {
            PropertyAccessorElement accessor = (PropertyAccessorElement) element;
            if (accessor.isSynthetic()) {
              element = accessor.getVariable();
            }
          }
          // show Element
          {
            String text = element.toString();
            text = WordUtils.wrap(text, 100);
            setGridVisible(elementSection, true);
            elementSection.setTitle(WordUtils.capitalize(element.getKind().getDisplayName()));
            elementSection.setText(text);
          }
          // show Library
          {
            LibraryElement library = element.getLibrary();
            CompilationUnitElement unit = element.getAncestor(CompilationUnitElement.class);
            if (library != null && unit != null) {
              String unitName = unit.getSource().getFullName();
              String libraryName = library.getDisplayName();
              String text = StringUtilities.abbreviateLeft(libraryName, 25) + " | "
                  + StringUtilities.abbreviateLeft(unitName, 35);
              setGridVisible(librarySection, true);
              librarySection.setText(text);
            }
          }
          // Dart Doc
          try {
            String dartDoc = element.computeDocumentationComment();
            if (dartDoc != null) {
              dartDoc = DartDocUtilities.cleanDartDoc(dartDoc);
              setGridVisible(docSection, true);
              docSection.setDoc(dartDoc);
            }
          } catch (Throwable e) {
          }
        }
        // types
        if (node instanceof Expression) {
          Expression expression = (Expression) node;
          // parameter
          {
            AstNode n = expression;
            while (n != null) {
              if (n instanceof Expression) {
                ParameterElement parameterElement = ((Expression) n).getBestParameterElement();
                if (parameterElement != null) {
                  setGridVisible(parameterSection, true);
                  parameterSection.setText(DartDocUtilities.getTextSummary(null, parameterElement));
                  break;
                }
              }
              n = n.getParent();
            }
          }
          // static type
          Type staticType = expression.getStaticType();
          if (staticType != null && element == null) {
            setGridVisible(staticTypeSection, true);
            staticTypeSection.setText(staticType.getDisplayName());
          }
          // propagated type
          if (!(element instanceof ExecutableElement)) {
            Type propagatedType = expression.getPropagatedType();
            if (propagatedType != null && !propagatedType.equals(staticType)) {
              setGridVisible(propagatedTypeSection, true);
              propagatedTypeSection.setText(propagatedType.getDisplayName());
            }
          }
        }
        // Annotations.
        {
          List<Annotation> annotations = hoverInfo.annotations;
          int size = annotations.size();
          if (size != 0) {
            setGridVisible(problemsSection, true);
            problemsSection.setAnnotations(annotations);
          }
        }
      } else {
        return;
      }
      // update 'hasContents' flag
      hasContents |= isGridVisible(elementSection);
      hasContents |= isGridVisible(librarySection);
      hasContents |= isGridVisible(problemsSection);
      hasContents |= isGridVisible(docSection);
      hasContents |= isGridVisible(staticTypeSection);
      hasContents |= isGridVisible(propagatedTypeSection);
      hasContents |= isGridVisible(parameterSection);
      // Layout and pack.
      Shell shell = getShell();
      shell.layout(true, true);
      shell.pack();
      shell.layout(true, true);
      shell.pack();
    }

    @Override
    protected void createContent(Composite parent) {
      container = toolkit.createComposite(parent);
      GridLayoutFactory.create(container);
      elementSection = new TextSection(container, "Element");
      librarySection = new TextSection(container, "Library");
      problemsSection = new AnnotationsSection(container, "Problems");
      docSection = new DocSection(container, "Documentation");
      staticTypeSection = new TextSection(container, "Static type");
      propagatedTypeSection = new TextSection(container, "Propagated type");
      parameterSection = new TextSection(container, "Parameter");
    }
  }

  private static class DartInformationControlCreator extends
      AbstractReusableInformationControlCreator {
    @Override
    protected IInformationControl doCreateInformationControl(Shell parent) {
      return new DartInformationControl(parent);
    }
  }

  private static class DocSection {
    private final FormToolkit toolkit;
    private final Section section;
    private final StyledText textWidget;

    public DocSection(Composite parent, String title) {
      toolkit = createToolkit(parent.getDisplay());
      this.section = toolkit.createSection(parent, Section.TITLE_BAR);
      GridDataFactory.create(section).grab().fill();
      section.setText(title);
      // create Composite to draw flat border
      Composite body = toolkit.createComposite(section);
      GridLayoutFactory.create(body).margins(2);
      section.setClient(body);
      // create StyledText widget
      textWidget = new StyledText(body, SWT.H_SCROLL | SWT.V_SCROLL);
      textWidget.setMargins(5, 5, 5, 5);
      // We do this to prevent line spacing changing.
      // See https://code.google.com/p/dart/issues/detail?id=15899
      textWidget.setLineSpacing(1);
      // configure flat border
      textWidget.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
      toolkit.paintBordersFor(body);
    }

    public void setDoc(String doc) {
      textWidget.setText(doc);
      textWidget.setSelection(0);
      // apply size
      Point requiredSize = textWidget.computeSize(SWT.DEFAULT, SWT.DEFAULT);
      GridDataFactory gdf = GridDataFactory.create(textWidget);
      int maxWidth = gdf.convertWidthInCharsToPixels(85);
      int maxHeight = gdf.convertHeightInCharsToPixels(15);
      int width = Math.min(requiredSize.x, maxWidth);
      int height = Math.min(requiredSize.y, maxHeight);
      gdf.hint(width, height).grab().fill();
    }
  }

  private static class HoverInfo_NEW {
    private HoverInformation hover;
    private List<Annotation> annotations;

    public HoverInfo_NEW(HoverInformation hover, List<Annotation> annotations) {
      this.hover = hover;
      this.annotations = annotations;
    }
  }

  private static class HoverInfo_OLD {
    AstNode node;
    Element element;
    List<Annotation> annotations;

    public HoverInfo_OLD(AstNode node, Element element, List<Annotation> annotations) {
      this.node = node;
      this.element = element;
      this.annotations = annotations;
    }
  }

  private static class TextSection {
    private final FormToolkit toolkit;
    private final Section section;
    private final StyledText textWidget;

    public TextSection(Composite parent, String title) {
      toolkit = createToolkit(parent.getDisplay());
      this.section = toolkit.createSection(parent, Section.TITLE_BAR);
      GridDataFactory.create(section).grabHorizontal().fill();
      section.setText(title);
      textWidget = new StyledText(section, SWT.MULTI | SWT.READ_ONLY | SWT.WRAP);
      toolkit.adapt(textWidget, false, false);
      section.setClient(textWidget);
    }

    public void setText(String text) {
      textWidget.setText(text);
      textWidget.setSelection(0);
    }

    public void setTitle(String title) {
      section.setText(title);
    }
  }

  private static final List<ITextHover> hoverContributors = Lists.newArrayList();

  private static FormToolkit toolkit;

  /**
   * Register a {@link ITextHover} tooltip contributor.
   */
  public static void addContributer(ITextHover hoverContributor) {
    hoverContributors.add(hoverContributor);
  }

  private static FormToolkit createToolkit(Display display) {
    if (toolkit == null) {
      toolkit = new FormToolkit(display);
    }

    return toolkit;
  }

  /**
   * Sorts given {@link Annotation}s by severity and location.
   */
  private static List<Annotation> getSortedAnnotations(List<Annotation> annotations) {
    annotations = Lists.newArrayList(annotations);
    Collections.sort(annotations, new Comparator<Annotation>() {
      @Override
      public int compare(Annotation o1, Annotation o2) {
        IMarker m1 = getMarker(o1);
        IMarker m2 = getMarker(o2);
        // no marker(s)
        if (m1 != null && m2 == null) {
          return 1;
        }
        if (m1 == null && m2 != null) {
          return -1;
        }
        if (m1 == null && m2 == null) {
          return 0;
        }
        // compare severity
        int val = m2.getAttribute(IMarker.SEVERITY, 0) - m1.getAttribute(IMarker.SEVERITY, 0);
        if (val != 0) {
          return val;
        }
        // compare offset
        return m2.getAttribute(IMarker.CHAR_START, 0) - m1.getAttribute(IMarker.CHAR_START, 0);
      }

      private IMarker getMarker(Annotation annotation) {
        return (annotation instanceof MarkerAnnotation)
            ? ((MarkerAnnotation) annotation).getMarker() : null;
      }
    });
    return annotations;
  }

  private final ISourceViewer viewer;
  private final DartSourceViewerConfiguration viewerConfiguration;
  private CompilationUnitEditor editor;
  private IInformationControlCreator informationControlCreator;

  private ITextHover lastReturnedHover;
  private int lastClickOffset;

  public DartHover(ITextEditor editor, ISourceViewer viewer,
      DartSourceViewerConfiguration viewerConfiguration) {
    this.viewer = viewer;
    this.viewerConfiguration = viewerConfiguration;
    if (editor instanceof CompilationUnitEditor) {
      this.editor = (CompilationUnitEditor) editor;
      this.editor.getViewer().getTextWidget().addMouseListener(new MouseAdapter() {
        @Override
        public void mouseDown(MouseEvent e) {
          SourceRange range = DartHover.this.editor.getTextSelectionRange();
          lastClickOffset = range != null ? range.getOffset() : -1;
        }
      });
    }
  }

  @Override
  public IInformationControlCreator getHoverControlCreator() {
    if (lastReturnedHover instanceof ITextHoverExtension) {
      return ((ITextHoverExtension) lastReturnedHover).getHoverControlCreator();
    }
    if (informationControlCreator == null) {
      informationControlCreator = new DartInformationControlCreator();
    }
    return informationControlCreator;
  }

  @Override
  public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
    lastReturnedHover = null;
    return null;
  }

  @Override
  public Object getHoverInfo2(ITextViewer textViewer, IRegion hoverRegion) {
    lastReturnedHover = null;
    // Check through the contributed hover providers.
    for (ITextHover hoverContributer : hoverContributors) {
      if (hoverContributer instanceof ITextHoverExtension2) {
        Object hoverInfo = ((ITextHoverExtension2) hoverContributer).getHoverInfo2(
            textViewer,
            hoverRegion);
        if (hoverInfo != null) {
          lastReturnedHover = hoverContributer;
          return hoverInfo;
        }
      }
    }
    // Editor based hover.
    if (editor != null) {
      List<Annotation> annotations = getAnnotations(hoverRegion);
      // prepare node
      int offset = hoverRegion.getOffset();
      if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
        String file = editor.getInputFilePath();
        if (file != null) {
          final CountDownLatch latch = new CountDownLatch(1);
          final HoverInformation[] result = new HoverInformation[1];
          DartCore.getAnalysisServer().analysis_getHover(file, offset, new GetHoverConsumer() {
            @Override
            public void computedHovers(HoverInformation[] hovers) {
              if (hovers != null && hovers.length > 0) {
                result[0] = hovers[0];
                latch.countDown();
              }
            }
          });
          // This executes on a background thread that does not hold the workspace lock
          // so block until analysis server responds or time expires.
          // Wait a long time only if there is nothing else to show
          long waitTimeMillis = annotations.isEmpty() ? 4000 : 500;
          Uninterruptibles.awaitUninterruptibly(latch, waitTimeMillis, TimeUnit.MILLISECONDS);
          return new HoverInfo_NEW(result[0], annotations);
        }
      } else {
        AstNode node = NewSelectionConverter.getNodeAtOffset(editor, offset);
        if (node instanceof MethodDeclaration) {
          MethodDeclaration method = (MethodDeclaration) node;
          node = method.getName();
        }
        // show Expression
        if (node instanceof Expression) {
          Element element = ElementLocator.locateWithOffset(node, offset);
          return new HoverInfo_OLD(node, element, annotations);
        }
      }
      // always show annotations, even if no node
      if (!annotations.isEmpty()) {
        return new HoverInfo_OLD(null, null, annotations);
      }
    }
    return null;
  }

  @Override
  public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
    IRegion wordRange = findWord(textViewer.getDocument(), offset);
    // ignore word if it was clicked
    {
      int wordOffset = wordRange.getOffset();
      int wordEnd = wordOffset + wordRange.getLength();
      if (wordOffset <= lastClickOffset && lastClickOffset <= wordEnd) {
        return null;
      }
    }
    // OK
    return wordRange;
  }

  private IRegion findWord(IDocument document, int offset) {
    int start = -2;
    int end = -1;

    try {

      int pos = offset;
      char c;

      while (pos >= 0) {
        c = document.getChar(pos);
        if (!Character.isUnicodeIdentifierPart(c)) {
          break;
        }
        --pos;
      }

      start = pos;

      pos = offset;
      int length = document.getLength();

      while (pos < length) {
        c = document.getChar(pos);
        if (!Character.isUnicodeIdentifierPart(c)) {
          break;
        }
        ++pos;
      }

      end = pos;

    } catch (BadLocationException x) {
    }

    if (start >= -1 && end > -1) {
      if (start == offset && end == offset) {
        return new Region(offset, 0);
      } else if (start == offset) {
        return new Region(start, end - start);
      } else {
        return new Region(start + 1, end - start - 1);
      }
    }

    return null;
  }

  private IAnnotationModel getAnnotationModel() {
    if (viewer instanceof ISourceViewerExtension2) {
      ISourceViewerExtension2 extension = (ISourceViewerExtension2) viewer;
      return extension.getVisualAnnotationModel();
    }
    return viewer.getAnnotationModel();
  }

  private List<Annotation> getAnnotations(IRegion region) {
    List<Annotation> annotations = Lists.newArrayList();
    IAnnotationModel model = getAnnotationModel();
    if (model != null) {
      @SuppressWarnings("unchecked")
      Iterator<Annotation> iter = model.getAnnotationIterator();
      while (iter.hasNext()) {
        Annotation annotation = iter.next();
        if (viewerConfiguration.isShownInText(annotation)) {
          Position p = model.getPosition(annotation);
          if (p != null && p.overlapsWith(region.getOffset(), region.getLength())) {
            String msg = annotation.getText();
            if (msg != null && msg.trim().length() > 0) {
              annotations.add(annotation);
            }
          }
        }
      }
    }
    return annotations;
  }

}
