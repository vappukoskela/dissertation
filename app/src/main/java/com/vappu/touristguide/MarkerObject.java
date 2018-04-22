package com.vappu.touristguide;


import com.google.android.gms.maps.model.LatLng;

public class MarkerObject {


    private LatLng latLng;
    private String title;
    private String wikiID;
    private String placeID;
    private double distance;

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public void setLatLng(LatLng latLng) {
        this.latLng = latLng;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getWikiID() {
        return wikiID;
    }

    public void setWikiID(String wikiID) {
        this.wikiID = wikiID;
    }

    public String getPlaceID() {
        return placeID;
    }

    public void setPlaceID(String placeID) {
        this.placeID = placeID;
    }
}
