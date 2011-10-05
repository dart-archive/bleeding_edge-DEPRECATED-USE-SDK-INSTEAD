// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * A function that formats a given [double] into a [String].
 */
typedef String NumericFormat(double x);

/**
 * A function that formats a given [String] using HTML tags.
 */
typedef String TextFormat(String x);

/**
 * A class holding information about cell style.
 *
 * There is a default style that is the initial value for any new instances.
 *
 * The style is immutable, so any modification results in a new Style object.
 * Objects can hence share styles in the knowledge that, if some other object
 * needs a new style, their own style will be unaffected.
 */
class Style {

  /**
   * Constant indicating the use of a custom function for text or numeric formatting.
   */
  static final int CUSTOM = -1;

  /**
   * Constant indicating a style element is unset (i.e., at its default setting).
   * For example, if the background color of a [Style] is unset, a white color is
   * used for rendering the cell; however, if a prior style (such as a sheet, row,
   * or column style) has set the background color to yellow, a value of UNSET will
   * allow the yellow to show through, whereas a value of WHITE will force a white
   * color.
   */
  static final int UNSET = 0;

  /**
   * Constant indicating a text or background color.
   */
  static final int WHITE = 1;

  /**
   * Constant indicating a text or background color.
   */
  static final int BLACK = 2;

  /**
   * Bit indicating a bold text style.
   */
  static final int BOLD = 1;

  /**
   * Bit indicating an italic text style.
   */
  static final int ITALIC = 2;

  /**
   * Bit indicating an underline text style.
   */
  static final int UNDERLINE = 4;

  /**
   * Bit indicating a strikethrough text style.
   */
  static final int STRIKETHROUGH = 8;

  /**
   * Constant indicating a text alignment.
   */
  static final int LEFT = 1;

  /**
   * Constant indicating a text alignment.
   */
  static final int CENTER = 2;

  /**
   * Constant indicating a text alignment.
   */
  static final int RIGHT = 3;

  static Style _defaultInstance;

  static List<TextFormat> _textFormats;
  
  /**
   * Returns a [Style] that is the result of applying [style1] atop [style2], or vice versa.
   * The later-defined style is applied atop the earlier-defined style.  If either
   * style is [:null:], the non-[:null:] style is returned.  If both styles are [:null:],
   * [:null:] is returned.
   */
  static Style merge(Style style1, int priority1, Style style2, int priority2) {
    Style s;
    if (style1 == null && style2 == null) {
      s = null;
    } else if (style2 == null) {
      s = style1;
    } else if (style1 == null) {
      s = style2;
    } else {
      if (priority1 < priority2) {
        s = style2.applyAtop(style1);
    } else {
      s = style1.applyAtop(style2);
      }
    }
    return s;
  }

  /**
   * Returns a [Style] that is the result of applying three styles, in the order in which
   * they were created.  If all styles are [:null:], [:null:] is returned.
   */
  static Style merge3(Style style1, int priority1, Style style2, int priority2, Style style3,
                      int priority3) {
    // Call merge for two-way merges (merge handles the case of 1 or 2 nulls)
    if (style1 == null) {
      return merge(style2, priority2, style3, priority3);
    } else if (style2 == null) {
      return merge(style1, priority1, style3, priority3);
    } else if (style3 == null) {
      return merge(style1, priority1, style2, priority2);
    }

    // Merge the two oldest styles, the apply the newest style on top of the result
    Style s = null;
    if (priority1 >= priority2 && priority1 >= priority3) {
      s = merge(style2, priority2, style3, priority3);
      s = style1.applyAtop(s);
    } else if (priority2 >= priority1 && priority2 >= priority3) {
      s = merge(style1, priority1, style3, priority3);
      s = style2.applyAtop(s);
    } else {
      s = merge(style1, priority1, style2, priority2);
      s = style3.applyAtop(s);
    }
    return s;
  }

  int _backgroundColor;
  Formats _formats;
  NumericFormat _numericFormat;
  int _numericFormatIndex; // CUSTOM == custom format
  int _textAlignment;
  int _textColor;
  TextFormat _textFormat;
  int _textFormatIndex; // CUSTOM == custom format

  int get backgroundColor() {
    return _backgroundColor;
  }

  NumericFormat get numericFormat() {
    return _numericFormat;
  }

  int get numericFormatIndex() {
    return _numericFormatIndex;
  }

  int get textAlignment() {
    return _textAlignment;
  }

  int get textColor() {
    return _textColor;
  }

  int get textFormatIndex() {
    return _textFormatIndex;
  }

  /**
   * Returns a default style with no settings.
   */
  factory Style() {
    if (Style._defaultInstance == null) {
      Style._defaultInstance = new Style._private();
    }
    return Style._defaultInstance;
  }

