package com.majeur.materialicons;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.AsyncTask;

import com.larvalabs.svgandroid.SVG;
import com.larvalabs.svgandroid.SVGParseException;
import com.larvalabs.svgandroid.SVGParser;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class AsyncExporter extends AsyncTask<AsyncExporter.Params, AsyncExporter.Progress, File> {

    private static int[] ICONS_SIZE = {24, 32, 48, 64, 96, 128};
    private static String[] FOLDER_NAMES = {"drawable-ldpi", "drawable-mdpi", "drawable-hdpi",
            "drawable-xhdpi", "drawable-xxhdpi", "drawable-xxxhdpi"};
    private static String ROOT_PATH_NAME = "Material-Icons-";

    private Context mContext;
    private ExportStateCallbacks mCallbacks;

    public AsyncExporter(Context context, ExportStateCallbacks callbacks) {
        mContext = context;
        mCallbacks = callbacks;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mCallbacks.onPreExport();
    }

    @Override
    protected File doInBackground(Params... args) {
        final Progress progress = new Progress();
        Params params = args[0];

        // Root directory that contains drawable-*dpi directories
        String rootDirName = ROOT_PATH_NAME + getDate();
        File rootDir = new File(mContext.getCacheDir().getPath() + File.separator + rootDirName);
        rootDir.mkdirs();

        progress.totalProgress = params.desiredFiles.size() * params.desiredDensities.size();

        for (int i = 0; i < params.desiredDensities.size(); i++) {

            // Index of current processed density in Density enum, this index match with ICONS_SIZE and FOLDER_NAMES,
            // it allows us to retrieve density folder name, and icons size relative to current processed density.
            int densityIndex = Utils.indexOf(Density.class, params.desiredDensities.get(i));

            String currentDensityName = FOLDER_NAMES[densityIndex];

            File densityDirectory = new File(rootDir.getPath() + File.separator + "res" + File.separator + currentDensityName);
            densityDirectory.mkdirs();

            progress.currentDensity = currentDensityName;

            // Calculate already done icons count in previous density folders
            final int currentDensityLevelProgress = i * params.desiredFiles.size();

            // Progress listener allows us to publish progress when each icon is processed.
            writeFiles(params, densityDirectory, densityIndex, new ProgressListener() {
                @Override
                public void onProgressChange(int i, String fileName) {

                    progress.currentProgress = currentDensityLevelProgress + i;
                    progress.currentFileName = fileName;
                    publishProgress(progress);
                }
            });
        }

        addReadme(rootDir);

        switch (params.saveType) {
            case DIR:
                File targetDir = new File(params.path + File.separator + rootDirName);

                try {
                    copyDirectory(rootDir, targetDir);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return targetDir;
            case ZIP:
                File targetZipFile = new File(params.path + File.separator + rootDirName + ".zip");

                if (!targetZipFile.exists()) {
                    try {
                        targetZipFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                zipFile(rootDir, targetZipFile);

                return targetZipFile;
            default:
                return null;
        }
    }

    private void writeFiles(Params params, File dir, int densityIndex, ProgressListener listener) {
        int i = 0;
        for (String fileName : params.desiredFiles) {
            String pngFileName = getPngFileName(fileName);
            listener.onProgressChange(i, pngFileName);

            try {
                File iconFile = new File(dir.getPath() + File.separator + pngFileName);
                iconFile.createNewFile();
                FileOutputStream outputStream = new FileOutputStream(iconFile);

                Bitmap bitmap = getBitmapSvg(params, fileName, densityIndex);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                outputStream.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
            i++;
        }
    }

    private String getPngFileName(String svgFileName) {
        return "ic_" + svgFileName.replace(".svg", ".png").replace("-", "_");
    }

    private String getDate() {
        return new SimpleDateFormat("MMddyy-hhmmss").format(new Date(System.currentTimeMillis()));
    }

    private Bitmap getBitmapSvg(Params params, String fileName, int densityIndex) {
        try {
            SVG svg = SVGParser.getSVGFromAsset(mContext.getAssets(), MainActivity.ICONS_PATH + fileName);

            // Icon size relative to current processed density
            int fSize = ICONS_SIZE[densityIndex];

            Bitmap bitmap = Bitmap.createBitmap(fSize, fSize, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawPicture(svg.getPicture(), new Rect(0, 0, fSize, fSize));

            Bitmap finalBitmap = createTintedBitmap(bitmap, params.desiredColor);
            bitmap.recycle();
            return finalBitmap;
        } catch (IOException | SVGParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Bitmap createTintedBitmap(Bitmap bitmap, int tintColor) {
        Bitmap tempBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas tempCanvas = new Canvas(tempBitmap);
        tempCanvas.drawColor(tintColor);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
        tempCanvas.drawBitmap(bitmap, 0, 0, paint);

        Bitmap resultBitmap = Bitmap.createBitmap(tempBitmap.getWidth(), tempBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas resultCanvas = new Canvas(resultBitmap);
        resultCanvas.drawColor(tintColor);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
        resultCanvas.drawBitmap(tempBitmap, 0, 0, paint);
        tempBitmap.recycle();

        return resultBitmap;
    }

    private void addReadme(File rootDir) {
        try {
            PrintWriter writer = new PrintWriter(rootDir.getAbsolutePath() + File.separator + "readme.txt");
            writer.write(mContext.getString(R.string.readme_msg));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void copyDirectory(File sourceLocation, File targetLocation) throws IOException {
        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists() && !targetLocation.mkdirs()) {
                throw new IOException("Cannot create dir " + targetLocation.getAbsolutePath());
            }

            String[] children = sourceLocation.list();
            for (String child : children) {
                copyDirectory(new File(sourceLocation, child),
                        new File(targetLocation, child));
            }
        } else {
            // make sure the directory we plan to store the recording in exists
            File directory = targetLocation.getParentFile();
            if (directory != null && !directory.exists() && !directory.mkdirs()) {
                throw new IOException("Cannot create dir " + directory.getAbsolutePath());
            }

            InputStream in = new FileInputStream(sourceLocation);
            OutputStream out = new FileOutputStream(targetLocation);

            // Copy the bits from instream to outstream
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        }
    }

    public boolean zipFile(File sourceFile, File targetFile) {
        final int BUFFER = 2048;

        try {
            BufferedInputStream origin;
            FileOutputStream dest = new FileOutputStream(targetFile);
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));

            if (sourceFile.isDirectory()) {
                zipSubFolder(out, sourceFile, sourceFile.getParent().length());
            } else {
                byte data[] = new byte[BUFFER];
                FileInputStream fi = new FileInputStream(sourceFile);
                origin = new BufferedInputStream(fi, BUFFER);
                ZipEntry entry = new ZipEntry(getLastPathComponent(sourceFile.getAbsolutePath()));
                out.putNextEntry(entry);
                int count;
                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
            }
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void zipSubFolder(ZipOutputStream out, File folder, int basePathLength) throws IOException {
        final int BUFFER = 2048;

        File[] fileList = folder.listFiles();
        BufferedInputStream origin = null;
        for (File file : fileList) {
            if (file.isDirectory()) {
                zipSubFolder(out, file, basePathLength);
            } else {
                byte data[] = new byte[BUFFER];
                String unmodifiedFilePath = file.getPath();
                String relativePath = unmodifiedFilePath.substring(basePathLength);
                FileInputStream fi = new FileInputStream(unmodifiedFilePath);
                origin = new BufferedInputStream(fi, BUFFER);
                ZipEntry entry = new ZipEntry(relativePath);
                out.putNextEntry(entry);
                int count;
                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
                origin.close();
            }
        }
    }

    public String getLastPathComponent(String filePath) {
        return filePath.substring(filePath.lastIndexOf("/"));
    }

    @Override
    protected void onProgressUpdate(Progress... values) {
        super.onProgressUpdate(values);
        mCallbacks.onExportProgressUpdate(values[0]);
    }

    @Override
    protected void onPostExecute(File f) {
        super.onPostExecute(f);
        mCallbacks.onPostExport(f);
    }

    public static class Params {
        List<String> desiredFiles;
        List<Density> desiredDensities;
        int desiredColor;
        String path;
        SaveType saveType;
    }

    public enum SaveType {
        ZIP, DIR
    }

    public static class Progress {
        int totalProgress;
        int currentProgress;
        String currentDensity;
        String currentFileName;
        SaveType saveType;
    }

    private interface ProgressListener {
        public void onProgressChange(int i, String fileName);
    }

    public interface ExportStateCallbacks {

        public void onPreExport();

        public void onExportProgressUpdate(Progress progress);

        public void onPostExport(File resultDirectory);
    }
}
