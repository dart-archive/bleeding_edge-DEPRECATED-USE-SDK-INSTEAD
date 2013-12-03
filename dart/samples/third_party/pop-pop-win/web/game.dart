library game;

import 'dart:html';
import 'dart:web_audio';
import 'package:bot/bot.dart';
import 'package:bot_web/bot_html.dart';
import 'package:bot_web/bot_texture.dart';
import 'package:poppopwin/canvas.dart';
import 'package:poppopwin/platform_target.dart';
import 'package:poppopwin/html.dart';

import 'texture_data.dart';

part '_audio.dart';

const String _TRANSPARENT_TEXTURE = 'images/transparent_animated.png';
const String _OPAQUE_TEXTURE = 'images/dart_opaque_01.jpg';
const String _TRANSPARENT_STATIC_TEXTURE = 'images/transparent_static.png';

const int _LOADING_BAR_PX_WIDTH = 398;

DivElement _loadingBar;
ImageLoader _imageLoader;

_Audio _audio;

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

  _audio = new _Audio();
}

void _onProgress(args) {
  int completedBytes = _imageLoader.completedBytes;
  int totalBytes = _imageLoader.totalBytes;

  completedBytes += _audio.completedBytes;
  totalBytes += _audio.totalBytes;

  final percent = completedBytes / totalBytes;
  final percentClean = (percent * 1000).floor() / 10;

  final barWidth = percent * _LOADING_BAR_PX_WIDTH;
  _loadingBar.style.width = '${barWidth.toInt()}px';
}

void _onLoaded(args) {
  if(_imageLoader.state == ResourceLoader.StateLoaded && _audio.done) {

    //
    // load textures
    //
    final opaqueImage = _imageLoader.getResource(_OPAQUE_TEXTURE);
    final transparentImage = _imageLoader.getResource(_TRANSPARENT_TEXTURE);

    // already loaded. Used in CSS.
    final staticTransparentImage = new ImageElement(src: _TRANSPARENT_STATIC_TEXTURE);

    final textures = getTextures(transparentImage, opaqueImage, staticTransparentImage);

    final textureData = new TextureData(textures);

    // run the app
    querySelector('#loading').style.display = 'none';
    _runPPW(textureData);
  }
}

void _runPPW(TextureData textureData) {
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
  if(!(args.toElement is AnchorElement)) {
    targetPlatform.toggleAbout(false);
  }
}

void _onKeyDown(KeyboardEvent args) {
  switch(args.keyCode) {
    case 27: // esc
      targetPlatform.toggleAbout(false);
      break;
    case 72: // h
      targetPlatform.toggleAbout();
      break;
  }
}

void _updateAbout() {
  var popDisplay = targetPlatform.showAbout ? 'inline-block' : 'none';
  querySelector('#popup').style.display = popDisplay;
}
