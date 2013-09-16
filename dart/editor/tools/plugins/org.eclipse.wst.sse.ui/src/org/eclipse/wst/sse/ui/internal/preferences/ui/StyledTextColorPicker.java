/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.preferences.ui;

import com.ibm.icu.text.Collator;

import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.ACC;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleControlAdapter;
import org.eclipse.swt.accessibility.AccessibleControlEvent;
import org.eclipse.swt.accessibility.AccessibleControlListener;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.wst.sse.core.internal.ltk.parser.RegionParser;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegionList;
import org.eclipse.wst.sse.core.internal.util.Debug;
import org.eclipse.wst.sse.ui.internal.SSEUIMessages;
import org.eclipse.wst.sse.ui.internal.util.EditorUtility;
import org.w3c.dom.Node;

import java.io.CharArrayReader;
import java.util.Dictionary;
import java.util.List;

/**
 * This class is configurable by setting 3 properties: 1) an array of Strings as the styleNames; one
 * unique entry for every style type meant to be configurable by the user 2) a Dictionary of
 * descriptions, mapping the styleNames to unique descriptions - meant for use within the selection
 * ComboBox TODO (pa) this should probably be working off document partitions now (2.1+) 3) a
 * Dictionary mapping parsed ITextRegion contexts (strings) to the locally defined styleNames
 */
public class StyledTextColorPicker extends Composite {
  protected class DescriptionSorter extends org.eclipse.wst.sse.ui.internal.util.Sorter {
    Collator collator = Collator.getInstance();

    public boolean compare(Object elementOne, Object elementTwo) {
      /**
       * Returns true if elementTwo is 'greater than' elementOne This is the 'ordering' method of
       * the sort operation. Each subclass overides this method with the particular implementation
       * of the 'greater than' concept for the objects being sorted.
       */
      return (collator.compare(elementOne.toString(), elementTwo.toString())) < 0;
    }
  }

  public static final String BACKGROUND = "background"; //$NON-NLS-1$
  public static final String BOLD = "bold"; //$NON-NLS-1$
  public static final String COLOR = "color"; //$NON-NLS-1$

  // names for preference elements ... non-NLS
  public static final String FOREGROUND = "foreground"; //$NON-NLS-1$
  public static final String ITALIC = "italic"; //$NON-NLS-1$
  public static final String NAME = "name"; //$NON-NLS-1$

  protected static final boolean showItalic = false;
  protected AccessibleControlListener backgroundAccListener = new AccessibleControlAdapter() {
    /**
     * @see org.eclipse.swt.accessibility.AccessibleControlAdapter#getValue(AccessibleControlEvent)
     */
    public void getValue(AccessibleControlEvent e) {
      if (e.childID == ACC.CHILDID_SELF) {
        e.result = fBackground.getColorValue().toString();
      }
    }
  };
  protected SelectionListener buttonListener = new SelectionListener() {

    public void widgetDefaultSelected(SelectionEvent e) {
      widgetSelected(e);
    }

    public void widgetSelected(SelectionEvent e) {
      String namedStyle = getStyleName(fStyleCombo.getItem(fStyleCombo.getSelectionIndex()));
      if (namedStyle == null)
        return;
      if (e.widget == fBold) {
        // get current (newly old) style
        String prefString = getPreferenceStore().getString(namedStyle);
        String[] stylePrefs = ColorHelper.unpackStylePreferences(prefString);
        if (stylePrefs != null) {
          String oldValue = stylePrefs[2];
          String newValue = String.valueOf(fBold.getSelection());
          if (!newValue.equals(oldValue)) {
            stylePrefs[2] = newValue;
            String newPrefString = ColorHelper.packStylePreferences(stylePrefs);
            getPreferenceStore().setValue(namedStyle, newPrefString);
            refresh();
          }
        }
      } else if (showItalic && e.widget == fItalic) {
        // get current (newly old) style
        String prefString = getPreferenceStore().getString(namedStyle);
        String[] stylePrefs = ColorHelper.unpackStylePreferences(prefString);
        if (stylePrefs != null) {
          String oldValue = stylePrefs[3];
          String newValue = String.valueOf(fItalic.getSelection());
          if (!newValue.equals(oldValue)) {
            stylePrefs[3] = newValue;
            String newPrefString = ColorHelper.packStylePreferences(stylePrefs);
            getPreferenceStore().setValue(namedStyle, newPrefString);
            refresh();
          }
        }
      } else if (e.widget == fClearStyle) {
        getPreferenceStore().setToDefault(namedStyle);
        refresh();
      }
    }
  };

