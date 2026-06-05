package com.example.exam_manager.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "teachers")
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "תעודת זהות לא יכולה להיות ריקה")
    @Column(unique = true)
    private String identityNumber;

    @NotBlank(message = "שם מלא לא יכול להיות ריק")
    private String fullName;

    private String email;

    @NotBlank(message = "סיסמה לא יכולה להיות ריקה")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(20) DEFAULT 'HOMEROOM'")
    private TeacherRole role = TeacherRole.HOMEROOM;

    private String specializedSubject;

    private String assignedClass;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIdentityNumber() { return identityNumber; }
    public void setIdentityNumber(String identityNumber) { this.identityNumber = identityNumber; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public TeacherRole getRole() { return role; }
    public void setRole(TeacherRole role) { this.role = role; }

    public String getSpecializedSubject() { return specializedSubject; }
    public void setSpecializedSubject(String specializedSubject) { this.specializedSubject = specializedSubject; }

    public String getAssignedClass() { return assignedClass; }
    public void setAssignedClass(String assignedClass) { this.assignedClass = assignedClass; }
}
