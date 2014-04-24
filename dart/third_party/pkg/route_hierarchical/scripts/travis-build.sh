#!/bin/bash

set -evx
. ./scripts/env.sh

dart -c test/url_template_test.dart
dart -c test/url_pattern_test.dart

node "node_modules/karma/bin/karma" start karma.conf \
  --reporters=junit,dots --port=8765 --runner-port=8766 \
  --browsers=Dartium,ChromeNoSandbox,Firefox --single-run --no-colors

