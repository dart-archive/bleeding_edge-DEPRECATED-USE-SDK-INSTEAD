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
package com.google.dart.tools.ui.internal.text.dart.hover;

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.PreferenceConstants;
import com.google.dart.tools.ui.internal.text.editor.DartSourceViewer;
import com.google.dart.tools.ui.internal.text.functions.SimpleDartSourceViewerConfiguration;
import com.google.dart.tools.ui.internal.util.SWTUtil;
import com.google.dart.tools.ui.text.DartPartitions;
import com.google.dart.tools.ui.text.IDartColorConstants;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlExtension;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.texteditor.AbstractTextEditor;

/**
 * Source viewer based implementation of <code>IInformationControl</code>. Displays information in a
 * source viewer.
 */
public class SourceViewerInformationControl implements IInformationControl,
    IInformationControlExtension, DisposeListener {

  /** Border thickness in pixels. */
  private static final int BORDER = 1;

  /**
   * Returns <code>null</code> if {@link SWT#COLOR_INFO_BACKGROUND} is visibly distinct from the
   * default Java source text color. Otherwise, returns the editor background color.
   * 
   * @param display the display
   * @return an RGB or <code>null</code>
   * @since 3.6.1
   */
  public static RGB getVisibleBackgroundColor(Display display) {
    float[] infoBgHSB = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND).getRGB().getHSB();

    Color javaDefaultColor = DartUI.getColorManager().getColor(IDartColorConstants.JAVA_DEFAULT);
    RGB javaDefaultRGB = javaDefaultColor != null ? javaDefaultColor.getRGB() : new RGB(
        255,
        255,
        255);
    float[] javaDefaultHSB = javaDefaultRGB.getHSB();

    if (Math.abs(infoBgHSB[2] - javaDefaultHSB[2]) < 0.5f) {
      // workaround for dark tooltip background color, see https://bugs.eclipse.org/309334
      IPreferenceStore preferenceStore = DartToolsPlugin.getDefault().getCombinedPreferenceStore();
      boolean useDefault = preferenceStore.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT);
      if (useDefault) {
        return display.getSystemColor(SWT.COLOR_LIST_BACKGROUND).getRGB();
      }
      return PreferenceConverter.getColor(
          preferenceStore,
          AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND);
    }
    return null;
  }

  /** The control's shell */
  private Shell fShell;
  /** The control's text widget */
  private StyledText fText;
  /** The control's source viewer */
  private SourceViewer fViewer;
  /**
   * The optional status field.
   */
  private Label fStatusField;
  /**
   * The separator for the optional status field.
   */
  private Label fSeparator;
  /**
   * The font of the optional status text label.
   */
  private Font fStatusTextFont;
  /**
   * The width size constraint.
   */
  private int fMaxWidth = SWT.DEFAULT;

  /**
   * The height size constraint.
   */
  private int fMaxHeight = SWT.DEFAULT;
  private Color fBackgroundColor;

  private boolean fIsSystemBackgroundColor = true;

  /**
   * Creates a default information control with the given shell as parent. No information presenter
   * is used to process the information to be displayed. No additional styles are applied to the
   * styled text widget.
   * 
   * @param parent the parent shell
   */
  public SourceViewerInformationControl(Shell parent) {
    this(parent, SWT.NONE);
  }

  /**
   * Creates a default information control with the given shell as parent. The given information
   * presenter is used to process the information to be displayed. The given styles are applied to
   * the created styled text widget.
   * 
   * @param parent the parent shell
   * @param style the additional styles for the styled text widget
   */
  public SourceViewerInformationControl(Shell parent, int style) {
    this(parent, SWT.NO_TRIM | SWT.TOOL, style);
  }

  /**
   * Creates a default information control with the given shell as parent. The given information
   * presenter is used to process the information to be displayed. The given styles are applied to
   * the created styled text widget.
   * 
   * @param parent the parent shell
   * @param shellStyle the additional styles for the shell
   * @param style the additional styles for the styled text widget
   */
  public SourceViewerInformationControl(Shell parent, int shellStyle, int style) {
    this(parent, shellStyle, style, null);
  }

  /**
   * Creates a default information control with the given shell as parent. The given information
   * presenter is used to process the information to be displayed. The given styles are applied to
   * the created styled text widget.
   * 
   * @param parent the parent shell
   * @param shellStyle the additional styles for the shell
   * @param style the additional styles for the styled text widget
   * @param statusFieldText the text to be used in the optional status field or <code>null</code> if
   *          the status field should be hidden
   */
  public SourceViewerInformationControl(Shell parent, int shellStyle, int style,
      String statusFieldText) {
    GridLayout layout;
    GridData gd;

    fShell = new Shell(parent, SWT.NO_FOCUS | SWT.ON_TOP | shellStyle);
    Display display = fShell.getDisplay();
    fShell.setBackground(display.getSystemColor(SWT.COLOR_BLACK));

    initializeColors();

    Composite composite = fShell;
    layout = new GridLayout(1, false);
    int border = ((shellStyle & SWT.NO_TRIM) == 0) ? 0 : BORDER;
    layout.marginHeight = border;
    layout.marginWidth = border;
    composite.setLayout(layout);
    gd = new GridData(GridData.FILL_HORIZONTAL);
    composite.setLayoutData(gd);

    if (statusFieldText != null) {
      composite = new Composite(composite, SWT.NONE);
      layout = new GridLayout(1, false);
      layout.marginHeight = 0;
      layout.marginWidth = 0;
      composite.setLayout(layout);
      gd = new GridData(GridData.FILL_BOTH);
      composite.setLayoutData(gd);
      composite.setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
      composite.setBackground(fBackgroundColor);
    }

    // Source viewer
    IPreferenceStore store = DartToolsPlugin.getDefault().getCombinedPreferenceStore();
    fViewer = new DartSourceViewer(composite, null, null, false, style, store);
    fViewer.configure(new SimpleDartSourceViewerConfiguration(
        DartToolsPlugin.getDefault().getDartTextTools().getColorManager(),
        store,
        null,
        DartPartitions.DART_PARTITIONING,
        false));
    fViewer.setEditable(false);

    fText = fViewer.getTextWidget();
    gd = new GridData(GridData.BEGINNING | GridData.FILL_BOTH);
    fText.setLayoutData(gd);
    fText.setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
    fText.setBackground(fBackgroundColor);

    initializeFont();

    fText.addKeyListener(new KeyListener() {

      @Override
      public void keyPressed(KeyEvent e) {
        if (e.character == 0x1B) {
          fShell.dispose();
        }
      }

      @Override
      public void keyReleased(KeyEvent e) {
      }
    });

    // Status field
    if (statusFieldText != null) {

      // Horizontal separator line
      fSeparator = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL | SWT.LINE_DOT);
      fSeparator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

      // Status field label
      fStatusField = new Label(composite, SWT.RIGHT);
      fStatusField.setText(statusFieldText);
      Font font = fStatusField.getFont();
      FontData[] fontDatas = font.getFontData();
      for (int i = 0; i < fontDatas.length; i++) {
        fontDatas[i].setHeight(fontDatas[i].getHeight() * 9 / 10);
      }
      fStatusTextFont = SWTUtil.getFont(fStatusField.getDisplay(), fontDatas);
      fStatusField.setFont(fStatusTextFont);
      GridData gd2 = new GridData(GridData.FILL_VERTICAL | GridData.FILL_HORIZONTAL
          | GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING);
      fStatusField.setLayoutData(gd2);

      // Regarding the color see bug 41128
      fStatusField.setForeground(display.getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));
      fStatusField.setBackground(fBackgroundColor);
    }

    addDisposeListener(this);
  }

  /**
   * Creates a default information control with the given shell as parent. The given information
   * presenter is used to process the information to be displayed. The given styles are applied to
   * the created styled text widget.
   * 
   * @param parent the parent shell
   * @param style the additional styles for the styled text widget
   * @param statusFieldText the text to be used in the optional status field or <code>null</code> if
   *          the status field should be hidden
   */
  public SourceViewerInformationControl(Shell parent, int style, String statusFieldText) {
    this(parent, SWT.NO_TRIM | SWT.TOOL, style, statusFieldText);
  }

  /**
   * Creates a default information control with the given shell as parent. No information presenter
   * is used to process the information to be displayed. No additional styles are applied to the
   * styled text widget.
   * 
   * @param parent the parent shell
   * @param statusFieldText the text to be used in the optional status field or <code>null</code> if
   *          the status field should be hidden
   */
  public SourceViewerInformationControl(Shell parent, String statusFieldText) {
    this(parent, SWT.NONE, statusFieldText);
  }

  /*
   * @see IInformationControl#addDisposeListener(DisposeListener)
   */
  @Override
  public void addDisposeListener(DisposeListener listener) {
    fShell.addDisposeListener(listener);
  }

  /*
   * @see IInformationControl#addFocusListener(FocusListener)
   */
  @Override
  public void addFocusListener(FocusListener listener) {
    fText.addFocusListener(listener);
  }

  /*
   * @see IInformationControl#computeSizeHint()
   */
  @Override
  public Point computeSizeHint() {
    // compute the preferred size
    int x = SWT.DEFAULT;
    int y = SWT.DEFAULT;
    Point size = fShell.computeSize(x, y);
    if (size.x > fMaxWidth) {
      x = fMaxWidth;
    }
    if (size.y > fMaxHeight) {
      y = fMaxHeight;
    }

    // recompute using the constraints if the preferred size is larger than the
// constraints
    if (x != SWT.DEFAULT || y != SWT.DEFAULT) {
      size = fShell.computeSize(x, y, false);
    }

    return size;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void dispose() {
    if (!fIsSystemBackgroundColor) {
      fBackgroundColor.dispose();
    }
    if (fShell != null && !fShell.isDisposed()) {
      fShell.dispose();
    } else {
      widgetDisposed(null);
    }
  }

  /*
   * @see IInformationControlExtension#hasContents()
   */
  @Override
  public boolean hasContents() {
    return fText.getCharCount() > 0;
  }

  /*
   * @see IInformationControl#isFocusControl()
   */
  @Override
  public boolean isFocusControl() {
    return fText.isFocusControl();
  }

  /*
   * @see IInformationControl#removeDisposeListener(DisposeListener)
   */
  @Override
  public void removeDisposeListener(DisposeListener listener) {
    fShell.removeDisposeListener(listener);
  }

  /*
   * @see IInformationControl#removeFocusListener(FocusListener)
   */
  @Override
  public void removeFocusListener(FocusListener listener) {
    fText.removeFocusListener(listener);
  }

  /*
   * @see IInformationControl#setBackgroundColor(Color)
   */
  @Override
  public void setBackgroundColor(Color background) {
    fText.setBackground(background);
  }

  /*
   * @see IInformationControl#setFocus()
   */
  @Override
  public void setFocus() {
    fShell.forceFocus();
    fText.setFocus();
  }

  /*
   * @see IInformationControl#setForegroundColor(Color)
   */
  @Override
  public void setForegroundColor(Color foreground) {
    fText.setForeground(foreground);
  }

  /*
   * @see IInformationControl#setInformation(String)
   */
  @Override
  public void setInformation(String content) {
    if (content == null) {
      fViewer.setInput(null);
      return;
    }

    IDocument doc = new Document(content);
    DartToolsPlugin.getDefault().getDartTextTools().setupDartDocumentPartitioner(
        doc,
        DartPartitions.DART_PARTITIONING);
    fViewer.setInput(doc);
  }

  /*
   * @see org.eclipse.jface.text.IInformationControlExtension2#setInput(java.lang .Object)
   */
  public void setInput(Object input) {
    if (input instanceof String) {
      setInformation((String) input);
    } else {
      setInformation(null);
    }
  }

  /*
   * @see IInformationControl#setLocation(Point)
   */
  @Override
  public void setLocation(Point location) {
    fShell.setLocation(location);
  }

  /*
   * @see IInformationControl#setSize(int, int)
   */
  @Override
  public void setSize(int width, int height) {

    if (fStatusField != null) {
      GridData gd = (GridData) fViewer.getTextWidget().getLayoutData();
      Point statusSize = fStatusField.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
      Point separatorSize = fSeparator.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
      gd.heightHint = height - statusSize.y - separatorSize.y;
    }
    fShell.setSize(width, height);

    if (fStatusField != null) {
      fShell.pack(true);
    }
  }

  /*
   * @see IInformationControl#setSizeConstraints(int, int)
   */
  @Override
  public void setSizeConstraints(int maxWidth, int maxHeight) {
    fMaxWidth = maxWidth;
    fMaxHeight = maxHeight;
  }

  /*
   * @see IInformationControl#setVisible(boolean)
   */
  @Override
  public void setVisible(boolean visible) {
    fShell.setVisible(visible);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void widgetDisposed(DisposeEvent event) {
    if (fStatusTextFont != null && !fStatusTextFont.isDisposed()) {
      fStatusTextFont.dispose();
    }

    fStatusTextFont = null;
    fShell = null;
    fText = null;
  }

  protected ISourceViewer getViewer() {
    return fViewer;
  }

  private RGB getHoverBackgroundColorRGB() {
    IPreferenceStore store = DartToolsPlugin.getDefault().getPreferenceStore();
    return store.getBoolean(PreferenceConstants.EDITOR_SOURCE_HOVER_BACKGROUND_COLOR_SYSTEM_DEFAULT)
        ? null : PreferenceConverter.getColor(
            store,
            PreferenceConstants.EDITOR_SOURCE_HOVER_BACKGROUND_COLOR);
  }

  private void initializeColors() {
    RGB bgRGB = getHoverBackgroundColorRGB();
    if (bgRGB != null) {
      fBackgroundColor = new Color(fShell.getDisplay(), bgRGB);
      fIsSystemBackgroundColor = false;
    } else {
      fBackgroundColor = fShell.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND);
      fIsSystemBackgroundColor = true;
    }
  }

  /**
   * Initialize the font to the Java editor font.
   */
  private void initializeFont() {
    Font font = JFaceResources.getFont("com.google.dart.tools.ui.editors.textfont"); //$NON-NLS-1$
    StyledText styledText = getViewer().getTextWidget();
    styledText.setFont(font);
  }
}
