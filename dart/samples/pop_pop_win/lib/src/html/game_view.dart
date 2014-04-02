part of pop_pop_win.html;

class GameView extends GameManager {
  static const String _xKey = 'x';
  static const String _yKey = 'y';

  final TableElement _table;
  final Element _leftCountDiv;
  final Element _gameStateDiv;
  final Element _clockDiv;

  GameView(int width, int height, int bombCount,
      this._table, this._leftCountDiv, this._gameStateDiv,
      this._clockDiv) : super(width, height, bombCount);

  void updateElement() {
    updateClock();
    _gameStateDiv.innerHtml = game.state.name;
    _leftCountDiv.innerHtml = game.bombsLeft.toString();

    if(_table.children.length == 0) {

      for(int r = 0; r < game.field.height; r++) {
        TableRowElement row = _table.insertRow(-1);

        for(int c = 0; c < game.field.width; c++) {
          TableCellElement cell = row.insertCell(-1);
          cell.onMouseDown.listen(_cellClick);
          cell.dataset[_xKey] = c.toString();
          cell.dataset[_yKey] = r.toString();
        }
      }
    }

    for(int r = 0; r < game.field.height; r++) {
      for(int c = 0; c < game.field.width; c++) {
        TableRowElement row = _table.rows[r];
        TableCellElement cell = row.cells[c];

        cell.classes.clear();
        cell.classes.add('game-square');
        final ss = game.getSquareState(c, r);
        cell.classes.add(ss.name);
        if(ss == SquareState.revealed) {
          final adj = game.field.getAdjacentCount(c, r);
          assert(adj != null);
          if(adj > 0) {
            cell.innerHtml = adj.toString();
          }
        }
      }
    }

  }

  void newGame() {
    super.newGame();
    _table.children.clear();
    updateElement();
  }

  void updateClock() {
    if(game.duration == null) {
      _clockDiv.innerHtml = '';
    } else {
      _clockDiv.innerHtml = game.duration.inSeconds.toString();
    }

    super.updateClock();
  }

  void _cellClick(MouseEvent args) {
    if(args.button == 0 && _canClick) {
      final TableCellElement cell = args.currentTarget;
      final xStr = cell.dataset[_xKey];
      final yStr = cell.dataset[_yKey];

      final x = int.parse(xStr);
      final y = int.parse(yStr);
      _click(x, y, args.shiftKey);
    }
  }

  void gameUpdated(args) {
    updateElement();
  }
}
