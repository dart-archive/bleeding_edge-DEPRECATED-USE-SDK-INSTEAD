package com.google.dart.tools.ui.internal.text.correction.proposals;

import com.google.dart.tools.internal.corext.refactoring.util.Messages;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.text.correction.CorrectionMessages;
import com.google.dart.tools.ui.text.dart.IDartCompletionProposal;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolution2;

public class MarkerResolutionProposal implements IDartCompletionProposal {

  private IMarkerResolution fResolution;
  private IMarker fMarker;

  /**
   * Constructor for MarkerResolutionProposal.
   * 
   * @param resolution the marker resolution
   * @param marker the marker
   */
  public MarkerResolutionProposal(IMarkerResolution resolution, IMarker marker) {
    fResolution = resolution;
    fMarker = marker;
  }

  @Override
  public void apply(IDocument document) {
    fResolution.run(fMarker);
  }

  @Override
  public String getAdditionalProposalInfo() {
    if (fResolution instanceof IMarkerResolution2) {
      return ((IMarkerResolution2) fResolution).getDescription();
    }
    if (fResolution instanceof IDartCompletionProposal) {
      return ((IDartCompletionProposal) fResolution).getAdditionalProposalInfo();
    }
    try {
      String problemDesc = (String) fMarker.getAttribute(IMarker.MESSAGE);
      return Messages.format(
          CorrectionMessages.MarkerResolutionProposal_additionaldesc,
          problemDesc);
    } catch (CoreException e) {
      DartToolsPlugin.log(e);
    }
    return null;
  }

  @Override
  public IContextInformation getContextInformation() {
    return null;
  }

  @Override
  public String getDisplayString() {
    return fResolution.getLabel();
  }

  @Override
  public Image getImage() {
    if (fResolution instanceof IMarkerResolution2) {
      return ((IMarkerResolution2) fResolution).getImage();
    }
    if (fResolution instanceof IDartCompletionProposal) {
      return ((IDartCompletionProposal) fResolution).getImage();
    }
    return DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE);
  }

  @Override
  public int getRelevance() {
    if (fResolution instanceof IDartCompletionProposal) {
      return ((IDartCompletionProposal) fResolution).getRelevance();
    }
    return 10;
  }

  @Override
  public Point getSelection(IDocument document) {
    if (fResolution instanceof IDartCompletionProposal) {
      return ((IDartCompletionProposal) fResolution).getSelection(document);
    }
    return null;
  }

}
