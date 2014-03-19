library ppw_canvas;

import 'dart:async';
import 'dart:html';
import 'dart:math';
import 'package:bot/bot.dart';
import 'package:bot_web/bot_html.dart';
import 'package:bot_web/bot_retained.dart';
import 'package:bot_web/bot_texture.dart';
import 'html.dart';
import 'poppopwin.dart';

part 'src/canvas/board_element.dart';
part 'src/canvas/game_background_element.dart';
part 'src/canvas/game_element.dart';
part 'src/canvas/game_root.dart';
part 'src/canvas/new_game_element.dart';
part 'src/canvas/score_element.dart';
part 'src/canvas/square_element.dart';
part 'src/canvas/title_element.dart';
part 'src/canvas/game_audio.dart';

final EventHandle _titleClickedEventHandle = new EventHandle<EventArgs>();

Stream get titleClickedEvent => _titleClickedEventHandle.stream;

