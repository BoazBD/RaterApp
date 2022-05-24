package com.bardavid.boaz.rater;

import java.util.List;

public class PersonModel {
    private String id;
    private String name;
    private int age;
    private String gender;
    private List<String> urls;
    private String genderPreference;
    private String agePreference;
    //constructors
    public PersonModel() {
    }

    public PersonModel(String name, int age, String gender,String genderPreference,String agePreference) {
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.genderPreference=genderPreference;
        this.agePreference=agePreference;
    }

    @Override
    public String toString() {
        return "PersonModel{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", gender='" + gender + '\'' +
                ", imageUrl='" + "urls" + '\'' +
                '}';
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getGenderPreference() {
        return genderPreference;
    }

    public void setGenderPreference(String genderPreference) {
        this.genderPreference = genderPreference;
    }

    public String getAgePreference() {
        return agePreference;
    }

    public void setAgePreference(String agePreference) {
        this.agePreference = agePreference;
    }

    public void addUrl(String url){
        urls.add(url);
    }
    public List<String> getUrls() {
        return urls;
    }
    public void setUrls(List<String> urls){
        this.urls=urls;
    }
    public String getUrl(int index){
        if(urls.size()>index)
            return urls.get(index);
        else
            return null;
    }
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
