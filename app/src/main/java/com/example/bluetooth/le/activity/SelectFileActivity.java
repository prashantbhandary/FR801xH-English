package com.example.bluetooth.le.activity;

import java.io.File;

import com.example.bluetooth.le.AdapterManager;
import com.example.bluetooth.le.BluetoothApplication;
import com.example.bluetooth.le.FileListAdapter;
import com.example.bluetooth.le.R;




import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;



public class SelectFileActivity extends Activity {
	ListView mFileListView;
	FileListAdapter mFileListAdapter;
	AdapterManager mAdapterManager;
	
	private Handler mOtherHandler;
	private Runnable updateFileListRunnable;
	
	private File file;   //Current file or folder
	
	private String sdcardPath;  //SD card path
	private String path;     //Current parent directory
	
	Button mBackBtn;  //Back button
	Button mEnsureBtn;   //Confirm button
	Button mCancelBtn;   //Cancel button

	TextView mLastClickView;   //Last clicked file - file name
	TextView mNowClickView;   //Currently clicked file - file name
	private boolean isSelected = false;   //Whether a file is selected (not a folder)

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.select_file);
		
		mFileListView = (ListView) findViewById(R.id.fileListView);
		mBackBtn = (Button) findViewById(R.id.selectFileBackBtn);
		mEnsureBtn = (Button) findViewById(R.id.selectFileEnsureBtn);
		mCancelBtn = (Button) findViewById(R.id.selectFileCancelBtn);
		Intent intent = getIntent();
		path = intent.getStringExtra("filepatch");
		//Get SD card directory
		sdcardPath = Environment.getExternalStorageDirectory().getAbsolutePath();
		if((path.length() == 0) || (path == null)){
			path = sdcardPath ;
		}else{
			path = path.substring(0, path.lastIndexOf("/"));
		}
		mAdapterManager = BluetoothApplication.getInstance().getAdapterManager();
		System.out.println("aaaa + "+BluetoothApplication.getInstance());
		mFileListView.setAdapter(mAdapterManager.getFileListAdapter());
		
		//First show all files and folders under SD card
		mAdapterManager.updateFileListAdapter(path);
		
		mFileListView.setOnItemClickListener(mFileListOnItemClickListener);
		mBackBtn.setOnClickListener(mBackBtnClickListener);
		mEnsureBtn.setOnClickListener(mEnsureBtnClickListener);
		mCancelBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				SelectFileActivity.this.finish();
			}
		});
		
	}
	
	/**
	 * 
	 */
	private OnItemClickListener mFileListOnItemClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view,
				int position, long id) {
			//Current file or folder operation
			file = (File) mFileListView.getAdapter().getItem(position);
			if(file.isFile()){
				//If it's a file, select it - change the file name color
				if(null != mLastClickView){
					//If a file was previously selected, cancel the previous selection - restore color
					mLastClickView.setTextColor(Color.WHITE);
				}
				//Change file name color, select file
				mNowClickView = (TextView) view.findViewById(R.id.fileNameTV);
				mNowClickView.setTextColor(Color.BLUE);
				isSelected = true;
				//Set as the last clicked file
				mLastClickView = mNowClickView;
			}else {
				//If it's a folder, display all files and folders under it
				path = file.getAbsolutePath();
				updateFileList();
			}							
		}

	};
	
	private OnClickListener mBackBtnClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			if(path.equals(sdcardPath)){
				//If current parent directory is SD card, do nothing
				return ;
			}
			//Return to the parent directory
			path = path.substring(0, path.lastIndexOf("/"));
			if(path.length() <= 0)
			{
				path = sdcardPath ;
				
			}	
			
			updateFileList();
		}
	};
	
	private OnClickListener mEnsureBtnClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			if(!isSelected){
				//No file selected
				Toast.makeText(SelectFileActivity.this, "Please select a file!", Toast.LENGTH_LONG).show();
				return ;
			}
			//Return the full path of the selected file
			Intent intent = new Intent();
			intent.putExtra(ReadWriteActivity.SEND_FILE_NAME, file.getAbsolutePath());
			SelectFileActivity.this.setResult(ReadWriteActivity.RESULT_CODE, intent);
			SelectFileActivity.this.finish();
		}
	};
	
	/**
	 * Display all files and folders under the parent directory path
	 */
	private void updateFileList() {
		if(null != mLastClickView){
			//If entering another folder, cancel the previous selection
			mLastClickView.setTextColor(Color.WHITE);
			mLastClickView = null;
			isSelected = false;
		}
		if(null == updateFileListRunnable){
			updateFileListRunnable = new Runnable() {
							
				@Override
				public void run() {
					
					mAdapterManager.updateFileListAdapter(path);
				}
			};
		}
		if(null == mOtherHandler){
			HandlerThread handlerThread = new HandlerThread("other_thread");
			handlerThread.start();
			mOtherHandler = new Handler(handlerThread.getLooper());
		}
		mOtherHandler.post(updateFileListRunnable);
	}
}
