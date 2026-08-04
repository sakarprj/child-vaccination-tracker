package com.hospital.vaccination.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Represents one row in the children table. */
public class Child {

    public enum Gender { MALE, FEMALE, OTHER }

    private int id;
    private String name;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String parentName;
    private String parentPhone;
    private String address;
    private int registeredBy;          // FK -> users.id (the nurse)
    private LocalDateTime registeredAt;

    public Child() { }

    public Child(int id, String name, LocalDate dateOfBirth, Gender gender,
                 String parentName, String parentPhone, String address,
                 int registeredBy, LocalDateTime registeredAt) {
        this.id = id;
        this.name = name;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.parentName = parentName;
        this.parentPhone = parentPhone;
        this.address = address;
        this.registeredBy = registeredBy;
        this.registeredAt = registeredAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dob) { this.dateOfBirth = dob; }

    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }

    public String getParentName() { return parentName; }
    public void setParentName(String parentName) { this.parentName = parentName; }

    public String getParentPhone() { return parentPhone; }
    public void setParentPhone(String parentPhone) { this.parentPhone = parentPhone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public int getRegisteredBy() { return registeredBy; }
    public void setRegisteredBy(int registeredBy) { this.registeredBy = registeredBy; }

    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }

    @Override
    public String toString() { return name + " (DOB " + dateOfBirth + ")"; }
}