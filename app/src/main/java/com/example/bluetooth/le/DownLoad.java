package com.example.bluetooth.le;
import android.app.Activity;
import android.app.Dialog;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import com.example.bluetooth.le.activity.OtaActiviy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class DownLoad {

	URL fileurl = null;
	String urlfile;
	boolean bisConnFlag = false;
	private OutputStream outStream;
	private InputStream inStream;
	private final int BUSY = 1;
	private final int SUCCESS = 2;
	private final int FAILED = 3;
	private final int TIMEOUT = 4;
	private final int OPEN = 5;
	private Message myMessage;
	private int length;
	private InputStream is;
	OutputStream fos;
	private Activity currentActivity;
	private Dialog mDialog;
	private String sdPath;
	private String PATH;
	
	public DownLoad(Activity activity, Dialog dialog) {
		this.currentActivity = activity;
		this.mDialog = dialog;
	}
	
	public void downloadFile(String urlfile, String savePath) throws MalformedURLException {
		this.urlfile = urlfile;
		this.PATH = savePath;
		
		ConnectivityManager conManager = (ConnectivityManager) currentActivity
				.getSystemService(currentActivity.CONNECTIVITY_SERVICE);
		myMessage = new Message();
		sdPath = Environment.getExternalStorageDirectory().getAbsolutePath();
		
		NetworkInfo network = conManager.getActiveNetworkInfo();
		if (network != null) {
			bisConnFlag = conManager.getActiveNetworkInfo().isAvailable();
		}
		if (bisConnFlag) {
			MyThread thread = new MyThread();
			thread.start();
		} else {
			myMessage = myhandler.obtainMessage(OPEN);
			myhandler.sendMessage(myMessage);
		}
	}

	class MyThread extends Thread {
		@Override
		public void run() {
			super.run();
			try {
				fileurl = new URL(urlfile);

				HttpURLConnection conn = (HttpURLConnection) fileurl
						.openConnection();
				conn.setConnectTimeout(15 * 1000);
				conn.setDoInput(true);

				conn.connect();

				is = conn.getInputStream();

				length = conn.getContentLength();
			} catch (IOException e) {
				myMessage = myhandler.obtainMessage(TIMEOUT);
				myhandler.sendMessage(myMessage);
				e.printStackTrace();
				return;
			}
			
			if (length > 0) {
				File file = new File(PATH);
				File parent = file.getParentFile();
				if (!parent.exists()) {
					parent.mkdirs();
				}
				
				try {
					fos = new FileOutputStream(file);
					
					byte[] buffer = new byte[4096];
					int readLen = 0;
					int total = 0;
					
					try {
						while ((readLen = is.read(buffer)) > 0) {
							fos.write(buffer, 0, readLen);
							total += readLen;
							
							// Update progress
							int progress = (int) ((total * 100) / length);
							Message progressMsg = new Message();
							progressMsg.what = 100; // Custom message for progress
							progressMsg.arg1 = progress;
							myhandler.sendMessage(progressMsg);
						}
						
						fos.flush();
						fos.close();
						is.close();
						
						if (file.length() == length) {
							myMessage = myhandler.obtainMessage(SUCCESS);
							myhandler.sendMessage(myMessage);
						} else {
							myMessage = myhandler.obtainMessage(FAILED);
							myhandler.sendMessage(myMessage);
						}
					} catch (IOException e) {
						e.printStackTrace();
						myMessage = myhandler.obtainMessage(FAILED);
						myhandler.sendMessage(myMessage);
					}
				} catch (FileNotFoundException e) {
					e.printStackTrace();
					myMessage = myhandler.obtainMessage(FAILED);
					myhandler.sendMessage(myMessage);
				}
			} else {
				myMessage = myhandler.obtainMessage(FAILED);
				myhandler.sendMessage(myMessage);
			}
		}
	}

	private Handler myhandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case SUCCESS:
				mDialog.dismiss();
				Toast.makeText(currentActivity, "Download successful",
						Toast.LENGTH_SHORT).show();
				
				// Update UI with file path
				if (currentActivity instanceof OtaActiviy) {
					((OtaActiviy) currentActivity).updateFilePath(PATH);
				}
				break;
			case FAILED:
				mDialog.dismiss();
				Toast.makeText(currentActivity, "Download failed",
						Toast.LENGTH_SHORT).show();
				break;
			case TIMEOUT:
				mDialog.dismiss();
				Toast.makeText(currentActivity, "Connection timeout",
						Toast.LENGTH_SHORT).show();
				break;
			case BUSY:
				mDialog.dismiss();
				Toast.makeText(currentActivity, "Device is busy",
						Toast.LENGTH_SHORT).show();
				break;
			case OPEN:
				mDialog.dismiss();
				Toast.makeText(currentActivity, "Please enable network connection",
						Toast.LENGTH_SHORT).show();
				break;
			case 100:
				// Update progress
				if (currentActivity instanceof OtaActiviy) {
					((OtaActiviy) currentActivity).updateDownloadProgress(msg.arg1);
				}
				break;
			}
		}
	};
}
