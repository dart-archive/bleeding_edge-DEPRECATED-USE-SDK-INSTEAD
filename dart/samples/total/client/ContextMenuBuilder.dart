// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * Helper class to build the Total context menu.
 */
class ContextMenuBuilder {

  ContextMenu _contextMenu;
  SpreadsheetPresenter _presenter;
  SelectionManager _selectionManager;
  Spreadsheet _spreadsheet;

  ContextMenuBuilder(this._presenter, this._spreadsheet, this._selectionManager,
      this._contextMenu) { }

  void build() {
    int idx = 0;
    _contextMenu.addMenuItem("New Spreadsheet", idx++,
      () => true,
      void _(Element elmt, int value) {
        Window window = _presenter.window;
        SpreadsheetPresenter presenter = new SpreadsheetPresenter.blank(window);
        presenter.recalculateViewport();
      });
    _contextMenu.addMenuItem("Cut", idx++,
        hasSelection,
        void _(Element elmt, int value) {
          _spreadsheet.clearDirtyCells();
          execute(new GeneralCommand(_spreadsheet, () {
            _presenter.copySelection();
            _presenter.clearSelection();
          }, "Cut"));
          _presenter. recalculate();
        });

    // TODO: make "copy" undoable
    _contextMenu.addMenuItem("Copy", idx++,
        hasSelection,
        void _(Element elmt, int value) {
          _presenter.copySelection();
        });

    _contextMenu.addMenuItem("Paste", idx++,
        bool _() => _presenter.hasCopiedData() && hasSelection(),
        void _(Element elmt, int value) {
          _spreadsheet.clearDirtyCells();
          execute(new GeneralCommand(_spreadsheet, () {
            _presenter.pasteSelection();
          }, "Paste"));
          _presenter.recalculate();
        });

    _contextMenu.addMenuItem("Clear", idx++,
        hasSelection,
        void _(Element elmt, int value) {
          _spreadsheet.clearDirtyCells();
          execute(new GeneralCommand(_spreadsheet, () {
            _presenter.clearSelection();
          }, "Clear"));
          _presenter.recalculate();
        });

    _contextMenu.addMenuItem("Sort A-Z", idx++,
        hasMultiCellSelection,
        void _(Element elmt, int value) {
          _spreadsheet.clearDirtyCells();
          execute(new GeneralCommand(_spreadsheet, () {
            _presenter.sortSelection(true);
          }, "Sort A-Z"));
          _presenter.recalculate();
        });

    _contextMenu.addMenuItem("Sort Z-A", idx++,
        hasMultiCellSelection,
        void _(Element elmt, int value) {
          _spreadsheet.clearDirtyCells();
          execute(new GeneralCommand(_spreadsheet, () {
            _presenter.sortSelection(false);
        }, "Sort Z-A"));
          _presenter.recalculate();
      });

    _contextMenu.addMenuItem("Undo", idx++,
        bool _() => _spreadsheet.undoStack.canUndo(),
        void _(Element elmt, int value) {
          _spreadsheet.clearDirtyCells();
          _spreadsheet.undoStack.undo();
          _selectionManager.selectionChanged();
          _presenter.recalculate();
        });

    _contextMenu.addMenuItem("Redo", idx++,
        bool _() => _spreadsheet.undoStack.canRedo(),
        void _(Element elmt, int value) {
          _spreadsheet.clearDirtyCells();
          _spreadsheet.undoStack.redo();
          _selectionManager.selectionChanged();
          _presenter.recalculate();
        });

    _contextMenu.addMenuItem("Insert and Shift Right", idx++,
        hasSelection,
        void _(Element elmt, int value) {
          _presenter.performEditOnSelection((CellRange selection) {
            if (selection.minCorner.row == 0 && selection.maxCorner.row == 0) {
              // Entire columns are selected.
              execute(new InsertColumnsCommand(_spreadsheet,
                  selection.minCorner.col, selection.maxCorner.col));
            } else {
              execute(new InsertBlockAndShiftRightCommand(_spreadsheet,
                  selection.minCorner, selection.maxCorner));
            }
          });
        });

    _contextMenu.addMenuItem("Insert and Shift Down", idx++,
        hasSelection,
        void _(Element elmt, int value) {
           _presenter.performEditOnSelection((CellRange selection) {
            if (selection.minCorner.col == 0 && selection.maxCorner.col == 0) {
              // Entire rows are selected.
              execute(new InsertRowsCommand(_spreadsheet,
                  selection.minCorner.row, selection.maxCorner.row));
            } else {
              execute(new InsertBlockAndShiftDownCommand(_spreadsheet,
                  selection.minCorner, selection.maxCorner));
            }
          });
        });

     _contextMenu.addMenuItem("Remove and Shift Left", idx++,
         hasSelection,
        void _(Element elmt, int value) {
           _presenter.performEditOnSelection((CellRange selection) {
            if (selection.minCorner.row == 0 && selection.maxCorner.row == 0) {
              // Entire columns are selected.
              execute(new RemoveColumnsCommand(_spreadsheet, selection.minCorner.col,
                  selection.maxCorner.col));
            } else {
              execute(new RemoveBlockAndShiftLeftCommand(_spreadsheet, selection.minCorner,
                  selection.maxCorner));
            }
          });
        });

    _contextMenu.addMenuItem("Remove and Shift Up", idx++,
        hasSelection,
        void _(Element elmt, int value) {
          _presenter.performEditOnSelection((CellRange selection) {
            if (selection.minCorner.col == 0 && selection.maxCorner.col == 0) {
              // Entire rows are selected.
              execute(new RemoveRowsCommand(_spreadsheet, selection.minCorner.row,
                  selection.maxCorner.row));
            } else {
              execute(new RemoveBlockAndShiftUpCommand(_spreadsheet, selection.minCorner,
                  selection.maxCorner));
            }
          });
        });

    _contextMenu.addMenuItem("Reset Row/Column Sizes", idx++,
        bool _() => true,
        void _(Element elmt, int value) {
        execute(new ResetRowColumnSizesCommand(_spreadsheet));
          _spreadsheet.refresh();
        });

    Window window = _presenter.window;
    Document doc = document;

    CanvasElement graphCanvas = new Element.tag("canvas");
    graphCanvas.id = "graphCanvas-${_spreadsheet.name}";
    graphCanvas.attributes["class"] = "graphCanvas";
    graphCanvas.classes.add("graphCanvas");
    graphCanvas.classes.add("fadeOut");
    graphCanvas.width = 600;
    graphCanvas.height = 400;
    graphCanvas.on.click.add((Event e) {
      graphCanvas.classes.remove("fadeIn");
      graphCanvas.classes.add("fadeOut");
    });
    _contextMenu.parent.nodes.add(graphCanvas);

    DivElement chartDiv = new Element.tag("div");
    chartDiv.id = "chartDiv-{$_spreadsheet.name}";
    chartDiv.attributes["class"] = "chartDiv";
    chartDiv.classes.add("chartDiv");
    chartDiv.classes.add("fadeOut");
    chartDiv.style.setProperty("width", HtmlUtils.toPx(600));
    chartDiv.on.click.add((Event e) {
      chartDiv.classes.remove("fadeIn");
      chartDiv.classes.add("fadeOut");
    });
    ImageElement image = new Element.tag("img");
    chartDiv.nodes.add(image);

    SelectElement chartType = new Element.tag("select");
    List<String> values = <String>[ "Line", "Pie", "3D Pie", "Concentric Pie", "Grouped Bar",
        "Stacked Bar", "Overlapped Bar" ];
    StringBuffer sb = new StringBuffer();
    values.forEach((String value) {
      sb.add("<option>");
      sb.add(value);
      sb.add("</option>");
    });
    chartType.innerHTML = sb.toString();
    chartType.on.change.add((Event e) {
      CellRange range = _selectionManager.getSelectionRange();
      ServerChart chart;
      switch (chartType.selectedIndex) {
        case 0:
          chart = new ServerLineChart(window);
          break;
        case 1:
          chart = new Server2DPieChart(window);
          break;
        case 2:
          chart = new Server3DPieChart(window);
          break;
        case 3:
          chart = new ServerConcentricPieChart(window);
          break;
        case 4:
          chart = new ServerVerticalGroupedBarChart(window);
          break;
        case 5:
          chart = new ServerVerticalStackedBarChart(window);
          break;
        case 6:
          chart = new ServerVerticalOverlappedBarChart(window);
          break;
        default:
          print("bad selectedIndex = ${chartType.selectedIndex}");
      }
      chart.setData(range);
      chart.render((String dataUrl) {
        image.src = dataUrl;
      });
    });
    chartDiv.nodes.add(chartType);
    _contextMenu.parent.nodes.add(chartDiv);

    _contextMenu.addMenuItem("Draw Graph", idx++,
        hasSelection,
        void _(Element elmt, int value) {
          graphCanvas.classes.remove("fadeOut");
          graphCanvas.classes.add("fadeIn");

          CellRange range = _selectionManager.getSelectionRange();

          chartType.selectedIndex = 0;
          ClientChart clientChart = new ClientLineChart(graphCanvas);
          clientChart.setData(range);
          clientChart.render();

          ServerChart serverChart = new ServerLineChart(window);
          serverChart.setData(range);
          serverChart.render((String dataUrl) {
            image.src = dataUrl;
            chartDiv.classes.remove("fadeOut");
            chartDiv.classes.add("fadeIn");
          });
        });
  }

  void execute(Command command) {
    _spreadsheet.execute(command);
  }

  bool hasMultiCellSelection() => !_selectionManager.isSingleCellSelection();

  bool hasSelection() => !_selectionManager.isSelectionEmpty();
}