  protected SelectionListener comboListener = new SelectionListener() {

    public void widgetDefaultSelected(SelectionEvent e) {
      widgetSelected(e);
    }

    public void widgetSelected(SelectionEvent e) {
      int selectedIndex = fStyleCombo.getSelectionIndex();
      String description = selectedIndex >= 0 ? fStyleCombo.getItem(selectedIndex) : null;
      activate(getStyleName(description));
    }
  };
  protected ColorSelector fBackground;
  protected Label fBackgroundLabel;
  protected Button fBold;
  protected Button fClearStyle;
  // Dictionary mapping the ITextRegion types above to color names, which
  // are, in turn, attributes
  protected Dictionary fContextStyleMap = null;
  protected Color fDefaultBackground = getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND);

  protected Color fDefaultForeground = getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND);
  // Dictionary mapping the ITextRegion types above to display strings, for
  // use in the combo box
  protected Dictionary fDescriptions = null;
  protected ColorSelector fForeground;
  protected Label fForegroundLabel;
//	private String fGeneratorKey;
  protected String fInput = ""; //$NON-NLS-1$
  protected Button fItalic;

  private IStructuredDocumentRegion fNodes = null;
  // defect 200764 - ACC:display values for color buttons
  protected AccessibleControlListener foregroundAccListener = new AccessibleControlAdapter() {
    /**
     * @see org.eclipse.swt.accessibility.AccessibleControlAdapter#getValue(AccessibleControlEvent)
     */
    public void getValue(AccessibleControlEvent e) {
      if (e.childID == ACC.CHILDID_SELF) {
        e.result = fForeground.getColorValue().toString();
      }
    }
  };
  // A RegionParser, which will turn the input into
  // IStructuredDocumentRegion(s) and Regions
  protected RegionParser fParser = null;

  private IPreferenceStore fPreferenceStore;
  protected Combo fStyleCombo = null;
  // The list of supported ITextRegion types [Strings]
  protected List fStyleList = null;

  // controls in picker
  protected StyledText fText = null;

  /**
   * XMLTextColorPicker constructor comment.
   * 
   * @param parent org.eclipse.swt.widgets.Composite
   * @param style int
   */
  public StyledTextColorPicker(Composite parent, int style) {
    super(parent, style);
    //GridLayout
    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    setLayout(layout);
    createControls(this);
  }

  // activate controls based on the given local color type
  protected void activate(String namedStyle) {
    if (namedStyle == null) {
      fForeground.setEnabled(false);
      fBackground.setEnabled(false);
      fClearStyle.setEnabled(false);
      fBold.setEnabled(false);
      if (showItalic)
        fItalic.setEnabled(false);
      fForegroundLabel.setEnabled(false);
      fBackgroundLabel.setEnabled(false);
    } else {
      fForeground.setEnabled(true);
      fBackground.setEnabled(true);
      fClearStyle.setEnabled(true);
      fBold.setEnabled(true);
      if (showItalic)
        fItalic.setEnabled(true);
      fForegroundLabel.setEnabled(true);
      fBackgroundLabel.setEnabled(true);

    }
    TextAttribute attribute = getAttribute(namedStyle);
    Color color = attribute.getForeground();
    if (color == null) {
      color = fDefaultForeground;
    }
    fForeground.setColorValue(color.getRGB());

    color = attribute.getBackground();
    if (color == null) {
      color = fDefaultBackground;
    }
    fBackground.setColorValue(color.getRGB());

    fBold.setSelection((attribute.getStyle() & SWT.BOLD) != 0);
    if (showItalic)
      fItalic.setSelection((attribute.getStyle() & SWT.ITALIC) != 0);
  }

  protected void applyStyles() {
    if (fText == null || fText.isDisposed() || fInput == null || fInput.length() == 0)
      return;
    //		List regions = fParser.getRegions();
    IStructuredDocumentRegion node = fNodes;
    while (node != null) {
      ITextRegionList regions = node.getRegions();
      for (int i = 0; i < regions.size(); i++) {
        ITextRegion currentRegion = regions.get(i);
        // lookup the local coloring type and apply it
        String namedStyle = (String) getContextStyleMap().get(currentRegion.getType());
        if (namedStyle == null)
          continue;
        TextAttribute attribute = getAttribute(namedStyle);
        if (attribute == null)
          continue;
        StyleRange style = new StyleRange(node.getStartOffset(currentRegion),
            currentRegion.getLength(), attribute.getForeground(), attribute.getBackground(),
            attribute.getStyle());
        fText.setStyleRange(style);
      }
      node = node.getNext();
    }
  }

  protected void close() {
  }

  /**
   * Creates an new checkbox instance and sets the default layout data.
   * 
   * @param group the composite in which to create the checkbox
   * @param label the string to set into the checkbox
   * @return the new checkbox
   */
  private Button createCheckBox(Composite group, String label) {
    Button button = new Button(group, SWT.CHECK | SWT.CENTER);
    if (label != null)
      button.setText(label);
    GridData data = new GridData(GridData.FILL_BOTH);
    data.horizontalAlignment = GridData.HORIZONTAL_ALIGN_END;
    //	data.verticalAlignment = GridData.VERTICAL_ALIGN_FILL;
    button.setLayoutData(data);
    return button;
  }

  private Combo createCombo(Composite parent, String[] labels, int selectedItem) {
    Combo combo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
    combo.setItems(labels);
    if (selectedItem >= 0)
      combo.select(selectedItem);
    GridData data = new GridData(GridData.FILL_HORIZONTAL);
    data.horizontalAlignment = GridData.HORIZONTAL_ALIGN_FILL;
    combo.setLayoutData(data);
    return combo;
  }

  /**
   * Creates composite control and sets the default layout data.
   */
  private Composite createComposite(Composite parent, int numColumns) {
    Composite composite = new Composite(parent, SWT.NULL);
    //GridLayout
    GridLayout layout = new GridLayout();
    layout.numColumns = numColumns;
    layout.horizontalSpacing = 5;
    layout.makeColumnsEqualWidth = false;
    composite.setLayout(layout);
    //GridData
    GridData data = new GridData(GridData.FILL_VERTICAL);
    data.grabExcessVerticalSpace = false;
    data.verticalAlignment = GridData.VERTICAL_ALIGN_CENTER;
    composite.setLayoutData(data);
    return composite;
  }

  protected void createControls(Composite parent) {
    Composite styleRow = createComposite(parent, 3);
    // row 1 - content type label, combo box, restore defaults
    createLabel(styleRow, SSEUIMessages.Content_type__UI_); //$NON-NLS-1$ = "Content type:"
    // Contexts combo box
    fStyleCombo = createCombo(styleRow, new String[0], -1);
    fClearStyle = createPushButton(styleRow, SSEUIMessages.Restore_Default_UI_); //$NON-NLS-1$ = "Restore Default"
    Composite styleRow2;
    if (showItalic)
      styleRow2 = createComposite(parent, 7);
    else
      styleRow2 = createComposite(parent, 6);
    // row 2 - foreground label, button, background label, button, bold,
    // italics?
    fForegroundLabel = createLabel(styleRow2, SSEUIMessages.Foreground_UI_); //$NON-NLS-1$ = "Foreground"
    fForeground = new ColorSelector(styleRow2);
    fForeground.getButton().setLayoutData(new GridData());
    setAccessible(fForeground.getButton(), fForegroundLabel.getText());
    fForeground.getButton().getAccessible().addAccessibleControlListener(foregroundAccListener); // defect
    // 200764
    // -
    // ACC:display
    // values
    // for
    // color
    // buttons
    ((GridData) fForeground.getButton().getLayoutData()).minimumWidth = 20;
    fBackgroundLabel = createLabel(styleRow2, SSEUIMessages.Background_UI_); //$NON-NLS-1$ = "Background"
    fBackground = new ColorSelector(styleRow2);
    fBackground.getButton().setLayoutData(new GridData());
    setAccessible(fBackground.getButton(), fBackgroundLabel.getText());
    fBackground.getButton().getAccessible().addAccessibleControlListener(backgroundAccListener); // defect
    // 200764
    // -
    // ACC:display
    // values
    // for
    // color
    // buttons
    ((GridData) fBackground.getButton().getLayoutData()).minimumWidth = 20;
    createLabel(styleRow2, ""); //$NON-NLS-1$
    fBold = createCheckBox(styleRow2, SSEUIMessages.Bold_UI_);
    if (showItalic)
      fItalic = createCheckBox(styleRow2, SSEUIMessages.Italics_UI);
    //		// Defaults checkbox
    fForeground.setEnabled(false);
    fBackground.setEnabled(false);
    fClearStyle.setEnabled(false);
    fBold.setEnabled(false);
    if (showItalic)
      fItalic.setEnabled(false);
    fForegroundLabel.setEnabled(false);
    fBackgroundLabel.setEnabled(false);
    Composite sample = createComposite(parent, 1);
    createLabel(sample, SSEUIMessages.Sample_text__UI_); //$NON-NLS-1$ = "&Sample text:"
    // BUG141089 - make sure text is left-aligned
    fText = new StyledText(sample, SWT.LEFT_TO_RIGHT | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL
        | SWT.BORDER | SWT.READ_ONLY);
    GridData data = new GridData(GridData.FILL_BOTH);
    fText.setLayoutData(data);
    fText.setEditable(false);
    fText.setBackground(fDefaultBackground);
    fText.setFont(JFaceResources.getTextFont());
    fText.addKeyListener(getTextKeyListener());
    fText.addSelectionListener(getTextSelectionListener());
    fText.addMouseListener(getTextMouseListener());
    fText.addTraverseListener(getTraverseListener()); // defect 220377 -
    // Provide tab
    // traversal for
    // fText widget
    setAccessible(fText, SSEUIMessages.Sample_text__UI_); //$NON-NLS-1$ = "&Sample text:"
    fForeground.addListener(new IPropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent event) {
        if (event.getProperty().equals(ColorSelector.PROP_COLORCHANGE)) {
          // get current (newly old) style
          String namedStyle = getStyleName(fStyleCombo.getItem(fStyleCombo.getSelectionIndex()));
          String prefString = getPreferenceStore().getString(namedStyle);
          String[] stylePrefs = ColorHelper.unpackStylePreferences(prefString);
          if (stylePrefs != null) {
            String oldValue = stylePrefs[0];
            String newValue = "null"; //$NON-NLS-1$
            Object newValueObject = event.getNewValue();
            if (newValueObject instanceof RGB) {
              newValue = ColorHelper.toRGBString((RGB) newValueObject);
            }

            if (!newValue.equals(oldValue)) {
              stylePrefs[0] = newValue;
              String newPrefString = ColorHelper.packStylePreferences(stylePrefs);
              getPreferenceStore().setValue(namedStyle, newPrefString);
              refresh();
            }
          }
        }
      }
    });
    fBackground.addListener(new IPropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent event) {
        if (event.getProperty().equals(ColorSelector.PROP_COLORCHANGE)) {
          // get current (newly old) style
          String namedStyle = getStyleName(fStyleCombo.getItem(fStyleCombo.getSelectionIndex()));
          String prefString = getPreferenceStore().getString(namedStyle);
          String[] stylePrefs = ColorHelper.unpackStylePreferences(prefString);
          if (stylePrefs != null) {
            String oldValue = stylePrefs[1];

            String newValue = "null"; //$NON-NLS-1$
            Object newValueObject = event.getNewValue();
            if (newValueObject instanceof RGB) {
              newValue = ColorHelper.toRGBString((RGB) newValueObject);
            }

            if (!newValue.equals(oldValue)) {
              stylePrefs[1] = newValue;
              String newPrefString = ColorHelper.packStylePreferences(stylePrefs);
              getPreferenceStore().setValue(namedStyle, newPrefString);
              refresh();
            }
          }
        }
      }
    });

    fClearStyle.addSelectionListener(buttonListener);
    fBold.addSelectionListener(buttonListener);
    if (showItalic)
      fItalic.addSelectionListener(buttonListener);
    fStyleCombo.addSelectionListener(comboListener);
  }

  /**
   * Utility method that creates a label instance and sets the default layout data.
   */
  private Label createLabel(Composite parent, String text) {
    Label label = new Label(parent, SWT.LEFT);
    label.setText(text);
    GridData data = new GridData();
    data.horizontalAlignment = GridData.FILL;
    label.setLayoutData(data);
    return label;
  }

  private Button createPushButton(Composite parent, String label) {
    Button button = new Button(parent, SWT.PUSH);
    button.setText(label);
    GridData data = new GridData(GridData.FILL_BOTH);
    //	data.horizontalAlignment = GridData.FILL;
    button.setLayoutData(data);
    return button;
  }

  protected TextAttribute getAttribute(String namedStyle) {
    TextAttribute ta = new TextAttribute(getDefaultForeground(), getDefaultBackground(), SWT.NORMAL);

    if (namedStyle != null && getPreferenceStore() != null) {
      String prefString = getPreferenceStore().getString(namedStyle);
      String[] stylePrefs = ColorHelper.unpackStylePreferences(prefString);
      if (stylePrefs != null) {
        RGB foreground = ColorHelper.toRGB(stylePrefs[0]);
        RGB background = ColorHelper.toRGB(stylePrefs[1]);

        int fontModifier = SWT.NORMAL;
        boolean bold = Boolean.valueOf(stylePrefs[2]).booleanValue();
        if (bold)
          fontModifier = fontModifier | SWT.BOLD;

        if (showItalic) {
          boolean italic = Boolean.valueOf(stylePrefs[3]).booleanValue();
          if (italic)
            fontModifier = fontModifier | SWT.ITALIC;
        }

        ta = new TextAttribute((foreground != null) ? EditorUtility.getColor(foreground) : null,
            (background != null) ? EditorUtility.getColor(background) : null, fontModifier);
      }
    }
    return ta;
  }

  // defect 200764 - ACC:display values for color buttons
  /**
   * @return String - color Button b's current RBG value
   */
