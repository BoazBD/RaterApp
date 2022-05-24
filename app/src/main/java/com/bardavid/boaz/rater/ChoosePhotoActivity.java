package com.bardavid.boaz.rater;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;


public class ChoosePhotoActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private Boolean isUserFirstPhoto;
    private Button openGalleryBtn,uploadBtn;
    private ImageView imageView;
    private ProgressBar progressBar;
    private BottomNavigationView bottomNavigationView;
    private Uri ImageUri;
    private StorageReference storageRef;
    private DatabaseReference databasePersonRef,databaseImageRef;
    private StorageTask uploadTask;
    private PersonModel person;
    private String name,id,gender,genderPreference, agePreference;
    private int age,photoCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_photo);
        setSupportActionBar(findViewById(R.id.toolbar));
        Intent intent = getIntent();
        isUserFirstPhoto = intent.getStringExtra("from")!= null && intent.getStringExtra("from").equals("main_activity");
        openGalleryBtn = findViewById(R.id.openGalleryBtn);
        uploadBtn = findViewById(R.id.uploadBtn);
        imageView = findViewById(R.id.imageView);
        progressBar = findViewById(R.id.progressBar);
        bottomNavigationView = findViewById(R.id.bottom_menu);
        loadPreferences();
        if(!isUserFirstPhoto) {
            showNavigationBar();
        }
        else {
            bottomNavigationView.setVisibility(View.INVISIBLE);
        }
        storageRef = FirebaseStorage.getInstance().getReference("uploads");
        databasePersonRef = FirebaseDatabase.getInstance().getReference().child(Prefs.PUBLIC);
        openGalleryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileChooser();
            }
        });
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileChooser();
            }
        });
        uploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (uploadTask != null && uploadTask.isInProgress()) {
                    Toast.makeText(ChoosePhotoActivity.this, "upload in progress", Toast.LENGTH_SHORT).show();
                } else {
                    uploadBtn.setVisibility(View.INVISIBLE);
                    progressBar.setVisibility(View.VISIBLE);
                    uploadFile();
                }
            }
        });
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //Loads the chosen picture on screen
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            ImageUri = data.getData();
            Picasso.get().load(ImageUri).into(imageView);
            uploadBtn.setVisibility(View.VISIBLE);
        }
    }

    public String getFileExtension(Uri uri) {
        ContentResolver cR = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cR.getType(uri));
    }
    private void uploadFile() {
        //Uploads the chosen image to FireBase Storage
        if (ImageUri != null) {
            //Creates custom string for image's storage name
            String randDigits= String.valueOf((int)(Math.random() * 9))+String.valueOf((int)(Math.random() * 9));
            final StorageReference fileReference = storageRef.child(id
                    +"p"+randDigits+ "." + getFileExtension(ImageUri));
            uploadTask=fileReference.putFile(ImageUri);
            Task<Uri>urlTask=uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>()
            {
                @Override
                public Task<Uri> then(@NonNull Task task) throws Exception {
                    if(!task.isSuccessful())
                        throw task.getException();
                    return fileReference.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if(task.isSuccessful()) {
                        //Uploaded successfully, now uploading to Database
                        Uri downloadUri = task.getResult();
                        String url = downloadUri.toString();
                        databasePersonRef.child(Prefs.PEOPLE).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.hasChild(id)) {
                                    person = snapshot.child(id).getValue(PersonModel.class);
                                    person.addUrl(url);
                                } else {
                                    person=new PersonModel(name,age,gender, genderPreference,agePreference);
                                    person.setId(id);
                                    List<String> urls = new ArrayList<>();
                                    urls.add(url);
                                    person.setUrls(urls);
                                    isUserFirstPhoto=true;
                                }
                                //Updates the person and also uploads the new image to database
                                databasePersonRef.child(Prefs.PEOPLE).child(id).setValue(person);
                                databaseImageRef = FirebaseDatabase.getInstance().getReference().child(Prefs.PUBLIC);
                                ImageRates newImg = new ImageRates();
                                newImg.setUrl(url);
                                newImg.setOwnerID(id);
                                databaseImageRef.child(Prefs.IMAGES_NEEDS_RATING).child(url.substring(url.length() - 10)).setValue(newImg);
                                progressBar.setVisibility(View.GONE);
                                imageView.setImageURI(null);
                                updatePreference();
                                moveToRate();
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(ChoosePhotoActivity.this, "Error", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    else {
                        Toast.makeText(ChoosePhotoActivity.this, "Failed uploading", Toast.LENGTH_LONG).show();
                        progressBar.setVisibility(View.INVISIBLE);
                        uploadBtn.setVisibility(View.VISIBLE);
                    }
                }
            });
           } else {
            Toast.makeText(this, "No file selected", Toast.LENGTH_LONG).show();
        }
    }
    public void moveToRate(){
        Intent intent=new Intent(ChoosePhotoActivity.this ,RateActivity.class);
        intent.putExtra("isUserFirstPhoto", isUserFirstPhoto);
        startActivity(intent);
    }
    public void showNavigationBar() {
        //Shows the navigation bar on bottom of screen
        bottomNavigationView.setSelectedItemId(R.id.upload);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.upload:
                        return true;
                    case R.id.graph:
                        Intent intent = new Intent(ChoosePhotoActivity.this, MyRatingsActivity.class);
                        startActivity(intent);
                        return true;
                    case R.id.home:
                        intent = new Intent(ChoosePhotoActivity.this, RateActivity.class);
                        startActivity(intent);
                        return true;
                    case R.id.settings:
                        intent = new Intent(ChoosePhotoActivity.this, SettingsActivity.class);
                        startActivity(intent);
                        return true;
                }
                return false;
            }
        });
    }
    public void updatePreference(){
        SharedPreferences sharedPreferences=getSharedPreferences(Prefs.PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor=sharedPreferences.edit();
        editor.putInt(Prefs.PHOTO_COUNT,photoCount+1);
        editor.apply();
    }
    public void loadPreferences(){
        SharedPreferences sharedPreferences= getSharedPreferences(Prefs.PREFS_NAME, MODE_PRIVATE);
        name=sharedPreferences.getString(Prefs.NAME,Prefs.ERROR_VALUE);
        id=sharedPreferences.getString(Prefs.ID,Prefs.ERROR_VALUE);
        gender= sharedPreferences.getString(Prefs.GENDER,Prefs.ERROR_VALUE);
        genderPreference=sharedPreferences.getString(Prefs.GENDER_PREFERENCE,Prefs.ALL_GENDERS);
        agePreference=sharedPreferences.getString(Prefs.AGE_PREFERENCE,Prefs.ALL_GENDERS);
        age=sharedPreferences.getInt(Prefs.AGE,0);
        photoCount=sharedPreferences.getInt(Prefs.PHOTO_COUNT,0);
    }
}