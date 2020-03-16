# Change Log
All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).

## [4.0.0]
- Fix error when android keystore path had spaces.
- Added scene counter in the editor ui.
- Updated ios minosversion to 8.
- Better calc of speed when walking and fake depth is used.

## [3.2.5]
- Some validations adding/editing animations and verbs to avoid errors.
- Fixed dealing with OpenAL bug in credits screen.
- Fixed bug extracting ink texts.

## [3.2.4]
- Added more external functions for Ink.
- Added support to load/save game preferences.
- Scale and Rotate actions now work with ui actors.
- Many bugs fixed.

## [3.2.3]
- Added "initVerb" param to the "Leave" action. This verb will be executed if set instead of the "init" verb.
- Now, the "init" verb doesn't run if the "test" verb is executed.
- Load/Save preferences support.
- Allow to move BaseActors in MoveToSceneAction.
- FIX: Scale and Rotate actions didn't work with ui actors.
- Save also callbacks that are not in the current scene.

## [3.2.2]
- Inventory button style now in the InventoryUI style. This allows to customize the inventory button by player.
- Update Blade Ink to v0.7.3 which fixes an important bug.
- A lot of bugs fixed (see git log).

## [3.2.1]

- Added bubble positioning parameters on ui.json.
- Better multiple inventory support in actions.
- Better Spine skin handling.
- Update Blade Ink to v0.7.2.
- A lot of bugs fixed (see git log).

## [3.2.0]

- Say Action: Talk animation also for text types PLAIN and SUBTITLE.
- Only save modified actor properties on savegames.
- New SetDesc action to change the actor descriptions.
- Added 'show_hotspots' config key to enable/disable the show hotspots feature.
- Added INSIDE property to IfAttrProperty action to check if an actor is inside other actor.
- Improve size of chapter files and savegames.
- Updated libgdx to v1.9.10
- Updated Spine to v3.8.
- Update gradle to v5.4.1

## [3.1.2]

- Animated ui icons and cursors.
- Update RoboVM to v2.3.7.
- Don't extract string expresions from ink files for i18n.
- Allow empty values in Property action.
- Added public methods to the SpineRenderer to get access to the current skeleton and animation.

## [3.1.1]

- Dialog to create the android keystore inside the editor.
- Label and text to warn about legacy dialogs.
- Added RandomPosition action.
- Added reload assets icon to scene list.
- Added support for combining skins in Spine.
- Delete last_project key if loadproject fails so the editor doesn't fail forever.
- Delete SetModelProp action and supporting library. It was not useful and make porting dificult.
- Added IN_UI if property.
- Fix: Now search in all inventories.
- Fix: Set skin now updates properly.

## [3.1.0]

- Update Spine runtime to v3.7
- Dialog to compile Ink stories.
- Show 'Saved game' text when saved game to give user feedback.

## [3.0.0] aka Johnny Bonasera 3

- Ink button in editor to link Ink stories with chapters.
- Better handling of up/down buttons when showing options.
- Improved debug info and fix tester bot.
- Update vis-ui lib to latest stable.
- FIX: Show warning message when a game is playing also if not saved before.
- FIX: Bug in edit anim spine atlas.
- FIX: Several fixes in InkManager handling concurrency.
- FIX: uiactors camera refresh.
- FIX: Bug saving current dialog actor.
- FIX: 'desktop:dist' task in template.

## [2.2.0]

- Java 11 support (Gradle v4.10.2).
- Update to libgdx v1.9.9.
- iOS 12 bug fix (robovm v2.3.5).
- Added property to change the text position of bubbles for character actors.
- Add max width/height in the create atlas dialog.
- Android SDK is not mandatory to create or edit projects.
- A lot of bugs fixed (see git log).

## [2.1.4]

- Now the editor works without having the Android SDK installed.
- FIX: Bug when moving target actor.
- FIX: Bug in 'Transition' action.  
- FIX:'actorTextPosition' property in 'SetActorAttr' action wasn't working.

## [2.1.3]

