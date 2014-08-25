/**
 * 
 */
package com.xored.glance.ui.utils;

import com.xored.glance.ui.sources.ITextBlock;
import com.xored.glance.ui.sources.ITextSource;
import com.xored.glance.ui.sources.ITextSourceListener;
import com.xored.glance.ui.sources.Match;
import com.xored.glance.ui.sources.SourceSelection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.swt.widgets.Control;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Yuri Strot
 */
public class UITextSource implements ITextSource, ITextSourceListener {

  private SourceSelection selection;

  private Map<ITextBlock, UITextBlock> blockToBlock;

  private final ListenerList listeners;

  private List<UITextBlock> blocks;

  private final ITextSource source;

  private final Control control;

  public UITextSource(final ITextSource source, final Control control) {
    this.source = source;
    this.control = control;

    blocks = new ArrayList<UITextBlock>();
    blockToBlock = new HashMap<ITextBlock, UITextBlock>();
    listeners = new ListenerList();
    source.addTextSourceListener(this);
    addBlocks(source.getBlocks());
    updateSelection();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.xored.glance.ui.sources.ITextSource#addTextSourceListener(com.xored
   * .glance.ui.sources.ITextSourceListener)
   */
  @Override
  public void addTextSourceListener(final ITextSourceListener listener) {
    listeners.add(listener);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.xored.glance.ui.sources.ITextSourceListener#blocksChanged(com.xored
   * .glance.ui.sources.ITextBlock[], com.xored.glance.ui.sources.ITextBlock[])
   */
  @Override
  public void blocksChanged(final ITextBlock[] removed, final ITextBlock[] added) {
    final ITextBlock[] uiRemoved = removeBlocks(removed);
    final ITextBlock[] uiAdded = addBlocks(added);
    final Object[] objects = listeners.getListeners();
    for (final Object object : objects) {
      final ITextSourceListener listener = (ITextSourceListener) object;
      listener.blocksChanged(uiRemoved, uiAdded);
    }
  }

  @Override
  public void blocksReplaced(final ITextBlock[] newBlocks) {
    synchronized (this.blocks) {
      for (final UITextBlock uiBlock : blockToBlock.values()) {
        uiBlock.dispose();
      }
      blockToBlock = new HashMap<ITextBlock, UITextBlock>();
      blocks = new ArrayList<UITextBlock>();
    }
    final ITextBlock[] uiAdded = addBlocks(newBlocks);
    final Object[] objects = listeners.getListeners();
    for (final Object object : objects) {
      final ITextSourceListener listener = (ITextSourceListener) object;
      listener.blocksReplaced(uiAdded);
    }
    selection = source.getSelection();
  }

  @Override
  public void dispose() {
    synchronized (blocks) {
      for (final UITextBlock block : blocks) {
        block.dispose();
      }
    }
    source.removeTextSourceListener(this);
    source.dispose();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.xored.glance.ui.sources.ITextSource#getBlocks()
   */
  @Override
  public ITextBlock[] getBlocks() {
    return blocks.toArray(new ITextBlock[blocks.size()]);
  }

  public Control getControl() {
    return control;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.xored.glance.ui.sources.ITextSource#getSelection()
   */
  @Override
  public SourceSelection getSelection() {
    return selection;
  }

  @Override
  public void index(final IProgressMonitor monitor) {
    source.index(monitor);
  }

  @Override
  public void init() {
    if (source != null) {
      source.init();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.xored.glance.ui.sources.ITextSource#isDisposed()
   */
  @Override
  public boolean isDisposed() {
    return source.isDisposed();
  }

  @Override
  public boolean isIndexRequired() {
    return source.isIndexRequired();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.xored.glance.ui.sources.ITextSource#removeTextSourceListener(com.
   * xored.glance.ui.sources.ITextSourceListener)
   */
  @Override
  public void removeTextSourceListener(final ITextSourceListener listener) {
    listeners.remove(listener);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.xored.glance.ui.sources.ITextSource#select(com.xored.glance.ui.sources .Match)
   */
  @Override
  public void select(final Match match) {
    UIUtils.asyncExec(control, new Runnable() {

      @Override
      public void run() {
        if (!source.isDisposed()) {
          if (match == null) {
            source.select(null);
          } else {
            final UITextBlock block = (UITextBlock) match.getBlock();
            source.select(new Match(block.getBlock(), match.getOffset(), match.getLength()));
          }
        }
      }
    });
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.xored.glance.ui.sources.ITextSourceListener#selectionChanged(com.
   * xored.glance.ui.sources.SourceSelection)
   */
  @Override
  public void selectionChanged(final SourceSelection selection) {
    final SourceSelection newSelection = updateSelection();
    final Object[] objects = listeners.getListeners();
    for (final Object object : objects) {
      final ITextSourceListener listener = (ITextSourceListener) object;
      listener.selectionChanged(newSelection);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.xored.glance.ui.sources.ITextSource#show(com.xored.glance.ui.sources .Match[])
   */
  @Override
  public void show(final Match[] matches) {
    UIUtils.asyncExec(control, new Runnable() {

      @Override
      public void run() {
        if (!source.isDisposed()) {
          final Match[] newMatches = new Match[matches.length];
          for (int i = 0; i < matches.length; i++) {
            final Match match = matches[i];
            final UITextBlock block = (UITextBlock) match.getBlock();
            newMatches[i] = new Match(block.getBlock(), match.getOffset(), match.getLength());
          }
          source.show(newMatches);
        }
      }
    });
  }

  @Override
  public void updateSourceSelection() {
    updateSelection();
  }

  protected ITextBlock[] addBlocks(final ITextBlock[] blocks) {
    synchronized (this.blocks) {
      final ITextBlock[] added = new ITextBlock[blocks.length];
      for (int i = 0; i < blocks.length; i++) {
        final ITextBlock block = blocks[i];
        final UITextBlock uiBlock = new UITextBlock(block);
        added[i] = uiBlock;
        this.blocks.add(uiBlock);
        blockToBlock.put(block, uiBlock);
      }
      return added;
    }
  }

  protected ITextBlock[] removeBlocks(final ITextBlock[] blocks) {
    synchronized (this.blocks) {
      final List<ITextBlock> removed = new ArrayList<ITextBlock>(blocks.length);
      for (int i = 0; i < blocks.length; i++) {
        final ITextBlock block = blocks[i];
        final UITextBlock uiBlock = blockToBlock.remove(block);
        if (uiBlock != null) {
          removed.add(uiBlock);
          this.blocks.remove(uiBlock);
          uiBlock.dispose();
        }
      }
      return removed.toArray(new ITextBlock[removed.size()]);
    }
  }

  protected SourceSelection updateSelection() {
    final SourceSelection sourceSelection = source.getSelection();
    if (sourceSelection == null) {
      selection = null;
    } else {
      selection = new SourceSelection(
          blockToBlock.get(sourceSelection.getBlock()),
          sourceSelection.getOffset(),
          sourceSelection.getLength());
    }
    return selection;
  }
}
