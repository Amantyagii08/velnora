package com.ruralmarketplace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {
    @NotBlank @Size(max = 100) private String fullName;
    @Pattern(regexp = "^$|^[0-9]{10,15}$", message = "Phone must contain 10 to 15 digits") private String phone;
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
