# Change Log
All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]

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
