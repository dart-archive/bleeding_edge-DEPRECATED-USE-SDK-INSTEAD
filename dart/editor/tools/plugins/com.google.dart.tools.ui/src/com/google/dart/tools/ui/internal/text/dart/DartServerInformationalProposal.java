package com.google.dart.tools.ui.internal.text.dart;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension6;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.TextStyle;

/**
 * {@link DartServerInformationalProposal} displays a message to the user by appearing in the list
 * of completions sorted towards the bottom of the visible list.
 */
public class DartServerInformationalProposal implements ICompletionProposal,
    ICompletionProposalExtension6 {

  /**
   * An informational message indicating that no completion results were received from the analysis
   * server.
   */
  public static final ICompletionProposal NO_RESPONSE = new DartServerInformationalProposal(
      "-- Analyzing; try again soon --");

  /**
   * An informational message indicating that analysis is complete and the completion result
   * notification(s) received from the analysis server did not provide any completions.
   */
  public static final ICompletionProposal NO_RESULTS_COMPLETE = new DartServerInformationalProposal(
      "-- No completions available --");

  /**
   * An informational message indicating that the completion result notification(s) received from
   * the analysis server did not provide any completions, but that analysis is still ongoing.
   */
  public static final ICompletionProposal NO_RESULTS_TIMED_OUT = new DartServerInformationalProposal(
      "-- Analyzing, try again soon --");

  /**
   * An informational message indicating only partial completion results were received from the
   * analysis server.
   */
  public static final ICompletionProposal PARTIAL_RESULTS = new DartServerInformationalProposal(
      "-- Partial results - still analyzing --");

  /**
   * An informational message indicating that the external analysis server process has stopped and
   * no code completion results are available.
   */
  public static final ICompletionProposal SERVER_DEAD = new DartServerInformationalProposal(
      "-- Analysis Server has crashed, sorry --");

  /**
   * Internal {@link Styler} for creating the {@link #getStyledDisplayString()}.
   */
  private static final Styler informationStyle = new Styler() {
    @Override
    public void applyStyles(TextStyle textStyle) {
      // TODO (danrubel) implement
    }
  };

  /**
   * The informational message to be displayed.
   */
  private final String message;

  private StyledString styledMessage;

  private DartServerInformationalProposal(String message) {
    this.message = message;
  }

  @Override
  public void apply(IDocument document) {
    // Informational only... nothing to apply
  }

  @Override
  public String getAdditionalProposalInfo() {
    return null;
  }

  @Override
  public IContextInformation getContextInformation() {
    return null;
  }

  @Override
  public String getDisplayString() {
    return message;
  }

  @Override
  public Image getImage() {
    return null;
  }

  @Override
  public Point getSelection(IDocument document) {
    return null;
  }

  @Override
  public StyledString getStyledDisplayString() {
    if (styledMessage == null) {
      styledMessage = new StyledString(getDisplayString());
      styledMessage.setStyle(0, getDisplayString().length(), informationStyle);
    }
    return styledMessage;
  }
}
