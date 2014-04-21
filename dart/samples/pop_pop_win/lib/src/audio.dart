library pop_pop_win.audio;

import 'dart:math';

import 'package:stagexl/stagexl.dart';

class GameAudio {
  static final Random _rnd = new Random();

  static ResourceManager _resourceManager;

  static const String _WIN = 'win',
      _CLICK = 'click',
      _POP = 'Pop',
      _FLAG = 'flag',
      _UNFLAG = 'unflag',
      _BOMB = 'Bomb',
      _THROW_DART = 'throw';

  static void initialize(ResourceManager resourceManager) {
    if (_resourceManager != null) throw new StateError('already initialized');
    _resourceManager = resourceManager;
  }

  static void win() => _playAudio(_WIN);

  static void click() => _playAudio(_CLICK);

  static void pop() => _playAudio(_POP);

  static void flag() => _playAudio(_FLAG);

  static void unflag() => _playAudio(_UNFLAG);

  static void bomb() => _playAudio(_BOMB);

  static void throwDart() => _playAudio(_THROW_DART);

  static void _playAudio(String name) {
    if (_resourceManager == null) throw new StateError('Not initialized');
    switch (name) {
      case GameAudio._POP:
        var i = _rnd.nextInt(8);
        name = '${GameAudio._POP}$i';
        break;
      case GameAudio._BOMB:
        var i = _rnd.nextInt(4);
        name = '${GameAudio._BOMB}$i';
        break;
    }
    _resourceManager.getSoundSprite('audio').play(name);
  }
}
