// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
library browser;

import "dart:async";
import "dart:convert" show LineSplitter, UTF8;
import "dart:core";
import "dart:io";

import 'android.dart';
import 'utils.dart';

class BrowserOutput {
  final StringBuffer stdout = new StringBuffer();
  final StringBuffer stderr = new StringBuffer();
  final StringBuffer eventLog = new StringBuffer();
}

/** Class describing the interface for communicating with browsers. */
abstract class Browser {
  BrowserOutput _allBrowserOutput = new BrowserOutput();
  BrowserOutput _testBrowserOutput = new BrowserOutput();

  // This is called after the process is closed, before the done future
  // is completed.
  // Subclasses can use this to cleanup any browser specific resources
  // (temp directories, profiles, etc). The function is expected to do
  // it's work synchronously.
  Function _cleanup;

  /** The version of the browser - normally set when starting a browser */
  String version = "";

  // The path to the browser executable.
  String _binary;

  /**
   * The underlying process - don't mess directly with this if you don't
   * know what you are doing (this is an interactive process that needs
   * special threatment to not leak).
   */
  Process process;

  Function logger;

  /**
   * Id of the browser
   */
  String id;

  /** Print everything (stdout, stderr, usageLog) whenever we add to it */
  bool debugPrint = false;

  // This future returns when the process exits. It is also the return value
  // of close()
  Future done;

  Browser();

  factory Browser.byName(String name,
                         String executablePath,
                         [bool checkedMode = false]) {
    var browser;
    if (name == 'firefox') {
      browser = new Firefox();
    } else if (name == 'chrome') {
      browser = new Chrome();
    } else if (name == 'dartium') {
      browser = new Dartium(checkedMode);
    } else if (name == 'safari') {
      browser = new Safari();
    } else if (name.startsWith('ie')) {
      browser = new IE();
    } else {
      throw "Non supported browser";
    }
    browser._binary = executablePath;
    return browser;
  }

  static const List<String> SUPPORTED_BROWSERS =
    const ['safari', 'ff', 'firefox', 'chrome', 'ie9', 'ie10',
           'ie11', 'dartium'];

  static const List<String> BROWSERS_WITH_WINDOW_SUPPORT = const [];

  // TODO(kustermann): add standard support for chrome on android
  static bool supportedBrowser(String name) {
    return SUPPORTED_BROWSERS.contains(name);
  }

  void _logEvent(String event) {
    String toLog = "$this ($id) - $event \n";
    if (debugPrint) print("usageLog: $toLog");
    if (logger != null) logger(toLog);

    _allBrowserOutput.eventLog.write(toLog);
    _testBrowserOutput.eventLog.write(toLog);
  }

  void _addStdout(String output) {
    if (debugPrint) print("stdout: $output");

    _allBrowserOutput.stdout.write(output);
    _testBrowserOutput.stdout.write(output);
  }

  void _addStderr(String output) {
    if (debugPrint) print("stderr: $output");

    _allBrowserOutput.stderr.write(output);
    _testBrowserOutput.stderr.write(output);
  }

  Future close() {
    _logEvent("Close called on browser");
    if (process != null) {
      if (process.kill(ProcessSignal.SIGKILL)) {
        _logEvent("Successfully sent kill signal to process.");
      } else {
        _logEvent("Sending kill signal failed.");
      }
      return done;
    } else {
      _logEvent("The process is already dead.");
      return new Future.value(true);
    }
  }

  /**
   * Start the browser using the supplied argument.
   * This sets up the error handling and usage logging.
   */
  Future<bool> startBrowser(String command,
                            List<String> arguments,
                            {Map<String,String> environment}) {
    return Process.start(command, arguments, environment: environment)
        .then((startedProcess) {
      process = startedProcess;
      // Used to notify when exiting, and as a return value on calls to
      // close().
      var doneCompleter = new Completer();
      done = doneCompleter.future;

      Completer stdoutDone = new Completer();
      Completer stderrDone = new Completer();

      bool stdoutIsDone = false;
      bool stderrIsDone = false;
      StreamSubscription stdoutSubscription;
      StreamSubscription stderrSubscription;

      // This timer is used to close stdio to the subprocess once we got
      // the exitCode. Sometimes descendants of the subprocess keep stdio
      // handles alive even though the direct subprocess is dead.
      Timer watchdogTimer;

      void closeStdout([_]){
        if (!stdoutIsDone) {
          stdoutDone.complete();
          stdoutIsDone = true;

          if (stderrIsDone && watchdogTimer != null) {
            watchdogTimer.cancel();
          }
        }
      }

      void closeStderr([_]) {
        if (!stderrIsDone) {
          stderrDone.complete();
          stderrIsDone = true;

          if (stdoutIsDone && watchdogTimer != null) {
            watchdogTimer.cancel();
          }
        }
      }

      stdoutSubscription =
        process.stdout.transform(UTF8.decoder).listen((data) {
        _addStdout(data);
      }, onError: (error) {
        // This should _never_ happen, but we really want this in the log
        // if it actually does due to dart:io or vm bug.
        _logEvent("An error occured in the process stdout handling: $error");
      }, onDone: closeStdout);

      stderrSubscription =
        process.stderr.transform(UTF8.decoder).listen((data) {
        _addStderr(data);
      }, onError: (error) {
        // This should _never_ happen, but we really want this in the log
        // if it actually does due to dart:io or vm bug.
        _logEvent("An error occured in the process stderr handling: $error");
      },  onDone: closeStderr);

      process.exitCode.then((exitCode) {
        _logEvent("Browser closed with exitcode $exitCode");

        if (!stdoutIsDone || !stderrIsDone) {
          watchdogTimer = new Timer(MAX_STDIO_DELAY, () {
            DebugLogger.warning(
                "$MAX_STDIO_DELAY_PASSED_MESSAGE (browser: $this)");
            watchdogTimer = null;
            stdoutSubscription.cancel();
            stderrSubscription.cancel();
            closeStdout();
            closeStderr();
          });
        }

        Future.wait([stdoutDone.future, stderrDone.future]).then((_) {
          process = null;
          if (_cleanup != null) {
            _cleanup();
          }
        }).catchError((error) {
          _logEvent("Error closing browsers: $error");
        }).whenComplete(() => doneCompleter.complete(true));
      });
      return true;
    }).catchError((error) {
      _logEvent("Running $command $arguments failed with $error");
      return false;
    });
  }

