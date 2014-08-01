#!/bin/sh

PROJECT_MODEL=../venus-android/assets/model

cd ..

find "$PROJECT_MODEL" -regex ".*\.\xml" -exec java -cp ./bin org.bladecoder.i18n.ExtractStrings {} \;
