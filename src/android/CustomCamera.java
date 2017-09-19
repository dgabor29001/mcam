/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.cordova.camera;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.cameraview.AspectRatio;
import com.google.android.cameraview.CameraView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;


/**
 * This demo app saves the taken picture to a constant file.
 * $ adb pull /sdcard/Android/data/com.google.android.cameraview.demo/files/Pictures/picture.jpg
 */
public class CustomCamera extends AppCompatActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = "CustomCamera";

    private static final int REQUEST_CAMERA_PERMISSION = 1;

    private static final String FRAGMENT_DIALOG = "dialog";

    private static final int[] FLASH_OPTIONS = {
            CameraView.FLASH_AUTO,
            CameraView.FLASH_OFF,
            CameraView.FLASH_ON,
    };
   

    private int mCurrentFlash;

    private CameraView mCameraView;

    private Handler mBackgroundHandler;

    private SimpleOrientationListener mOrientationListener;

    private RecyclerView mGallery;
    private GalleryItemAdapter adapter;

    private String package_name;
    private Resources resources;

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.take_picture:
                    if (mCameraView != null) {
                        mCameraView.takePicture();
                    }
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // remove title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //end remove title


        package_name = getApplication().getPackageName();
        resources = getApplication().getResources();
        
        private static final int[] FLASH_ICONS = {
            resources.getIdentifier("ic_flash_auto", "drawable", package_name),
            resources.getIdentifier("ic_flash_off", "drawable", package_name),
            resources.getIdentifier("ic_flash_on", "drawable", package_name)
        };
         
        private static final int[] FLASH_TITLES = {
            resources.getIdentifier("flash_auto", "string", package_name),
            resources.getIdentifier("flash_off", "string", package_name),
            resources.getIdentifier("flash_on", "string", package_name)
        };

        setContentView(resources.getIdentifier("mcam_main", "layout", package_name));//setContentView(R.layout.mcam_main);

        mCameraView = (CameraView) findViewById(resources.getIdentifier("camera", "id", package_name));//mCameraView = (CameraView) findViewById(R.id.camera);


        FloatingActionButton fab = (FloatingActionButton) findViewById(resources.getIdentifier("take_picture", "id", package_name));//(FloatingActionButton) findViewById(R.id.take_picture);
        if (fab != null) {
            fab.setOnClickListener(mOnClickListener);
        }

        Toolbar toolbar = (Toolbar) findViewById(resources.getIdentifier("toolbar", "id", package_name));//Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }

        mGallery = (RecyclerView) findViewById(resources.getIdentifier("mcam_gallery", "id", package_name));//mGallery = (RecyclerView) findViewById(R.id.mcam_gallery);

        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            mGallery.setLayoutManager(new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.VERTICAL, false));
        } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            mGallery.setLayoutManager(new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.HORIZONTAL, false));
        }

        mOrientationListener = new SimpleOrientationListener(getApplicationContext()) {
            @Override
            public void onSimpleOrientationChanged(int orientation) {
                //mOrientation = orientation;

                if(orientation == Configuration.ORIENTATION_UNDEFINED || orientation == Configuration.ORIENTATION_LANDSCAPE){
                    //mOrientation = orientation;
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }else if(orientation == Configuration.ORIENTATION_PORTRAIT){
                    //mOrientation = orientation;
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                }else{
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }

            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mOrientationListener!=null) mOrientationListener.enable();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED ) {

            mCameraView.start();
            loadGallery();

            //start custom ratios
            final Set<AspectRatio> ratios = mCameraView.getSupportedAspectRatios();
            if (ratios != null && ratios.size() != 0) {
                AspectRatio[] aRatios = ratios.toArray(new AspectRatio[ratios.size()]);
                Arrays.sort(aRatios);
                mCameraView.setAspectRatio(aRatios[aRatios.length-1]);
            }
            mCameraView.addCallback(mCallback);
            //end custom ratio

        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ConfirmationDialogFragment
                    .newInstance(resources.getIdentifier("camera_permission_confirmation", "string", package_name),
                            new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE},
                            REQUEST_CAMERA_PERMISSION,
                            resources.getIdentifier("camera_permission_not_granted", "string", package_name))
                    .show(getSupportFragmentManager(), FRAGMENT_DIALOG);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    protected void onPause() {
        mCameraView.stop();
        mOrientationListener.disable();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBackgroundHandler != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mBackgroundHandler.getLooper().quitSafely();
            } else {
                mBackgroundHandler.getLooper().quit();
            }
            mBackgroundHandler = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION:
                if (permissions.length != 2 || grantResults.length != 2) {
                    throw new RuntimeException("Error on requesting camera permission.");
                }
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, resources.getIdentifier("camera_permission_not_granted", "string", package_name),
                            Toast.LENGTH_SHORT).show();
                }
                // No need to start camera here; it is handled by onResume
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(resources.getIdentifier("main", "menu", package_name), menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case resources.getIdentifier("switch_flash", "id", package_name):
                if (mCameraView != null) {
                    mCurrentFlash = (mCurrentFlash + 1) % FLASH_OPTIONS.length;
                    item.setTitle(FLASH_TITLES[mCurrentFlash]);
                    item.setIcon(FLASH_ICONS[mCurrentFlash]);
                    mCameraView.setFlash(FLASH_OPTIONS[mCurrentFlash]);
                }
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    private Handler getBackgroundHandler() {
        if (mBackgroundHandler == null) {
            HandlerThread thread = new HandlerThread("background");
            thread.start();
            mBackgroundHandler = new Handler(thread.getLooper());
        }
        return mBackgroundHandler;
    }

    private CameraView.Callback mCallback
            = new CameraView.Callback() {

        @Override
        public void onCameraOpened(CameraView cameraView) {
            Log.d(TAG, "onCameraOpened");
        }

        @Override
        public void onCameraClosed(CameraView cameraView) {
            Log.d(TAG, "onCameraClosed");
        }

        @Override
        public void onPictureTaken(CameraView cameraView, final byte[] data) {
            Log.d(TAG, "onPictureTaken " + data.length);            
            getBackgroundHandler().post(new Runnable() {
                @Override
                public void run() {
                    Calendar cal = new GregorianCalendar();
                    Date date = cal.getTime();
                    SimpleDateFormat df = new SimpleDateFormat("yyyyMMddhhmmss", new Locale("es_ES"));
                    String formatteDate = df.format(date);

                    String fotoName = "gecor" + System.currentTimeMillis();

                    File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                            fotoName + formatteDate + ".png");
                    OutputStream os = null;
                    try {
                        os = new FileOutputStream(file);
                        os.write(data);
                        os.close();
                       

                    } catch (IOException e) {
                        Log.w(TAG, "Cannot write to " + file, e);
                    } finally {
                        if (os != null) {
                            try {
                                os.close();
                            } catch (IOException e) {
                                // Ignore
                            }

                            setResult(Activity.RESULT_OK, new Intent().setData(Uri.fromFile(file)));
                            finish();
                        } else {
                            setResult(Activity.RESULT_CANCELED, new Intent());
                            finish();
                        }
                    }
                }
            });
        }

    };

    private ArrayList<String> getAllShownImagesPath() {

        Cursor cursor;
        String absolutePathOfImage;
        int column_index_data;
        ArrayList<String> listOfAllImages = new ArrayList<>();

        cursor = getApplicationContext().getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{
                        MediaStore.Images.Media._ID,
                        MediaStore.Images.Media.DATA,
                        MediaStore.Images.Media.ORIENTATION,
                        MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                        MediaStore.Images.Media.BUCKET_ID,
                        MediaStore.Images.Media.MIME_TYPE ,
                },
                null,
                null,
                MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC"
        );

        if(cursor != null) {
            column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);

            while (cursor.moveToNext()) {
                absolutePathOfImage = cursor.getString(column_index_data);

                listOfAllImages.add(absolutePathOfImage);
            }
        }

        cursor = getApplicationContext().getContentResolver().query(
                MediaStore.Images.Media.INTERNAL_CONTENT_URI,
                new String[]{
                        MediaStore.Images.Media._ID,
                        MediaStore.Images.Media.DATA,
                        MediaStore.Images.Media.ORIENTATION,
                        MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                        MediaStore.Images.Media.BUCKET_ID,
                        MediaStore.Images.Media.MIME_TYPE
                },
                null,
                null,
                MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC"
        );

        if(cursor != null) {
            column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);

            while (cursor.moveToNext()) {
                absolutePathOfImage = cursor.getString(column_index_data);

                listOfAllImages.add(absolutePathOfImage);
            }
        }
        return listOfAllImages;
    }

    private void loadGallery(){

        adapter = new GalleryItemAdapter(getApplicationContext(), getAllShownImagesPath(),
                new GalleryItemAdapter.OnItemClickListener() {
                    @Override
                    public void galleryResponse(String img) {
                        Log.e(TAG, "galleryResponse: " + img);
                        try{
                            //Bundle conData = new Bundle();
                            //conData.putString("SDCardUrl", img);

                            Intent intent = new Intent();
                            //intent.putExtras(conData);
                            intent.setData(Uri.fromFile(new File(img)));
                            setResult(Activity.RESULT_OK, intent);
                        } catch (Exception e){
                            setResult(Activity.RESULT_CANCELED, new Intent());
                        }
                        finish();
                    }
                });

        mGallery.setAdapter(adapter);

        mGallery.setVisibility(View.VISIBLE);
    }

    public static class ConfirmationDialogFragment extends DialogFragment {

        private static final String ARG_MESSAGE = "message";
        private static final String ARG_PERMISSIONS = "permissions";
        private static final String ARG_REQUEST_CODE = "request_code";
        private static final String ARG_NOT_GRANTED_MESSAGE = "not_granted_message";

        public static ConfirmationDialogFragment newInstance(@StringRes int message,
                String[] permissions, int requestCode, @StringRes int notGrantedMessage) {
            ConfirmationDialogFragment fragment = new ConfirmationDialogFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_MESSAGE, message);
            args.putStringArray(ARG_PERMISSIONS, permissions);
            args.putInt(ARG_REQUEST_CODE, requestCode);
            args.putInt(ARG_NOT_GRANTED_MESSAGE, notGrantedMessage);
            fragment.setArguments(args);
            return fragment;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Bundle args = getArguments();
            return new AlertDialog.Builder(getActivity())
                    .setMessage(args.getInt(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String[] permissions = args.getStringArray(ARG_PERMISSIONS);
                                    if (permissions == null) {
                                        throw new IllegalArgumentException();
                                    }
                                    ActivityCompat.requestPermissions(getActivity(),
                                            permissions, args.getInt(ARG_REQUEST_CODE));
                                }
                            })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Toast.makeText(getActivity(),
                                            args.getInt(ARG_NOT_GRANTED_MESSAGE),
                                            Toast.LENGTH_SHORT).show();
                                }
                            })
                    .create();
        }

    }

}