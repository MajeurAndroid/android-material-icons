package com.majeur.materialicons;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.List;


public class MainActivity extends ActionBarActivity implements Adapter.ItemsClickListener, ActionMode.Callback {

    private static final String TAG = "MainActivity";
    public static final String ICONS_PATH = "/icons/";

    private String[] mFilesName;
    private Adapter mAdapter;

    private ActionMode mActionMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(getResources().getInteger(R.integer.main_grid_num_col),
                StaggeredGridLayoutManager.VERTICAL));
        mAdapter = new Adapter(MainActivity.this, MainActivity.this);
        recyclerView.setAdapter(mAdapter);

        new DataAsyncTask(this, new DataAsyncTask.OnDataLoadedListener() {
            @Override
            public void onDataLoaded(String[] fileNames) {
                if (fileNames == null) {
                    Toast.makeText(MainActivity.this, R.string.io_error, Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    mFilesName = fileNames;
                    mAdapter.setFilesName(fileNames);
                }
            }
        }).execute();
    }

    @Override
    public void onOverflowClick(View v, int i) {
        // Not used
    }

    @Override
    public void onItemClick(View v, int i) {
        if (mActionMode != null) {
            mAdapter.toggleSelected(i);
            mActionMode.invalidate();
        }
    }

    @Override
    public boolean onItemLongClick(View v, int i) {
        if (mActionMode == null) {
            mAdapter.toggleSelected(i);
            startSelectionActionMode();
            return true;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_export:
                startSelectionActionMode();
                return true;
            case R.id.action_info:
                showInfoDialog();
                return true;
            default:
                return false;
        }
    }

    private void showInfoDialog() {
        new MaterialDialog.Builder(this)
                .title(R.string.action_about)
                .customView(R.layout.about_dialog_message, true)
                .negativeText(android.R.string.cancel)
                .show();
    }

    private void startSelectionActionMode() {
        mActionMode = startActionMode(this);
        mActionMode.setTitle(R.string.action_export);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        getMenuInflater().inflate(R.menu.menu_action_mode, menu);
        checkActionMode(mode, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        checkActionMode(mode, menu);
        return true;
    }

    private void checkActionMode(ActionMode actionMode, Menu menu) {
        boolean exportAllowed = mAdapter.getSelectedItemsCount() != 0;
        menu.findItem(R.id.action_export).setEnabled(exportAllowed);
        actionMode.setSubtitle(mAdapter.getSelectedItemsCount() + " " + getString(R.string.action_item_selected));
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (item.getItemId() == R.id.action_export) {
            Intent intent = new Intent(this, ExportActivity.class);
            String[] selectedFilesNames = getFilesNameForPosition(mAdapter.getSelectedItems());
            intent.putExtra(ExportActivity.EXTRA_SELECTED_ITEMS, selectedFilesNames);
            startActivity(intent);
            mode.finish();
        }
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mAdapter.clearSelection();
        mActionMode = null;
    }

    private String[] getFilesNameForPosition(List<Integer> list) {
        String[] fileNames = new String[list.size()];
        for (int i = 0; i < fileNames.length; i++)
            fileNames[i] = mFilesName[list.get(i)];
        return fileNames;
    }
}
