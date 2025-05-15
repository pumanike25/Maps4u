package com.example.maps4u;

public class BiometricData {
    private float height; // in cm
    private float weight; // in kg
    private int age;
    private String gender;
    private int stepCount;

    public BiometricData(float height, float weight, int age, String gender, int stepCount) {
        this.height = height;
        this.weight = weight;
        this.age = age;
        this.gender = gender;
        this.stepCount = stepCount;
    }

    // Getters and setters
    public float getHeight() { return height; }
    public float getWeight() { return weight; }
    public int getAge() { return age; }
    public String getGender() { return gender; }
    public int getStepCount() { return stepCount; }

    public void setHeight(float height) { this.height = height; }
    public void setWeight(float weight) { this.weight = weight; }
    public void setAge(int age) { this.age = age; }
    public void setGender(String gender) { this.gender = gender; }
    public void setStepCount(int stepCount) { this.stepCount = stepCount; }
}