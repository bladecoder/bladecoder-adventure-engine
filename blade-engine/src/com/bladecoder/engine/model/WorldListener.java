package com.bladecoder.engine.model;

public interface WorldListener {
    void cutMode(boolean value);

    void text(Text t);

    void dialogOptions();

    void inventoryEnabled(boolean value);

    void pause(boolean value);
}