  /**
   * Get the output that was written so far to stdout/stderr/eventLog.
   */
  BrowserOutput get allBrowserOutput => _allBrowserOutput;
  BrowserOutput get testBrowserOutput => _testBrowserOutput;

  void resetTestBrowserOutput() {
    _testBrowserOutput = new BrowserOutput();
  }

  String toString();

  /** Starts the browser loading the given url */
  Future<bool> start(String url);
}

class Safari extends Browser {
  /**
   * We get the safari version by parsing a version file
   */
  static const String versionFile =
      "/Applications/Safari.app/Contents/version.plist";

  /**
   * Directories where safari stores state. We delete these if the deleteCache
   * is set
   */
  static const List<String> CACHE_DIRECTORIES =
      const ["Library/Caches/com.apple.Safari",
             "Library/Safari",
             "Library/Saved Application State/com.apple.Safari.savedState",
             "Library/Caches/Metadata/Safari"];


  Future<bool> allowPopUps() {
    var command = "defaults";
    var args = ["write", "com.apple.safari",
                "com.apple.Safari.ContentPageGroupIdentifier."
                "WebKit2JavaScriptCanOpenWindowsAutomatically",
                "1"];
    return Process.run(command, args).then((result) {
      if (result.exitCode != 0) {
        _logEvent("Could not disable pop-up blocking for safari");
        return false;
      }
      return true;
    });
  }

  Future<bool> deleteIfExists(Iterator<String> paths) {
    if (!paths.moveNext()) return new Future.value(true);
    Directory directory = new Directory(paths.current);
    return directory.exists().then((exists) {
      if (exists) {
        _logEvent("Deleting ${paths.current}");
        return directory.delete(recursive: true)
	    .then((_) => deleteIfExists(paths))
	    .catchError((error) {
	      _logEvent("Failure trying to delete ${paths.current}: $error");
	      return false;
	    });
      } else {
        _logEvent("${paths.current} is not present");
        return deleteIfExists(paths);
      }
    });
  }

  // Clears the cache if the static deleteCache flag is set.
  // Returns false if the command to actually clear the cache did not complete.
  Future<bool> clearCache() {
    if (!deleteCache) return new Future.value(true);
    var home = Platform.environment['HOME'];
    Iterator iterator = CACHE_DIRECTORIES.map((s) => "$home/$s").iterator;
    return deleteIfExists(iterator);
  }

  Future<String> getVersion() {
    /**
     * Example of the file:
     * <?xml version="1.0" encoding="UTF-8"?>
     * <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
     * <plist version="1.0">
     * <dict>
     *	     <key>BuildVersion</key>
     * 	     <string>2</string>
     * 	     <key>CFBundleShortVersionString</key>
     * 	     <string>6.0.4</string>
     * 	     <key>CFBundleVersion</key>
     * 	     <string>8536.29.13</string>
     * 	     <key>ProjectName</key>
     * 	     <string>WebBrowser</string>
     * 	     <key>SourceVersion</key>
     * 	     <string>7536029013000000</string>
     * </dict>
     * </plist>
     */
    File f = new File(versionFile);
    return f.readAsLines().then((content) {
      bool versionOnNextLine = false;
      for (var line in content) {
        if (versionOnNextLine) return line;
        if (line.contains("CFBundleShortVersionString")) {
          versionOnNextLine = true;
        }
      }
      return null;
    });
  }

  void _createLaunchHTML(var path, var url) {
    var file = new File("${path}/launch.html");
    var randomFile = file.openSync(mode: FileMode.WRITE);
    var content = '<script language="JavaScript">location = "$url"</script>';
    randomFile.writeStringSync(content);
    randomFile.close();
  }

  Future<bool> start(String url) {
    _logEvent("Starting Safari browser on: $url");
    return allowPopUps().then((success) {
      if (!success) {
        return false;
      }
      return clearCache().then((cleared) {
        if (!cleared) {
          _logEvent("Could not clear cache");
          return false;
        }
        // Get the version and log that.
        return getVersion().then((version) {
          _logEvent("Got version: $version");
          return Directory.systemTemp.createTemp().then((userDir) {
            _cleanup = () { userDir.deleteSync(recursive: true); };
            _createLaunchHTML(userDir.path, url);
            var args = ["${userDir.path}/launch.html"];
            return startBrowser(_binary, args);
          });
        }).catchError((error) {
          _logEvent("Running $_binary --version failed with $error");
          return false;
        });
      });
    });
  }

  String toString() => "Safari";

  // Delete the user specific browser cache and profile data.
  // Safari only have one per user, and you can't specify one by command line.
  static bool deleteCache = false;

}


class Chrome extends Browser {
  String _version = "Version not found yet";

  Map<String, String> _getEnvironment() => null;

