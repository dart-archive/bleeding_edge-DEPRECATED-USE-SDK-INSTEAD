import 'dart:html';
import 'package:poppopwin/html.dart';

main(){
  final int w = 16, h = 16, m = 40;

  final TableElement poppopwinTable = query('#gameTable');
  final Element bombsLeftDiv = query('#bombsLeft');
  final Element gameStateDiv = query('#gameState');
  final Element clockDiv = query('#clock');
  final gameView = new GameView(w, h, m,
      poppopwinTable, bombsLeftDiv, gameStateDiv, clockDiv);

  final DivElement highScoreDiv = query('#highScore');
  final highScoreView = new HighScoreView(gameView, highScoreDiv);

  final ButtonElement newGameButton = query('#newGame');
  newGameButton.onClick.listen((args) => gameView.newGame());
}
