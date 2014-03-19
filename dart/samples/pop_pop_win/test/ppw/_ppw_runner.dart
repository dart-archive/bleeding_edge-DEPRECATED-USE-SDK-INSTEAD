library ppw_test;

import 'dart:math';
import 'package:bot/bot.dart';
import 'package:pop_pop_win/poppopwin.dart';
import 'package:unittest/unittest.dart';

part 'test_field.dart';
part 'test_game.dart';

runppwTests() {
  group('ppw', (){
    TestField.run();
    TestGame.run();
  });
}