//	private String getColorButtonValue(Button b) {
//		if ((b == null) || (b.getImage() == null) || (b.getImage().getImageData() == null) || (b.getImage().getImageData().getRGBs() == null) || (b.getImage().getImageData().getRGBs()[0] == null))
//			return null;
//		String val = b.getImage().getImageData().getRGBs()[0].toString();
//		return val;
//	}

  /**
   * @deprecated use getPreferenceStore instead left for legacy clients, delete by WTP M4
   */
  public Node getColorsNode() {
    //return fColorsNode;
    return null;
  }

  /**
   * @return java.util.Dictionary
   */
  public Dictionary getContextStyleMap() {
    return fContextStyleMap;
  }

  /**
   * @return org.eclipse.swt.graphics.Color
   */
  public Color getDefaultBackground() {
    return fDefaultBackground;
  }

  /**
   * @return org.eclipse.swt.graphics.Color
   */
  public Color getDefaultForeground() {
    return fDefaultForeground;
  }

  /**
   * @return java.util.Dictionary
   */
  public Dictionary getDescriptions() {
    return fDescriptions;
  }

  public Font getFont() {
    return fText.getFont();
  }

  protected String getNamedStyleAtOffset(int offset) {
    // ensure the offset is clean
    if (offset >= fInput.length())
      return getNamedStyleAtOffset(fInput.length() - 1);
    else if (offset < 0)
      return getNamedStyleAtOffset(0);
    // find the ITextRegion at this offset
    if (fNodes == null)
      return null;
    IStructuredDocumentRegion aNode = fNodes;
    while (aNode != null && !aNode.containsOffset(offset))
      aNode = aNode.getNext();
    if (aNode != null) {
      // find the ITextRegion's Context at this offset
      ITextRegion interest = aNode.getRegionAtCharacterOffset(offset);
      if (interest == null)
        return null;
      if (offset > aNode.getTextEndOffset(interest))
        return null;
      String regionContext = interest.getType();
      if (regionContext == null)
        return null;
      // find the named style (internal/selectable name) for that
      // context
      String namedStyle = (String) getContextStyleMap().get(regionContext);
      if (namedStyle != null) {
        return namedStyle;
      }
    }
    return null;
  }

  public RegionParser getParser() {
    return fParser;
  }

  private IPreferenceStore getPreferenceStore() {
    return fPreferenceStore;
  }

  /**
   * @return String[]
   */
  public List getStyleList() {
    return fStyleList;
  }

  private String getStyleName(String description) {
    if (description == null)
      return null;
    String styleName = null;
    java.util.Enumeration keys = getDescriptions().keys();
    while (keys.hasMoreElements()) {
      String test = keys.nextElement().toString();
      if (getDescriptions().get(test).equals(description)) {
        styleName = test;
        break;
      }
    }
    return styleName;
  }

  public String getText() {
    return fInput;
  }

  private KeyListener getTextKeyListener() {
    return new KeyListener() {
      public void keyPressed(KeyEvent e) {
        if (e.widget instanceof StyledText) {
          int x = ((StyledText) e.widget).getCaretOffset();
          selectColorAtOffset(x);
        }
      }

      public void keyReleased(KeyEvent e) {
        if (e.widget instanceof StyledText) {
          int x = ((StyledText) e.widget).getCaretOffset();
          selectColorAtOffset(x);
        }
      }
    };
  }

  private MouseListener getTextMouseListener() {
    return new MouseListener() {
      public void mouseDoubleClick(MouseEvent e) {
      }

      public void mouseDown(MouseEvent e) {
      }

      public void mouseUp(MouseEvent e) {
        if (e.widget instanceof StyledText) {
          int x = ((StyledText) e.widget).getCaretOffset();
          selectColorAtOffset(x);
        }
      }
    };
  }

  private SelectionListener getTextSelectionListener() {
    return new SelectionListener() {
      public void widgetDefaultSelected(SelectionEvent e) {
        selectColorAtOffset(e.x);
        if (e.widget instanceof StyledText) {
          ((StyledText) e.widget).setSelection(e.x);
        }
      }

// Commented out when moving to RC2 to remove "unused" error/warning			
//			public void widgetDoubleSelected(SelectionEvent e) {
//				selectColorAtOffset(e.x);
//				if (e.widget instanceof StyledText) {
//					((StyledText) e.widget).setSelection(e.x);
//				}
//			}

      public void widgetSelected(SelectionEvent e) {
        selectColorAtOffset(e.x);
        if (e.widget instanceof StyledText) {
          ((StyledText) e.widget).setSelection(e.x);
        }
      }
    };
  }

  // defect 220377 - Provide tab traversal for fText widget
  private TraverseListener getTraverseListener() {
    return new TraverseListener() {
      /**
       * @see org.eclipse.swt.events.TraverseListener#keyTraversed(TraverseEvent)
       */
      public void keyTraversed(TraverseEvent e) {
        if (e.widget instanceof StyledText) {
          if ((e.detail == SWT.TRAVERSE_TAB_NEXT) || (e.detail == SWT.TRAVERSE_TAB_PREVIOUS))
            e.doit = true;
        }
      }
    };
  }

  // refresh the GUI after a color change
  public void refresh() {
    fText.setRedraw(false);
    int selectedIndex = fStyleCombo.getSelectionIndex();
    String description = selectedIndex >= 0 ? fStyleCombo.getItem(selectedIndex) : null;
    activate(getStyleName(description));
    // update Font
    fText.setFont(JFaceResources.getTextFont());
    // reapplyStyles
    applyStyles();
    fText.setRedraw(true);
  }

  public void releasePickerResources() {
//		if (fForeground != null && !fForeground.isDisposed() && fForeground.getImage() != null)
//			fForeground.getImage().dispose();
//		if (fBackground != null && !fBackground.isDisposed() && fBackground.getImage() != null)
//			fBackground.getImage().dispose();
  }

  private void selectColorAtOffset(int offset) {
    String namedStyle = getNamedStyleAtOffset(offset);
    if (namedStyle == null) {
      fStyleCombo.deselectAll();
      activate(null);
      return;
    }
    String description = (String) getDescriptions().get(namedStyle);
    if (description == null)
      return;
    int itemCount = fStyleCombo.getItemCount();
    for (int i = 0; i < itemCount; i++) {
      if (fStyleCombo.getItem(i).equals(description)) {
        fStyleCombo.select(i);
        break;
      }
    }
    activate(namedStyle);
  }

  /**
   * Specifically set the reporting name of a control for accessibility
   */
  private void setAccessible(Control control, String name) {
    if (control == null)
      return;
    final String n = name;
    control.getAccessible().addAccessibleListener(new AccessibleAdapter() {
      public void getName(AccessibleEvent e) {
        if (e.childID == ACC.CHILDID_SELF)
          e.result = n;
      }
    });
  }

  /**
   * @deprecated use setPreferenceStore instead left for legacy clients, delete by WTP M4
   */
  public void setColorsNode(Node newColorsNode) {
    //fColorsNode = newColorsNode;
  }

  /**
   * @param newContextStyleMap java.util.Dictionary
   */
  public void setContextStyleMap(Dictionary newContextStyleMap) {
    fContextStyleMap = newContextStyleMap;
  }

  /**
   * @param newDefaultBackground org.eclipse.swt.graphics.Color
   */
  public void setDefaultBackground(Color newDefaultBackground) {
    fDefaultBackground = newDefaultBackground;
  }

  /**
   * @deprecated use setPreferenceStore instead left for legacy clients, delete by WTP M4
   */
  public void setDefaultColorsNode(Node newDefaultColorsNode) {
    //fDefaultColorsNode = newDefaultColorsNode;
  }

  /**
   * @param newDefaultForeground org.eclipse.swt.graphics.Color
   */
  public void setDefaultForeground(Color newDefaultForeground) {
    fDefaultForeground = newDefaultForeground;
  }

  /**
   * @param newDescriptions java.util.Dictionary
   */
  public void setDescriptions(Dictionary newDescriptions) {
    fDescriptions = newDescriptions;
    updateStyleList();
  }

  public void setFont(Font font) {
    fText.setFont(font);
    fText.redraw();
  }

  /**
   * @deprecated generator key should no longer be needed
   */
  public void setGeneratorKey(String key) {
//		fGeneratorKey = key;
  }

  /**
   * @param newParser
   */
  public void setParser(RegionParser newParser) {
    fParser = newParser;
  }

  public void setPreferenceStore(IPreferenceStore store) {
    fPreferenceStore = store;
  }

  /**
   * @param newStyleList String[]
   */
  public void setStyleList(List newStyleList) {
    fStyleList = newStyleList;
    updateStyleList();
  }

  public void setText(String s) {
    fInput = s;
    getParser().reset(new CharArrayReader(fInput.toCharArray()));
    fNodes = getParser().getDocumentRegions();
    if (Debug.displayInfo)
      System.out.println("Length of input: " //$NON-NLS-1$
          //$NON-NLS-1$
          + s.length() + ", " //$NON-NLS-1$
          + getParser().getRegions().size() + " regions."); //$NON-NLS-1$
    if (fText != null)
      fText.setText(s);
    applyStyles();
  }

  /**
   * @return org.eclipse.swt.graphics.RGB
   * @param anRGBString java.lang.String
   * @param defaultRGB org.eclipse.swt.graphics.RGB
   */
  // TODO: never used
  RGB toRGB(String anRGBString, RGB defaultRGB) {
    RGB result = ColorHelper.toRGB(anRGBString);
    if (result == null)
      return defaultRGB;
    return result;
  }

  private void updateStyleList() {
    if (fStyleList == null || fDescriptions == null)
      return;
    String[] descriptions = new String[fStyleList.size()];
    for (int i = 0; i < fStyleList.size(); i++) {
      if (fStyleList.get(i) != null)
        descriptions[i] = (String) getDescriptions().get(fStyleList.get(i));
      else
        descriptions[i] = (String) fStyleList.get(i);
    }
    Object[] sortedObjects = new DescriptionSorter().sort(descriptions);
    String[] sortedDescriptions = new String[descriptions.length];
    for (int i = 0; i < descriptions.length; i++) {
      sortedDescriptions[i] = sortedObjects[i].toString();
    }
    fStyleCombo.setItems(sortedDescriptions);
    fStyleCombo.select(0); //defect 219855 - initially select first item
    // in comboBox
    //		fStyleCombo.deselectAll();
  }

}
