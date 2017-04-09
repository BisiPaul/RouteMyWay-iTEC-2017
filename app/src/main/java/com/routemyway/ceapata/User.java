package com.routemyway.ceapata;

public class User {
    private String Username;
    private String Longitude;
    private String Latitude;
    private String initTimestamp;
    private String presentTimestamp;

    public User(String Username, String Longitude, String Latitude, String initTimestamp, String presentTimestamp) {
        this.Username = Username;
        this.Longitude = Longitude;
        this.Latitude = Latitude;
        this.initTimestamp = initTimestamp;
        this.presentTimestamp = presentTimestamp;
    }

    public User(){

    }

    public String getUsername() {
        return Username;
    }

    public String getLongitude() {
        return Longitude;
    }

    public void setLongitude(String Longitude) {
        this.Longitude = Longitude;
    }

    public String getLatitude() {
        return Latitude;
    }

    public void setLatitude(String Latitude) {
        this.Latitude = Latitude;
    }

    public String getInitTimestamp() {
        return initTimestamp;
    }

    public void setInitTimestamp(String initTimestamp) { this.initTimestamp = initTimestamp; }

    public String getPresentTimestamp() {
        return presentTimestamp;
    }

    public void setPresentTimestamp(String presentTimestamp) { this.presentTimestamp = presentTimestamp; }
}