  Future<bool> _getVersion() {
    if (Platform.isWindows) {
      // The version flag does not work on windows.
      // See issue:
      // https://code.google.com/p/chromium/issues/detail?id=158372
      // The registry hack does not seem to work.
      _version = "Can't get version on windows";
      // We still validate that the binary exists so that we can give good
      // feedback.
      return new File(_binary).exists().then((exists) {
        if (!exists) {
          _logEvent("Chrome binary not available.");
          _logEvent("Make sure $_binary is a valid program for running chrome");
        }
        return exists;
      });
    }
    return Process.run(_binary, ["--version"]).then((var versionResult) {
      if (versionResult.exitCode != 0) {
        _logEvent("Failed to chrome get version");
        _logEvent("Make sure $_binary is a valid program for running chrome");
        return false;
      }
      _version = versionResult.stdout;
      return true;
    });
  }


  Future<bool> start(String url) {
    _logEvent("Starting chrome browser on: $url");
    // Get the version and log that.
    return _getVersion().then((success) {
      if (!success) return false;
      _logEvent("Got version: $_version");

      return Directory.systemTemp.createTemp().then((userDir) {
        _cleanup = () { userDir.deleteSync(recursive: true); };
        var args = ["--user-data-dir=${userDir.path}", url,
                    "--disable-extensions", "--disable-popup-blocking",
                    "--bwsi", "--no-first-run"];
        return startBrowser(_binary, args, environment: _getEnvironment());
      });
    }).catchError((e) {
      _logEvent("Running $_binary --version failed with $e");
      return false;
    });
  }

  String toString() => "Chrome";
}

class Dartium extends Chrome {
  final bool checkedMode;

  Dartium(this.checkedMode);

  Map<String, String> _getEnvironment() {
    var environment = new Map<String,String>.from(Platform.environment);
    // By setting this environment variable, dartium will forward "print()"
    // calls in dart to the top-level javascript function "dartPrint()" if
    // available.
    environment['DART_FORWARDING_PRINT'] = '1';
    if (checkedMode) {
      environment['DART_FLAGS'] = '--checked';
    }
    return environment;
  }

  String toString() => "Dartium";
}

class IE extends Browser {
  Future<String> getVersion() {
    var args = ["query",
                "HKEY_LOCAL_MACHINE\\Software\\Microsoft\\Internet Explorer",
                "/v",
                "svcVersion"];
    return Process.run("reg", args).then((result) {
      if (result.exitCode == 0) {
        // The string we get back looks like this:
        // HKEY_LOCAL_MACHINE\Software\Microsoft\Internet Explorer
        //    version    REG_SZ    9.0.8112.16421
        var findString = "REG_SZ";
        var index = result.stdout.indexOf(findString);
        if (index > 0) {
          return result.stdout.substring(index + findString.length).trim();
        }
      }
      return "Could not get the version of internet explorer";
    });
  }

  Future<bool> start(String url) {
    _logEvent("Starting ie browser on: $url");
    return getVersion().then((version) {
      _logEvent("Got version: $version");
      return startBrowser(_binary, [url]);
    });
  }
  String toString() => "IE";
}


class AndroidBrowserConfig {
  final String name;
  final String package;
  final String activity;
  final String action;
  AndroidBrowserConfig(this.name, this.package, this.activity, this.action);
}


final contentShellOnAndroidConfig = new AndroidBrowserConfig(
    'ContentShellOnAndroid',
    'org.chromium.content_shell_apk',
    '.ContentShellActivity',
    'android.intent.action.VIEW');


final dartiumOnAndroidConfig = new AndroidBrowserConfig(
    'DartiumOnAndroid',
    'com.google.android.apps.chrome',
    '.Main',
    'android.intent.action.VIEW');


class AndroidBrowser extends Browser {
  final bool checkedMode;
  AdbDevice _adbDevice;
  AndroidBrowserConfig _config;

  AndroidBrowser(this._adbDevice, this._config, this.checkedMode);

  Future<bool> start(String url) {
    var intent = new Intent(
        _config.action, _config.package, _config.activity, url);
    return _adbDevice.waitForBootCompleted().then((_) {
      return _adbDevice.forceStop(_config.package);
    }).then((_) {
      return _adbDevice.killAll();
    }).then((_) {
      return _adbDevice.adbRoot();
    }).then((_) {
      return _adbDevice.setProp("DART_FORWARDING_PRINT", "1");
    }).then((_) {
      if (checkedMode) {
        return _adbDevice.setProp("DART_FLAGS", "--checked");
      } else {
        return _adbDevice.setProp("DART_FLAGS", "");
      }
    }).then((_) {
      return _adbDevice.startActivity(intent).then((_) => true);
    });
  }

  Future<bool> close() {
    if (_adbDevice != null) {
      return _adbDevice.forceStop(_config.package).then((_) {
        return _adbDevice.killAll().then((_) => true);
      });
    }
    return new Future.value(true);
  }

  String toString() => _config.name;
}


class AndroidChrome extends Browser {
  static const String viewAction = 'android.intent.action.VIEW';
  static const String mainAction = 'android.intent.action.MAIN';
  static const String chromePackage = 'com.android.chrome';
  static const String browserPackage = 'com.android.browser';
  static const String firefoxPackage = 'org.mozilla.firefox';
  static const String turnScreenOnPackage = 'com.google.dart.turnscreenon';

  AndroidEmulator _emulator;
  AdbDevice _adbDevice;

  AndroidChrome(this._adbDevice);