  /**
   * Constructs a Style that is a copy of another style.
   */
  factory Style._copy(Style other) {
    Style s = new Style._private();
    s._formats = other._formats;
    s._numericFormat = other._numericFormat;
    s._numericFormatIndex = other._numericFormatIndex;
    s._textFormat = other._textFormat;
    s._textFormatIndex = other._textFormatIndex;
    s._textAlignment = other._textAlignment;
    s._textColor = other._textColor;
    s._backgroundColor = other._backgroundColor;
    return s;
  }

  /**
   * Constructs a Style with no settings.
   */
  Style._private() : _formats = new Formats(),
      _numericFormat = Formats.DEFAULT_NUMERIC_FORMAT,
      _numericFormatIndex = UNSET,
      _textFormat = Formats.DEFAULT_TEXT_FORMAT,
      _textFormatIndex = UNSET,
      _textAlignment = UNSET,
      _textColor = UNSET,
      _backgroundColor = UNSET {
  }

  bool operator==(Object o) {
    if (!(o is Style)) {
      return false;
    }
    if (this === o) {
      return true;
    }

    // No need to check the format indices
    Style other = o;
    return _backgroundColor == other._backgroundColor
        && _numericFormat == other._numericFormat
        && _textAlignment == other._textAlignment
        && _textColor == other._textColor
        && _textFormat == other._textFormat;
  }

  // Return a new style that is the result of applying the properties of this
  // style that have been explicitly set to a given style.
  Style applyAtop(Style s) {
    Style result = new Style._copy(s);
    if (_backgroundColor != UNSET) {
      result._backgroundColor = _backgroundColor;
    }
    if (_textColor != UNSET) {
      result._textColor = _textColor;
    }
    if (_textAlignment != UNSET) {
      result._textAlignment = _textAlignment;
    }
    if (_textFormatIndex != UNSET) {
      // Note that if _textFormatIndex is CUSTOM, this will force result._textFormatIndex to
      // CUSTOM as well
      result._textFormatIndex |= _textFormatIndex;
      result._textFormat = getTextFormatByIndex(result._textFormatIndex);
    }
    if (_numericFormatIndex != UNSET) {
      result._numericFormatIndex = _numericFormatIndex;
      result._numericFormat = _numericFormat;
    }
    return result;
  }

  Style clearBackgroundColor() {
    Style result = new Style._copy(this);
    result._backgroundColor = UNSET;
    return result;
  }

  Style clearNumericFormat() {
    Style result = new Style._copy(this);
    result._numericFormat = Formats.DEFAULT_NUMERIC_FORMAT;
    result._numericFormatIndex = UNSET;
    return result;
  }

  // Clear settings that are present in another style
  Style clearSettings(Style s) {
    Style newStyle = this;
    if (s._backgroundColor != UNSET) {
      newStyle = newStyle.clearBackgroundColor();
    }
    if (s._textColor != UNSET) {
      newStyle = newStyle.clearTextColor();
    }
    if (s._textAlignment != UNSET) {
      newStyle = newStyle.clearTextAlignment();
    }
    if (s._textFormatIndex != UNSET) {
      int newTextFormatIndex = newStyle._textFormatIndex & ~s._textFormatIndex;
      newStyle = newStyle.setTextFormatByIndex(newTextFormatIndex);
    }
    if (s._numericFormatIndex != UNSET) {
      newStyle = newStyle.clearNumericFormat();
    }
    return newStyle;
  }

  Style clearTextAlignment() {
    Style result = new Style._copy(this);
    result._textAlignment = UNSET;
    return result;
  }

  Style clearTextColor() {
    Style result = new Style._copy(this);
    result._textColor = UNSET;
    return result;
  }

  Style clearTextFormat() {
    Style result = new Style._copy(this);
    result._textFormat = Formats.DEFAULT_TEXT_FORMAT;
    result._textFormatIndex = UNSET;
    return result;
  }

  String formatNumber(int datatype, double input) {
    NumericFormat f;
    // Use datatype-specific formatting in the default case
    if (_numericFormatIndex == UNSET) {
      int index = _formats.getFormatForDatatype(datatype);
      f = _formats.getNumericFormat(index);
    } else {
      f = _numericFormat;
    }

    return f(input);
  }

  String formatText(String input) {
    return getTextFormat()(input);
  }

  String getTextAlignmentString() {
    switch (_textAlignment) {
    case UNSET: // fallthrough
    case LEFT:
      return "left";
    case CENTER:
      return "center";
    case RIGHT:
      return "right";
    }
  }

  TextFormat getTextFormat() {
    return _textFormat;
  }

