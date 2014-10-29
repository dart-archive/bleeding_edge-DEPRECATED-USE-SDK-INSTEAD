#!/bin/bash
(cd $(dirname $0); cat upgrade_todomvc.patch | patch -p1)