- Support for several walkzones in a scene. Now walkzones are also actors.
- Added icons to translate, scale and rotate actors in editor.
- Android: Added runtime permission query when necessary to make expansion files work in newer android apis.
- EDITOR: Added play in fullscreen option.
- Added talking text position to characters.
- Update Blade Ink to v0.5.1.
- FIX: Reset pointer always when updating UI to avoid showing last pointer when changing scene.
- FIX: Bad bubble alignment showing small texts.
- FIX: Bug creating/deleting chapters in editor.
- FIX: The current scene was setting twice when loading a saved game triggering an error. 
- FIX: The dialog option was showing for 1 frame when autoselected enabled.

## [2.1.2]

- Sprite Actors can have different scales in X and Y axis allowing unproportional scaling.
- Check project version before loading + show loading message.
- Fix gradle deprecation warnings.
- Continue loading custom classes if some error found in one of them.

## [2.1.1]

-  Fix several UI bugs: inventory button doesn't hide, dialog not working well when autoselect one option and text doesn't show when returning to a scene without init.
-  Add -s parameter to DestkopLauncher.

## [2.1.0]

- TextManager now sets the talk animation for characters. This ease characters to have conversations in the background.
- Code refactor to get rid of the 'World' singleton. This breaks custom action backwards compatibility.
- Leave action have the param 'init' to avoid call the init verb when false. This allows to change between scenes without worrying about losing the state.
- Update blade-ink lib to v0.5.0.
- Upgrade to gradle 2.6. Maybe it needs some more tweaks.
- Added new text type 'UI' to show debug or ui messages.
- Added one click 'action' verb.
- Don't use ref point in position actions if the target actor is the same that the current actor.
- All intensive tasks (load music, voices and ink) are now executed in a background thread.
- FIX: filteredbox height bigger than screen and don't show the last element.

## [2.0.6]

- All intensive tasks (load music, voices and ink) are now executed in a background thread.

- Added search box to combo boxes.
- Added desktop files to generate Flatpak package.
- Don't generate the key for desc if exists when editing actor. Better generation of not duplicated keys.
- Safety nullchecks.
- Error message if actor with same id exists when adding an actor to scene.
- RunVerb now uses the Verb IP.
- Added search box to combo boxes.
- Added desktop files to generate Flatpak package.

## [2.0.2]

- FIX: Stop voices when changing scene.
- FIX: Volume action musn't wait if duration=0
- FIX: Changes to try to fix the OpenAL error in desktop

## [2.0.1]

- Added option to the create atlas dialog to allow generating .jpg atlases.
- Added build script custom parameter input box in the package dialog.
- FIX: Capture random music error on desktop in credit screen.

## [2.0.0]

- HelpScreen doesn't stretch in 4:3 aspect ratio.
- InkManager translation files support.
- Import ink texts in order. Better I18N checks.
- Add music volume fade support in MusicVolume action.
- VoiceManager doesn't hide texts, now waits to the calc time.
- FIX: bug when saving ink cb.

## [1.3.5]
- Added Spine's skins support.
- Choose the best matching 'use' verb when target and inventory actor have it.
- Added pitch parameter to sounds.
- Updated libgdx to v1.9.8.
- Special keys (testerbot, record, play, etc) only enabled in debug mode.
- Delete red tint in hotspots.
- Added sound input panel.
- FIX: Error in walking path calculation.
- FIX: running test scene in android.
- FIX: bad viewport configuration when pausing and restore the qqqgame when screen in 4:3.
- FIX: bug when setting ink property if it doesn't exists after load game.
- FIX: getCurrentTarget() in RunVerbAction.
- FIX: sound list reload when changing chapters.
- FIX: main class detection in desktop packaging.
- FIX: error reading Cb from InkManager.
- FIX: particles and bboxfromrenderer for resolutions other than 1.

## [1.3.4]
- Added currentTarget to verbs.
- Null checks in SoundManager before pause/resume sounds.
- Fix loading callbacks from saved inventories.
- Fix: Loop, volume and pan was not used when playing a sound.
- Missing fbo dispose.
- Fix: toString() instead of casting in getModelProp.

