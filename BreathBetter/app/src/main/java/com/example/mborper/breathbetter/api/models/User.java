package com.example.mborper.breathbetter.api.models;

/**
 * Represents a user in the BreathBetter application, storing key information
 * such as personal details, contact information, and user role.
 * Provides getter and setter methods for accessing and modifying user details.
 *
 * @author Alejandro Rosado
 * @since 2024-10-28
 */
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

    /**
     * Gets the unique identifier for this user.
     *
     * @return the user ID.
     */
    public int getIdUser() {
        return idUser;
    }

    /**
     * Gets the user's first name.
     *
     * @return the name of the user.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the user's first name.
     *
     * @param name the first name to set for the user.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the user's surname.
     *
     * @return the surname of the user.
     */
    public String getSurname() {
        return surname;
    }

    /**
     * Sets the user's surname.
     *
     * @param surname the surname to set for the user.
     */
    public void setSurname(String surname) {
        this.surname = surname;
    }

    /**
     * Gets the user's email address.
     *
     * @return the email address of the user.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the user's email address.
     *
     * @param email the email address to set for the user.
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the user's password.
     * Note: This method currently returns the email instead of the password
     *
     * @return the password of the user.
     */
    public String getPassword() {
        return email;
    }

    /**
     * Sets the user's password.
     * Note: This method currently sets the email instead of the password
     *
     * @param password the password to set for the user.
     */
    public void setPassword(String password) {
        this.email = email;
    }

    /**
     * Gets the user's phone number.
     *
     * @return the phone number of the user.
     */
    public String getPhone() {
        return phone;
    }

    /**
     * Gets the user's country of residence.
     *
     * @return the country where the user resides.
     */
    public String getCountry() {
        return country;
    }

    /**
     * Gets the user's city of residence.
     *
     * @return the city where the user resides.
     */
    public String getCity() {
        return city;
    }

    /**
     * Gets the user's postal code.
     *
     * @return the postal code of the user's address.
     */
    public String getZip() {
        return zip;
    }

    /**
     * Gets the user's street address.
     *
     * @return the address of the user.
     */
    public String getAddress() {
        return address;
    }

    /**
     * Gets the user's role identifier, which indicates the role assigned
     * to the user within the application.
     *
     * @return the role ID of the user.
     */
    public int getRoleId() {
        return roleId;
    }
}
