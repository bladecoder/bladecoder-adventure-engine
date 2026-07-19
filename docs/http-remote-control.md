# HTTP remote control

Blade Engine can expose a localhost-only HTTP service so that a development
tool or agent can control a running game. It never accepts connections outside
`127.0.0.1` and it adds no runtime dependency.

## Start and stop

On desktop, start the generated launcher with `-http` or `-http <port>`; the
default port is `8080`.

```sh
./gradlew desktop:run -PappArgs="['-w', '-http']"
```

The Debug screen has an equivalent button. The service stops automatically when
the game UI is disposed. Android games need the `android.permission.INTERNET`
permission; use `adb forward tcp:8080 tcp:8080` to reach the loopback service
from the development machine.

## API

Every response is JSON. Commands are accepted into a FIFO queue and return
`202`; one command is executed per rendered frame once the world is not paused,
in cut mode, replaying a recording, or running the tester bot.

```sh
curl http://127.0.0.1:8080/health
curl http://127.0.0.1:8080/state
```

`GET /state` contains the current scene, player, interactive actors, configured
verbs, dialogue options and automation state.

Send commands to `POST /command` with `Content-Type: application/json`:

```sh
curl -X POST http://127.0.0.1:8080/command \
  -H 'Content-Type: application/json' \
  -d '{"type":"actorVerb","actorId":"door","verb":"action"}'

curl -X POST http://127.0.0.1:8080/command \
  -H 'Content-Type: application/json' \
  -d '{"type":"sceneVerb","verb":"goto"}'

curl -X POST http://127.0.0.1:8080/command \
  -H 'Content-Type: application/json' \
  -d '{"type":"dialogOption","option":0}'

curl -X POST http://127.0.0.1:8080/command \
  -H 'Content-Type: application/json' \
  -d '{"type":"goto","x":320,"y":180}'

curl -X POST http://127.0.0.1:8080/command \
  -H 'Content-Type: application/json' \
  -d '{"type":"saveGame","target":"agent-save"}'

curl -X POST http://127.0.0.1:8080/command \
  -H 'Content-Type: application/json' \
  -d '{"type":"screenshot","target":"agent-screenshot.png"}'
```

`actorVerb` accepts an optional string `target`. Invalid request schemas return
`400`; unknown routes return `404`; unsupported methods return `405`.

`screenshot` writes a 1920-pixel-wide PNG to the game's user-data directory.
