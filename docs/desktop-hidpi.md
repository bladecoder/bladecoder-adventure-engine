# Desktop window and editor scaling

GLFW can select X11/XWayland even in a Wayland session. With GNOME fractional
scaling enabled, XWayland can expose a global scale of 2 and doubled monitor
resolutions, including on outputs configured at 100%. A 960x540 X11 window then
occupies 480x270 desktop units. `Gdx.graphics.getBackBufferScale()` remains 1;
it does not describe this compositor scaling.

Desktop launchers initialize GLFW, query its actual platform and primary monitor
content scale, and pass that scale to `WindowUtils`. Only X11 window dimensions
are multiplied. Native Wayland, macOS and other backends retain their logical
window dimensions. No factor is inferred from `XDG_SESSION_TYPE`, physical DPI,
or the presence of a Wayland socket. At scale 1 all conversions are identities.
Launchers reread the scale on startup; restart after changing desktop scaling.

The editor uses `EditorDpi` to express its ScreenViewport, fixed-size icons,
dialog dimensions and pointer coordinates in desktop units. BladeSkin already
generates fonts from backend dimensions, so their drawing scale and DPI-derived
spacing are divided by the same factor. VisUI's fixed-size fonts follow the
viewport normally. ScnWidget projects its scene viewport from stage coordinates
to screen coordinates before HdpiUtils converts to framebuffer pixels.

The game's window/fullscreen toggle restores the previous backend dimensions
without scaling them twice. If started fullscreen, its first windowed fallback
converts the world dimensions through WindowUtils. Remote `fixture.viewport`
continues to specify backend dimensions for reproducible capture fixtures.
