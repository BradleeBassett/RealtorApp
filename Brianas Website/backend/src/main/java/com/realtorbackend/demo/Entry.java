package com.realtorbackend.demo;

import java.util.ArrayList;
import java.util.List;

public class Entry {
    private Long id;
    private String address;
    private String city;
    private String state;
    private String zipcode;
    private java.math.BigDecimal price;
    private String description;
    private String pictureUrl;
    private List<String> pictureUrls = new ArrayList<>();
    private String status = "ACTIVE";

    public Entry() {
    }

    public Entry(Long id, String address, String city, String state, String zipcode,
            java.math.BigDecimal price, String pictureUrl, String status) {
        this.id = id;
        this.address = address;
        this.city = city;
        this.state = state;
        this.zipcode = zipcode;
        this.price = price;
        this.pictureUrl = pictureUrl;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getZipcode() {
        return zipcode;
    }

    public void setZipcode(String zipcode) {
        this.zipcode = zipcode;
    }

    public java.math.BigDecimal getPrice() {
        return price;
    }

    public void setPrice(java.math.BigDecimal price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public void setPictureUrl(String pictureUrl) {
        this.pictureUrl = pictureUrl;
    }

    public List<String> getPictureUrls() {
        return pictureUrls;
    }

    public void setPictureUrls(List<String> pictureUrls) {
        this.pictureUrls = pictureUrls == null ? new ArrayList<>() : pictureUrls;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status == null ? "ACTIVE" : status.toUpperCase();
    }
}
