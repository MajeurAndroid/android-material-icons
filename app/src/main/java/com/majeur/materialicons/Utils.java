package com.majeur.materialicons;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;

import com.larvalabs.svgandroid.SVG;
import com.larvalabs.svgandroid.SVGParseException;
import com.larvalabs.svgandroid.SVGParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Utils {

    public static String svgFileNameToLabel(String s) {
        if (s.length() == 0)
            return s;
        s = s.replace(".svg", "");
        String[] strings = s.split("-");
        String result = "";
        for (String s1 : strings)
            result += s1.substring(0, 1).toUpperCase() + s1.substring(1) + " ";

        return result.substring(0, result.length() - 1);
    }

    public static Drawable getDrawableForSvg(Context context, String fileName) {
        try {
            FileInputStream inputStream = new FileInputStream(context.getFilesDir() + MainActivity.ICONS_PATH + fileName);
            SVG svg = SVGParser.getSVGFromInputStream(inputStream, Color.BLACK, Color.DKGRAY);

            return svg.createPictureDrawable();
        } catch (IOException | SVGParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static int indexOf(Class<? extends Enum> e, Enum<?> w) {
        Enum[] array = e.getEnumConstants();
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(w))
                return i;
        }
        return -1;
    }

    public static void writeFile(String path, InputStream inputStream) {
        try {
            File file = new File(path);
            if (!file.exists()) file.createNewFile();

            FileOutputStream outputStream = new FileOutputStream(path);
            byte data[] = new byte[1024];
            int count;
            while ((count = inputStream.read(data)) != -1)
                // writing data to file
                outputStream.write(data, 0, count);

            inputStream.close();
            outputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void runOnUiThread(Runnable r) {
        new Handler(Looper.getMainLooper()).post(r);
    }

    public static class Tuple<T, K> {

        private T mT;
        private K mK;

        public Tuple(T t, K k) {
            mT = t;
            mK = k;
        }

        public T get0() {
            return mT;
        }

        public K get1() {
            return mK;
        }
    }
}
