package com.google.dart.tools.search.internal.ui;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.IFileBuffer;
import org.eclipse.core.filebuffers.IFileBufferListener;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Position;

import java.util.List;
import java.util.Map;

/**
 * Helper for updating search {@link Position}s when corresponding files are changed.
 * 
 * @coverage dart.editor.ui.search
 */
class PositionTracker implements IFileBufferListener {
  private final SearchMatchPage page;
  private final Map<IFile, List<Position>> fileToPositions = Maps.newHashMap();

  public PositionTracker(SearchMatchPage page) {
    this.page = page;
    FileBuffers.getTextFileBufferManager().addFileBufferListener(this);
  }

  @Override
  public void bufferContentAboutToBeReplaced(IFileBuffer buffer) {
    page.close();
  }

  @Override
  public void bufferContentReplaced(IFileBuffer buffer) {
    // not interesting for us
  }

  @Override
  public void bufferCreated(IFileBuffer buffer) {
    if (!(buffer instanceof ITextFileBuffer)) {
      return;
    }
    ITextFileBuffer textFileBuffer = (ITextFileBuffer) buffer;
    // prepare IFile
    IFile file = FileBuffers.getWorkspaceFileAtLocation(buffer.getLocation());
    if (file == null) {
      return;
    }
    // add positions to ITextFileBuffer document
    List<Position> positions = fileToPositions.get(file);
    if (positions != null) {
      for (Position position : positions) {
        try {
          textFileBuffer.getDocument().addPosition(position);
        } catch (BadLocationException e) {
        }
      }
    }
  }

  @Override
  public void bufferDisposed(IFileBuffer buffer) {
    if (!(buffer instanceof ITextFileBuffer)) {
      return;
    }
    ITextFileBuffer textFileBuffer = (ITextFileBuffer) buffer;
    // prepare IFile
    IFile file = FileBuffers.getWorkspaceFileAtLocation(buffer.getLocation());
    if (file == null) {
      return;
    }
    // remove positions from ITextFileBuffer document
    List<Position> positions = fileToPositions.get(file);
    if (positions != null) {
      for (Position position : positions) {
        textFileBuffer.getDocument().removePosition(position);
      }
    }
  }

  @Override
  public void dirtyStateChanged(IFileBuffer buffer, boolean isDirty) {
    // not interesting for us
  }

  @Override
  public void stateChangeFailed(IFileBuffer buffer) {
    // not interesting for us
  }

  @Override
  public void stateChanging(IFileBuffer buffer) {
    // not interesting for us
  }

  @Override
  public void stateValidationChanged(IFileBuffer buffer, boolean isStateValidated) {
    // not interesting for us
  }

  @Override
  public void underlyingFileDeleted(IFileBuffer buffer) {
    // not interesting for us
  }

  @Override
  public void underlyingFileMoved(IFileBuffer buffer, IPath path) {
    // not interesting for us
  }

  void dispose() {
    FileBuffers.getTextFileBufferManager().removeFileBufferListener(this);
  }

  void trackPosition(IFile file, Position position) {
    // remember position to track
    List<Position> positions = fileToPositions.get(file);
    if (positions == null) {
      positions = Lists.newArrayList();
      fileToPositions.put(file, positions);
    }
    positions.add(position);
    // track position now
    ITextFileBuffer textFileBuffer = FileBuffers.getTextFileBufferManager().getTextFileBuffer(
        file.getFullPath(),
        LocationKind.IFILE);
    if (textFileBuffer != null) {
      try {
        textFileBuffer.getDocument().addPosition(position);
      } catch (BadLocationException e) {
      }
    }
  }
}
