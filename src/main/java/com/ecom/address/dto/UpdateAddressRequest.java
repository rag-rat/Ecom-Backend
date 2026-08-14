package com.ecom.address.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;


@Data
public class UpdateAddressRequest {

    private String fullName;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Please provide a valid phone number")
    private String phoneNumber;

    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;

    @Pattern(regexp = "^[0-9]{6}$", message = "Pincode must be exactly 6 digits")
    private String pincode;

    private String country;

}
