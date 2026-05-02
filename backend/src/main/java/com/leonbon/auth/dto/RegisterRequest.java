package com.leonbon.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {
    @NotBlank
    @Size(min = 3, max = 32)
    private String username;

    @NotBlank
    @Email
    @Size(max = 120)
    private String email;

    @NotBlank
    @Size(min = 6, max = 72)
    private String password;

    /** Apodo visible dentro de la comunidad (opcional). */
    private String nickname;

    @Size(max = 120)
    private String fullName;

    @Size(max = 80)
    private String country;

    /** Cuando informas fullName puedes optar por hacerlo visible públicamente. */

    private Boolean profileShowFullName;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Boolean getProfileShowFullName() {
        return profileShowFullName;
    }

    public void setProfileShowFullName(Boolean profileShowFullName) {
        this.profileShowFullName = profileShowFullName;
    }
}
