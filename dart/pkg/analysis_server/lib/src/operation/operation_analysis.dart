// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library operation.analysis;

import 'package:analysis_server/src/analysis_server.dart';
import 'package:analysis_server/src/computer/computer_highlights.dart';
import 'package:analysis_server/src/computer/computer_navigation.dart';
import 'package:analysis_server/src/computer/computer_occurrences.dart';
import 'package:analysis_server/src/computer/computer_outline.dart';
import 'package:analysis_server/src/computer/computer_overrides.dart';
import 'package:analysis_server/src/operation/operation.dart';
import 'package:analysis_server/src/protocol_server.dart' as protocol;
import 'package:analysis_server/src/services/index/index.dart';
import 'package:analyzer/src/generated/ast.dart';
import 'package:analyzer/src/generated/engine.dart';
import 'package:analyzer/src/generated/error.dart';
import 'package:analyzer/src/generated/html.dart';
import 'package:analyzer/src/generated/source.dart';


void sendAnalysisNotificationErrors(AnalysisServer server, String file,
    LineInfo lineInfo, List<AnalysisError> errors) {
  try {
    if (errors == null) {
      errors = <AnalysisError>[];
    }
    var serverErrors =
        protocol.doAnalysisError_listFromEngine(lineInfo, errors);
    var params = new protocol.AnalysisErrorsParams(file, serverErrors);
    server.sendNotification(params.toNotification());
  } catch (exception, stackTrace) {
    server.sendServerErrorNotification(exception, stackTrace);
  }
}


void sendAnalysisNotificationHighlights(AnalysisServer server, String file,
    CompilationUnit dartUnit) {
  try {
    var regions = new DartUnitHighlightsComputer(dartUnit).compute();
    var params = new protocol.AnalysisHighlightsParams(file, regions);
    server.sendNotification(params.toNotification());
  } catch (exception, stackTrace) {
    server.sendServerErrorNotification(exception, stackTrace);
  }
}


void sendAnalysisNotificationNavigation(AnalysisServer server, String file,
    CompilationUnit dartUnit) {
  try {
    var computer = new DartUnitNavigationComputer(dartUnit);
    computer.compute();
    var params = new protocol.AnalysisNavigationParams(
        file,
        computer.regions,
        computer.targets,
        computer.files);
    server.sendNotification(params.toNotification());
  } catch (exception, stackTrace) {
    server.sendServerErrorNotification(exception, stackTrace);
  }
}


void sendAnalysisNotificationOccurrences(AnalysisServer server, String file,
    CompilationUnit dartUnit) {
  try {
    var occurrences = new DartUnitOccurrencesComputer(dartUnit).compute();
    var params = new protocol.AnalysisOccurrencesParams(file, occurrences);
    server.sendNotification(params.toNotification());
  } catch (exception, stackTrace) {
    server.sendServerErrorNotification(exception, stackTrace);
  }
}


void sendAnalysisNotificationOutline(AnalysisServer server, String file,
    LineInfo lineInfo, CompilationUnit dartUnit) {
  try {
    var computer = new DartUnitOutlineComputer(file, lineInfo, dartUnit);
    var outline = computer.compute();
    var params = new protocol.AnalysisOutlineParams(file, outline);
    server.sendNotification(params.toNotification());
  } catch (exception, stackTrace) {
    server.sendServerErrorNotification(exception, stackTrace);
  }
}


void sendAnalysisNotificationOverrides(AnalysisServer server, String file,
    CompilationUnit dartUnit) {
  try {
    var overrides = new DartUnitOverridesComputer(dartUnit).compute();
    var params = new protocol.AnalysisOverridesParams(file, overrides);
    server.sendNotification(params.toNotification());
  } catch (exception, stackTrace) {
    server.sendServerErrorNotification(exception, stackTrace);
  }
}


/**
 * Instances of [PerformAnalysisOperation] perform a single analysis task.
 */
class PerformAnalysisOperation extends ServerOperation {
  static const int IDLE_CACHE_SIZE = AnalysisOptionsImpl.DEFAULT_CACHE_SIZE;
  static const int WORKING_CACHE_SIZE = 512;

  final AnalysisContext context;
  final bool isPriority;
  final bool isContinue;

  PerformAnalysisOperation(this.context, this.isPriority, this.isContinue);

  @override
  ServerOperationPriority get priority {
    if (isPriority) {
      if (isContinue) {
        return ServerOperationPriority.PRIORITY_ANALYSIS_CONTINUE;
      } else {
        return ServerOperationPriority.PRIORITY_ANALYSIS;
      }
    } else {
      if (isContinue) {
        return ServerOperationPriority.ANALYSIS_CONTINUE;
      } else {
        return ServerOperationPriority.ANALYSIS;
      }
    }
  }

  @override
  void perform(AnalysisServer server) {
    //
    // TODO(brianwilkerson) Add an optional function-valued parameter to
    // performAnalysisTask that will be called when the task has been computed
    // but before it is performed and send notification in the function:
    //
    // AnalysisResult result = context.performAnalysisTask((taskDescription) {
    //   sendStatusNotification(context.toString(), taskDescription);
    // });
    if (!isContinue) {
      _setCacheSize(WORKING_CACHE_SIZE);
    }
    // prepare results
    AnalysisResult result = context.performAnalysisTask();
    List<ChangeNotice> notices = result.changeNotices;
    if (notices == null) {
      _setCacheSize(IDLE_CACHE_SIZE);
      server.sendContextAnalysisDoneNotifications(
          context,
          AnalysisDoneReason.COMPLETE);
      return;
    }
    // process results
    _sendNotices(server, notices);
    _updateIndex(server, notices);
    // continue analysis
    server.addOperation(
        new PerformAnalysisOperation(context, isPriority, true));
  }

