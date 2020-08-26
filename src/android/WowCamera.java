package org.apache.cordova.camera;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.ActionMenuView;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import com.camerakit.CameraKitView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.theartofdev.edmodo.cropper.CropImageView;

import static com.camerakit.CameraKit.FLASH_AUTO;
import static com.camerakit.CameraKit.FLASH_OFF;
import static com.camerakit.CameraKit.FLASH_ON;

public class WowCamera extends AppCompatActivity {

    private static final String TAG = "wOwCamera";

    private static final int REQUEST_READ_PERMISSION = 6969;

    private static final String FRAGMENT_DIALOG = "dialog";

    private static final int[] FLASH_OPTIONS = {
            FLASH_AUTO,
            FLASH_OFF,
            FLASH_ON,
    };

    private int[] FLASH_ICONS;

    private RecyclerView gallery;
    private GalleryItemAdapter adapter;
    private FloatingActionButton btnCapture;
    private AppCompatImageView btnFlash;

    private ActionMenuView wowMenu;

    private CameraKitView camera;
    private CropImageView cropImageView;

    private FakeR fakeR;
    private int currentFlash;
    private boolean save_on_sd;
    private long captureStartTime;

    @SuppressLint("WrongConstant")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //save_on_sd = getIntent().getExtras().getBoolean("yourBoolName");
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
          if (extras.containsKey("save")) {
            save_on_sd = extras.getBoolean("save", false);

            // TODO: Do something with the value of isNew.
          }
        }

        fakeR = new FakeR(this);

        setContentView(fakeR.getId("layout", "wowcam_main"));

        Toolbar t = (Toolbar) findViewById(fakeR.getId("id", "wowToolbar" ));
        wowMenu = (ActionMenuView) t.findViewById(fakeR.getId("id", "wowMenu" ));
        wowMenu.setOnMenuItemClickListener(new ActionMenuView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                //Log.e(TAG, "onMenuItemClick: " + menuItem.toString());

                if(menuItem.getItemId() == fakeR.getId("id", "cancel" )) {
                    setResult(Activity.RESULT_CANCELED, new Intent());
                    finish();
                } else if(menuItem.getItemId() == fakeR.getId("id", "rotate" )) {
                    if(cropImageView != null) cropImageView.rotateImage(90);
                } else if(menuItem.getItemId() == fakeR.getId("id", "save" )) {

                    Bitmap b = cropImageView.getCroppedImage();

                    //String url = MediaStore.Images.Media.insertImage(getApplicationContext().getContentResolver(), b, "" , "");
                    //Log.w(TAG, "MediaStore.Images.Media.insertImage: " + url);


                    Calendar cal = new GregorianCalendar();
                    Date date = cal.getTime();
                    SimpleDateFormat df = new SimpleDateFormat("yyyyMMddhhmmss", new Locale("es_ES"));
                    String formatteDate = df.format(date);

                    String fotoName = "gecor" + System.currentTimeMillis();

                    //if()

                    File file = new File(getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/" + fotoName + formatteDate + ".jpg");

                    Boolean res = false;

                    try {

                        res = b.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(file));

                    } catch (IOException e) {
                        Log.w(TAG, "Cannot write to " + file, e);
                    } finally {
                        if (res) {
                            setResult(Activity.RESULT_OK, new Intent().setData(Uri.fromFile(file)).putExtra("save", save_on_sd));
                            finish();
                        } else {
                            setResult(Activity.RESULT_CANCELED, new Intent());
                            finish();
                        }
                    }

                }//endif

                return onOptionsItemSelected(menuItem);
            }
        });

        setSupportActionBar(t);
        getSupportActionBar().setTitle(null);
        getSupportActionBar().hide();

        FLASH_ICONS = new int[] {
                fakeR.getId("drawable", "ic_flash_auto" ),
                fakeR.getId("drawable","ic_flash_off"),
                fakeR.getId( "drawable", "ic_flash_on"),
        };

        cropImageView = (CropImageView) findViewById(fakeR.getId("id", "cropImageView" ));

        camera = (CameraKitView) findViewById(fakeR.getId("id", "camera" ));
        /*
        Old Camera
        camera.setCameraListener(new CameraListener() {
            @Override
            public void onPictureTaken(byte[] picture) {
                super.onPictureTaken(picture);

                switchToCropper(true);
                btnCapture.setEnabled(true);

                // Create a bitmap
                Bitmap result = BitmapFactory.decodeByteArray(picture, 0, picture.length);
                cropImageView.setImageBitmap(result);
            }
        });
        */

        gallery = (RecyclerView) findViewById(fakeR.getId("id", "wowGallery" ));

        if(gallery != null) {

            if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                gallery.setLayoutManager(new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.VERTICAL, false));
            } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                gallery.setLayoutManager(new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.HORIZONTAL, false));
            }


            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED ) {
                loadGallery();
            } else if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) || ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                ConfirmationDialogFragment
                        .newInstance(fakeR.getId("string", "camera_permission_confirmation"),//resources.getIdentifier("camera_permission_confirmation", "string", package_name
                                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                REQUEST_READ_PERMISSION,
                                fakeR.getId("string", "camera_permission_not_granted"))
                        .show(getSupportFragmentManager(), FRAGMENT_DIALOG);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_READ_PERMISSION);
            }


        }

        btnCapture = (FloatingActionButton) findViewById(fakeR.getId("id", "btn_take_picture" ));
        if (btnCapture != null) {
            btnCapture.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(camera!=null) {
                        btnCapture.setEnabled(false);
                        //camera.captureImage();
                        captureStartTime = System.currentTimeMillis();
                        camera.captureImage(new CameraKitView.ImageCallback() {
                            @Override
                            public void onImage(CameraKitView cameraKitView, byte[] bytes) {
                                switchToCropper(true);
                                btnCapture.setEnabled(true);

                                // Create a bitmap
                                Bitmap result = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                cropImageView.setImageBitmap(result);
                            }
                        });
                    }
                }
            });
        }

        btnFlash = (AppCompatImageView) findViewById(fakeR.getId("id", "btn_change_flash" ));
        if (btnFlash != null) {

            if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) btnFlash.setVisibility(View.GONE);

            btnFlash.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(camera!=null) {
                        currentFlash = (currentFlash + 1) % FLASH_OPTIONS.length;
                        //btnFlash.setBackgroundResource(FLASH_ICONS[currentFlash]);
                        btnFlash.setImageResource(FLASH_ICONS[currentFlash]);
                        camera.setFlash(FLASH_OPTIONS[currentFlash]);
                    }
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        // use wowMenu here
        inflater.inflate(fakeR.getId("menu", "main" ), wowMenu.getMenu());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Do your actions here
        return true;
    }

    private ArrayList<String> getAllShownImagesPath() {

        Cursor cursor;
        String absolutePathOfImage;
        int column_index_data;
        ArrayList<String> listOfAllImages = new ArrayList<String>();

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
                            Intent intent = new Intent();
                            intent.putExtra("save", false);
                            intent.setData(Uri.fromFile(new File(img)));
                            setResult(Activity.RESULT_OK, intent);
                        } catch (Exception e){
                            e.printStackTrace();
                            setResult(Activity.RESULT_CANCELED, new Intent());
                        }
                        finish();
                    }
                });

        gallery.setAdapter(adapter);

        gallery.setVisibility(View.VISIBLE);
    }

    @SuppressLint("RestrictedApi") // also suppressed the warning
    private void switchToCropper(Boolean iddle){
        if(iddle) {
            getSupportActionBar().show();
            camera.setVisibility(View.GONE);
            gallery.setVisibility(View.GONE);
            btnCapture.setVisibility(View.GONE);
            btnFlash.setVisibility(View.GONE);
            cropImageView.setVisibility(View.VISIBLE);
        } else {
            getSupportActionBar().hide();
            camera.setVisibility(View.VISIBLE);
            gallery.setVisibility(View.VISIBLE);
            btnCapture.setVisibility(View.VISIBLE);
            btnFlash.setVisibility(View.VISIBLE);
            cropImageView.setVisibility(View.GONE);
        }
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        camera.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        camera.onResume();
    }

    @Override
    protected void onPause() {
        camera.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        camera.onStop();
        super.onStop();
    }

    /*
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        camera.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    */
}
