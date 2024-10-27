package com.example.mborper.breathbetter.login.pojo;

// User.java
public class User {
    private int idUser;
    private String name;
    private String surname;
    private String email;
    private String password;
    private String phone;
    private String country;
    private String city;
    private String zip;
    private String address;
    private int roleId;

    public int getIdUser() {
        return idUser;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return email;
    }

    public void setPassword(String password) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public String getCountry() {
        return country;
    }

    public String getCity() {
        return city;
    }

    public String getZip() {
        return zip;
    }

    public String getAddress() {
        return address;
    }

    public int getRoleId() {
        return roleId;
    }
}
