package com.example.exam_manager.dto;

public class StudentAverageDTO {

    private String studentName;
    private double averageScore;
    private int    submissionCount;

    public StudentAverageDTO(String studentName, double averageScore, int submissionCount) {
        this.studentName     = studentName;
        this.averageScore    = averageScore;
        this.submissionCount = submissionCount;
    }

    public String getStudentName()    { return studentName; }
    public double getAverageScore()   { return averageScore; }
    public int    getSubmissionCount(){ return submissionCount; }
}
