package edu.ucsb.cs.cs185.connortinsley.cameraroll;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.melnykov.fab.FloatingActionButton;

import java.io.File;
import java.io.IOException;


public class MainActivity extends ActionBarActivity {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final String PHOTO_COUNT = "photo count";
    static final String DELETE_COUNT = "delete count";

    private String currentPhotoPath;

    FloatingActionButton fab;
    TextView emptyText;

    private RecyclerView mRecyclerView;
    private MyAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // create photo dir if necessary
        createPhotoDir();

        // setup view objects
        mRecyclerView = (RecyclerView)findViewById(R.id.list);
        emptyText = (TextView)findViewById(R.id.empty);
        fab = (FloatingActionButton)findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchPictureTakingIntent();
            }
        });

        // update empty text appropriately
        updateEmptyText();

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        mAdapter = new MyAdapter(this, getPhotoTakenCount());
        mRecyclerView.setAdapter(mAdapter);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // user selected to delete all photos
        if (id == R.id.delete) {
            deletePhotoDir();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void deletePhotoDir() {
        // delete all the files in dir
        File dir = new File(this.getExternalFilesDir(null) + "/" + "CS185Pics" + Integer.valueOf(getDeleteCount()).toString());
        for(File file: dir.listFiles()) file.delete();

        // delete folder
        dir.delete();

        // update photo and delete counts
        incrementDeleteCount();
        resetPhotoTakenCount();

        // create new dir. This is a picasso cache invalidation workaround
        createPhotoDir();

        // notify adapter of changes
        mAdapter.updatePhotoCount(0);
        mAdapter.notifyDataSetChanged();

        // update empty text appropriately
        updateEmptyText();
    }

    private void createPhotoDir() {
        // creates new photo dir taking into account delete count
        final File dir = new File(this.getExternalFilesDir(null) + "/" + "CS185Pics" + Integer.valueOf(getDeleteCount()).toString());
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public int getDeleteCount() {
        SharedPreferences prefs = this.getPreferences(MODE_PRIVATE);
        int deleteCount = prefs.getInt(DELETE_COUNT, 0);
        return deleteCount;
    }

    private void incrementDeleteCount() {
        int deleteCount = getDeleteCount();
        ++deleteCount;
        SharedPreferences prefs = this.getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(DELETE_COUNT, deleteCount);
        editor.commit();
    }

    private int getPhotoTakenCount() {
        SharedPreferences prefs = this.getPreferences(MODE_PRIVATE);
        int photoCount = prefs.getInt(PHOTO_COUNT, 0);
        return photoCount;
    }

    private void incrementPhotoTakenCount() {
        int photoCount = getPhotoTakenCount();
        ++photoCount;
        SharedPreferences prefs = this.getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PHOTO_COUNT, photoCount);
        editor.commit();
    }

    private void resetPhotoTakenCount() {
        SharedPreferences prefs = this.getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PHOTO_COUNT, 0);
        editor.commit();
    }

    private File createImageFile() throws IOException {
        // Create an image filename
        int numPhotos = getPhotoTakenCount();
        ++numPhotos;
        String imageFileName;
        imageFileName = "photo-" + Integer.valueOf(numPhotos).toString();

        // Create file
        File dir = new File(this.getExternalFilesDir(null) + "/" + "CS185Pics" + Integer.valueOf(getDeleteCount()).toString());
        File image = new File(dir, imageFileName + ".jpg");
        if (!image.exists() ) {
            image.createNewFile();
        }

        // Save file path for possible future deletion
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void launchPictureTakingIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException e) {
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private void updateEmptyText() {
        if (getPhotoTakenCount() == 0) {
            emptyText.setVisibility(View.VISIBLE);
        }
        else {
            emptyText.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {

            // update photo count
            incrementPhotoTakenCount();

            // notify adapter that new photo has been taken and list should update
            mAdapter.updatePhotoCount(getPhotoTakenCount());
            mAdapter.notifyDataSetChanged();
        }

        // something went wrong so delete created photo file
        if (resultCode != RESULT_OK) {
            File file = new File(currentPhotoPath);
            file.delete();
        }

        // update empty text appropriately
        updateEmptyText();
    }

}
