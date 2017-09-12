package com.sanjay.gifmaker;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * Created by fen on 8/3/16.
 */
public class ViewActivity extends AppCompatActivity {
    final int chunkSize = 1024; // One kb at a time
    private File tmpFile;
    private WebView webView;
    private Uri uri;
    private byte[] imageData = new byte[chunkSize];
    private Button btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view);
        if (savedInstanceState != null) {
            tmpFile = new File(Uri.parse(savedInstanceState.getString("tmpFileUri")).getPath());
            uri = Uri.parse(savedInstanceState.getString("fileUri"));
            new displayFile(tmpFile).execute();
        }
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        btn = (Button) findViewById(R.id.load_btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), FilesActivity.class);
                startActivityForResult(intent, 0);
            }
        });
        if (tmpFile == null) {
            btn.setVisibility(View.VISIBLE);
        } else {
            btn.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        tmpFile = new File(Environment.getExternalStorageDirectory() +
                File.separator + "tmp.jpeg");
        tmpFile.delete();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        File bundledFile = new File(Environment.getExternalStorageDirectory() +
                File.separator + "tmp.jpeg");
        if (bundledFile.exists()) {
            outState.putString("tmpFileUri", bundledFile.toURI().toString());
            if (uri != null) { // This is probs unnecessary
                outState.putString("fileUri", uri.toString());
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_load) {
            showFileChooser();
            return true;
        } else if (id == R.id.action_select) {
            Intent intent = new Intent(this, FilesActivity.class);
            startActivityForResult(intent, 0);
            return true;
        } else if (id == R.id.action_upload) {
            // TODO: upload to image server?
            Toast.makeText(this, "Uploads are not implemented yet.",
                    Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_share) {
            Toast.makeText(this, "Sharing is not implemented yet.",
                    Toast.LENGTH_SHORT).show();
        }
        return super.onOptionsItemSelected(item);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 0:
                if (resultCode == RESULT_OK) {
                    // Load image
                    uri = data.getData();
                    // Create a tmp file for the compression
                    tmpFile = new File(Environment.getExternalStorageDirectory() +
                            File.separator + "tmp.jpeg");
                    try {
                        tmpFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    new displayFile(tmpFile).execute();
                }
                break;
        }
    }

    private void showFileChooser() {
        // TODO: Check if have permission
        File GIF_DIR = new File(Environment.getExternalStorageDirectory() + File.separator +
                Environment.DIRECTORY_PICTURES + File.separator + "Gifs");
        Uri dirUri = Uri.parse(GIF_DIR.getPath());
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setDataAndType(dirUri, "image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select a File to Upload"),
                    0);
        } catch (ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "Please install a File Manager.",
                    Toast.LENGTH_SHORT).show();
        }
    }


    public class displayFile extends AsyncTask<String, String, String> {

        File tmpFile;

        public displayFile(File tmpFile) {
            this.tmpFile = tmpFile;
        }

        @Override
        protected String doInBackground(String... strings) {
            OutputStream out = null;
            InputStream in = null;
            try {
                out = new FileOutputStream(tmpFile);
                in = getContentResolver().openInputStream(uri);
                int bytesRead;
                while ((bytesRead = in.read(imageData)) > 0) {
                    out.write(Arrays.copyOfRange(imageData, 0, Math.max(0, bytesRead)));
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    out.close();
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            // Construct path and load into Webview
            String gif = "file://" + tmpFile.getPath();
            Log.d("TMP file", tmpFile.getPath());
            // TODO: Create padding
            return "<style>img{padding-top:3%;padding-right:2%;padding-left:2%;display: inline; height: auto; max-width: 95%;}" +
                    "</style><body><img src=\"" + gif + "\"/></body>";
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            webView = (WebView) findViewById(R.id.gifView);
            webView.clearCache(true); // For changing the view, literally
            webView.loadDataWithBaseURL("file://android_asset/", result, "text/html", "utf-8", null);
            // tmpFile deletes onDestroy()
            btn.setVisibility(View.GONE);

        }
    }

}