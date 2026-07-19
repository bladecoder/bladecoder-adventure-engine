package com.bladecoder.engine.remote;

final class RemoteCommand {
    enum Type {
        ACTOR_VERB, SCENE_VERB, DIALOG_OPTION, GOTO, SAVE_GAME, SCREENSHOT
    }

    final Type type;
    final String actorId;
    final String verb;
    final String target;
    final int option;
    final float x;
    final float y;

    private RemoteCommand(Type type, String actorId, String verb, String target, int option, float x, float y) {
        this.type = type;
        this.actorId = actorId;
        this.verb = verb;
        this.target = target;
        this.option = option;
        this.x = x;
        this.y = y;
    }

    static RemoteCommand actorVerb(String actorId, String verb, String target) {
        return new RemoteCommand(Type.ACTOR_VERB, actorId, verb, target, 0, 0, 0);
    }

    static RemoteCommand sceneVerb(String verb) {
        return new RemoteCommand(Type.SCENE_VERB, null, verb, null, 0, 0, 0);
    }

    static RemoteCommand dialogOption(int option) {
        return new RemoteCommand(Type.DIALOG_OPTION, null, null, null, option, 0, 0);
    }

    static RemoteCommand gotoPosition(float x, float y) {
        return new RemoteCommand(Type.GOTO, null, null, null, 0, x, y);
    }

    static RemoteCommand saveGame(String target) {
        return new RemoteCommand(Type.SAVE_GAME, null, null, target, 0, 0, 0);
    }

    static RemoteCommand screenshot(String target) {
        return new RemoteCommand(Type.SCREENSHOT, null, null, target, 0, 0, 0);
    }
}
