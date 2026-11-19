package org.example.studentskillmanagement;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/students")
public class StudentController {

    private final JdbcTemplate jdbc;

    public StudentController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Body for the update API (wrapper types so missing fields = null)
    public record UpdateRequest(Double programmingScore, Double mathsScore, List<String> skills) {
    }

    // ===================== Q3.1 ADD STUDENT =====================
    @PostMapping
    @Transactional
    public ResponseEntity<String> addStudent(@RequestBody Student s) {
        String error = validate(s);
        if (error != null) {
            return ResponseEntity.badRequest().body(error);
        }
        if (exists(s.getStudentId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Student ID " + s.getStudentId() + " already exists.");
        }
        Integer emailCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM student WHERE email = ?", Integer.class, s.getEmail());
        if (emailCount != null && emailCount > 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Email " + s.getEmail() + " is already registered.");
        }

        jdbc.update("INSERT INTO student (student_id, name, email, course, programming_score, maths_score) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                s.getStudentId(), s.getName(), s.getEmail(), s.getCourse(),
                s.getProgrammingScore(), s.getMathsScore());
        saveSkills(s.getStudentId(), s.getSkills());

        return ResponseEntity.status(HttpStatus.CREATED).body("Student added successfully!");
    }

    // ===================== Q3.2 GET ALL =====================
    @GetMapping
    public List<Student> getAllStudents() {
        return loadStudents("");
    }

    // ===================== Q3.3 GET BY ID =====================
    @GetMapping("/{id}")
    public ResponseEntity<Object> getStudentById(@PathVariable int id) {
        List<Student> list = loadStudents("WHERE student_id = ?", id);
        if (list.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Student not found.");
        }
        return ResponseEntity.ok(list.get(0));
    }

    // ===================== Q2.4 SEARCH BY EMAIL OR COURSE =====================
    @GetMapping("/search")
    public ResponseEntity<Object> search(@RequestParam(required = false) String email,
                                         @RequestParam(required = false) String course) {
        if (email != null) {
            return ResponseEntity.ok(loadStudents("WHERE email = ?", email));
        }
        if (course != null) {
            return ResponseEntity.ok(loadStudents("WHERE course = ?", course));
        }
        return ResponseEntity.badRequest().body("Provide email or course as a query parameter.");
    }

    // ===================== Q3.4 UPDATE SCORES AND SKILLS =====================
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<String> updateStudent(@PathVariable int id, @RequestBody UpdateRequest req) {
        if (!exists(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Student not found.");
        }
        if (req.programmingScore() != null && !validScore(req.programmingScore())) {
            return ResponseEntity.badRequest().body("Programming score must be between 0 and 100.");
        }
        if (req.mathsScore() != null && !validScore(req.mathsScore())) {
            return ResponseEntity.badRequest().body("Mathematics score must be between 0 and 100.");
        }

        if (req.programmingScore() != null) {
            jdbc.update("UPDATE student SET programming_score = ? WHERE student_id = ?",
                    req.programmingScore(), id);
        }
        if (req.mathsScore() != null) {
            jdbc.update("UPDATE student SET maths_score = ? WHERE student_id = ?",
                    req.mathsScore(), id);
        }
        if (req.skills() != null) {
            jdbc.update("DELETE FROM student_skill WHERE student_id = ?", id);
            saveSkills(id, req.skills());
        }
        return ResponseEntity.ok("Student updated successfully!");
    }

    // ===================== Q3.5 DELETE =====================
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteStudent(@PathVariable int id) {
        int rows = jdbc.update("DELETE FROM student WHERE student_id = ?", id);
        if (rows == 0) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Student not found.");
        }
        return ResponseEntity.ok("Student deleted successfully!");
    }

    // ===================== Q3.6 / Q2.5 MERIT LIST =====================
    @GetMapping("/merit")
    public List<Student> meritList() {
        List<Student> students = loadStudents("");
        students.sort(Comparator.comparingDouble(Student::getAverage).reversed());
        return students;
    }

    // ===================== Q2.6 STUDENTS WITH SAME SKILLS =====================
    @GetMapping("/same-skills")
    public Map<String, List<String>> sameSkills() {
        Map<String, List<String>> bySkill = new TreeMap<>();
        for (Student s : loadStudents("")) {
            for (String skill : s.getSkills()) {
                bySkill.computeIfAbsent(skill.toLowerCase(), k -> new ArrayList<>()).add(s.getName());
            }
        }
        bySkill.entrySet().removeIf(e -> e.getValue().size() < 2);
        return bySkill;
    }

    // ===================== Q2.7 WEAK IN PROGRAMMING BUT MATHS >= 75 =====================
    @GetMapping("/needs-improvement")
    public List<Student> weakProgrammingStrongMaths() {
        return loadStudents("").stream()
                .filter(s -> s.getProgrammingScore() < 50 && s.getMathsScore() >= 75)
                .collect(Collectors.toList());
    }

    // ===================== HELPER METHODS =====================

    private List<Student> loadStudents(String where, Object... args) {
        List<Student> students = jdbc.query("SELECT * FROM student " + where, (rs, i) -> {
            Student s = new Student();
            s.setStudentId(rs.getInt("student_id"));
            s.setName(rs.getString("name"));
            s.setEmail(rs.getString("email"));
            s.setCourse(rs.getString("course"));
            s.setProgrammingScore(rs.getDouble("programming_score"));
            s.setMathsScore(rs.getDouble("maths_score"));
            return s;
        }, args);

        Map<Integer, List<String>> skillMap = new HashMap<>();
        List<Map<String, Object>> rows =
                jdbc.queryForList("SELECT student_id, skill FROM student_skill ORDER BY id");
        for (Map<String, Object> row : rows) {
            int sid = ((Number) row.get("student_id")).intValue();
            String skill = (String) row.get("skill");
            skillMap.computeIfAbsent(sid, k -> new ArrayList<>()).add(skill);
        }
        for (Student s : students) {
            s.setSkills(skillMap.getOrDefault(s.getStudentId(), new ArrayList<>()));
        }
        return students;
    }

    private void saveSkills(int studentId, List<String> skills) {
        if (skills == null) {
            return;
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String skill : skills) {
            if (skill != null && !skill.isBlank()) {
                unique.add(skill.trim());
            }
        }
        for (String skill : unique) {
            jdbc.update("INSERT INTO student_skill (student_id, skill) VALUES (?, ?)", studentId, skill);
        }
    }

    private boolean exists(int id) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM student WHERE student_id = ?", Integer.class, id);
        return count != null && count > 0;
    }

    private boolean validScore(double score) {
        return score >= 0 && score <= 100;
    }

    private String validate(Student s) {
        if (s.getStudentId() <= 0) return "Student ID must be a positive number.";
        if (s.getName() == null || s.getName().isBlank()) return "Name is required.";
        if (s.getEmail() == null || !s.getEmail().contains("@")) return "A valid email is required.";
        if (s.getCourse() == null || s.getCourse().isBlank()) return "Course is required.";
        if (!validScore(s.getProgrammingScore())) return "Programming score must be between 0 and 100.";
        if (!validScore(s.getMathsScore())) return "Mathematics score must be between 0 and 100.";
        return null;
    }
}