## [1.3.3]
- Added 'textStyle' attribute to character actors.
- Added basic QA rules/metrics of project.
- EDITOR: Understand SNAPSHOT versions when checking for updating versions.
- Actor 'tint' property are now used to draw inventory actors when dragged.
- Catch back key in android.
- Added load chapter option to the debug screen.
- Fix not showing saved games in tests folder.
- Fix error removing all items from inventory in 'DropItem' action.
- Fix error creating screenshots for savedgames in Mac.
- Fix crash when screenshot of a savegame doesn't exists.

## [1.3.2]
- Added Google Play expansion file support.
- Added 64 and 32 bit support when generating package for windows. Previously only 64 bits was supported.
- Bigger edit toolbar icons.
- Create initial scene in new projects.
- Fix: Recorder not recording dialog options.
- Add target scene to DropItemAction and mark RemoveInventoryItem action deprecated.
- Updated to libgdx v1.9.7.
- EDITOR: Fix testing not working with scenes with spaces in ID
- EDITOR: Fix nullpointer error when disabled imagen not exists.
- EDITOR: Fix bug when undo bbox points.


## [1.3.1]
- Asset folder is created now in the project root.
- Updated Gradle to v4.0.2
- FIX sound error preventing to save the game.

## [1.3.0]
- New Sound System. Now adding sounds is more usable. Backwards compatibility preserved.
- World properties can also be set in BladeEngine.properties.
- Added PLATFORM property.
- Added OpenURL action.
- Added actions for accessing to Ink variables: InkVariable and IfInkVariable actions.
- Added DIRECTION attribute to IfActorAttr action.
- Initial support for assets in the root folder.
- Ink: Change the ':' character talk separator by '>'.
- Set actor fake depth as boolean.
- Custom icon support for the 'leave' verb.
- Updated blade-ink to v0.4.3.
- Updated InGameConsole to v0.7.1.
- 'world.json' is now called 'world'. Old files can still be loaded.
- EDITOR: Add refPoint to the EditActorDialog.
- EDITOR: Undo bbox points, depth markers, ref. points and walkzone points.
- EDITOR: Added zoom keys: '+' and '-'.
- EDITOR: Filter .zip files in File Input Dialog when selecting JRE in the package dialog.
- And many fixes Fixes.

## [1.2.7]
- Added secondary animation support for Spine actors.
- Added 'keepDirection' param to Animation action.
- Added 'target' actor to position in Camera action.
- Added 'LAYER' and 'IN_SCENE' params in IfAttr action.
- Updated Spine plugin to latest Spine version.
- Updated blade-ink dependency to v0.4.2
- Added title image and aligment to the menu screen.
- EDITOR: Edit Animation dialog now sets the initial source based in previous sources.

## [1.2.6]

- Actors in inventory can be animated now.
- Added UI actors: Actors that stays in all scenes not affected by scrolling nor any other scene camera effect. Normally used to create UI buttons.
- Multiline texts can have a voice file per line using a '#' mark in each line.
- Check for Ink engine errors after each line.
- FIX: SpineRenderer serialization error when loading.
- FIX: error loading objects in inventory when they have changed from layer.

## [1.2.5]

- Update Robovm to v2.3.1 fixing problems with the latest XCode update.
- FIX: VoiceManager show log and continues when the voice file is not found instead of crash.
- FIX: voice was mandatory in the edit option dialog.
- FIX: Show only the first line of comment actions in the action list.

## [1.2.4]

- NEW BIG FEATURE: Voice support.
- OSX Sandbox support to support releasing the game in the Mac App Store.
- Updated Spine engine to v3.5.51
- When adding new assets throught the Asset List view, create the asset folder if it doesn't exist.
- FIX: Scene parameter in LoadChapterAction incorrect.
- FIX: Scale and Rotation actions.
- FIX: Loading integer atributes in actions.
- FIX: Serialization error in TextManager.

## [1.2.3]

