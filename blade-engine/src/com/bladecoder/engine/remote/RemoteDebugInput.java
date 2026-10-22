package com.bladecoder.engine.remote;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.utils.JsonValue;
import java.util.HashSet;
import java.util.Set;

/** Debug-session input adapter. Routes events through the real UI without moving the OS cursor. */
final class RemoteDebugInput implements Input {
    private final Input delegate;
    private final RemoteDebugEvents events;
    private InputProcessor processor;
    private boolean virtual, touched;
    private int x, y, dx, dy;
    private long eventTime;
    private final Set<Integer> buttons = new HashSet<Integer>();
    private final Set<Integer> keys = new HashSet<Integer>();
    private final Set<Integer> newButtons = new HashSet<Integer>();
    private final Set<Integer> newKeys = new HashSet<Integer>();

    RemoteDebugInput(Input delegate, RemoteDebugEvents events) {
        this.delegate=delegate;this.events=events;
        x=delegate.getX();y=delegate.getY();processor=delegate.getInputProcessor();
        delegate.setInputProcessor(physical);
    }
    private void physical() {
        if(virtual) {
            reset();virtual=false;
            if(events!=null)events.add("input.physical",RemoteDebugState.map("synthetic_state_released",true));
        }
    }
    private final InputProcessor physical = new InputProcessor() {
        public boolean keyDown(int k){physical();return processor!=null && processor.keyDown(k);}
        public boolean keyUp(int k){physical();return processor!=null && processor.keyUp(k);}
        public boolean keyTyped(char c){physical();return processor!=null && processor.keyTyped(c);}
        public boolean touchDown(int x,int y,int p,int b){physical();return processor!=null && processor.touchDown(x,y,p,b);}
        public boolean touchUp(int x,int y,int p,int b){physical();return processor!=null && processor.touchUp(x,y,p,b);}
        public boolean touchCancelled(int x,int y,int p,int b){physical();return processor!=null && processor.touchCancelled(x,y,p,b);}
        public boolean touchDragged(int x,int y,int p){physical();return processor!=null && processor.touchDragged(x,y,p);}
        public boolean mouseMoved(int x,int y){physical();return processor!=null && processor.mouseMoved(x,y);}
        public boolean scrolled(float x,float y){physical();return processor!=null && processor.scrolled(x,y);}
    };
    /** LWJGL makeCurrent restores Gdx.input after a window mode/size change. */
    void restoreAfterBackendReset() {
        if(com.badlogic.gdx.Gdx.input==delegate) {
            InputProcessor current=delegate.getInputProcessor();
            if(current!=physical)processor=current;
            delegate.setInputProcessor(physical);
            com.badlogic.gdx.Gdx.input=this;
            if(events!=null)events.add("input.backend_restored",RemoteDebugState.map("processor",processor==null?null:processor.getClass().getName()));
        }
    }
    Input delegate(){return delegate;}
    void close(){reset();delegate.setInputProcessor(processor);}
    void endFrame(){newKeys.clear();newButtons.clear();touched=false;dx=dy=0;}
    private void reset() {
        if(processor!=null) {
            for(int b:new HashSet<Integer>(buttons))processor.touchCancelled(x,y,0,b);
            // Key release is delivered so focused controls do not retain a held key.
            for(int k:new HashSet<Integer>(keys))processor.keyUp(k);
        }
        buttons.clear();keys.clear();endFrame();
    }
    static int integer(JsonValue a,String name,int min,int max) {
        JsonValue v=a.require(name);
        if(!v.isLong() || v.asLong()<min || v.asLong()>max)throw new IllegalArgumentException("Invalid input "+name);
        return v.asInt();
    }
    boolean inject(JsonValue a,int width,int height) {
        String type=a.getString("type");
        int nx=getX(),ny=getY(),button=0,key=0;char character=0;float amountX=0,amountY=0;
        if(processor==null)throw new IllegalStateException("No active input processor");
        if(type.equals("mouseMoved") || type.equals("touchDown") || type.equals("touchDragged") || type.equals("touchUp")) {
            nx=integer(a,"x",0,width-1);ny=integer(a,"y",0,height-1);
            if(type.equals("touchDown") || type.equals("touchUp"))button=integer(a,"button",0,2);
            if(type.equals("touchDown") && !buttons.isEmpty())throw new IllegalArgumentException("Only one synthetic pointer/button is supported");
            if(type.equals("touchUp") && !buttons.contains(button))throw new IllegalArgumentException("Synthetic button is not held");
            if(type.equals("touchDragged") && buttons.isEmpty())throw new IllegalArgumentException("Press a synthetic button before dragging");
        } else if(type.equals("keyDown") || type.equals("keyUp")) {
            key=integer(a,"keycode",0,Keys.MAX_KEYCODE);
            if(type.equals("keyUp") && !keys.contains(key))throw new IllegalArgumentException("Synthetic key is not held");
        } else if(type.equals("keyTyped")) {
            String text=a.getString("character");
            if(text.length()!=1 || Character.isSurrogate(text.charAt(0)))throw new IllegalArgumentException("Use one UTF-16 character");
            character=text.charAt(0);
        } else if(type.equals("scrolled")) {
            amountX=a.getFloat("amountX",0);amountY=a.getFloat("amountY",0);
            if(!Float.isFinite(amountX) || !Float.isFinite(amountY) || Math.abs(amountX)>64 || Math.abs(amountY)>64)
                throw new IllegalArgumentException("Invalid scroll amount");
        } else if(!type.equals("reset"))throw new IllegalArgumentException("Unsupported input event");
        if(!virtual){x=delegate.getX();y=delegate.getY();}
        virtual=true;dx=nx-x;dy=ny-y;x=nx;y=ny;eventTime=System.nanoTime();
        if(type.equals("reset")){reset();virtual=false;return true;}
        if(type.equals("mouseMoved"))return processor.mouseMoved(x,y);
        if(type.equals("touchDown")){buttons.add(button);newButtons.add(button);touched=true;return processor.touchDown(x,y,0,button);}
        if(type.equals("touchDragged"))return processor.touchDragged(x,y,0);
        if(type.equals("touchUp")){buttons.remove(button);return processor.touchUp(x,y,0,button);}
        if(type.equals("keyDown")){if(keys.add(key))newKeys.add(key);return processor.keyDown(key);}
        if(type.equals("keyUp")){keys.remove(key);return processor.keyUp(key);}
        if(type.equals("keyTyped"))return processor.keyTyped(character);
        return processor.scrolled(amountX,amountY);
    }
    @Override public int getX(){return virtual?x:delegate.getX();}
    @Override public int getY(){return virtual?y:delegate.getY();}
    @Override public int getX(int p){return virtual && p==0?x:delegate.getX(p);}
    @Override public int getY(int p){return virtual && p==0?y:delegate.getY(p);}
    @Override public int getDeltaX(){return virtual?dx:delegate.getDeltaX();}
    @Override public int getDeltaY(){return virtual?dy:delegate.getDeltaY();}
    @Override public int getDeltaX(int p){return virtual && p==0?dx:delegate.getDeltaX(p);}
    @Override public int getDeltaY(int p){return virtual && p==0?dy:delegate.getDeltaY(p);}
    @Override public boolean isTouched(){return virtual?!buttons.isEmpty():delegate.isTouched();}
    @Override public boolean isTouched(int p){return virtual && p==0?!buttons.isEmpty():delegate.isTouched(p);}
    @Override public boolean justTouched(){return virtual?touched:delegate.justTouched();}
    @Override public float getPressure(){return virtual?(isTouched()?1:0):delegate.getPressure();}
    @Override public float getPressure(int p){return virtual && p==0?getPressure():delegate.getPressure(p);}
    @Override public boolean isButtonPressed(int b){return virtual?buttons.contains(b):delegate.isButtonPressed(b);}
    @Override public boolean isButtonJustPressed(int b){return virtual?newButtons.contains(b):delegate.isButtonJustPressed(b);}
    @Override public boolean isKeyPressed(int k){return virtual?(k==Keys.ANY_KEY?!keys.isEmpty():keys.contains(k)):delegate.isKeyPressed(k);}
    @Override public boolean isKeyJustPressed(int k){return virtual?(k==Keys.ANY_KEY?!newKeys.isEmpty():newKeys.contains(k)):delegate.isKeyJustPressed(k);}
    @Override public long getCurrentEventTime(){return virtual?eventTime:delegate.getCurrentEventTime();}
    @Override public void setInputProcessor(InputProcessor p){processor=p;delegate.setInputProcessor(physical);}
    @Override public InputProcessor getInputProcessor(){return processor;}
    @Override public void setCursorPosition(int x,int y){if(virtual){this.x=x;this.y=y;}else delegate.setCursorPosition(x,y);}
    @Override public float getAccelerometerX() { return delegate.getAccelerometerX(); }
    @Override public float getAccelerometerY() { return delegate.getAccelerometerY(); }
    @Override public float getAccelerometerZ() { return delegate.getAccelerometerZ(); }
    @Override public float getGyroscopeX() { return delegate.getGyroscopeX(); }
    @Override public float getGyroscopeY() { return delegate.getGyroscopeY(); }
    @Override public float getGyroscopeZ() { return delegate.getGyroscopeZ(); }
    @Override public int getMaxPointers() { return delegate.getMaxPointers(); }
    @Override public void getTextInput(com.badlogic.gdx.Input.TextInputListener p0, java.lang.String p1, java.lang.String p2, java.lang.String p3) { delegate.getTextInput(p0, p1, p2, p3); }
    @Override public void getTextInput(com.badlogic.gdx.Input.TextInputListener p0, java.lang.String p1, java.lang.String p2, java.lang.String p3, com.badlogic.gdx.Input.OnscreenKeyboardType p4) { delegate.getTextInput(p0, p1, p2, p3, p4); }
    @Override public void setOnscreenKeyboardVisible(boolean p0) { delegate.setOnscreenKeyboardVisible(p0); }
    @Override public void setOnscreenKeyboardVisible(boolean p0, com.badlogic.gdx.Input.OnscreenKeyboardType p1) { delegate.setOnscreenKeyboardVisible(p0, p1); }
    @Override public void openTextInputField(com.badlogic.gdx.input.NativeInputConfiguration p0) { delegate.openTextInputField(p0); }
    @Override public void closeTextInputField(boolean p0) { delegate.closeTextInputField(p0); }
    @Override public void closeTextInputField(boolean p0, com.badlogic.gdx.input.NativeInputConfiguration.NativeInputCloseCallback p1) { delegate.closeTextInputField(p0, p1); }
    @Override public boolean isTextInputFieldOpened() { return delegate.isTextInputFieldOpened(); }
    @Override public void setKeyboardHeightObserver(com.badlogic.gdx.Input.KeyboardHeightObserver p0) { delegate.setKeyboardHeightObserver(p0); }
    @Override public void vibrate(int p0) { delegate.vibrate(p0); }
    @Override public void vibrate(int p0, boolean p1) { delegate.vibrate(p0, p1); }
    @Override public void vibrate(int p0, int p1, boolean p2) { delegate.vibrate(p0, p1, p2); }
    @Override public void vibrate(com.badlogic.gdx.Input.VibrationType p0) { delegate.vibrate(p0); }
    @Override public float getAzimuth() { return delegate.getAzimuth(); }
    @Override public float getPitch() { return delegate.getPitch(); }
    @Override public float getRoll() { return delegate.getRoll(); }
    @Override public void getRotationMatrix(float[] p0) { delegate.getRotationMatrix(p0); }
    @Override public void setCatchKey(int p0, boolean p1) { delegate.setCatchKey(p0, p1); }
    @Override public boolean isCatchKey(int p0) { return delegate.isCatchKey(p0); }
    @Override public boolean isPeripheralAvailable(com.badlogic.gdx.Input.Peripheral p0) { return delegate.isPeripheralAvailable(p0); }
    @Override public int getRotation() { return delegate.getRotation(); }
    @Override public com.badlogic.gdx.Input.Orientation getNativeOrientation() { return delegate.getNativeOrientation(); }
    @Override public void setCursorCatched(boolean p0) { delegate.setCursorCatched(p0); }
    @Override public boolean isCursorCatched() { return delegate.isCursorCatched(); }
}