  Future<bool> start(String url) {
    var browserIntent = new Intent(
        viewAction, browserPackage, '.BrowserActivity', url);
    var chromeIntent = new Intent(viewAction, chromePackage, '.Main', url);
    var firefoxIntent = new Intent(viewAction, firefoxPackage, '.App', url);
    var turnScreenOnIntent =
        new Intent(mainAction, turnScreenOnPackage, '.Main');

    var testing_resources_dir =
        new Path('third_party/android_testing_resources');
    if (!new Directory(testing_resources_dir.toNativePath()).existsSync()) {
      DebugLogger.error("$testing_resources_dir doesn't exist. Exiting now.");
      exit(1);
    }

    var chromeAPK = testing_resources_dir.append('com.android.chrome-1.apk');
    var turnScreenOnAPK = testing_resources_dir.append('TurnScreenOn.apk');
    var chromeConfDir = testing_resources_dir.append('chrome_configuration');
    var chromeConfDirRemote = new Path('/data/user/0/com.android.chrome/');

    return _adbDevice.waitForBootCompleted().then((_) {
      return _adbDevice.forceStop(chromeIntent.package);
    }).then((_) {
      return _adbDevice.killAll();
    }).then((_) {
      return _adbDevice.adbRoot();
    }).then((_) {
      return _adbDevice.installApk(turnScreenOnAPK);
    }).then((_) {
      return _adbDevice.installApk(chromeAPK);
    }).then((_) {
      return _adbDevice.pushData(chromeConfDir, chromeConfDirRemote);
    }).then((_) {
      return _adbDevice.chmod('777', chromeConfDirRemote);
    }).then((_) {
      return _adbDevice.startActivity(turnScreenOnIntent).then((_) => true);
    }).then((_) {
      return _adbDevice.startActivity(chromeIntent).then((_) => true);
    });
  }

  Future<bool> close() {
    if (_adbDevice != null) {
      return _adbDevice.forceStop(chromePackage).then((_) {
        return _adbDevice.killAll().then((_) => true);
      });
    }
    return new Future.value(true);
  }

  String toString() => "chromeOnAndroid";
}


class Firefox extends Browser {
  static const String enablePopUp =
      'user_pref("dom.disable_open_during_load", false);';
  static const String disableDefaultCheck =
      'user_pref("browser.shell.checkDefaultBrowser", false);';
  static const String disableScriptTimeLimit =
      'user_pref("dom.max_script_run_time", 0);';

  Future _createPreferenceFile(var path) {
    var file = new File("${path.toString()}/user.js");
    var randomFile = file.openSync(mode: FileMode.WRITE);
    randomFile.writeStringSync(enablePopUp);
    randomFile.writeStringSync(disableDefaultCheck);
    randomFile.writeStringSync(disableScriptTimeLimit);
    randomFile.close();
  }

  Future<bool> start(String url) {
    _logEvent("Starting firefox browser on: $url");
    // Get the version and log that.
    return Process.run(_binary, ["--version"]).then((var versionResult) {
      if (versionResult.exitCode != 0) {
        _logEvent("Failed to firefox get version");
        _logEvent("Make sure $_binary is a valid program for running firefox");
        return new Future.value(false);
      }
      version = versionResult.stdout;
      _logEvent("Got version: $version");

      return Directory.systemTemp.createTemp().then((userDir) {
        _createPreferenceFile(userDir.path);
        _cleanup = () { userDir.deleteSync(recursive: true); };
        var args = ["-profile", "${userDir.path}",
                    "-no-remote", "-new-instance", url];
        return startBrowser(_binary, args);

      });
    }).catchError((e) {
      _logEvent("Running $_binary --version failed with $e");
      return false;
    });
  }

  String toString() => "Firefox";
}


/**
 * Describes the current state of a browser used for testing.
 */
class BrowserTestingStatus {
  Browser browser;
  BrowserTest currentTest;

  // This is currently not used for anything except for error reporting.
  // Given the usefulness of this in debugging issues this should not be
  // removed even when we have really stable system.
  BrowserTest lastTest;
  bool timeout = false;
  BrowserTestingStatus(Browser this.browser);
}


/**
 * Describes a single test to be run int the browser.
 */
class BrowserTest {
  // TODO(ricow): Add timeout callback instead of the string passing hack.
  Function doneCallback;
  String url;
  int timeout;
  String lastKnownMessage = '';
  Stopwatch stopwatch;

  // This might be null
  Duration delayUntilTestStarted;

  // We store this here for easy access when tests time out (instead of
  // capturing this in a closure)
  Timer timeoutTimer;

  // Used for debugging, this is simply a unique identifier assigned to each
  // test.
  int id;
  static int _idCounter = 0;

  BrowserTest(this.url, this.doneCallback, this.timeout) {
    id = _idCounter++;
  }
}

/* Describes the output of running the test in a browser */
class BrowserTestOutput {
  final Duration delayUntilTestStarted;
  final Duration duration;

  final String lastKnownMessage;

  final BrowserOutput browserOutput;
  final bool didTimeout;

  BrowserTestOutput(
      this.delayUntilTestStarted, this.duration, this.lastKnownMessage,
      this.browserOutput, {this.didTimeout: false});
}

/**
 * Encapsulates all the functionality for running tests in browsers.
 * The interface is rather simple. After starting, the runner tests
 * are simply added to the queue and a the supplied callbacks are called
 * whenever a test completes.
 */
class BrowserTestRunner {
  final Map globalConfiguration;
  final bool checkedMode; // needed for dartium

  String localIp;
  String browserName;
  int maxNumBrowsers;
  // Used to send back logs from the browser (start, stop etc)
  Function logger;
  int browserIdCount = 0;

  bool underTermination = false;

  List<BrowserTest> testQueue = new List<BrowserTest>();
  Map<String, BrowserTestingStatus> browserStatus =
      new Map<String, BrowserTestingStatus>();

  var adbDeviceMapping = new Map<String, AdbDevice>();
  // This cache is used to guarantee that we never see double reporting.
  // If we do we need to provide developers with this information.
  // We don't add urls to the cache until we have run it.
  Map<int, String> testCache = new Map<int, String>();
  Map<int, String> doubleReportingOutputs = new Map<int, String>();

