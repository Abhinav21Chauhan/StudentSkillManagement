package org.example.studentskillmanagement;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Student {

    private int studentId;
    private String name;
    private String email;
    private String course;
    private double programmingScore;
    private double mathsScore;
    private List<String> skills = new ArrayList<>();

    public Student() {
    }

    public Student(int studentId, String name, String email, String course,
                   double programmingScore, double mathsScore, List<String> skills) {
        this.studentId = studentId;
        this.name = name;
        this.email = email;
        this.course = course;
        this.programmingScore = programmingScore;
        this.mathsScore = mathsScore;
        this.skills = (skills == null) ? new ArrayList<>() : skills;
    }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }

    public double getProgrammingScore() { return programmingScore; }
    public void setProgrammingScore(double programmingScore) { this.programmingScore = programmingScore; }

    public double getMathsScore() { return mathsScore; }
    public void setMathsScore(double mathsScore) { this.mathsScore = mathsScore; }

    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) {
        this.skills = (skills == null) ? new ArrayList<>() : skills;
    }

    // ---------- Business logic (calculated, not stored in DB) ----------

    // Q2.1 Average score
    public double getAverage() {
        return (programmingScore + mathsScore) / 2.0;
    }

    // Q2.2 Performance category
    public String getCategory() {
        double avg = getAverage();
        if (avg >= 85) return "Excellent";
        if (avg >= 70) return "Good";
        if (avg >= 50) return "Satisfactory";
        return "Needs Improvement";
    }

    // Q2.3 Stronger subject
    public String getStrongerSubject() {
        if (programmingScore > mathsScore) return "Programming";
        if (mathsScore > programmingScore) return "Mathematics";
        return "Both Equal";
    }

    // ---------- Object methods ----------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Student)) return false;
        Student other = (Student) o;
        return studentId == other.studentId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(studentId);
    }

    @Override
    public String toString() {
        return "Student{id=" + studentId + ", name='" + name + "', email='" + email
                + "', course='" + course + "', programming=" + programmingScore
                + ", maths=" + mathsScore + ", skills=" + skills + "}";
    }
}