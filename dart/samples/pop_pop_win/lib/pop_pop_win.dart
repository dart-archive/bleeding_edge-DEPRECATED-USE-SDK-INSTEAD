library pop_pop_win;

import 'dart:async';
import 'dart:html';

import 'package:stagexl/stagexl.dart';

import 'platform_target.dart';
import 'src/audio.dart';
import 'src/platform.dart';
import 'src/stage.dart';

const String _ASSET_DIR = 'resources/';

const String _TRANSPARENT_TEXTURE = '${_ASSET_DIR}images/transparent.json';
const String _OPAQUE_TEXTURE = '${_ASSET_DIR}images/opaque.json';
const String _TRANSPARENT_STATIC_TEXTURE = '${_ASSET_DIR}images/static.json';

Future startGame(PlatformTarget platform) {
  initPlatform(platform);

  var stage = new Stage(querySelector('#gameCanvas'), webGL: true,
      color: 0xb4ad7f, frameRate: 60);

  var renderLoop = new RenderLoop()
      ..addStage(stage);

  BitmapData.defaultLoadOptions.webp = true;

  //have to load the loading bar first...
  var resourceManager = new ResourceManager()
      ..addTextureAtlas("static", "resources/images/static.json",
          TextureAtlasFormat.JSON);

  return resourceManager.load()
      .then((resMan) => _initialLoad(resMan, stage));
}

void _initialLoad(ResourceManager resourceManager, Stage stage) {
  var atlas = resourceManager.getTextureAtlas('static');

  var bar = new Gauge(atlas.getBitmapData('loading_bar'), Gauge.DIRECTION_RIGHT)
      ..x = 51
      ..y = 8
      ..ratio = 0;

  var loadingText = new Bitmap(atlas.getBitmapData('loading_text'))
      ..x = 141
      ..y = 10;

  var loadingSprite = new Sprite()
      ..addChild(new Bitmap(atlas.getBitmapData('loading_background')))
      ..addChild(bar)
      ..addChild(loadingText)
      ..x = stage.sourceWidth ~/ 2 - 1008 ~/ 2
      ..y = 400
      ..scaleX = 2
      ..scaleY = 2
      ..addTo(stage);

  resourceManager
      ..addTextureAtlas('opaque', 'resources/images/opaque.json',
          TextureAtlasFormat.JSON)
      ..addTextureAtlas('animated', 'resources/images/animated.json',
          TextureAtlasFormat.JSON);

  resourceManager.addSoundSprite('audio', 'resources/audio/audio.json');

  resourceManager.onProgress.listen((e) {
    bar.ratio = resourceManager.finishedResources.length /
        resourceManager.resources.length;
  });

  resourceManager.load().then((resMan) =>
      _secondaryLoad(resMan, stage, loadingSprite));
}

void _secondaryLoad(ResourceManager resourceManager, Stage stage,
    Sprite loadingSprite) {
  var tween = stage.juggler.tween(loadingSprite, .5)
      ..animate.alpha.to(0)
      ..onComplete = () => stage.removeChild(loadingSprite);

  _updateAbout();

  targetPlatform.aboutChanged.listen((_) => _updateAbout());

  var size = targetPlatform.size;
  var m = (size * size * 0.15625).toInt();

  GameAudio.initialize(resourceManager);
  var gameRoot = new GameRoot(size, size, m, stage, resourceManager);

  // disable touch events
  window.onTouchMove.listen((args) => args.preventDefault());

  window.onKeyDown.listen(_onKeyDown);

  querySelector('#popup').onClick.listen(_onPopupClick);

  titleClickedEvent.listen((args) => targetPlatform.toggleAbout(true));
}

void _onPopupClick(args) {
  if (args.toElement is! AnchorElement) {
    targetPlatform.toggleAbout(false);
  }
}

void _onKeyDown(args) {
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
