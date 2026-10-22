package com.bladecoder.engine.remote;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.utils.JsonReader;

/** Input contract checks without graphics or OS input injection. */
final class RemoteDebugInputTest {
    private static int checks;
    private static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);checks++;}
    private static boolean inject(RemoteDebugInput input,String event){return input.inject(new JsonReader().parse(event),960,540);}
    private static void reject(RemoteDebugInput input,String event) {
        try {inject(input,event);throw new AssertionError("Invalid event accepted: "+event);}
        catch(IllegalArgumentException expected){checks++;}
    }
    static int run() {
        FakeInput backend=new FakeInput();
        final int[] received=new int[5];
        InputProcessor target=new InputAdapter() {
            public boolean mouseMoved(int x,int y){check(x==100 && y==200,"injected coordinates delivered");received[0]++;return true;}
            public boolean touchDown(int x,int y,int p,int b){check(p==0 && b==0,"pointer and button preserved");received[1]++;return true;}
            public boolean touchCancelled(int x,int y,int p,int b){received[2]++;return true;}
            public boolean keyDown(int key){received[3]++;return true;}
            public boolean keyUp(int key){received[4]++;return true;}
        };
        backend.processor=target;
        RemoteDebugInput input=new RemoteDebugInput(backend,null);
        check(input.getInputProcessor()==target,"original processor exposed");
        check(backend.getInputProcessor()!=target,"physical input bridge installed");
        check(inject(input,"{type:mouseMoved,x:100,y:200}"),"processor return preserved");
        check(input.getX()==100 && input.getY()==200,"polling sees injected coordinates");
        check(received[0]==1 && backend.cursorMoves==0,"event routing does not move the OS pointer");
        inject(input,"{type:touchDown,x:100,y:200,button:0}");
        check(input.isTouched() && input.justTouched() && input.isButtonPressed(0),"pressed polling state");
        input.endFrame();check(input.isTouched() && !input.justTouched(),"frame edges expire, held state persists");
        reject(input,"{type:touchDown,x:100,y:200,button:1}");
        reject(input,"{type:touchUp,x:100,y:200,button:1}");
        reject(input,"{type:mouseMoved,x:-1,y:200}");
        reject(input,"{type:mouseMoved,x:960,y:200}");
        reject(input,"{type:mouseMoved,x:1.5,y:200}");
        reject(input,"{type:keyDown,keycode:99999}");
        reject(input,"{type:keyTyped,character:ab}");
        reject(input,"{type:unknown}");
        check(input.getX()==100 && input.isButtonPressed(0),"invalid events do not change state");
        inject(input,"{type:reset}");
        check(received[2]==1 && !input.isTouched() && input.getX()==17,"reset cancels pointer focus and restores backend polling");
        reject(input,"{type:touchDragged,x:100,y:200}");
        inject(input,"{type:keyDown,keycode:62}");
        check(input.isKeyPressed(62) && input.isKeyPressed(Input.Keys.ANY_KEY) && input.isKeyJustPressed(62),"held key polling");
        input.endFrame();check(input.isKeyPressed(62) && !input.isKeyJustPressed(62),"key frame edge expires");
        // Real input takes over, releases synthetic state and still reaches the active processor.
        backend.getInputProcessor().keyDown(30);
        check(!input.isKeyPressed(62) && received[4]==1 && received[3]==2,"physical takeover releases synthetic keys");
        InputProcessor replacement=new InputAdapter();input.setInputProcessor(replacement);
        check(input.getInputProcessor()==replacement,"screen processor changes preserved");
        com.badlogic.gdx.Input saved=com.badlogic.gdx.Gdx.input;
        try {
            com.badlogic.gdx.Gdx.input=backend;
            input.restoreAfterBackendReset();
            check(com.badlogic.gdx.Gdx.input==input && input.getInputProcessor()==replacement,"backend global reset reinstalls adapter without losing screen");
            InputProcessor resized=new InputAdapter(){public boolean keyUp(int key){return true;}};
            com.badlogic.gdx.Gdx.input=backend;backend.setInputProcessor(resized);
            input.restoreAfterBackendReset();
            check(input.getInputProcessor()==resized && backend.getInputProcessor()!=resized,"direct backend screen changes adopted after resize");
            inject(input,"{type:keyDown,keycode:111}");
            check(inject(input,"{type:keyUp,keycode:111}"),"resized screen receives synthetic key release");
            input.close();check(backend.getInputProcessor()==resized,"close restores the resized screen processor");
        } finally {com.badlogic.gdx.Gdx.input=saved;}
        return checks;
    }
    private static final class FakeInput implements Input {
        InputProcessor processor;int cursorMoves;
        public float getAccelerometerX() { return 0; }
        public float getAccelerometerY() { return 0; }
        public float getAccelerometerZ() { return 0; }
        public float getGyroscopeX() { return 0; }
        public float getGyroscopeY() { return 0; }
        public float getGyroscopeZ() { return 0; }
        public int getMaxPointers() { return 0; }
        public int getX() { return 17; }
        public int getX(int p0) { return 17; }
        public int getDeltaX() { return 0; }
        public int getDeltaX(int p0) { return 0; }
        public int getY() { return 29; }
        public int getY(int p0) { return 29; }
        public int getDeltaY() { return 0; }
        public int getDeltaY(int p0) { return 0; }
        public boolean isTouched() { return false; }
        public boolean justTouched() { return false; }
        public boolean isTouched(int p0) { return false; }
        public float getPressure() { return 0; }
        public float getPressure(int p0) { return 0; }
        public boolean isButtonPressed(int p0) { return false; }
        public boolean isButtonJustPressed(int p0) { return false; }
        public boolean isKeyPressed(int p0) { return false; }
        public boolean isKeyJustPressed(int p0) { return false; }
        public void getTextInput(com.badlogic.gdx.Input.TextInputListener p0, java.lang.String p1, java.lang.String p2, java.lang.String p3) {  }
        public void getTextInput(com.badlogic.gdx.Input.TextInputListener p0, java.lang.String p1, java.lang.String p2, java.lang.String p3, com.badlogic.gdx.Input.OnscreenKeyboardType p4) {  }
        public void setOnscreenKeyboardVisible(boolean p0) {  }
        public void setOnscreenKeyboardVisible(boolean p0, com.badlogic.gdx.Input.OnscreenKeyboardType p1) {  }
        public void openTextInputField(com.badlogic.gdx.input.NativeInputConfiguration p0) {  }
        public void setKeyboardHeightObserver(com.badlogic.gdx.Input.KeyboardHeightObserver p0) {  }
        public void vibrate(int p0) {  }
        public void vibrate(int p0, boolean p1) {  }
        public void vibrate(int p0, int p1, boolean p2) {  }
        public void vibrate(com.badlogic.gdx.Input.VibrationType p0) {  }
        public float getAzimuth() { return 0; }
        public float getPitch() { return 0; }
        public float getRoll() { return 0; }
        public void getRotationMatrix(float[] p0) {  }
        public long getCurrentEventTime() { return 0; }
        public void setCatchKey(int p0, boolean p1) {  }
        public boolean isCatchKey(int p0) { return false; }
        public void setInputProcessor(com.badlogic.gdx.InputProcessor p0) { processor=p0; }
        public com.badlogic.gdx.InputProcessor getInputProcessor() { return processor; }
        public boolean isPeripheralAvailable(com.badlogic.gdx.Input.Peripheral p0) { return false; }
        public int getRotation() { return 0; }
        public com.badlogic.gdx.Input.Orientation getNativeOrientation() { return null; }
        public void setCursorCatched(boolean p0) {  }
        public boolean isCursorCatched() { return false; }
        public void setCursorPosition(int p0, int p1) { cursorMoves++; }
    }
}
