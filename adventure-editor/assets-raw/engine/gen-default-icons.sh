#!/bin/sh

#ANDROID ICONS
inkscape -w 144 -h 144 --export-area-page --export-png=../../src/main/resources/projectTmpl/android/res/drawable-xxhdpi/ic_launcher.png  engine-default-icon.svg;
inkscape -w 96 -h 96 --export-area-page --export-png=../../src/main/resources/projectTmpl/android/res/drawable-xhdpi/ic_launcher.png  engine-default-icon.svg;
inkscape -w 72 -h 72 --export-area-page --export-png=../../src/main/resources/projectTmpl/android/res/drawable-hdpi/ic_launcher.png  engine-default-icon.svg;
inkscape -w 48 -h 48 --export-area-page --export-png=../../src/main/resources/projectTmpl/android/res/drawable-mdpi/ic_launcher.png  engine-default-icon.svg;
#inkscape -w 36 -h 36 --export-area-page --export-png=../../src/main/resources/projectTmpl/android/res/drawable-ldpi/ic_launcher.png  engine-default-icon.svg;

inkscape -w 512 -h 512 --export-area-page --export-png=../../src/main/resources/projectTmpl/android/ic_launcher-web.png  engine-default-icon.svg;


#DESKTOP
inkscape -w 16 -h 16 --export-area-page --export-png=../../src/main/resources/projectTmpl/desktop/src/icons/icon16.png  engine-default-icon.svg;
inkscape -w 32 -h 32 --export-area-page --export-png=../../src/main/resources/projectTmpl/desktop/src/icons/icon32.png  engine-default-icon.svg;
inkscape -w 128 -h 128 --export-area-page --export-png=../../src/main/resources/projectTmpl/desktop/src/icons/icon128.png  engine-default-icon.svg;

#IOS
inkscape -w 57 -h 57 --export-area-page --export-png=../../src/main/resources/projectTmpl/ios/data/Icon.png  engine-default-icon.svg;
inkscape -w 114 -h 114 --export-area-page --export-png=../../src/main/resources/projectTmpl/ios/data/Icon@2x.png  engine-default-icon.svg;
inkscape -w 72 -h 72 --export-area-page --export-png=../../src/main/resources/projectTmpl/ios/data/Icon-72.png  engine-default-icon.svg;
inkscape -w 144 -h 144 --export-area-page --export-png=../../src/main/resources/projectTmpl/ios/data/Icon-72@2x.png  engine-default-icon.svg;

#IOS 7.0
inkscape -w 76 -h 76 --export-area-page --export-png=../../src/main/resources/projectTmpl/ios/data/Icon-76.png  engine-default-icon.svg;
inkscape -w 152 -h 152 --export-area-page --export-png=../../src/main/resources/projectTmpl/ios/data/Icon-76@2x.png  engine-default-icon.svg;
inkscape -w 120 -h 120 --export-area-page --export-png=../../src/main/resources/projectTmpl/ios/data/Icon-120.png  engine-default-icon.svg;

inkscape -w 750 -h 1334 --export-area-page --export-png=../../src/main/resources/projectTmpl/ios/data/Default-375w-667h@2x.png ios-default-images.svg;
inkscape -w 1242 -h 2208 --export-area-page --export-png=../../src/main/resources/projectTmpl/ios/data/Default-414w-736h@3x.png ios-default-images.svg;
inkscape -w 640 -h 1136 --export-area-page --export-png=../../src/main/resources/projectTmpl/ios/data/Default-568h@2x.png ios-default-images.svg;

inkscape -w 320 -h 480 --export-area-page --export-png=../../src/main/resources/projectTmpl/ios/data/Default.png ios-default-images.svg;
inkscape -w 640 -h 960 --export-area-page --export-png=../../src/main/resources/projectTmpl/ios/data/Default@2x.png ios-default-images.svg;
inkscape -w 1536 -h 2008 --export-area-page --export-png=../../src/main/resources/projectTmpl/ios/data/Default@2x~ipad.png  ios-default-images.svg;
inkscape -w 768 -h 1004 --export-area-page --export-png=../../src/main/resources/projectTmpl/ios/data/Default~ipad.png ios-default-images.svg;
inkscape -w 2048 -h 2732 --export-area-page --export-png=../../src/main/resources/projectTmpl/ios/data/Default-1024w-1366h@2x~ipad.png ios-default-images.svg;

