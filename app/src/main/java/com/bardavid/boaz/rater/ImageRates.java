package com.bardavid.boaz.rater;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

public class ImageRates {
    private String url;
    private List<Integer> rates;
    private int numRates;
    private String ownerID;
    private int reportsCount;

    public ImageRates(){
        this.numRates=0;
        this.reportsCount=0;
        this.rates=new ArrayList<Integer>(11);
        for(int i=0;i<11;i++)
            rates.add(0);
    }

    public String getOwnerID() {
        return ownerID;
    }

    public void setOwnerID(String ownerID) {
        this.ownerID = ownerID;
    }

    public int getReportsCount() {
        return reportsCount;
    }

    public void setReportsCount(int reportsCount) {
        this.reportsCount = reportsCount;
    }
    public void addReport() {
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference().child(Prefs.PUBLIC);
        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean isRated = false;
                ImageRates image = snapshot.child(Prefs.IMAGES_NEEDS_RATING).child(getUrl().substring(getUrl().length() - 10)).getValue(ImageRates.class);
                if (image == null) {
                    isRated = true;
                    image = snapshot.child(Prefs.IMAGES_RATED).child(getUrl().substring(getUrl().length() - 10)).getValue(ImageRates.class);
                }
                image.setReportsCount(getReportsCount() + 1);
                if (image.getReportsCount() == 3) {
                    //removes the image from it's place and moves it to reportedImages
                    if (isRated)
                        databaseRef.child(Prefs.IMAGES_RATED).child(getUrl().substring(getUrl().length() - 10)).removeValue();
                    else
                        databaseRef.child(Prefs.IMAGES_NEEDS_RATING).child(getUrl().substring(getUrl().length() - 10)).removeValue();

                    databaseRef.child(Prefs.REPORTED_IMAGES).child(getUrl().substring(getUrl().length() - 10)).setValue(image);
                } else if (isRated)
                    databaseRef.child(Prefs.IMAGES_RATED).child(getUrl().substring(getUrl().length() - 10)).setValue(image);
                else
                    databaseRef.child(Prefs.IMAGES_NEEDS_RATING).child(getUrl().substring(getUrl().length() - 10)).setValue(image);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
    public int getNumRates() {
        return numRates;
    }

    public void setNumRates(int numRates) {
        this.numRates = numRates;
    }
    public List<Integer> getRates() {
        return rates;
    }

    public void setRates(List<Integer> rates) {
        this.rates = rates;
    }
    public void setRate(int grade){
        rates.set(grade,rates.get(grade)+1);
    }


    public void addRating(int grade) {
        //Adds given rating to the image
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference().child(Prefs.PUBLIC);
        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean isPhotoRated=false;
                ImageRates imageRates = snapshot.child(Prefs.IMAGES_NEEDS_RATING).child(getUrl().substring(getUrl().length() - 10)).getValue(ImageRates.class);
                if(imageRates==null) {
                    imageRates = snapshot.child(Prefs.IMAGES_RATED).child(getUrl().substring(getUrl().length() - 10)).getValue(ImageRates.class);
                    isPhotoRated = true;
                }
                imageRates.setNumRates(imageRates.getNumRates() + 1);
                imageRates.setRate(grade);
                try {
                    if (isPhotoRated)
                        databaseRef.child(Prefs.IMAGES_RATED).child(getUrl().substring(getUrl().length() - 10)).setValue(imageRates);
                    else
                        databaseRef.child(Prefs.IMAGES_NEEDS_RATING).child(getUrl().substring(getUrl().length() - 10)).setValue(imageRates);
                    if (imageRates.getNumRates() == Prefs.PHOTO_RATINGS_LIMIT_SIZE) {
                        try {
                            movePhotoToRatedFolder();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                catch (Exception e){
                    System.out.println("Problem occured");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }
    public void movePhotoToRatedFolder() {
        //Moves the image from ImagesNeedsRating to ImagesRated
        DatabaseReference oldDatabaseRef = FirebaseDatabase.getInstance().getReference(Prefs.PUBLIC).child(Prefs.IMAGES_NEEDS_RATING);
        DatabaseReference newDatabaseRef = FirebaseDatabase.getInstance().getReference(Prefs.PUBLIC).child(Prefs.IMAGES_RATED);
        oldDatabaseRef.child(this.getUrl().substring(this.getUrl().length()-10)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ImageRates image=snapshot.getValue(ImageRates.class);
                newDatabaseRef.child(image.getUrl().substring(image.getUrl().length()-10)).setValue(image);
                oldDatabaseRef.child(image.getUrl().substring(image.getUrl().length()-10)).removeValue();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}
