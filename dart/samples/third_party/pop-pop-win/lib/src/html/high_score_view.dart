part of ppw_html;

class HighScoreView {
  final DivElement _div;
  final GameManager _manager;

  HighScoreView(this._manager, this._div) {
    assert(_div != null);
    assert(_manager != null);

    _manager.bestTimeUpdated.listen((args) => _update());

    _update();
  }

  void _update() {
    _manager.bestTimeMilliseconds
      .then((int milliseconds) {
        if(milliseconds != null) {
          final duration = new Duration(seconds: milliseconds ~/ 1000);
          _div.innerHtml = duration.toString();
        } else {
          _div.innerHtml = '';
        }
      });
  }
}
