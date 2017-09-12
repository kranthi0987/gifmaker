package com.sanjay.gifmaker.Adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.sanjay.gifmaker.R;

import java.io.File;
import java.util.List;

/**
 * Created by fen on 8/18/16.
 */
public class FilesAdapter extends ArrayAdapter<File> {

    public FilesAdapter(Context context, int resource, List<File> files) {
        super(context, resource, files);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;

        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.row_file, null);
        }

        File f = getItem(position);
        if (f != null) {
            ImageView img = (ImageView) v.findViewById(R.id.row_image);
            TextView txt = (TextView) v.findViewById(R.id.row_title);
            TextView tag = (TextView) v.findViewById(R.id.row_tag);
            String title = f.getName();
            // Image Preview
            SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(getContext());
            boolean loadImages = sharedPreferences.getBoolean("pref_img_load", true);
            if (loadImages) {
                Bitmap bmap = BitmapFactory.decodeFile(f.getPath());
                img.setImageBitmap(bmap);
            }
            // Txt
            if (title.endsWith(".gif")) {
                txt.setText(title.substring(0, title.length() - 4));
            } else {
                txt.setText(title);
            }
            // TODO: Change to String.format()
            tag.setText(Long.toString((f.length() / 1024) / 1000) + "MB");

        }

        return v;
    }
}
