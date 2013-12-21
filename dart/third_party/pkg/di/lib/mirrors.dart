library mirrors;

import 'dart:mirrors';
export 'dart:mirrors';

export 'reflected_type.dart';

String getSymbolName(Symbol symbol) => MirrorSystem.getName(symbol);
