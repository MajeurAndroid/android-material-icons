package com.majeur.materialicons;

import android.content.res.AssetManager;
import android.os.AsyncTask;

import java.io.IOException;

public class AsyncAssetsLoader extends AsyncTask<String, Void, String[]> {

    private AssetManager mManager;
    private OnAssetsLoadedListener mListener;


    public AsyncAssetsLoader(AssetManager assetManager, OnAssetsLoadedListener listener) {
        mManager = assetManager;
        mListener = listener;
    }

    @Override
    protected String[] doInBackground(String... params) {
        try {
            return mManager.list(params[0]);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onPostExecute(String[] strings) {
        super.onPostExecute(strings);
        mListener.onAssetsLoaded(strings);
    }

    public interface OnAssetsLoadedListener {
        public void onAssetsLoaded(String[] assets);
    }
}
