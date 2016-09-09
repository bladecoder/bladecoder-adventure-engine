/*******************************************************************************
 * Copyright 2014 Rafael Garcia Moreno.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.bladecoder.engineeditor.common;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author Aurelien Ribon | http://www.aurelienribon.com/
 */
public class HttpUtils {
	public static DownloadTask downloadAsync(URL input, OutputStream output, Callback callback) {
		final DownloadTask task = new DownloadTask(input, output, callback);

		new Thread(new Runnable() {
			@Override
			public void run() {
				task.download();
			}
		}).start();

		return task;
	}

	public static interface Callback {
		public void completed();

		public void canceled();

		public void error(IOException ex);

		public void updated(int length, int totalLength);
	}

	public static class DownloadTask {
		private final URL input;
		private final OutputStream output;
		private final Callback callback;
		private boolean run = true;

		public DownloadTask(URL input, OutputStream output, Callback callback) {
			this.input = input;
			this.output = output;
			this.callback = callback;
		}

		public void stop() {
			run = false;
		}

		public URL getInput() {
			return input;
		}

		public OutputStream getOutput() {
			return output;
		}

		public Callback getCallback() {
			return callback;
		}

		private void download() {
			OutputStream os = null;
			InputStream is = null;
			IOException ex = null;

			try {
				HttpURLConnection connection = (HttpURLConnection) input.openConnection();
				connection.setDoInput(true);
				connection.setDoOutput(false);
				connection.setUseCaches(true);
				connection.setConnectTimeout(3000);
				connection.connect();

				is = new BufferedInputStream(connection.getInputStream(), 4096);
				os = output;

				byte[] data = new byte[4096];
				int length = connection.getContentLength();
				int total = 0;

				int count;
				while (run && (count = is.read(data)) != -1) {
					total += count;
					os.write(data, 0, count);
					if (callback != null)
						callback.updated(total, length);
				}

			} catch (IOException ex1) {
				ex = ex1;

			} finally {
				if (os != null)
					try {
						os.flush();
						os.close();
					} catch (IOException ex1) {
					}
				if (is != null)
					try {
						is.close();
					} catch (IOException ex1) {
					}

				if (callback != null) {
					if (ex != null)
						callback.error(ex);
					else if (run == true)
						callback.completed();
					else
						callback.canceled();
				}
			}
		}
	}

	public static String excutePost(String targetURL, String urlParameters) {
		HttpURLConnection connection = null;
		try {
			// Create connection
			URL url = new URL(targetURL);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

			connection.setRequestProperty("Content-Length", Integer.toString(urlParameters.getBytes().length));
			connection.setRequestProperty("Content-Language", "en-US");

			connection.setUseCaches(false);
			connection.setDoOutput(true);

			// Send request
			DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
			wr.writeBytes(urlParameters);
			wr.close();

			// Get Response
			InputStream is = connection.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			StringBuilder response = new StringBuilder(); // or StringBuffer if
															// not Java 5+
			String line;
			while ((line = rd.readLine()) != null) {
				response.append(line);
				response.append('\r');
			}
			rd.close();
			return response.toString();
		} catch (Exception e) {
			EditorLogger.printStackTrace(e);
			return null;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	public static String excuteHTTP(String targetURL, String urlParameters) {
		BufferedReader in = null;
		StringBuilder response = new StringBuilder();

		try {
			String httpsURL = targetURL;
			URL myurl = new URL(httpsURL);
			URLConnection con = myurl.openConnection();
			InputStream ins = con.getInputStream();
//			((HttpURLConnection)con).setRequestMethod("GET");
			InputStreamReader isr = new InputStreamReader(ins);
			in = new BufferedReader(isr);

			String inputLine;

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
		} catch (IOException e) {
			EditorLogger.printStackTrace(e);
			return null;
		} finally {
			if(in != null)
				try {
					in.close();
				} catch (IOException e) {
					return null;
				}
		}
		
		return response.toString();
	}
}
