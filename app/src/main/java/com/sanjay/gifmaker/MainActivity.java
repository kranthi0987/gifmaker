package com.sanjay.gifmaker;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.gun0912.tedpicker.ImagePickerActivity;
import com.sanjay.gifmaker.PrefHelper.SettingsActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    ArrayList<Bitmap> bitmaps = new ArrayList<>();
    ArrayList<String> images;
    ArrayList<Uri> uris = new ArrayList<>();
    ArrayAdapter<String> adapter;
    EditText editTxt;
    ImageView prevImg;
    String filename;
    private ProgressBar progressBar;
    private AdView mAdView;
    private File GIF_DIR;

    private String DEFAULT_TITLE;
    private int COMPRESSION; // not a big diff eh?
    private int SAMPLE_SIZE = 3; // ?? unclear to me...
    private int REPEAT;
    private int DELAY; // milliseconds
    //firebase
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }

// Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        // Create GIF folder if it doesn't exit
        GIF_DIR = new File(Environment.getExternalStorageDirectory() + File.separator +
                Environment.DIRECTORY_PICTURES + File.separator + "Gifs");
        if (!GIF_DIR.exists()) {
            GIF_DIR.mkdir();
            // Somehow mount this guy?
        }

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
        // Settings
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        DEFAULT_TITLE = sharedPreferences.getString("pref_default_title", "fissureGIF");
        COMPRESSION = Integer.valueOf(sharedPreferences.getString("pref_compression", "30"));
        DELAY = Integer.valueOf(sharedPreferences.getString("pref_delay", "500"));
        if (sharedPreferences.getBoolean("pref_repeat", true)) {
            REPEAT = 0; // 0 is indefinite
        } else {
            REPEAT = 2;
        }

        // ActionBar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
