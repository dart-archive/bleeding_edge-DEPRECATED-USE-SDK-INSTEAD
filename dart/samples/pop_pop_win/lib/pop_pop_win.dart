library pop_pop_win;

import 'dart:html';
import 'dart:web_audio';
import 'package:bot/bot.dart';
import 'package:bot_web/bot_html.dart';
import 'package:bot_web/bot_texture.dart';

import 'package:pop_pop_win/src/canvas.dart';
import 'package:pop_pop_win/platform_target.dart';
import 'package:pop_pop_win/src/html.dart';

import 'src/textures.dart';

part 'src/pop_pop_win/audio.dart';

const String _ASSET_DIR = 'resources/';

const String _TRANSPARENT_TEXTURE =
    '${_ASSET_DIR}images/transparent_animated.png';
const String _OPAQUE_TEXTURE = '${_ASSET_DIR}images/dart_opaque_01.jpg';
const String _TRANSPARENT_STATIC_TEXTURE =
    '${_ASSET_DIR}images/transparent_static.png';

const int _LOADING_BAR_PX_WIDTH = 398;

DivElement _loadingBar;
ImageLoader _imageLoader;

final _Audio _audio = new _Audio();

void startGame(PlatformTarget platform) {
  initPlatform(platform);

  _loadingBar = querySelector('.sprite.loading_bar');
  _loadingBar.style.display = 'block';
  _loadingBar.style.width = '0';

  _imageLoader = new ImageLoader([_TRANSPARENT_TEXTURE,
                                  _OPAQUE_TEXTURE]);
  _imageLoader.loaded.listen(_onLoaded);
  _imageLoader.progress.listen(_onProgress);
  _imageLoader.load();
}

void _onProgress(_) {
  int completedBytes = _imageLoader.completedBytes;
  int totalBytes = _imageLoader.totalBytes;

  completedBytes += _audio.completedBytes;
  totalBytes += _audio.totalBytes;

  var percent = completedBytes / totalBytes;
  if (percent == double.INFINITY) percent = 0;
  var percentClean = (percent * 1000).floor() / 10;

  var barWidth = percent * _LOADING_BAR_PX_WIDTH;
  _loadingBar.style.width = '${barWidth.toInt()}px';
}

void _onLoaded(_) {
  if (_imageLoader.state == ResourceLoader.StateLoaded && _audio.done) {

    //
    // load textures
    //
    var opaqueImage = _imageLoader.getResource(_OPAQUE_TEXTURE);
    var transparentImage = _imageLoader.getResource(_TRANSPARENT_TEXTURE);

    // already loaded. Used in CSS.
    var staticTransparentImage =
        new ImageElement(src: _TRANSPARENT_STATIC_TEXTURE);

    var textures = getTextures(transparentImage, opaqueImage,
        staticTransparentImage);

    var textureData = new TextureData(textures);

    // run the app
    querySelector('#loading').style.display = 'none';
    _runGame(textureData);
  }
}

void _runGame(TextureData textureData) {
  _updateAbout();

  targetPlatform.aboutChanged.listen((_) => _updateAbout());

  final size = targetPlatform.renderBig ? 16 : 7;
  final int m = (size * size * 0.15625).toInt();

  final CanvasElement gameCanvas = querySelector('#gameCanvas');
  gameCanvas.style.userSelect = 'none';

  final gameRoot = new GameRoot(size, size, m, gameCanvas, textureData);

  // disable touch events
  window.onTouchMove.listen((args) => args.preventDefault());

  window.onKeyDown.listen(_onKeyDown);

  querySelector('#popup').onClick.listen(_onPopupClick);

  titleClickedEvent.listen((args) => targetPlatform.toggleAbout(true));
}

void _onPopupClick(MouseEvent args) {
  if (args.toElement is! AnchorElement) {
    targetPlatform.toggleAbout(false);
  }
}

void _onKeyDown(KeyboardEvent args) {
  var keyEvent = new KeyEvent.wrap(args);
  switch (keyEvent.keyCode) {
    case KeyCode.ESC: // esc
      targetPlatform.toggleAbout(false);
      break;
    case KeyCode.H: // h
      targetPlatform.toggleAbout();
      break;
  }
}

void _updateAbout() {
  var popDisplay = targetPlatform.showAbout ? 'inline-block' : 'none';
  querySelector('#popup').style.display = popDisplay;
}