  BrowserTestingServer testingServer;

  /**
   * The TestRunner takes the testingServer in as a constructor parameter in
   * case we wish to have a testing server with different behavior (such as the
   * case for performance testing.
   */
  BrowserTestRunner(this.globalConfiguration,
                    this.localIp,
                    this.browserName,
                    this.maxNumBrowsers,
                    {bool this.checkedMode: false,
                    BrowserTestingServer this.testingServer});

  Future<bool> start() {
    // If [browserName] doesn't support opening new windows, we use new iframes
    // instead.
    bool useIframe =
        !Browser.BROWSERS_WITH_WINDOW_SUPPORT.contains(browserName);
    if (testingServer == null) {
      testingServer = new BrowserTestingServer(
          globalConfiguration, localIp, useIframe);
    }
    return testingServer.start().then((_) {
      testingServer.testDoneCallBack = handleResults;
      testingServer.testStatusUpdateCallBack = handleStatusUpdate;
      testingServer.testStartedCallBack = handleStarted;
      testingServer.nextTestCallBack = getNextTest;
      return getBrowsers().then((browsers) {
        var futures = [];
        for (var browser in browsers) {
          var url = testingServer.getDriverUrl(browser.id);
          var future = browser.start(url).then((success) {
            if (success) {
              browserStatus[browser.id] = new BrowserTestingStatus(browser);
            }
            return success;
          });
          futures.add(future);
        }
        return Future.wait(futures).then((values) {
          return !values.contains(false);
        });
      });
    });
  }

  Future<List<Browser>> getBrowsers() {
    // TODO(kustermann): This is a hackisch way to accomplish it and should
    // be encapsulated
    var browsersCompleter = new Completer();
    var androidBrowserCreationMapping = {
      'chromeOnAndroid' : (AdbDevice device) => new AndroidChrome(device),
      'ContentShellOnAndroid' : (AdbDevice device) =>
          new AndroidBrowser(device, contentShellOnAndroidConfig, checkedMode),
      'DartiumOnAndroid' : (AdbDevice device) =>
          new AndroidBrowser(device, dartiumOnAndroidConfig, checkedMode),
    };
    if (androidBrowserCreationMapping.containsKey(browserName)) {
      AdbHelper.listDevices().then((deviceIds) {
        if (deviceIds.length > 0) {
          var browsers = [];
          for (int i = 0; i < deviceIds.length; i++) {
            var id = "BROWSER$i";
            var device = new AdbDevice(deviceIds[i]);
            adbDeviceMapping[id] = device;
            var browser = androidBrowserCreationMapping[browserName](device);
            browsers.add(browser);
            // We store this in case we need to kill the browser.
            browser.id = id;
          }
          browsersCompleter.complete(browsers);
        } else {
          throw new StateError("No android devices found.");
        }
      });
    } else {
      var browsers = [];
      for (int i = 0; i < maxNumBrowsers; i++) {
        var id = "BROWSER$browserIdCount";
        browserIdCount++;
        var browser = getInstance();
        browsers.add(browser);
        // We store this in case we need to kill the browser.
        browser.id = id;
      }
      browsersCompleter.complete(browsers);
    }
    return browsersCompleter.future;
  }

  var timedOut = [];

  void handleResults(String browserId, String output, int testId) {
    var status = browserStatus[browserId];
    if (testCache.containsKey(testId)) {
      doubleReportingOutputs[testId] = output;
      return;
    }

    if (status == null || status.timeout) {
      // We don't do anything, this browser is currently being killed and
      // replaced. The browser here can be null if we decided to kill the
      // browser.
    } else if (status.currentTest != null) {
      status.currentTest.timeoutTimer.cancel();
      status.currentTest.stopwatch.stop();

      if (status.currentTest.id != testId) {
        print("Expected test id ${status.currentTest.id} for"
              "${status.currentTest.url}");
        print("Got test id ${testId}");
        print("Last test id was ${status.lastTest.id} for "
              "${status.currentTest.url}");
        throw("This should never happen, wrong test id");
      }
      testCache[testId] = status.currentTest.url;
      Stopwatch watch = new Stopwatch()..start();

      // Report that the test is finished now
      var browserTestOutput = new BrowserTestOutput(
          status.currentTest.delayUntilTestStarted,
          status.currentTest.stopwatch.elapsed,
          output,
          status.browser.testBrowserOutput);
      status.currentTest.doneCallback(browserTestOutput);

      watch.stop();
      status.lastTest = status.currentTest;
      status.currentTest = null;
    } else {
      print("\nThis is bad, should never happen, handleResult no test");
      print("URL: ${status.lastTest.url}");
      print(output);
      terminate().then((_) {
        exit(1);
      });
    }
  }

  void handleStatusUpdate(String browserId, String output, int testId) {
    var status = browserStatus[browserId];

    if (status == null || status.timeout) {
      // We don't do anything, this browser is currently being killed and
      // replaced. The browser here can be null if we decided to kill the
      // browser.
    } else if (status.currentTest != null && status.currentTest.id == testId) {
      status.currentTest.lastKnownMessage = output;
    }
  }

  void handleStarted(String browserId, String output, int testId) {
    var status = browserStatus[browserId];

    if (status != null && !status.timeout && status.currentTest != null) {
      status.currentTest.timeoutTimer.cancel();
      status.currentTest.timeoutTimer =
          createTimeoutTimer(status.currentTest, status);
      status.currentTest.delayUntilTestStarted =
          status.currentTest.stopwatch.elapsed;
    }
  }

