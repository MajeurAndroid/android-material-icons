package com.majeur.materialicons;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DataAsyncTask extends AsyncTask<Void, String, String[]> {

    private static final String TAG = "DataAsyncTask";

    private static final String MANIFEST_URI = "http://majeurandroid.github.io/Material-Icons/manifest";
    private final String MANIFEST_PATH; // Not static because it is set later
    private static final String JSON_ARG_COUNT = "count";
    private static final String JSON_ARG_ITEMS = "items";
    private static final String JSON_ARG_URL = "url";
    private static final String JSON_ARG_NAME = "name";

    private Context mContext;
    private ProgressDialog mDialog;
    private OnDataLoadedListener mListener;

    private File mIconsDirectory;

    private JSONObject mNetManifest, mLocalManifest;

    DataAsyncTask(Context context, OnDataLoadedListener loadedListener) {
        mContext = context;
        mListener = loadedListener;

        mDialog = new ProgressDialog(context);
        mDialog.setMessage("");
        mDialog.setIndeterminate(true);
        mDialog.setCancelable(false);

        MANIFEST_PATH = context.getCacheDir() + File.separator + "manifest";

        mIconsDirectory = new File(context.getCacheDir().getAbsolutePath() + MainActivity.ICONS_PATH);
        if (!mIconsDirectory.exists())
            mIconsDirectory.mkdir();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mDialog.show();
    }

    @Override
    protected String[] doInBackground(Void... params) {
        publishProgress("Checking if local data is up to date ...");
        updateLocalManifest();
        setManifests();

        if (mNetManifest != null)
            checkLocalData();
        else
            Log.e(TAG, "Error when downloading manifest");


        String[] fileNames = mIconsDirectory.list();
        Arrays.sort(fileNames);
        return fileNames;
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        mDialog.setMessage(values[0]);
    }

    @Override
    protected void onPostExecute(String[] files) {
        super.onPostExecute(files);
        mDialog.dismiss();

        mListener.onDataLoaded(files);
    }

    private void setManifests() {
        try {
            String rawLocalManifest = "";

            FileInputStream inputStream = new FileInputStream(MANIFEST_PATH);
            byte[] buffer = new byte[1024];
            int n;
            while ((n = inputStream.read(buffer)) != -1)
                rawLocalManifest += new String(buffer, 0, n);

            if (!TextUtils.isEmpty(rawLocalManifest))
                mLocalManifest = new JSONObject(rawLocalManifest);

        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }

        try {
            String rawNetManifest = HttpWrapper.makeRequest(MANIFEST_URI);

            if (rawNetManifest != null)
                mNetManifest = new JSONObject(rawNetManifest);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void checkLocalData() {
        if (!localDataUpToDate()) {
            List<JSONObject> jsonsToLoad = getJsonsToLoad();
            int size = jsonsToLoad.size();

            for (int i = 0; i < size; i++) {
                publishProgress("Downloading new icons ... (" + i + "/" + size + ")");

                JSONObject json = jsonsToLoad.get(i);
                if (!downloadIcon(json))
                    Log.e(TAG, "Error when downloading following Json: " + json.toString());
            }
            publishProgress("Updating local data ...");
            updateLocalManifest();
        }
    }

    private boolean localDataUpToDate() {
        try {
            return mLocalManifest != null && mLocalManifest.getInt(JSON_ARG_COUNT) == mNetManifest.getInt(JSON_ARG_COUNT);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    private List<JSONObject> getJsonsToLoad() {
        List<JSONObject> urlsToLoad = new ArrayList<>();

        try {
            JSONArray netItems = mNetManifest.getJSONArray(JSON_ARG_ITEMS);

            if (mLocalManifest == null) {
                // Download everything
                for (int i = 0; i < netItems.length(); i++)
                    urlsToLoad.add(netItems.getJSONObject(i));

            } else {
                // Download only missing icons
                JSONArray localItems = mLocalManifest.getJSONArray(JSON_ARG_ITEMS);

                for (int i = 0; i < netItems.length(); i++) {
                    JSONObject netJson = netItems.getJSONObject(i);
                    if (!containsName(localItems, netJson))
                        urlsToLoad.add(netJson);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return urlsToLoad;
    }

    private boolean containsName(JSONArray localItems, JSONObject netJson) throws JSONException {
        String netName = netJson.getString(JSON_ARG_NAME);

        for (int i = 0; i < localItems.length(); i++) {
            String localName = localItems.getJSONObject(i).getString(JSON_ARG_NAME);
            if (netName.equals(localName))
                return true;
        }
        return false;
    }

    private boolean downloadIcon(JSONObject json) {
        Log.i(TAG, "Downloading Json: " + json.toString());
        try {
            String url = json.getString(JSON_ARG_URL);
            // Output stream
            File outputFile = new File(mContext.getCacheDir().getAbsolutePath() + MainActivity.ICONS_PATH +
                    File.separator + json.getString(JSON_ARG_NAME));

            if (!outputFile.exists())
                outputFile.createNewFile();

            OutputStream outputStream = new FileOutputStream(outputFile);

            // HttpWrapper takes care of closing streams
            HttpWrapper.downloadFile(url, outputStream);

            updateLocalManifest();

            return true;
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return false;
        }


    }

    private void updateLocalManifest() {
        try {
            JSONObject rootJson = new JSONObject();

            String[] iconNames = mIconsDirectory.list();

            rootJson.put(JSON_ARG_COUNT, iconNames.length);

            JSONArray jsonArray = new JSONArray();

            for (String iconName : iconNames) {
                JSONObject jsonChild = new JSONObject();
                jsonChild.put(JSON_ARG_NAME, iconName);

                jsonArray.put(jsonChild);
            }

            rootJson.put(JSON_ARG_ITEMS, jsonArray);

            FileWriter fileWriter = new FileWriter(MANIFEST_PATH);
            fileWriter.write(rootJson.toString());
            fileWriter.close();

        } catch (IOException | JSONException e) {
            Log.e(TAG, "Error when updating local manifest");
            e.printStackTrace();
        }

    }

    interface OnDataLoadedListener {
        public void onDataLoaded(String[] fileNames);
    }
}
