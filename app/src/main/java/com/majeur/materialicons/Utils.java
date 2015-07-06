package com.majeur.materialicons;

import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

import com.larvalabs.svgandroid.SVG;
import com.larvalabs.svgandroid.SVGParseException;
import com.larvalabs.svgandroid.SVGParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    public static Drawable getDrawableForSvg(AssetManager assetManager, String fileName) {
        try {
            SVG svg = SVGParser.getSVGFromAsset(assetManager, MainActivity.ICONS_PATH + fileName, Color.BLACK, Color.DKGRAY);

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

    public static List<String> arrayAsList(String[] a) {
        List<String> list = new ArrayList<>(a.length);
        for (String s : a)
            list.add(s);
        return list;
    }

}