  void handleTimeout(BrowserTestingStatus status) {
    // We simply kill the browser and starts up a new one!
    // We could be smarter here, but it does not seems like it is worth it.
    status.timeout = true;
    timedOut.add(status.currentTest.url);
    var id = status.browser.id;

    status.currentTest.stopwatch.stop();
    status.browser.close().then((_) {
      var lastKnownMessage =
          'Dom could not be fetched, since the test timed out.';
      if (status.currentTest.lastKnownMessage.length > 0) {
        lastKnownMessage = status.currentTest.lastKnownMessage;
      }
      // Wait until the browser is closed before reporting the test as timeout.
      // This will enable us to capture stdout/stderr from the browser
      // (which might provide us with information about what went wrong).
      var browserTestOutput = new BrowserTestOutput(
          status.currentTest.delayUntilTestStarted,
          status.currentTest.stopwatch.elapsed,
          lastKnownMessage,
          status.browser.testBrowserOutput,
          didTimeout: true);
      status.currentTest.doneCallback(browserTestOutput);
      status.currentTest = null;

      // We don't want to start a new browser if we are terminating.
      if (underTermination) return;
      var browser;
      var new_id = id;
      if (browserName == 'chromeOnAndroid') {
        browser = new AndroidChrome(adbDeviceMapping[id]);
      } else if (browserName == 'ContentShellOnAndroid') {
        browser = new AndroidBrowser(adbDeviceMapping[id],
                                     contentShellOnAndroidConfig,
                                     checkedMode);
      } else if (browserName == 'DartiumOnAndroid') {
        browser = new AndroidBrowser(adbDeviceMapping[id],
                                     dartiumOnAndroidConfig,
                                     checkedMode);
      } else {
        browserStatus.remove(id);
        browser = getInstance();
        new_id = "BROWSER$browserIdCount";
        browserIdCount++;
        browserStatus[new_id] = new BrowserTestingStatus(browser);
      }
      browser.id = new_id;
      browser.start(testingServer.getDriverUrl(new_id)).then((success) {
        // We may have started terminating in the mean time.
        if (underTermination) {
          browser.close().then((success) {
            // We should never hit this, print it out.
            if (!success) {
              print("Could not kill browser ($id) started due to timeout");
            }
          });
          return;
        }
        if (success) {
          browserStatus[browser.id] = new BrowserTestingStatus(browser);
        } else {
          // TODO(ricow): Handle this better.
          print("This is bad, should never happen, could not start browser");
          exit(1);
        }
      });
    });
  }

  BrowserTest getNextTest(String browserId) {
    if (testQueue.isEmpty) return null;
    var status = browserStatus[browserId];
    if (status == null) return null;

    // We are currently terminating this browser, don't start a new test.
    if (status.timeout) return null;

    BrowserTest test = testQueue.removeLast();
    if (status.currentTest == null) {
      status.currentTest = test;
      status.currentTest.lastKnownMessage = '';
    } else {
      // TODO(ricow): Handle this better.
      print("This is bad, should never happen, getNextTest all full");
      print("This happened for browser $browserId");
      print("Old test was: ${status.currentTest.url}");
      print("Timed out tests:");
      for (var v in timedOut) {
        print("  $v");
      }
      exit(1);
    }

    status.currentTest.timeoutTimer = createTimeoutTimer(test, status);
    status.currentTest.stopwatch = new Stopwatch()..start();

    // Reset the test specific output information (stdout, stderr) on the
    // browser since a new test is begin started.
    status.browser.resetTestBrowserOutput();

    return test;
  }

  Timer createTimeoutTimer(BrowserTest test, BrowserTestingStatus status) {
    return new Timer(
        new Duration(seconds: test.timeout), () { handleTimeout(status); });
  }

  void queueTest(BrowserTest test) {
    testQueue.add(test);
  }

  void printDoubleReportingTests() {
    if (doubleReportingOutputs.length == 0) return;
    // TODO(ricow): die on double reporting.
    // Currently we just report this here, we could have a callback to the
    // encapsulating environment.
    print("");
    print("Double reporting tests");
    for (var id in doubleReportingOutputs.keys) {
      print("  ${testCache[id]}");
    }

    DebugLogger.warning("Double reporting tests:");
    for (var id in doubleReportingOutputs.keys) {
      DebugLogger.warning("${testCache[id]}, output: ");
      DebugLogger.warning("${doubleReportingOutputs[id]}");
      DebugLogger.warning("");
      DebugLogger.warning("");
    }
  }

  Future<bool> terminate() {
    var futures = [];
    underTermination = true;
    testingServer.underTermination = true;
    for (BrowserTestingStatus status in browserStatus.values) {
      futures.add(status.browser.close());
    }
    return Future.wait(futures).then((values) {
      testingServer.httpServer.close();
      testingServer.errorReportingServer.close();
      printDoubleReportingTests();
      return !values.contains(false);
    });
  }

  Browser getInstance() {
    if (browserName == 'ff') browserName = 'firefox';
    var path = Locations.getBrowserLocation(browserName, globalConfiguration);
    var browser = new Browser.byName(browserName, path, checkedMode);
    browser.logger = logger;
    return browser;
  }
}

class BrowserTestingServer {
  final Map globalConfiguration;
  /// Interface of the testing server:
  ///
  /// GET /driver/BROWSER_ID -- This will get the driver page to fetch
  ///                           and run tests ...
  /// GET /next_test/BROWSER_ID -- returns "WAIT" "TERMINATE" or "url#id"
  /// where url is the test to run, and id is the id of the test.
  /// If there are currently no available tests the waitSignal is send
  /// back. If we are in the process of terminating the terminateSignal
  /// is send back and the browser will stop requesting new tasks.
  /// POST /report/BROWSER_ID?id=NUM -- sends back the dom of the executed
  ///                                   test

