package com.bladecoder.engine.remote;

final class RemoteCommandResult {
    final boolean successful;
    final int status;
    final String message;

    private RemoteCommandResult(boolean successful, int status, String message) {
        this.successful = successful;
        this.status = status;
        this.message = message;
    }

    static RemoteCommandResult success() {
        return new RemoteCommandResult(true, 200, null);
    }

    static RemoteCommandResult rejected(String message) {
        return new RemoteCommandResult(false, 409, message);
    }

    static RemoteCommandResult failed(String message) {
        return new RemoteCommandResult(false, 500, message);
    }
}
