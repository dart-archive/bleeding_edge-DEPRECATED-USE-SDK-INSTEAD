import 'dart:html';
import 'package:poppopwin/html.dart';

void main() {
  final int w = 16, h = 16, m = 40;

  final TableElement poppopwinTable = querySelector('#gameTable');
  final Element bombsLeftDiv = querySelector('#bombsLeft');
  final Element gameStateDiv = querySelector('#gameState');
  final Element clockDiv = querySelector('#clock');
  final gameView = new GameView(w, h, m, poppopwinTable, bombsLeftDiv,
      gameStateDiv, clockDiv);

  final DivElement highScoreDiv = querySelector('#highScore');
  final highScoreView = new HighScoreView(gameView, highScoreDiv);

  final ButtonElement newGameButton = querySelector('#newGame');
  newGameButton.onClick.listen((args) => gameView.newGame());
}
