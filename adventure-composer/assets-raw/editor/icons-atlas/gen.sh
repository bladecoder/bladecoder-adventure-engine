#!/bin/sh
VERSION=1.5.3
LIBGDX_BASE_PATH=~/.gradle/caches/modules-2/files-2.1/com.badlogicgames.gdx/
GDX_PATH=`find $LIBGDX_BASE_PATH -name gdx-$VERSION.jar`
GDX_TOOLS_PATH=`find $LIBGDX_BASE_PATH -name gdx-tools-$VERSION.jar`

java -cp $GDX_PATH:$GDX_TOOLS_PATH com.badlogic.gdx.tools.texturepacker.TexturePacker images ../../../src/main/resources/images icons
