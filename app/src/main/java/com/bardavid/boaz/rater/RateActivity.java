package com.bardavid.boaz.rater;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class RateActivity extends AppCompatActivity {

    private int[] buttonIds = {0, R.id.button1,R.id.button2, R.id.button3,R.id.button4, R.id.button5,
            R.id.button6,R.id.button7,R.id.button8, R.id.button9, R.id.button10};
    private Button [] rateBtns=new Button[11];
    private BottomNavigationView bottomNavigationView;
    private ImageView image1,image2,reportImg;
    private Boolean isShowingImage1;
    private TextView numRatings;
    private ProgressBar progressBar;
    private List<ImageRates> uploads=new ArrayList<>();
    private ImageRates currPhoto;
    private int indexOfPicToRate;
    private int indexOfPicToLoad;
    private DatabaseReference databaseRef;

    private String name;
    private String id;
    private String gender;
    private int age;
    private int ratingsCount, photoCount;
    private Dialog instructionsDialog;
    private Animation slideLeftFromCenter;
    private Animation slideLeftToCenter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rate);
        loadPreferences();
        showNavigationBar();
        isShowingImage1=Boolean.TRUE;
        instructionsDialog=new Dialog(this);
        if(ratingsCount==0)
            openInstructionsDialog();
        indexOfPicToRate =ratingsCount;
        indexOfPicToLoad=0;
        for (int i = 1; i <= 10; i++) {
            Button button = findViewById(buttonIds[i]);
            rateBtns[i] = button;
        }
        reportImg=findViewById(R.id.reportImg);
        image1 = findViewById(R.id.image1);
        image2=findViewById(R.id.image2);
        progressBar = findViewById(R.id.progressBar);
        numRatings=findViewById(R.id.numRatings);
        numRatings.setText(ratingsCount + "/"+ (Prefs.NUMBER_OF_RATINGS_NEEDED_FOR_EACH_PHOTO *photoCount));
        disableButtons();
        slideLeftFromCenter = AnimationUtils.loadAnimation(this,R.anim.slide_left_from_center);
        slideLeftToCenter = AnimationUtils.loadAnimation(this,R.anim.slide_left_to_center);

        reportImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(RateActivity.this);
                    builder.setTitle("Do you want to report this picture?");
                    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            currPhoto.addReport();
                            nextPhoto();
                            loadPhoto(0); //grade 0 means don't add any grade to the photo
                            Toast.makeText(RateActivity.this,"Photo reported successfully",Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                        }
                    });
                    builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
            }
        });
        databaseRef = FirebaseDatabase.getInstance().getReference().child(Prefs.PUBLIC);
        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //Creates a list of images that the user is eligible to rate. max size of 200
                //First adds images that need rating, if there's more room then adds also rated photos
                int photoCounter=0;
                for (DataSnapshot postSnapshot : snapshot.child(Prefs.IMAGES_NEEDS_RATING).getChildren()) {
                    ImageRates image = postSnapshot.getValue(ImageRates.class);
                    PersonModel imageOwner=snapshot.child(Prefs.PEOPLE).child(image.getOwnerID()).getValue(PersonModel.class);
                    if(imageOwner!=null && !(imageOwner.getId().equals(id)) && isLegalRater(imageOwner)) {
                        uploads.add(image);
                        photoCounter++;
                    }
                    if(photoCounter>=200)
                        break;
                }
                for(DataSnapshot postSnapshot:snapshot.child(Prefs.IMAGES_RATED).getChildren()){
                    ImageRates image=postSnapshot.getValue(ImageRates.class);
                    PersonModel imageOwner=snapshot.child(Prefs.PEOPLE).child(image.getOwnerID()).getValue(PersonModel.class);
                    if(imageOwner!=null && !(imageOwner.getId().equals(id)) && isLegalRater(imageOwner)) {
                        uploads.add(image);
                        photoCounter++;
                    }
                    if(photoCounter>=200)
                        break;
                }
                if(indexOfPicToRate >=uploads.size())
                    indexOfPicToRate =0;
                currPhoto = uploads.get(indexOfPicToRate);
                for(int i=0;i<5;i++)
                    loadNextPhotoFromCache();
                loadPhoto(0);
                //grade 0 means dont add rating to currPhoto
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(RateActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
    public void btn1(View v){ rateButtonClicked(1); }
    public void btn2(View v){ rateButtonClicked(2); }
    public void btn3(View v){ rateButtonClicked(3); }
    public void btn4(View v){ rateButtonClicked(4); }
    public void btn5(View v){ rateButtonClicked(5); }
    public void btn6(View v){ rateButtonClicked(6); }
    public void btn7(View v){ rateButtonClicked(7); }
    public void btn8(View v){ rateButtonClicked(8); }
    public void btn9(View v){ rateButtonClicked(9); }
    public void btn10(View v){ rateButtonClicked(10); }

        public void rateButtonClicked(int grade){
        //Adds chosen rating to database, then loads next photo
            disableButtons();
            rateBtns[grade].setBackgroundResource(R.drawable.greenbutton);
            try {
                currPhoto.addRating(grade);
            }
            catch (Exception e){
                System.out.println("EXECTION: "+e.getMessage());
            }
            nextPhoto();
            loadPhoto(grade);
    }
    public void loadPhoto(int grade){
        Boolean shouldAnimate=image1.getDrawable()!=null;
        //first tries to fetch next photo from cache (it tried to load to cache 5 next photos previously)
        Picasso.get().load(currPhoto.getUrl()).networkPolicy(NetworkPolicy.OFFLINE).into(image2, new Callback() {
            @Override
            public void onSuccess() {
                //Toast.makeText(RateActivity.this, "SUCCESS OFFLINE", Toast.LENGTH_SHORT).show();
                if (shouldAnimate) {
                    animateImageSwitch(grade);
                }
                else
                {
                    image1.setVisibility(View.INVISIBLE);
                    image2.setVisibility(View.VISIBLE);
                    handleSuccessfulImageLoad(grade);
                }
            }
            @Override
            public void onError(Exception e) { //could not load from cache, now loads from online
                image2.setVisibility(View.INVISIBLE);
                image1.setVisibility(View.INVISIBLE);
                progressBar.setVisibility(View.VISIBLE);
                Picasso.get().load(currPhoto.getUrl()).into(image2, new Callback() {
                    @Override
                    public void onSuccess() {
                            image1.setVisibility(View.INVISIBLE);
                            image2.setVisibility(View.VISIBLE);
                            handleSuccessfulImageLoad(grade);
                    }
                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(RateActivity.this, "Error loading image", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        loadNextPhotoFromCache();
    }
    public void animateImageSwitch(int grade) {
        image1.startAnimation(slideLeftFromCenter);
        image2.setVisibility(View.VISIBLE);
        image2.startAnimation(slideLeftToCenter);
        image1.setVisibility(View.INVISIBLE);
        slideLeftToCenter.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                handleSuccessfulImageLoad(grade);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }
    public void handleSuccessfulImageLoad(int grade){
        ImageView temp=image1;
        image1=image2;
        image2=temp;
        progressBar.setVisibility(View.GONE);
        if(grade!=0) {
            rateBtns[grade].setBackgroundResource(R.drawable.redbutton); //resets clicked btn's color
            ratingsCount++;
            updatePreferences();
            if (ratingsCount == Prefs.NUMBER_OF_RATINGS_NEEDED_FOR_EACH_PHOTO *photoCount) {
                Intent intent = new Intent(RateActivity.this, MyRatingsActivity.class);
                startActivity(intent);
            }
        }
        if(ratingsCount>Prefs.NUMBER_OF_RATINGS_NEEDED_FOR_EACH_PHOTO *photoCount)
            numRatings.setVisibility(View.INVISIBLE);
        numRatings.setText(ratingsCount + "/"+(Prefs.NUMBER_OF_RATINGS_NEEDED_FOR_EACH_PHOTO *photoCount));
        enableButtons();
    }
    public void nextPhoto() {
        //Places next photo in line in currPhoto
        indexOfPicToRate++;
        if (indexOfPicToRate == uploads.size()) {
            Toast.makeText(RateActivity.this, "Out of people!", Toast.LENGTH_LONG).show();
            switchToMyRatingsActivity();
            return;
        }
        currPhoto = uploads.get(indexOfPicToRate);
    }
    public Boolean isLegalRater(PersonModel owner) {
        //Returns true if the user fits the preferences of photo owner
        String ownerGenderPreference = owner.getGenderPreference();
        String ownerAgePreference = owner.getAgePreference();
        switch (ownerGenderPreference) {
            case Prefs.MALES_ONLY:
                if (gender != Prefs.MALE)
                    return false;
                break;
            case Prefs.FEMALES_ONLY:
                if (gender != Prefs.FEMALE)
                    return false;
                break;
        }
        switch (ownerAgePreference) {
            case Prefs.UNDER_18:
                if (age > 18)
                    return false;
                break;
            case Prefs.BETWEEN_18_29:
                if (age < 18 || age > 29)
                    return false;
                break;
            case Prefs.OVER_30:
                if (age < 30)
                    return false;
                break;
        }
        return true;
    }

    public void switchToMyRatingsActivity(){
        Intent intent=new Intent(RateActivity.this ,MyRatingsActivity.class);
        startActivity(intent);
        finish();
    }
    public void showNavigationBar(){
        bottomNavigationView=findViewById(R.id.bottom_menu);
        bottomNavigationView.setSelectedItemId(R.id.home);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId()){
                    case R.id.upload:
                        Intent intent=new Intent(RateActivity.this , ChoosePhotoActivity.class);
                        startActivity(intent);
                        return true;
                    case R.id.graph:
                        intent=new Intent(RateActivity.this , MyRatingsActivity.class);
                        startActivity(intent);
                        finish();
                        return true;
                    case R.id.home:
                        return true;
                    case R.id.settings:
                        intent = new Intent(RateActivity.this, SettingsActivity.class);
                        startActivity(intent);
                        return true;
                }
                return false;
            }
        });
    }
    public void loadPreferences(){
        SharedPreferences sharedPreferences= getSharedPreferences(Prefs.PREFS_NAME, MODE_PRIVATE);
        name=sharedPreferences.getString(Prefs.NAME,Prefs.ERROR_VALUE);
        id=sharedPreferences.getString(Prefs.ID,Prefs.ERROR_VALUE);
        gender= sharedPreferences.getString(Prefs.GENDER,Prefs.ERROR_VALUE);
        age=sharedPreferences.getInt(Prefs.AGE,0);
        ratingsCount=sharedPreferences.getInt(Prefs.RATINGS_COUNT,0);
        photoCount=sharedPreferences.getInt(Prefs.PHOTO_COUNT,0);
    }
    public void updatePreferences(){
        SharedPreferences sharedPreferences=getSharedPreferences(Prefs.PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor=sharedPreferences.edit();
        editor.putInt(Prefs.RATINGS_COUNT,ratingsCount);
        editor.apply();
    }
    public void openInstructionsDialog(){
        instructionsDialog.setContentView(R.layout.instructions_layout_dialog);
        instructionsDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        ImageView closeImageView=instructionsDialog.findViewById(R.id.closeImageView);
        Button gotItBtn=instructionsDialog.findViewById(R.id.gotItBtn);
        closeImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                instructionsDialog.dismiss();
            }
        });
        gotItBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                instructionsDialog.dismiss();
            }
        });
        instructionsDialog.show();
    }
    public void disableButtons(){
        for(int i=1;i<=10;i++){
            findViewById(buttonIds[i]).setEnabled(false);
        }
        reportImg.setEnabled(false);
    }
    public void enableButtons(){
        for(int i=1;i<=10;i++){
            findViewById(buttonIds[i]).setEnabled(true);
        }
        reportImg.setEnabled(true);
    }
    public void loadNextPhotoFromCache() {
        if (indexOfPicToLoad <= 200 && indexOfPicToLoad<uploads.size()) {
            Picasso.get().load(uploads.get(indexOfPicToLoad).getUrl()).fetch(new Callback() {
                @Override
                public void onSuccess() {
                    //Toast.makeText(RateActivity.this,"finished loading photo"+indexOfPicToLoad,Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(Exception e) {

                }
            });
            indexOfPicToLoad++;
        }
    }
}
