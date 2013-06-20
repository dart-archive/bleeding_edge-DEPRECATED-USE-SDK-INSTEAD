/**
 * 
 */
package com.xored.glance.ui.controls.text.styled;

import com.xored.glance.internal.ui.GlancePlugin;
import com.xored.glance.ui.sources.Match;
import com.xored.glance.ui.utils.TextUtils;

import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.swt.custom.ExtendedModifyEvent;
import org.eclipse.swt.custom.LineStyleEvent;
import org.eclipse.swt.custom.LineStyleListener;
import org.eclipse.swt.custom.StyledText;

/**
 * @author Yuri Strot
 */
public class ListeningStyledTextSource extends AbstractStyledTextSource implements
    LineStyleListener {

  private Match[] matches = Match.EMPTY;

  /**
   * @param text
   */
  public ListeningStyledTextSource(final StyledText text) {
    super(text);
    text.addLineStyleListener(this);
  }

  @Override
  public void lineGetStyle(final LineStyleEvent event) {
    if (matches.length > 0) {
      final int offset = event.lineOffset;
      final int length = event.lineText.length();

      int size = event.styles == null ? 0 : event.styles.length;
      if (size == 0) {
        size = 1;
      }
      final TextPresentation presentation = new TextPresentation(new Region(offset, length), size);
      if (event.styles != null && event.styles.length > 0) {
        presentation.replaceStyleRanges(event.styles);
      }
      applyTextPresentation(presentation);
      event.styles = TextUtils.getStyles(presentation);
    }
  }

  @Override
  public void select(final Match match) {
    super.select(match);
    refresh();
  }

  @Override
  public void show(final Match[] matches) {
    this.matches = matches;
    refresh();
  }

  @Override
  protected StyledTextBlock createTextBlock() {
    return new StyledTextBlock(getText()) {
      @Override
      public void modifyText(final ExtendedModifyEvent event) {
        matches = Match.EMPTY;
        super.modifyText(event);
      }
    };
  }

  @Override
  protected void doDispose() {
    final StyledText text = getText();
    try {
      super.doDispose();
      try {
        text.removeLineStyleListener(this);
      } finally {
        refresh();
      }
    } catch (final Exception e) {
      GlancePlugin.log("Problems with '" + text.getClass() + "'", e);
    }
  }

  private void applyTextPresentation(final TextPresentation presentation) {
    TextUtils.applyStyles(presentation, matches, selected);
  }

  private void refresh() {
    final String text = getText().getText();
    getText().redrawRange(0, text.length(), false);
  }
}
