package com.majeur.materialicons;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import com.larvalabs.svgandroid.SVG;
import com.larvalabs.svgandroid.SVGParseException;
import com.larvalabs.svgandroid.SVGParser;

import java.io.FileInputStream;
import java.io.IOException;

public class SVGView extends ImageView {

    public SVGView(Context context) {
        super(context);
    }

    public SVGView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SVGView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setSVGPath(String path) {
        new AsyncSetter().execute(path);
    }

    private class AsyncSetter extends AsyncTask<String, Void, SVG> {

        @Override
        protected SVG doInBackground(String... params) {
            try {
                FileInputStream inputStream = new FileInputStream(params[0]);
                SVG svg = SVGParser.getSVGFromInputStream(inputStream, Color.BLACK, Color.DKGRAY);
                inputStream.close();

                return svg;
            } catch (IOException | SVGParseException e) {
                Log.e("SVG", params[0]);
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(SVG svg) {
            super.onPostExecute(svg);
            setImageDrawable(svg == null ? null : svg.createPictureDrawable());
        }
    }
}