//        getSupportActionBar().setIcon(R.drawable.ic_fissure_logo_white);

        // Views
        Button btnGen = (Button) findViewById(R.id.generateGIF);
        editTxt = (EditText) findViewById(R.id.titleValue);
        prevImg = (ImageView) findViewById(R.id.preview);

        // List view of images?
        images = new ArrayList<>();
        // TODO: add saved Instance state
        // Or disable orientation switch?
        ListView lv = (ListView) findViewById(R.id.listImage);
        registerForContextMenu(lv);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, images);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(getBaseContext(), images.get(position),
                        Toast.LENGTH_SHORT).show();
                prevImg.setImageURI(uris.get(position));
            }
        });
        //TODO: Onclick Listener for individual deletes


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showFileChooser();
                Snackbar.make(view, "Select Image", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        btnGen.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                EditText et = (EditText) findViewById(R.id.titleValue);
                filename = et.getText().toString();

                // Launch Concurrent GIF generation
                new GenerateGif().execute(bitmaps);
            }
        });
        mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);


    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //ArrayList<Uri> uris = new ArrayList<>();
        //outState.putStringArrayList("uris", uris.eachToString());
        // TODO: Convert uris array to string array
        //outState.putStringArrayList("titles", images);
        super.onSaveInstanceState(outState);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_clear) {
            ClearAll();
            Toast.makeText(getBaseContext(), "Reset Data",
                    Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_view) {
            Intent intent = new Intent(this, ViewActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_instructions) {
            Intent intent = new Intent(this, InstructionsActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        /*
        FOR LONG CLICKS (For my future reference...
        Must register list view for CONTEXT MENU
        and then implement these two override methods
         */
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId() == R.id.listImage) {
            MenuInflater inflater = new MenuInflater(this);
            menu.setHeaderTitle(R.string.image_options);
            inflater.inflate(R.menu.menu_context, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // TODO: Move Item in list
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if (item.getItemId() == R.id.action_remove) {
            RemoveItem(info.position);
            return true;
        } else {
            return super.onContextItemSelected(item);
        }


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 0:
                if (resultCode == RESULT_OK) {
                    ListView lv = (ListView) findViewById(R.id.listImage);

                    ArrayList<Uri> image_uris = data.getParcelableArrayListExtra(ImagePickerActivity.EXTRA_IMAGE_URIS);

                    for (int i = 0; i < image_uris.size(); i++) {

                        Uri uri = Uri.fromFile(new File(image_uris.get(i).toString()));// URI, not file, of selected File
                        Bitmap bitmap = null;
                        try {
                            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        // Compress
                        File tmpFile = new File(Environment.getExternalStorageDirectory() +
                                File.separator + "tmp.jpeg");
                        FileOutputStream out = null;
                        try {
                            tmpFile.createNewFile();
                            out = new FileOutputStream(tmpFile);
                            bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION, out);
                            out.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inSampleSize = SAMPLE_SIZE;
                        options.inJustDecodeBounds = false;
                        bitmap = BitmapFactory.decodeFile(tmpFile.getPath(), options);
                        Log.d("tmpFile", tmpFile.getPath());
                        tmpFile.delete();
                        bitmaps.add(bitmap);
                        // TODO: FILE name?
                        String[] imageId = uri.getPath().split(":");
                        images.add("Preview Image: " + imageId[imageId.length - 1]);
                        uris.add(uri);
                        ((ArrayAdapter) lv.getAdapter()).notifyDataSetChanged();
                    }
                }
                break;
        }
    }

    // Return a byte[] which is in fact an encoded GIF
    public byte[] generateGIF(ArrayList<Bitmap> bitmaps) { // pass in bitmap array
        // Preferences
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        COMPRESSION = Integer.valueOf(sharedPreferences.getString("pref_compression", "30"));
        DELAY = Integer.valueOf(sharedPreferences.getString("pref_delay", "500"));
        if (sharedPreferences.getBoolean("pref_repeat", true)) {
            REPEAT = 0; // 0 is indefinite
        } else {
            REPEAT = 2;
        }
        // Encode Gif from bitmap frames
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        AnimatedGifEncoder encoder = new AnimatedGifEncoder();
        encoder.start(bos);
        // Settings must be invoked before adding first image!
        encoder.setRepeat(REPEAT);
        encoder.setDelay(DELAY);
        // TODO: Size
        for (Bitmap bitmap : bitmaps) {
            encoder.addFrame(bitmap);
        }
        encoder.finish();
        return bos.toByteArray();
    }

    private void showFileChooser() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 0);
        }
        Intent intent = new Intent(MainActivity.this, ImagePickerActivity.class);
        startActivityForResult(intent, 0);

    }

    private void RemoveItem(int pos) {
        bitmaps.remove(pos);
        uris.remove(pos);
        images.remove(pos);
        adapter.notifyDataSetChanged();
    }

    private void ClearAll() {
        bitmaps.clear();
        uris.clear();
        images.clear();
        adapter.clear();
        prevImg.setImageResource(android.R.color.transparent);
    }

    private void NotifyWroteGIF() {
        ClearAll();
        Toast.makeText(getBaseContext(), "Finished Writing GIF",
                Toast.LENGTH_SHORT).show();
    }

    private void toggleOrientationLock() {
        // TODO: Doesn't work?
        // Check if locked
        int isLocked = android.provider.Settings.System.getInt(
                getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION, 0
        );
        if (isLocked == 0) {
            int currentOrientation = getResources().getConfiguration().orientation;
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
            }
        } else {
            // renable
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }
    }

    private class GenerateGif extends AsyncTask<ArrayList, Integer, Void> {
        // Second param is Progress
        @Override
        protected Void doInBackground(ArrayList... params) {
            // Write Gif
            String fn;
            if (filename.isEmpty()) {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                DEFAULT_TITLE = sharedPreferences.getString("pref_default_title", "fissureGIF");
                fn = DEFAULT_TITLE.concat(".gif");
            } else {
                fn = filename.concat(".gif");
            }
            try {
                // TODO: Save to special Gif folder?
                FileOutputStream outStream = new FileOutputStream(GIF_DIR + File.separator + fn);
                outStream.write(generateGIF(bitmaps));

                outStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            // TODO: Somethin' pretty
            //progressBar.setProgress(values[0]);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(getBaseContext(), "Writing GIF",
                    Toast.LENGTH_SHORT).show();
            //progressBar.setMax(10);
            progressBar.setVisibility(View.VISIBLE);
            //progressBar.setProgress(0);
        }

        protected void onPostExecute(Void a) {
            progressBar.setVisibility(View.GONE);
            NotifyWroteGIF();
        }
    }
}
