package com.bladecoder.engine.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;

public class EngineLogger {
    public interface Listener {
        void log(String level, String message, Throwable failure);
        default void diagnostic(String type, java.util.Map<String,Object> data) { }
    }
    private static final java.util.concurrent.CopyOnWriteArrayList<Listener> listeners = new java.util.concurrent.CopyOnWriteArrayList<Listener>();
    public static void diagnostic(String type, Object... fields) {
        if (listeners.isEmpty()) return;
        java.util.Map<String,Object> data = new java.util.LinkedHashMap<String,Object>();
        for (int i=0;i<fields.length;i+=2) data.put((String)fields[i],fields[i+1]);
        for (Listener listener : listeners) { try { listener.diagnostic(type,data); } catch (RuntimeException ignored) { } }
    }
    public static boolean hasListeners(){return !listeners.isEmpty();}
    public static void addListener(Listener listener) { listeners.addIfAbsent(listener); }
    public static void removeListener(Listener listener) { listeners.remove(listener); }
    private static void emit(String level, String message, Throwable error) {
        for (Listener listener : listeners) { try { listener.log(level, message, error); } catch (RuntimeException ignored) { } }
    }

	private static String TAG = "ENGINE";
	private static int level = Application.LOG_ERROR;
	private static final int MAX_BUFFER_SIZE = 512 * 1024;

	public static final int DEBUG0 = 0;
	public static final int DEBUG1 = 1;

	public static int debugLevel = DEBUG0;

	public static String lastError = null;
	public static Exception lastException = null;
	public static StringBuilder errorBuffer = new StringBuilder();

	public static void debug(String message) {
		emit("debug", message, null);
		Gdx.app.debug(TAG, message);
	}

	public static void error(String message) {
		if (message != null) {
			emit("error", message, null);
			Gdx.app.error(TAG, message);
			lastError = message;
			lastException = null;

			if (errorBuffer.length() > MAX_BUFFER_SIZE) {
				errorBuffer.setLength(0);
			}

			errorBuffer.append(message + "\n");
		}
	}

	public static void error(String message, Exception e) {
		error(message);

		if (e != null) {
			emit("exception", message, e);
			Gdx.app.error(TAG, message, e);
			lastException = e;

			if (errorBuffer.length() > MAX_BUFFER_SIZE) {
				errorBuffer.setLength(0);
			}

			// print stack trace to buffer
			Writer result = new StringWriter();
			PrintWriter printWriter = new PrintWriter(result);
			e.printStackTrace(printWriter);
			errorBuffer.append(result.toString());
		}
	}

	public static void toggle() {
		if (level == Application.LOG_DEBUG)
			level = Application.LOG_ERROR;
		else
			level = Application.LOG_DEBUG;

		Gdx.app.setLogLevel(level);
	}

	public static boolean debugMode() {
		if (level == Application.LOG_DEBUG)
			return true;

		return false;
	}

	public static int getDebugLevel() {
		return debugLevel;
	}

	public static void setDebugLevel(int level) {
		debugLevel = level;
	}

	public static void setDebug() {
		level = Application.LOG_DEBUG;

		Gdx.app.setLogLevel(level);
	}
}