  TextFormat getTextFormatByIndex(int formatIndex) {
    if (_textFormats == null) {
      _textFormats = new List<TextFormat>(16);
      _textFormats[0] = Formats.DEFAULT_TEXT_FORMAT;
    }

    // Create and cache a suitable formatting function
    if (_textFormats[formatIndex] == null) {
      StringBuffer sb = new StringBuffer();
      sb.add("<span");
      if (formatIndex > 0) {
        sb.add(" style='");
        if ((formatIndex & BOLD) != 0) {
          sb.add("font-weight: bold;");
        }
        if ((formatIndex & ITALIC) != 0) {
          if ((formatIndex & BOLD) != 0) {
            sb.add(" ");
          }
          sb.add("font-style: italic;");
        }
        if ((formatIndex & (UNDERLINE | STRIKETHROUGH)) != 0) {
          sb.add("text-decoration:");
          if ((formatIndex & UNDERLINE) != 0) {
            sb.add(" underline");
          }
          if ((formatIndex & STRIKETHROUGH) != 0) {
            sb.add(" line-through");
          }
          sb.add(";");
        }
        sb.add("'");
      }
      sb.add(">");
      String prefix = sb.toString();

      _textFormats[formatIndex] = (String x) {
        return "${prefix}${x}</span>";
      };
    }

    return _textFormats[formatIndex];
  }

  bool isDefault() {
    return this === _defaultInstance;
  }

  bool isEmpty() {
    return _backgroundColor == UNSET && _textColor == UNSET && _textAlignment == UNSET
        && _textFormatIndex == UNSET && _numericFormatIndex == UNSET;
  }

  Style setBackgroundColor(int backgroundColor) {
    assert(backgroundColor >= 0 && backgroundColor < Formats.numColorDescriptions);
    Style result = new Style._copy(this);
    result._backgroundColor = backgroundColor;
    return result;
  }

  Style setNumericFormat(NumericFormat format) {
    assert (format != null);
    Style result = new Style._copy(this);
    result._numericFormat = format;
    result._numericFormatIndex = CUSTOM;
    return result;
  }

  Style setNumericFormatByIndex(int formatIndex) {
    assert(formatIndex >= 0 && formatIndex < _formats.numNumericFormats);
    Style result = new Style._copy(this);
    result._numericFormat = _formats.getNumericFormat(formatIndex);
    result._numericFormatIndex = formatIndex;
    return result;
  }

  Style setTextAlignment(int alignment) {
    assert(alignment >= 0 && alignment < Formats.numTextAlignmentDescriptions);
    Style result = new Style._copy(this);
    result._textAlignment = alignment;
    return result;
  }

  Style setTextColor(int textColor) {
    assert(textColor >= 0 && textColor < Formats.numColorDescriptions);
    Style result = new Style._copy(this);
    result._textColor = textColor;
    return result;
  }

  Style setTextFormat(TextFormat format) {
    assert(format != null);
    Style result = new Style._copy(this);
    result._textFormat = format;
    result._textFormatIndex = CUSTOM;
    return result;
  }

  Style setTextFormatByIndex(int formatIndex) {
    assert(formatIndex >= 0 && formatIndex <= BOLD + ITALIC + UNDERLINE + STRIKETHROUGH);
    Style result = new Style._copy(this);

    result._textFormat = getTextFormatByIndex(formatIndex);
    result._textFormatIndex = formatIndex;
    return result;
  }

  String toHtml() {
    return _toString(true);
  }

  String toString() {
    return _toString(false);
  }

  String _toString(bool html) {
    StringBuffer sb = new StringBuffer();
    if (_textFormatIndex != UNSET) {
      if (_textFormatIndex == CUSTOM) {
        sb.add(_textFormat("Text"));
      } else {
        sb.add(_textFormat(_formats.getTextFormatDescription(_textFormatIndex)));
      }
      sb.add(" ");
    }
    if (_numericFormatIndex != UNSET) {
      if (_numericFormatIndex == CUSTOM) {
        sb.add(_numericFormat(-1000.12));
      } else {
        sb.add(_formats.getNumericFormatDescription(_numericFormatIndex));
      }
      sb.add(" ");
    }
    if (_textAlignment != UNSET) {
      sb.add(Formats.getTextAlignmentDescription(_textAlignment));
      sb.add(" ");
    }
    if (_textColor != UNSET || _backgroundColor != UNSET) {
      if (html) {
        sb.add("<span style='border:1px solid black;");
        if (_textColor != UNSET) {
          sb.add("color:${Formats.getHtmlColor(_textColor)};");
        }
        if (_backgroundColor != UNSET) {
          sb.add("background-color:${Formats.getHtmlColor(_backgroundColor)};");
        }
        sb.add("'>");
      }
      if (_textColor != UNSET) {
        sb.add(Formats.getColorDescription(_textColor));
      }
      if (_textColor != UNSET || _backgroundColor != UNSET) {
        sb.add("/");
      }
      if (_backgroundColor != UNSET) {
        sb.add(Formats.getColorDescription(_backgroundColor));
      }
      if (html) {
        sb.add("</span>");
      }
    }
    return sb.toString();
  }
}
