package com.majeur.materialicons;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class is used to retrieve icons and keep local copies up to date.
 * On the server side, there is two manifests, main.manifest which contains the count
 * of available icons on the server, and files.manifest which contains the name of each files.
 * These manifest are separated to allow us to compare available items count without downloading
 * all file list (up to 1000 items), thus we use a minimal amount of the user connection data.
 * <p/>
 * On the server files are in the same folder, to get download url, we just need the file name, then
 * we format it with the "folder" url.
 */
public class DataAsyncTask extends AsyncTask<Void, String, String[]> {

    private static final String TAG = "DataAsyncTask";

    private static final String MANIFEST_MAIN_URL = "http://majeurandroid.github.io/Material-Icons/main.manifest";
    private static final String MANIFEST_FILES_URL = "http://majeurandroid.github.io/Material-Icons/files.manifest";
    private static final String FILES_URL_UNFORMATTED = "http://majeurandroid.github.io/Material-Icons/icons/%s";
    private static final String JSON_ARG_COUNT = "count";
    private static final String JSON_ARG_NAME = "name";

    private Context mContext;
    private MaterialDialog mDialog;
    private OnDataLoadedListener mListener;

    private File mIconsDirectory;

    private JSONObject mNetManifest;

    DataAsyncTask(Context context, OnDataLoadedListener loadedListener) {
        mContext = context;
        mListener = loadedListener;

        mDialog = new MaterialDialog.Builder(context)
                .progressIndeterminateStyle(false)
                .progress(true, 0)
                .content("")
                .cancelable(false)
                .positiveText(android.R.string.cancel)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        super.onPositive(dialog);
                        cancel(false); // Cancel but don't interrupt, we still want already downloaded icons
                    }
                })
                .build();

        mIconsDirectory = new File(context.getFilesDir().getAbsolutePath() + MainActivity.ICONS_PATH);
        if (!mIconsDirectory.exists())
            // Create local icons directory
            mIconsDirectory.mkdir();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mDialog.show();
    }

    @Override
    protected String[] doInBackground(Void... params) {
        publishProgress(mContext.getString(R.string.data_task_msg_1));

        checkFirstLaunch();

        mNetManifest = getNetManifest();

        if (mNetManifest != null) {
            // If network manifest is available, we check if local data is up to date
            checkLocalData();
        } else {
            Utils.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, R.string.net_required, Toast.LENGTH_LONG).show();
                }
            });
            Log.e(TAG, "Error when downloading manifest");
        }

        publishProgress(mContext.getString(R.string.data_task_msg_3));

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
    protected void onCancelled(String[] files) {
        super.onCancelled(files);
        mDialog.dismiss();

        mListener.onDataLoaded(files);
    }

    @Override
    protected void onPostExecute(String[] files) {
        super.onPostExecute(files);
        mDialog.dismiss();

        mListener.onDataLoaded(files);
    }

    private void checkFirstLaunch() {
        if (mIconsDirectory.list().length == 0) {
            AssetManager manager = mContext.getAssets();
            try {
                for (String assetChild : manager.list("icons")) {

                    String path = mContext.getFilesDir().getAbsolutePath() + MainActivity.ICONS_PATH +
                            File.separator + assetChild;

                    Utils.writeFile(path, manager.open("icons/" + assetChild));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Retrieve, if possible, local and network manifest.
     */
    private JSONObject getNetManifest() {
        try {
            String rawNetManifest = HttpWrapper.makeRequest(MANIFEST_MAIN_URL);

            if (rawNetManifest != null)
                return new JSONObject(rawNetManifest);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Check if local data is up to date, if not, update it.
     */
    private void checkLocalData() {
        if (isLocalDataUpToDate())
            return;// Data is up to date

        List<JSONObject> itemsToDownload = getItemsToDownload();
        int size = itemsToDownload.size();

        // Download missing items
        for (int i = 0; i < size; i++) {
            if (isCancelled())
                return;

            publishProgress(mContext.getString(R.string.data_task_msg_2, i, size));

            JSONObject json = itemsToDownload.get(i);

            if (!downloadIcon(json)) // Downloading icon
                Log.e(TAG, "Error when downloading following Json: " + json.toString());
        }
    }

    /**
     * Check if local icons count matches server items count.
     *
     * @return True if local data is up to date
     */
    private boolean isLocalDataUpToDate() {
        try {
            return mIconsDirectory.list().length == mNetManifest.getInt(JSON_ARG_COUNT);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Retrieve a list of locally missing icons.
     *
     * @return List of icons to download formatted in JSON
     */
    private List<JSONObject> getItemsToDownload() {
        List<JSONObject> urlsToLoad = new ArrayList<>();

        try {
            // We need the server files.manifest to find missing icons
            String rawNetFilesManifest = HttpWrapper.makeRequest(MANIFEST_FILES_URL);
            JSONArray netItems = new JSONArray(rawNetFilesManifest);

            if (mIconsDirectory.list().length == 0) {
                // Download everything
                for (int i = 0; i < netItems.length(); i++)
                    urlsToLoad.add(netItems.getJSONObject(i));

            } else {
                List<String> localItems = Arrays.asList(mIconsDirectory.list());

                for (int i = 0; i < netItems.length(); i++) {
                    // We iterate through network items to check if each one is present in local folder.

                    JSONObject netJson = netItems.getJSONObject(i);
                    String netItemName = netJson.getString(JSON_ARG_NAME);

                    if (!localItems.contains(netItemName))
                        urlsToLoad.add(netJson);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return urlsToLoad;
    }

    /**
     * Downloads requested item from the server.
     *
     * @param json JSON that contains icon name
     * @return True if download was successful
     */
    private boolean downloadIcon(JSONObject json) {
        Log.i(TAG, "Downloading Json: " + json.toString());

        try {
            // Getting download url
            String url = String.format(FILES_URL_UNFORMATTED, json.getString(JSON_ARG_NAME));

            File outputFile = new File(mContext.getFilesDir().getAbsolutePath() + MainActivity.ICONS_PATH +
                    File.separator + json.getString(JSON_ARG_NAME));

            if (!outputFile.exists())
                outputFile.createNewFile();

            OutputStream outputStream = new FileOutputStream(outputFile);

            // HttpWrapper takes care of closing streams
            HttpWrapper.downloadFile(url, outputStream);

            return true;
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    interface OnDataLoadedListener {
        public void onDataLoaded(String[] fileNames);
    }
}
