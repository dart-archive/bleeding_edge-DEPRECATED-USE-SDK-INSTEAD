#!/bin/bash

# installing ffmpeg on Mac w/ homebrew
# brew install ffmpeg --with-libvpx --with-libvorbis

for file in `find . -type f -name '*.wav'`
do
  echo $file
  cmd="ffmpeg -y -strict experimental -i $file -acodec libvorbis ${file/wav/webm}"
  echo $cmd
  `$cmd`
done
