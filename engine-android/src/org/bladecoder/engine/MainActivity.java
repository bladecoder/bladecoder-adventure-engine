package org.bladecoder.engine;

import android.os.Bundle; 
import android.view.WindowManager;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

public class MainActivity extends AndroidApplication {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        AndroidApplicationConfiguration cfg = new AndroidApplicationConfiguration();
        cfg.useAccelerometer = false;
        cfg.useCompass = false;
        cfg.numSamples = 2;
        
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        BladeEngine engine = new BladeEngine();
        
//        engine.setDebugMode();
//        engine.forceResolution("1920");
//        engine.setPlayMode("full");
        
        initialize(engine, cfg);
    }
}