  final String localIp;

  static const String driverPath = "/driver";
  static const String nextTestPath = "/next_test";
  static const String reportPath = "/report";
  static const String statusUpdatePath = "/status_update";
  static const String startedPath = "/started";
  static const String waitSignal = "WAIT";
  static const String terminateSignal = "TERMINATE";

  var testCount = 0;
  var httpServer;
  var errorReportingServer;
  bool underTermination = false;
  bool useIframe = false;

  Function testDoneCallBack;
  Function testStatusUpdateCallBack;
  Function testStartedCallBack;
  Function nextTestCallBack;

  BrowserTestingServer(this.globalConfiguration, this.localIp, this.useIframe);

  Future start() {
    int port = globalConfiguration['test_driver_port'];
    return HttpServer.bind(localIp, port).then((createdServer) {
      httpServer = createdServer;
      void handler(HttpRequest request) {
        // Don't allow caching of resources from the browser controller, i.e.,
        // we don't want the browser to cache the result of getNextTest.
        request.response.headers.set("Cache-Control",
                                     "no-cache, no-store, must-revalidate");
        bool isReport = request.uri.path.startsWith(reportPath);
        bool isStatusUpdate = request.uri.path.startsWith(statusUpdatePath);
        if (isReport || isStatusUpdate) {
          var browserId;
          if (isStatusUpdate) {
            browserId = request.uri.path.substring(statusUpdatePath.length + 1);
          } else {
            browserId = request.uri.path.substring(reportPath.length + 1);
          }
          var testId =
              int.parse(request.uri.queryParameters["id"].split("=")[1]);
          handleReport(
              request, browserId, testId, isStatusUpdate: isStatusUpdate);
          // handleReport will asynchroniously fetch the data and will handle
          // the closing of the streams.
          return;
        }
        if (request.uri.path.startsWith(startedPath)) {
          var browserId = request.uri.path.substring(startedPath.length + 1);
          var testId =
              int.parse(request.uri.queryParameters["id"].split("=")[1]);
          handleStarted(request, browserId, testId);
          return;
        }
        var textResponse = "";
        if (request.uri.path.startsWith(driverPath)) {
          var browserId = request.uri.path.substring(driverPath.length + 1);
          textResponse = getDriverPage(browserId);
        } else if (request.uri.path.startsWith(nextTestPath)) {
          var browserId = request.uri.path.substring(nextTestPath.length + 1);
          textResponse = getNextTest(browserId);
        } else {
          // /favicon.ico requests
        }
        request.response.write(textResponse);
        request.listen((_) {}, onDone: request.response.close);
        request.response.done.catchError((error) {
          if (!underTermination) {
            print("URI ${request.uri}");
            print("Textresponse $textResponse");
            throw "Error returning content to browser: $error";
          }
        });
      }
      void errorHandler(e) {
        if (!underTermination) print("Error occured in httpserver: $e");
      };

      httpServer.listen(handler, onError: errorHandler);

      // Set up the error reporting server that enables us to send back
      // errors from the browser.
      port = globalConfiguration['test_driver_error_port'];
      return HttpServer.bind(localIp, port).then((createdReportServer) {
        errorReportingServer = createdReportServer;
        void errorReportingHandler(HttpRequest request) {
          StringBuffer buffer = new StringBuffer();
          request.transform(UTF8.decoder).listen((data) {
            buffer.write(data);
          }, onDone: () {
              String back = buffer.toString();
              request.response.headers.set("Access-Control-Allow-Origin", "*");
              request.response.done.catchError((error) {
                DebugLogger.error("Error getting error from browser"
                                  "on uri ${request.uri.path}: $error");
              });
              request.response.close();
              DebugLogger.error("Error from browser on : "
                               "${request.uri.path}, data:  $back");
          }, onError: (error) { print(error); });
        }
        errorReportingServer.listen(errorReportingHandler,
                                    onError: errorHandler);
        return true;
      });
    });
  }

  void handleReport(HttpRequest request, String browserId, var testId,
                    {bool isStatusUpdate}) {
    StringBuffer buffer = new StringBuffer();
    request.transform(UTF8.decoder).listen((data) {
      buffer.write(data);
    }, onDone: () {
      String back = buffer.toString();
      request.response.close();
      if (isStatusUpdate) {
        testStatusUpdateCallBack(browserId, back, testId);
      } else {
        testDoneCallBack(browserId, back, testId);
      }
      // TODO(ricow): We should do something smart if we get an error here.
    }, onError: (error) { DebugLogger.error("$error"); });
  }

  void handleStarted(HttpRequest request, String browserId, var testId) {
    StringBuffer buffer = new StringBuffer();
    // If an error occurs while receiving the data from the request stream,
    // we don't handle it specially. We can safely ignore it, since the started
    // events are not crucial.
    request.transform(UTF8.decoder).listen((data) {
      buffer.write(data);
    }, onDone: () {
      String back = buffer.toString();
      request.response.close();
      testStartedCallBack(browserId, back, testId);
    }, onError: (error) { DebugLogger.error("$error"); });
  }

  String getNextTest(String browserId) {
    var nextTest = nextTestCallBack(browserId);
    if (underTermination) {
      // Browsers will be killed shortly, send them a terminate signal so
      // that they stop pulling.
      return terminateSignal;
    } else if (nextTest == null) {
      // We don't currently have any tests ready for consumption, wait.
      return waitSignal;
    } else {
      return "${nextTest.url}#id=${nextTest.id}";
    }
  }