  /**
   * Send the information in the given list of notices back to the client.
   */
  void _sendNotices(AnalysisServer server, List<ChangeNotice> notices) {
    for (int i = 0; i < notices.length; i++) {
      ChangeNotice notice = notices[i];
      Source source = notice.source;
      String file = source.fullName;
      // Dart
      CompilationUnit dartUnit = notice.compilationUnit;
      if (dartUnit != null) {
        if (server.hasAnalysisSubscription(
            protocol.AnalysisService.HIGHLIGHTS,
            file)) {
          server.addOperation(new _DartHighlightsOperation(file, dartUnit));
        }
        if (server.hasAnalysisSubscription(
            protocol.AnalysisService.NAVIGATION,
            file)) {
          server.addOperation(new _DartNavigationOperation(file, dartUnit));
        }
        if (server.hasAnalysisSubscription(
            protocol.AnalysisService.OCCURRENCES,
            file)) {
          server.addOperation(new _DartOccurrencesOperation(file, dartUnit));
        }
        if (server.hasAnalysisSubscription(
            protocol.AnalysisService.OUTLINE,
            file)) {
          LineInfo lineInfo = notice.lineInfo;
          server.addOperation(
              new _DartOutlineOperation(file, lineInfo, dartUnit));
        }
        if (server.hasAnalysisSubscription(
            protocol.AnalysisService.OVERRIDES,
            file)) {
          server.addOperation(new _DartOverridesOperation(file, dartUnit));
        }
      }
      if (server.shouldSendErrorsNotificationFor(file)) {
        server.addOperation(
            new _NotificationErrorsOperation(file, notice.lineInfo, notice.errors));
      }
      server.fileAnalyzed(notice);
    }
  }

  void _setCacheSize(int cacheSize) {
    AnalysisOptionsImpl options =
        new AnalysisOptionsImpl.con1(context.analysisOptions);
    options.cacheSize = cacheSize;
    context.analysisOptions = options;
  }

  void _updateIndex(AnalysisServer server, List<ChangeNotice> notices) {
    Index index = server.index;
    if (index == null) {
      return;
    }
    for (ChangeNotice notice in notices) {
      // Dart
      try {
        CompilationUnit dartUnit = notice.compilationUnit;
        if (dartUnit != null) {
          index.indexUnit(context, dartUnit);
        }
      } catch (exception, stackTrace) {
        server.sendServerErrorNotification(exception, stackTrace);
      }
      // HTML
      try {
        HtmlUnit htmlUnit = notice.htmlUnit;
        if (htmlUnit != null) {
          index.indexHtmlUnit(context, htmlUnit);
        }
      } catch (exception, stackTrace) {
        server.sendServerErrorNotification(exception, stackTrace);
      }
    }
  }
}


class _DartHighlightsOperation extends _DartNotificationOperation {
  _DartHighlightsOperation(String file, CompilationUnit unit)
      : super(file, unit);

  @override
  void perform(AnalysisServer server) {
    sendAnalysisNotificationHighlights(server, file, unit);
  }
}


class _DartNavigationOperation extends _DartNotificationOperation {
  _DartNavigationOperation(String file, CompilationUnit unit)
      : super(file, unit);

  @override
  void perform(AnalysisServer server) {
    sendAnalysisNotificationNavigation(server, file, unit);
  }
}


abstract class _DartNotificationOperation extends ServerOperation {
  final String file;
  final CompilationUnit unit;

  _DartNotificationOperation(this.file, this.unit);

  @override
  ServerOperationPriority get priority {
    return ServerOperationPriority.ANALYSIS_NOTIFICATION;
  }
}


class _DartOccurrencesOperation extends _DartNotificationOperation {
  _DartOccurrencesOperation(String file, CompilationUnit unit)
      : super(file, unit);

  @override
  void perform(AnalysisServer server) {
    sendAnalysisNotificationOccurrences(server, file, unit);
  }
}


class _DartOutlineOperation extends _DartNotificationOperation {
  final LineInfo lineInfo;

  _DartOutlineOperation(String file, this.lineInfo, CompilationUnit unit)
      : super(file, unit);

  @override
  void perform(AnalysisServer server) {
    sendAnalysisNotificationOutline(server, file, lineInfo, unit);
  }
}


class _DartOverridesOperation extends _DartNotificationOperation {
  _DartOverridesOperation(String file, CompilationUnit unit)
      : super(file, unit);

  @override
  void perform(AnalysisServer server) {
    sendAnalysisNotificationOverrides(server, file, unit);
  }
}


class _NotificationErrorsOperation extends ServerOperation {
  final String file;
  final LineInfo lineInfo;
  final List<AnalysisError> errors;

  _NotificationErrorsOperation(this.file, this.lineInfo, this.errors);

  @override
  ServerOperationPriority get priority {
    return ServerOperationPriority.ANALYSIS_NOTIFICATION;
  }

  @override
  void perform(AnalysisServer server) {
    sendAnalysisNotificationErrors(server, file, lineInfo, errors);
  }
}