- Added 'ignoreWalkZone' parameter to Goto.
- Get rid of SpriteActor restriction in PositionAnim target.
- Update to gradle v3.4.1
- EDITOR: Dialogs can exit the editor borders.
- EDITOR: Scroll the actor list when actor is selected in ScnWidget.
- FIX: bad rotation in Spine actors.
- FIX: Error disposing source in getInternalAnimations.
- FIX: Add WHITE tint to Sprite actor in anim if the actor doesn't have
    it.

## [1.2.2]

- Added I18N support to Text Actors.
- New AlphaAnimation action to animate the alpha channel of sprite actors.
- Added 'orgAlign' to Sprite Actors to set the origin for scale and rotation.
- Updated library libgdx to v1.9.6
- AnimationRenderer refactor.
- Added text align to text actors.
- FIX: tint for text actors not working.
- FIX: Error saving/loading tweens state.
- EDITOR: Added icons for text and particle actors.
- EDITOR FIX: Problem when changing id to animations.

## [1.2.1]

- Added music support to the default Menu screen.
- Added rotation property to SetActorAttr action.
- FIX: Check if renderer is an AnimationRenderer when changing the selected animation in the editor.
- FIX: Error disposing Spine actors when creating new project.
- FIX: Error creating new project in Linux and MacOS due to CRLF in gradlew.

## [1.2.0]

- Added particle effect actors.
- Added text actors.
- Added rotation to actors.
- Several improvements to the ink integration. Updated 'blade-ink' package to v0.4.0.
- Updated 'packr' package to the latest version.
- Toggle debug mode tapping in the menu version label.
- Autodetect the Android SDK version installed.
- EDITOR: Show actor position when editing actor for precision positioning.
- EDITOR: Added color selector dialog for color input.
- EDITOR FIX: Init animation was not showing properly.

## [1.1.1]

- Added new action to add an integer value to a property to increment/decrement it. Useful to create counters.
- Add MacOS .icon when packaging.
- Add .app extension to the MacOS package.
- Added autoselect style property to the DialogUI.
- Ink support cleanup.
- FIX: music was stopped changing from scene when a change to the
loading screen was needed.

## [1.1.0]

