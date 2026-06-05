package com.example.exam_manager.dto;

import java.util.List;

public class ClassReportDTO {

    private double averageScore;
    private int    totalSubmissions;
    private double successRate;    // אחוז ציונים >= 56
    private int    excellentCount; // 86-100
    private int    goodCount;      // 71-85
    private int    passingCount;   // 56-70
    private int    failCount;      // 0-55
    private List<StudentAverageDTO> studentAverages;

    public ClassReportDTO(double averageScore, int totalSubmissions, double successRate,
                          int excellentCount, int goodCount, int passingCount, int failCount,
                          List<StudentAverageDTO> studentAverages) {
        this.averageScore     = averageScore;
        this.totalSubmissions = totalSubmissions;
        this.successRate      = successRate;
        this.excellentCount   = excellentCount;
        this.goodCount        = goodCount;
        this.passingCount     = passingCount;
        this.failCount        = failCount;
        this.studentAverages  = studentAverages;
    }

    public double getAverageScore()     { return averageScore; }
    public int    getTotalSubmissions() { return totalSubmissions; }
    public double getSuccessRate()      { return successRate; }
    public int    getExcellentCount()   { return excellentCount; }
    public int    getGoodCount()        { return goodCount; }
    public int    getPassingCount()     { return passingCount; }
    public int    getFailCount()        { return failCount; }
    public List<StudentAverageDTO> getStudentAverages() { return studentAverages; }
}
