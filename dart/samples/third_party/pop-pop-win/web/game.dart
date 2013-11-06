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

const String _transparentTextureName = 'images/transparent_animated.png';
const String _opaqueTextureName = 'images/dart_opaque_01.jpg';
const String _transparentStaticTexture = 'images/transparent_static.png';

const int _loadingBarPxWidth = 398;

DivElement _loadingBar;
ImageLoader _imageLoader;

_Audio _audio;

void startGame(PlatformTarget platform) {
  initPlatform(platform);

  _loadingBar = querySelector('.sprite.loading_bar');
  _loadingBar.style.display = 'block';
  _loadingBar.style.width = '0';

  _imageLoader = new ImageLoader([_transparentTextureName,
                                  _opaqueTextureName]);
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

  final barWidth = percent * _loadingBarPxWidth;
  _loadingBar.style.width = '${barWidth.toInt()}px';
}

void _onLoaded(args) {
  if(_imageLoader.state == ResourceLoader.StateLoaded && _audio.done) {

    //
    // load textures
    //
    final opaqueImage = _imageLoader.getResource(_opaqueTextureName);
    final transparentImage = _imageLoader.getResource(_transparentTextureName);

    // already loaded. Used in CSS.
    final staticTransparentImage = new ImageElement(src: _transparentStaticTexture);

    final textures = getTextures(transparentImage, opaqueImage, staticTransparentImage);

    final textureData = new TextureData(textures);

    // run the app
    querySelector('#loading').style.display = 'none';
    _runPPW(textureData);
  }
}

void _runPPW(TextureData textureData) {
  final size = _processUrlHash(false) ? 16 : 7;
  final int m = (size * size * 0.15625).toInt();

  final CanvasElement gameCanvas = querySelector('#gameCanvas');
  gameCanvas.style.userSelect = 'none';

  final gameRoot = new GameRoot(size, size, m, gameCanvas, textureData);

  // disable touch events
  window.onTouchMove.listen((args) => args.preventDefault());
  window.onPopState.listen((args) => _processUrlHash(true));

  window.onKeyDown.listen(_onKeyDown);

  querySelector('#popup').onClick.listen(_onPopupClick);

  titleClickedEvent.listen((args) => _toggleAbout(true));
}

void _onPopupClick(MouseEvent args) {
  if(!(args.toElement is AnchorElement)) {
    _toggleAbout(false);
  }
}

void _onKeyDown(KeyboardEvent args) {
  switch(args.keyCode) {
    case 27: // esc
      _toggleAbout(false);
      break;
    case 72: // h
      _toggleAbout();
      break;
  }
}

void _toggleAbout([bool value = null]) {
  final Location loc = window.location;
  // ensure we treat empty hash like '#', which makes comparison easy later
  final hash = loc.hash.length == 0 ? '#' : loc.hash;

  final isOpen = hash == '#about';
  if(value == null) {
    // then toggle the current value
    value = !isOpen;
  }

  var targetHash = value ? '#about' : '#';
  if(targetHash != hash) {
    loc.assign(targetHash);
  }
}

bool _processUrlHash(bool forceReload) {
  final Location loc = window.location;
  final hash = loc.hash;
  final href = loc.href;

  final History history = window.history;
  bool showAbout = false;
  switch(hash) {
    case "#reset":
      assert(href.endsWith(hash));
      var newLoc = href.substring(0, href.length - hash.length);

      window.localStorage.clear();

      loc.replace(newLoc);
      break;
    case '#big':
      if(forceReload) {
        loc.reload();
      }
      return true;
    case '#about':
      showAbout = true;
      break;
  }

  querySelector('#popup').style.display = showAbout ? 'inline-block' : 'none';

  return false;
}