- Ink support. (http://www.inklestudios.com/ink/)
- Update libgdx version to 1.9.5.
- Improve text styles in ui.json for TextManager.
- Added 'target' parameter to TextAction.
- Added tint/color animation support.
- Added interpolation to camera animation.

## [1.0.3]

- FIX: error loading recordings from 'tests' folder.

## [1.0.2]

- Added support to have several games packaged in the same application.
- Reset previous walk animation before new walk animation added.

## [1.0.1]

- Music not working for iOS in CreditScreen
- EDITOR: FIX bug when setting the init scene. The document wasn't marking as modified.

## [1.0.0]

- Images can be localized now.
- Added xxxhdpi icon folder to Android.
- Better scroll handling in text areas.
- Added "show console" checkbox in the view menu.

## [0.9.19]

- EDITOR: Use libgdx FileChooser and get rid of JavaFX to avoid freezing when loading projects.
- EDITOR: Added UI Atlas import/export buttons in Tools menu.
- EDITOR: Added scroll bars to Text Boxes.
- EDITOR: Add "clean" target in IOS because the app is not signing well if not cleaning first.

- Added tags to Skin styles to increase the ui.json readability.
- Better scroll amount calculation in dialog ui scroll buttons.

- FIX: error when loading project with custom actions. Not detecting no compiled project properly.
- FIX: DisableAction must not be visible in the action combo.
- FIX: The editor was losing the scroll focus when the log console was shown.


## [0.9.18]

- EDITOR: The Action list now supports multiple selection to copy, paste, show... use Shift and Ctrl keys to multiselect.
- EDITOR: Added console log. Use F1 key to show/hide.
- EDITOR: Better scrollbars. Always showing now.
- Now the editor generates IOS packages ready to submit to the Apple Store. Fixed several config parameters related to this problem.
- Added "Comment" action.
- Updated packr to v2.0. Better desktop packages are generated now.
- Dialog option UI now shows buttons to scroll add and down when necessary. New styles added to ui.json.
- Added parameter in TextManagerUI style inside ui.json to set the subtitle Y position.
- Improvement to the pathfinder. Now the clap algorithm always returns a point inside the polygon.
- In IOS platform, sounds and music in ACC (.m4a) format are used if exists. As .ogg is not supported in IOS.
- FIX: The name of the loaded chapter was not set correctly, causing saved games not loading the current chapter.
- FIX: Weird bug. Use toUpperCase with Locale.English to read enum values.
- FIX: Bad IOS splash screen orientation.

## [0.9.17]

- Updated libgdx library to v1.9.4.
- Added null sanity check for actor parameter in SayAction.

## [0.9.16]

- NEW FEAUTRE: The text manager can now show a character image/icon in texts.
- Added MusicVolumeAction
- Play recorded game directly if passed as parameter in command line.
- Added relative position to target in position and goto actions.
- Added animation param to SayAction.
- Added -aspect parameter to desktop launcher.
- Update spine plugin to latest spine runtime version.
- EDITOR: Save/Restore version in package dialog.
- EDITOR: Save/Restore selected scene.
- EDITOR: Show verb panel when no actor is selected to allow adding scene and
world verbs.

- FIX: Reset UI when changing scene.
- FIX: CameraAction animation params not mandatory.
- FIX: Android Test: Uninstall android debug version before install.
- FIX: Count default value in AnimationAction.
- FIX: SceneActorInputPanel getText() when no actor is selected.
- FIX: bug pasting spine actors.

## [0.9.15]

- Ask for updating the engine version of the game if the editor detects that is old.
- Custom actions now are autodetected by the editor and handled as engine actions.
- Save view menu state.
- Show project path in window title.
- Select 'init' scene when loading a project.
- Ask for saving current project when loading or creating new project.
- Added preload parameter to sounds to avoid loading all scene sounds in init.
- FIX: Music was not restored from savedgames.
- FIX: Error in REVERSE_REPEAT animations.

## [0.9.14]

- Multiinventory support.
- EDITOR: Fix when adding custom actions.

## [0.9.13]

- Added new buttons in the Tools menu to test the scene in Iphone/Ipad emulator and device.
- Support for changing players in runtime: Added $PLAYER variable.
- Added actor Tint attribute to allow lighting effects.
- EDITOR: Disable not working HTML platform.
- Update RoboVM to Mobidevelop 2.1.0 release.
- Added 'initNewGame' and 'initSavedGame' verbs to allow calling actions when a game is started or loaded.
- Fix error in text position when drawing SUBTITLE texts.
- Fix action color serialize.
- DialogUI gets scrollbar style.
- Update fonts for better language support.
- Fix bug when loading project and scene scope is selected.
- Delete unused 'delay' field in AnimationDesc.
- Fix: bug in yoyo animatinons.


## [0.9.12]

- Added Refpoint to interactive actors.
- Support for fast leave scene when double click.
- Updated Spine Runtime library to latest Spine release.
- Updated to libgdx v1.9.3

## [0.9.11]

  - Unicode character support. Previously only ISO-8859-1 character set was supported.
  - Shadow and outline font support.
  - Editor fonts are now .ttf
  - Added "Single Action" support to inventory.
  - FIX: Hide inventory in cutmode
  - EDITOR: Avoid dragging object by mistake when clicked to select it.
  - FIX: Multiply position by scale for multiresolution support in TextAction.
  - Better tolerance handling when dragging inventory objects.
  - FIX: Use screen height instead of world height for size calculation in InventoryUI.

## [0.9.10]

  - FIX: Bad width calculation in DialogUI.
  - Change help screen language in runtime.
  - Sets Ctrl+d to toggle debug mode.
  - Catch 'BACK' key in android.

## [0.9.9]

- New music engine.
- I18N: language change support in runtime.
- EDITOR: Added I18N tools for importing and exporting texts in .tsv format for translation.
- EDITOR: Added button to play sounds in EditSoundDialog and SoundList.
- Added methods to MenuScreen to ease extension.
- FIX: Get desktop launcher properly when packaging.
- FIX: EndGameAction now returns true to stop the verb execution.
- FIX: bug playing music in credit screen.
- Doesn't play animation sounds when animation is in reverse mode.
- FIX: Pass debug flag when testing scene.
- FIX: In EditableSelectBox, check empty list before setting default value.


## [0.9.8]

- Show only ui state and time in screen debug text
- Add 'id' attribute to SoundFX.
- FIX: Pausing sounds when show menu.
- Support new properties in 'if' actions: in_inventory, interactive, current scene, previous scene and target actor in 'use' verbs
- FIX: It was cleaning sound list when disposing sounds.
- FIX: bug when disposing Spine and sound assets.
- SoundAction: Delete stop parameter. Now the current sound stops if the play parameter is empty.
- EDITOR: Disable Spine event handling in editor.
- FIX: Flickering due to swapBuffer.
- More fault tolerant when loading actions: Action field not found.
- Change stopSound from actor for stopCurrentSound
- Doesn't play spine events in reverse mode. Spine doesn't works well in that case, maybe a spine bug?
- EDITOR: New View and Tools menu. 'Run on Android device' button.
- FIX: Inventory capacity error. Inventory now change its item size dynamically to increase his capacity.
- FIX: Bug obtaining the window title from properties in desktop launcher.
- FIX: bug saving interpolation
- Automatic generation of versions.properties
- Better version handling when packaging for android.
- Show game version in menu screen.
- Update UI ATLAS
- FIX: EDITOR bug in zindex not setting for interactive actor when editing.
- FIX: EDITOR bug setting interaction property in editor. Better interaction property handling.
- Prevent event trigger when retrieving assets
- FIX: added hotspot image
- 'Remove Savegame' button size depends on dpi


## [0.9.7]

- Better Load/Save game screen.
- Confirmation dialog when overwrite current game in new game or load game.
- Updated spine plugin to Spine v3
- Show time in debug. Time is now long to avoid float overflow.
- Updated to libgdx 1.9.1
- Android SDK not mandatory when creating a project.
- More fault tolerant when loading saved games. Good for patches.
- Saved games can be stored in 'tests' folder inside game. These games are distributed with the game and in debug mode these saved games can be loaded. Good for testing.
- When creating a verb an icon can be specified. This icon will be showed in the UI.
- Add movement to the inventory button when picking an object.
- Doesn't hide inventory when running a verb.
- And tons of bug fixing.

## [0.9.6]

- Layer parallax feature
- Several fixes in atlasrenderer related with drawing atlases with striped whitespaces
- Drop lightmap support
- Fix inventory loading bug
- Fix inventory auto width calc
- Change default character actor speed
- Editor: add combo values in world props
- EDITOR: Fix losing player attr when editing actor.
- EDITOR: HTML not supported message
- Fix checking if a path is absolute in windows.
- Avoid nullpointer when lookat if animation doesn't exists in 3d sprite
- EDITOR: fixed NullPointer error when creating 3d sprite actor.
- Drop XML Loader


## [0.9.5]

- ENGINE: Added infinity text duration when duration < 0.
- ENGINE: fix: ImageRenderer check if currentAnimation==null in toString()
- EDITOR: align left actor panel to left. To always show the '+' button.
- ENGINE: fix sceneextendviewport world size calc.
- ENGINE: NEW ScreenPositionAction
- EDITOR: fixed bug when editing assets
- EDITOR: Fixed bug in TextInputPanel when text=null
- EDITOR: Fixed Issue #25. Error deleting elements from lists.
- EDITOR: fix: change world.json SayAction changed for LookatAction
- EDITOR: fix bug when paste IfAttr actions.
- EDITOR: Fix generated build.gradle BladeEngine.properties path reference when updating versions.


## [0.9.4]

- Compile custom classes when not found in loading project.
- Fix issue #23: Edit an existing actor
- Sets editor window size to 0.9 * screen size
- Better version control:
  - Extract version strings from build.gradle to gradle.properties in games.
  - Put versions in BladeEngine.properties when compiling games.
  - Show versions in DebugScreen
  - Added version to game model and saved games for further checks.
  - Put version variables in game gradle.properties
- Bug fix adding assets because of bad filter strings.
- Get appName from gradle.properties
- fix little bug when loading project and the custom actions are not compiled
- Editor: Better msg handling: Message Dialog
- Change RECTANGLE to SUBTITLE type of Text
- Editor: adjust colors in skin for a more pleasant feel.
- update pathfinder classes for no warnings.
- Use Gdx library instead JavaFX to set the window size.
- Use enum toString() as key to retrieve custom screens implementation.

## [0.9.3]

- Fix error when interpolation=null

## [0.9.2]

- Issue #22 fixed. Something went wrong while creating project.
- Set document modified when changing player
- Walking speed depends on scale
- Better direction calculation
- fixes: Null checks in animation dialogs/widget
- Fix calculating position when speed!=1 in reversing animation.
- Some ajustements in Action annotations: Better description and required field

## [0.9.1]

- Fixes several bugs in the editor related to the big refactor.
- Created EndGameAction
- Action refactor. VerbRunner parameter instead of ActionCallback.

## [0.9.0]
- Game model and saved games are now in JSON format.
  * XML is deprecated. Backward compatibility broken.
  * Saved games are patch friendly.
- EDITOR: Big refactor. Editor uses engine model objects now.
- Change I18N file encoding from ISO-889-1 to UTF-8

## [0.8.10]

### Added
- Change to TEXT_INPUT for Lookat and Say actions text fields.

### Fixed

- fix: set last frame in atlasrenderer when reverse mode

## [0.8.9]

### Added
- EDITOR: Added input panels for text input.
- Better aspect ratio support. Correct support for 4:3, 16:9 and 16:10.
- Pause the game when an exception/error is thrown and debug mode is
activated.
- Updated to libgdx v0.6.4
- Text from dialog ui wrap to screen size.

### Fixed
- fix: stop processing ActionCallbackQueue when changing scene


## [0.8.8]

### Added
- Interpolation support for position and scale actions
- Update to libgdx v1.6.2. WARNING: Projects have to be modified in order to work the IOS version.
	More info: http://www.badlogicgames.com/wordpress/?p=3694

### Fixed
- Compute BBox in renderer Refactor to allow recompute bbox when animation complete.
- Fix animationTime when reverse animation in SpineRenderer
- Fix save/game screen slot size
- EDITOR: fix when generating world keys for i18n + doesn't remove ui.* keys.

## [0.8.7]

### Fixed
- Fix: use scale factor for speed in PositionAction
- Added ui missing translations for menu screen
- Spine RT updated to latest version

## [0.8.6]

### Fixed
- Fix fakeDepthScale() calc: added world scale factor
- Change 'assets/test' folder name for 'assets/tests' when creating a project
- Some debugscreen changes
- Load/Save Game Screen improvements

## [0.8.5]

### Fixed

- SetActorAttrAction: multiply position by scale
- Check because the ActionCallbackQueue can call to setCurrentScene()

## [0.8.4]

### Fixed

- fix camera loading state
- fix bad casting when read saved game
- fix walking speed double scale multiply

## [0.8.3]

### Added

- UI Fixes
  - Inventory ui over inventory icon
  - Edit verbs dialog improvement
  - Custom autosize button


## [0.8.2]

### Added

- Inventory improved
	- Added configurable align (top, down, left, right, center)
	- Added configurable autosize behaviour
- Added arrow icon for exits when showing hotspots
- EDITOR: Added several config properties in the Game Properties tab

### Fixed

-EDITOR: fix world width and height was not recalculated when loading

## [0.8.1]

### Fixed
- Tester Bot fixes
- Dialog render fixes when character position is not inside the screen
- Dialog nullpointer fix when playing recorded files

## [0.8.0]
### Added
- Added a Tester Bot that plays the game randomly
- Spine atlas in animations

### Fixed
- EDITOR: Dialog editing fix

## [0.7.2]
### Added
- libgdx v1.5.6 update
- update to the latest spine libgdx runtime
- EDITOR: Enable/disable actions
- Backround images must be inside atlas now
- Added control actions
- Added ActorAttrAction
- GotoAction: Change margin.
- Added log when verb finished
- GotoAction: add anchor parameter
- PositionAction now works with BaseActors (no animation)

### Fixed
- EDITOR: fixes to inputpanels
- fix OptionInputPanel when mandatory
- Reset testScene when changing current scene
- ActorProps: Show pos for BaseActors
- EDITOR: change some edit defaults
- walkzone fix when dinamic obstacles
- fill animation/actor list when setText()

## [0.7.1]
- Action refactor
- EDITOR: Undo support
- EDITOR: Fake depth vector can be setting dragging ui markers
- Actor ZIndex
- RunVerb now allows iterating over a list of verbs
- Scene cache
- SpriteActor: scale support
- SpineRenderer: Reverse animation support
- Scene layer support

## [0.6.9]
- libgdx updated to v1.5.4
- Sprite Actor Scale support
- Added scene state handling

### Fixed
- javadoc fixes for jdk 1.8

## [0.6.8]

### Fixed
- Editor only release: Fix bug when saving project

## [0.6.7]

### Added
- Load/Save game screens
- libgdx updated to v1.5.3.

### Fixed
- fixed fillanimations combo bug. set selection to the first element
- Fix for windows gradle exec

## [0.6.6]
- creditscreen: set scroll speed resolution independent
- creditscreen: added background style. Style now obtained from skin

### Fixed
- fixed textureunpacker bug when image was rotated in atlas

## [0.6.5]
- better text size management for small screens
- text bubble smaller and better management

### Fixed
- fix ActionCallbackQueue serialization


## [0.6.4]
- ActionCallbackQueue serialization
- world defaultverbs serialization
- i18n UI support

## [0.6.3]
- Updated libgdx to 1.5.2 version
- Menu Screen Refactor
- Transition moved to World

## [0.6.2]
- i18n workflow in Editor working
- Added event handling in Spine plugin
- Editor dialog tree: edit and delete fixes
- fix CameraAction when no selecting any target
- fix enter/leave verb conflicts name. Rename to enter/exit
- fix xml action loading
- Call cb before cleaning text fifo
- fix RunVerb action in repeat

## [0.6.1]
- fix show assets folder
- fix when packaging android release (build.gradle bug)

## [0.6.0]
- Created Spine plugin and set as optional when creating a project.
- Refactor: FrameAnimation -> AnimationDesc, SpriteRenderer -> ActorRenderer
- EDITOR: fix several IOS related bugs. IOS Ipad/Iphone testing and working fine.
- EDITOR: fix create resolution. Now atlas upacking/packing is supported

## [0.5.0]
- Updated to libgdx 1.4.1
- ENGINE: Debug screen with speed control, record/play games and go to any scene in runtime
- ENGINE: Material style buttons in engine UI. Better look and feel for inventory and pie menu.

## [0.4.0]
- ENGINE: Custom game UI Screen support

## [0.3.2]
- EDITOR: Fixed bug when running project without console

## [0.3.1]
- EDITOR: Fixed accessing opengl context issue when creating project in the new thread.

## [0.3.0]
- ENGINE: Action refactoring. WARNING: Names have changed. All previous games are not compatible.
- ENGINE: New DebugScreen (Work in progress)
- ENGINE: Change speed support for fastforward.
- ENGINE: The blade-engine.jar are now in Maven Central. When creating a new game, the Maven dependency is added instead of adding the engine jar in libs folder.

## [0.2.0]
- EDITOR: Fixed NullPointer error when creating project
- EDITOR: Threads for long tasks to show UI message status
- EDITOR: FIXED packaging with embedded JRE.
- ENGINE: CreditsScreen fonts now obtained from Skin

## [0.1.0]
- Initial release