  String getDriverUrl(String browserId) {
    if (httpServer == null) {
      print("Bad browser testing server, you are not started yet. Can't "
            "produce driver url");
      exit(1);
      // This should never happen - exit immediately;
    }
    return "http://$localIp:${httpServer.port}/driver/$browserId";
  }


  String getDriverPage(String browserId) {
    var errorReportingUrl =
        "http://$localIp:${errorReportingServer.port}/$browserId";
    String driverContent = """
<!DOCTYPE html><html>
<head>
  <title>Driving page</title>
  <script type='text/javascript'>
    function startTesting() {
      var number_of_tests = 0;
      var current_id;
      var next_id;
      // Describes a state where we are currently fetching the next test
      // from the server. We use this to never double request tasks.
      var test_completed = true;
      var testing_window;

      var embedded_iframe = document.getElementById('embedded_iframe');
      var number_div = document.getElementById('number');
      var executing_div = document.getElementById('currently_executing');
      var error_div = document.getElementById('unhandled_error');
      var use_iframe = ${useIframe};
      var start = new Date();

      function newTaskHandler() {
        if (this.readyState == this.DONE) {
          if (this.status == 200) {
            if (this.responseText == '$waitSignal') {
              setTimeout(getNextTask, 500);
            } else if (this.responseText == '$terminateSignal') {
              // Don't do anything, we will be killed shortly.
            } else {
              var elapsed = new Date() - start;
              // The task is send to us as:
              // URL#ID
              var split = this.responseText.split('#');
              var nextTask = split[0];
              next_id = split[1];
              run(nextTask);
            }
          } else {
            reportError('Could not contact the server and get a new task');
          }
        }
      }

      function contactBrowserController(method,
                                        path,
                                        callback,
                                        msg,
                                        isUrlEncoded) {
        var client = new XMLHttpRequest();
        client.onreadystatechange = callback;
        client.open(method, path);
        if (isUrlEncoded) {
          client.setRequestHeader('Content-type',
                                  'application/x-www-form-urlencoded');
        }
        client.send(msg);
      }

      function getNextTask() {
        // Until we have the next task we set the current_id to a specific
        // negative value.
        contactBrowserController(
            'GET', '$nextTestPath/$browserId', newTaskHandler, "", false);
      }

      function run(url) {
        number_of_tests++;
        number_div.innerHTML = number_of_tests;
        executing_div.innerHTML = url;
        if (use_iframe) {
          embedded_iframe.src = url;
        } else {
          if (testing_window == undefined) {
            testing_window = window.open(url);
          } else {
            testing_window.location = url;
          }
        }
      }

      window.onerror = function (message, url, lineNumber) {
        if (url) {
          reportError(url + ':' + lineNumber + ':' + message);
        } else {
          reportError(message);
        }
      }

      function reportError(msg) {
        function handleReady() {
          if (this.readyState == this.DONE && this.status != 200) {
            var error = 'Sending back error did not succeeed: ' + this.status;
            error = error + '. Failed to send msg: ' + msg;
            error_div.innerHTML = error;
          }
        }
        contactBrowserController(
            'POST', '$errorReportingUrl?test=1', handleReady, msg, true);
      }

      function reportMessage(msg, isFirstMessage, isStatusUpdate) {
        if (isFirstMessage) {
          test_completed = false;
          current_id = next_id;
          contactBrowserController(
            'POST', '$startedPath/${browserId}?id=' + current_id,
            function () {}, msg, true);
        } else if (isStatusUpdate) {
            contactBrowserController(
              'POST', '$statusUpdatePath/${browserId}?id=' + current_id,
              function() {}, msg, true);
        } else {
          var is_double_report = test_completed;
          var retry = 0;
          test_completed = true;

          function reportDoneMessage() {
            contactBrowserController(
                'POST', '$reportPath/${browserId}?id=' + current_id,
                handleReady, msg, true);
          }

          function handleReady() {
            if (this.readyState == this.DONE) {
              if (this.status == 200) {
                if (!is_double_report) {
                  getNextTask();
                }
              } else {
                reportError('Error sending result to server. Status: ' +
                            this.status + ' Retry: ' + retry);
                retry++;
                if (retry < 3) {
                  setTimeout(reportDoneMessage, 1000);
                }
              }
            }
          }

          reportDoneMessage();
        }
      }

      function parseResult(result) {
        var parsedData = null;
        try {
          parsedData = JSON.parse(result);
        } catch(error) { }
        return parsedData;
      }

      function messageHandler(e) {
        var msg = e.data;
        if (typeof msg != 'string') return;

        var parsedData = parseResult(msg);
        if (parsedData) {
          // Only if the JSON message contains all required parameters,
          // will we handle it and post it back to the test controller.
          if ('message' in parsedData &&
              'is_first_message' in parsedData &&
              'is_status_update' in parsedData &&
              'is_done' in parsedData) {
            var message = parsedData['message'];
            var isFirstMessage = parsedData['is_first_message'];
            var isStatusUpdate = parsedData['is_status_update'];
            var isDone = parsedData['is_done'];
            if (!isFirstMessage && !isStatusUpdate) {
              if (!isDone) {
                alert("Bug in test_controller.js: " +
                      "isFirstMessage/isStatusUpdate/isDone were all false");
              }
            }
            reportMessage(message, isFirstMessage, isStatusUpdate);
          }
        }
      }

      window.addEventListener('message', messageHandler, false);
      waitForDone = false;

      getNextTask();
    }

  </script>
</head>
  <body onload="startTesting()">
    Dart test driver, number of tests: <div id="number"></div><br>
    Currently executing: <div id="currently_executing"></div><br>
    Unhandled error: <div id="unhandled_error"></div>
    <iframe id="embedded_iframe"></iframe>
  </body>
</html>
""";
    return driverContent;
  }
}
