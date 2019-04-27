#!/bin/sh
VERSION=1.9.9
LIBGDX_BASE_PATH=$HOME/.gradle/caches/modules-2/files-2.1/com.badlogicgames.gdx/
GDX_PATH=`find $LIBGDX_BASE_PATH -name gdx-$VERSION.jar`
GDX_TOOLS_PATH=`find $LIBGDX_BASE_PATH -name gdx-tools-$VERSION.jar`
OUT_DIR=../../src/main/resources/images

cp pack.json $target
java -cp $GDX_PATH:$GDX_TOOLS_PATH com.badlogic.gdx.tools.texturepacker.TexturePacker icons $OUT_DIR icons

