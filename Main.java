package Team;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.io.*;

// 1. Subject 클래스
class Subject implements Serializable {
    private String semester; // 학기
    private String name;     // 과목명
    private int credits;     // 이수학점
    private String grade;    // 성적
    private String type;     // 이수구분 (전공필수, 전공선택, 교양 등)
    private static final long serialVersionUID = 1L;

    public Subject(String semester, String name, int credits, String grade, String type) {
        this.semester = semester;
        this.name = name;
        this.credits = credits;
        this.grade = grade;
        this.type = type;
    }

    public double getGradePoint() {
        switch (grade) {
            case "A+": return 4.5;
            case "A": return 4.0;
            case "B+": return 3.5;
            case "B": return 3.0;
            case "C+": return 2.5;
            case "C": return 2.0;
            case "D+": return 1.5; 
            case "D": return 1.0;  
            case "F": return 0.0;
            default: return 0.0;
        }
    }

    public String getSemester() { return semester; }
    public int getCredits() { return credits; }
    public String getName() { return name; }
    public String getGrade() { return grade; }
    public String getType() { return type; }
}

// 2. GradeManager 클래스
class GradeManager {
    private List<Subject> subjects = new ArrayList<>();

    // --- 학교 졸업 요건 상수 ---
    public static final int REQ_TOTAL_CREDITS = 130;
    public static final int REQ_MAJOR_BASIC = 3;
    public static final int REQ_MAJOR_REQ = 15;
    public static final int REQ_MAJOR_ELEC = 21;
    public static final int REQ_MAJOR_TOTAL = 78;
    public static final int REQ_GEN_AREA_COUNT = 3; // 선필교 필수 이수 영역 개수 (추가됨)

    public void addSubject(Subject s) {
        subjects.add(s);
    }

    public List<Subject> getSubjects() {
        return subjects;
    }

    public double calculateGPA() {
        double totalPoints = 0;
        int totalCredits = 0;

        for (Subject s : subjects) {
            totalPoints += s.getGradePoint() * s.getCredits();
            totalCredits += s.getCredits();
        }
        
        if (totalCredits == 0) return 0;
        
        double gpa = totalPoints / totalCredits;
        // 소수점 셋째 자리에서 버림 처리
        return Math.floor(gpa * 100) / 100.0;
    }

    public int getTotalCredits() {
        return subjects.stream().mapToInt(Subject::getCredits).sum();
    }

    // 이수 구분명 정규화 (UI의 콤보박스 값과 매핑)
    public int getCreditsByCategory(String category) {
        int sum = 0;
        for(Subject s : subjects) {
            if(category.equals("전공")) {
                if(s.getType().contains("전공")) sum += s.getCredits();
            } else if(category.equals("전공기초")) {
                if(s.getType().equals("전공기초")) sum += s.getCredits();
            } else if(category.equals("전공필수")) {
                if(s.getType().equals("전공필수")) sum += s.getCredits();
            } else if(category.equals("전공선택")) {
                if(s.getType().equals("전공선택")) sum += s.getCredits();
            } else if(category.equals("교양필수")) {
                if(s.getType().contains("교양필수")) sum += s.getCredits();
            }
        }
        return sum;
    }
    
    // 이수한 교양필수 영역(예술, 인문학 등) 추출 메서드 (추가됨)
    public Set<String> getCompletedGenEdAreas() {
        Set<String> completedAreas = new HashSet<>();
        for(Subject s : subjects) {
            if(s.getType().startsWith("교양필수(")) {
                // "교양필수(예술)" -> "예술" 추출
                String area = s.getType().replace("교양필수(", "").replace(")", "");
                completedAreas.add(area);
            }
        }
        return completedAreas;
    }
}

// 데이터 저장&관리
class FileManager {
    private static final String FILE_NAME = "grade_data.dat";
    public static void save(List<Subject> subjects) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(FILE_NAME));
            out.writeObject(subjects);
            out.close();
        } 
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    @SuppressWarnings("unchecked")
    public static List<Subject> load() {
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(FILE_NAME));
            List<Subject> subjects = (List<Subject>) in.readObject();
            in.close();
            return subjects;
        } 
        catch (Exception e) {
            return new ArrayList<>();
        }
    }
}

// 3. Main (GUI) 클래스
public class Main extends JFrame {
    private GradeManager manager = new GradeManager();
    private DefaultTableModel tableModel;
    private JTextField nameField, creditField;
    private JComboBox<String> semesterCombo, gradeCombo, typeCombo;
    private JLabel resultLabel, messageLabel;

    // 상태 관리 플래그 (창이 여러 번 뜨는 것 방지)
    private boolean isExtraCreditPrompted = false;
    private boolean isUnderGpaWarned = false;

    // 🎨 테마 컬러 설정 (러블리 핑크)
    private final Color MAIN_PINK = new Color(255, 230, 240); // 아주 연한 핑크 배경
    private final Color POINT_PINK = new Color(255, 105, 180); // 진한 핑크 (버튼 등)
    private final Color TEXT_COLOR = new Color(100, 50, 70);    // 짙은 브라운 핑크 (글씨)

    public Main() {
        // 1. 기본 창 설정
        setTitle("🎀 나의 러블리 학점 계산기");
        setSize(750, 800); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(MAIN_PINK);

        // 폰트 설정
        Font labelFont = new Font("맑은 고딕", Font.BOLD, 14);
        Font fieldFont = new Font("맑은 고딕", Font.PLAIN, 14);

        // --- 상단 패널 (입력부) ---
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(MAIN_PINK);
        
        JPanel inputPanel = new JPanel(new GridLayout(6, 2, 8, 8));
        inputPanel.setBackground(MAIN_PINK);
        inputPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));

        // 학기 입력란
        JLabel semesterLabel = new JLabel("📅 학기:");
        semesterLabel.setFont(labelFont);
        semesterLabel.setForeground(TEXT_COLOR);
        inputPanel.add(semesterLabel);
        String[] semesters = {"1학년 1학기", "1학년 2학기", "2학년 1학기", "2학년 2학기", "3학년 1학기", "3학년 2학기", "4학년 1학기", "4학년 2학기"};
        semesterCombo = new JComboBox<>(semesters);
        semesterCombo.setFont(fieldFont);
        inputPanel.add(semesterCombo);

        addInputRow(inputPanel, "💘 과목명:", nameField = new JTextField(), labelFont, fieldFont);
        
        // 이수구분 콤보박스 (세부 영역 추가됨)
        JLabel typeLabel = new JLabel("📚 이수구분:");
        typeLabel.setFont(labelFont);
        typeLabel.setForeground(TEXT_COLOR);
        inputPanel.add(typeLabel);
        String[] types = {
            "전공기초", "전공필수", "전공선택", 
            "교양필수(예술)", "교양필수(인문학)", "교양필수(자연과학)", "교양필수(사회과학)", "교양필수(융합)", 
            "일반교양"
        };
        typeCombo = new JComboBox<>(types);
        typeCombo.setFont(fieldFont);
        inputPanel.add(typeCombo);

        addInputRow(inputPanel, "🔢 학점:", creditField = new JTextField(), labelFont, fieldFont);

        JLabel gradeLabel = new JLabel("⭐ 성적:");
        gradeLabel.setFont(labelFont);
        gradeLabel.setForeground(TEXT_COLOR);
        inputPanel.add(gradeLabel);
        String[] grades = {"A+", "A", "B+", "B", "C+", "C", "D+", "D", "F"};
        gradeCombo = new JComboBox<>(grades);
        gradeCombo.setFont(fieldFont);
        gradeCombo.setBackground(Color.WHITE);
        inputPanel.add(gradeCombo);

        JButton addButton = new JButton("과목 추가하기 ✨");
        styleButton(addButton, new Font("맑은 고딕", Font.BOLD, 15));
        inputPanel.add(addButton);
        
        // 부가 기능 버튼 패널
        JPanel featurePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        featurePanel.setBackground(MAIN_PINK);
        
        JButton gradCheckBtn = new JButton("🎓 졸업 요건 확인");
        JButton targetGpaBtn = new JButton("🎯 목표 학점 역산");
        JButton retakeBtn = new JButton("🔄 재수강 가성비");
        
        styleButton(gradCheckBtn, labelFont);
        styleButton(targetGpaBtn, labelFont);
        styleButton(retakeBtn, labelFont);
        
        featurePanel.add(gradCheckBtn);
        featurePanel.add(targetGpaBtn);
        featurePanel.add(retakeBtn);

        topPanel.add(inputPanel, BorderLayout.CENTER);
        topPanel.add(featurePanel, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        // --- 중앙 패널 (표) ---
        String[] columnNames = {"학기", "과목명", "이수구분", "학점", "성적"};
        tableModel = new DefaultTableModel(columnNames, 0);
        JTable table = new JTable(tableModel);
        table.setFont(fieldFont);
        table.setRowHeight(25);
        table.setGridColor(MAIN_PINK);
        table.setSelectionBackground(new Color(255, 200, 220));

        JTableHeader header = table.getTableHeader();
        header.setFont(labelFont);
        header.setBackground(new Color(255, 180, 200));
        header.setForeground(TEXT_COLOR);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);

        // --- 하단 패널 (결과) ---
        JPanel resultPanel = new JPanel(new GridLayout(2, 1));
        resultPanel.setBackground(MAIN_PINK);
        resultPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        resultLabel = new JLabel("평균 평점: ✨ 0.00 / 총 학점: 0", SwingConstants.CENTER);
        resultLabel.setFont(new Font("맑은 고딕", Font.BOLD, 22));
        resultLabel.setForeground(TEXT_COLOR);

        messageLabel = new JLabel("<html>첫 과목을 입력해 보세요! 🍀</html>", SwingConstants.CENTER);
        messageLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 15));
        messageLabel.setForeground(TEXT_COLOR);

        resultPanel.add(resultLabel);
        resultPanel.add(messageLabel);
        add(resultPanel, BorderLayout.SOUTH);

        // --- 이벤트 리스너 ---
        addButton.addActionListener(e -> addSubjectAction());
        gradCheckBtn.addActionListener(e -> showGraduationStatus());
        targetGpaBtn.addActionListener(e -> showTargetGpaCalculator());
        retakeBtn.addActionListener(e -> showRetakeAnalysis());
        loadPreviousData();
    }

    private void styleButton(JButton btn, Font font) {
        btn.setFont(font);
        btn.setBackground(POINT_PINK);
        btn.setForeground(Color.WHITE);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
    }

    private void addInputRow(JPanel panel, String labelText, JTextField field, Font labelFont, Font fieldFont) {
        JLabel label = new JLabel(labelText);
        label.setFont(labelFont);
        label.setForeground(TEXT_COLOR);
        panel.add(label);
        field.setFont(fieldFont);
        field.setBorder(BorderFactory.createLineBorder(POINT_PINK, 1));
        panel.add(field);
    }

    private void addSubjectAction() {
        try {
            String semester = (String) semesterCombo.getSelectedItem();
            String name = nameField.getText().trim();
            String creditStr = creditField.getText().trim();
            String type = (String) typeCombo.getSelectedItem();
            String grade = (String) gradeCombo.getSelectedItem();

            if (name.isEmpty() || creditStr.isEmpty()) {
                throw new Exception("빈칸을 채워주세요!");
            }
            
            // 과목 중복 방지 검사
            for (Subject s : manager.getSubjects()) {
                if (s.getName().equalsIgnoreCase(name)) {
                    throw new Exception("이미 입력된 과목입니다! (중복 입력 불가)");
                }
            }

            int credits = Integer.parseInt(creditStr);
            manager.addSubject(new Subject(semester, name, credits, grade, type));
            tableModel.addRow(new Object[]{semester, name, type, credits, grade});
            FileManager.save(manager.getSubjects());

            updateResultLabel();
            
            // 4.0 이상 시 추가 학점 안내 창
            
            double gpa = manager.calculateGPA();

            if (gpa >= 4.0 && !isExtraCreditPrompted) {
                JOptionPane.showMessageDialog(this, 
                    "🎉 평점 4.0 이상을 달성하셨습니다!\n다음 학기에는 기준 학점 외에 '추가 학점(ex: +3학점)' 이수가 가능합니다.", 
                    "추가 학점 이수 가능 안내", 
                    JOptionPane.INFORMATION_MESSAGE);
                isExtraCreditPrompted = true; 
            } else if (gpa < 4.0) {
                isExtraCreditPrompted = false; 
            }

            nameField.setText("");
            creditField.setText("");
            nameField.requestFocus();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "학점은 숫자로 입력해주세요!", "입력 오류", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "알림", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void updateResultLabel() {
        double gpa = manager.calculateGPA();
        int totalCredits = manager.getTotalCredits();
        resultLabel.setText(String.format("평균 평점: ✨ %.2f / 총 학점: %d", gpa, totalCredits));

        if (gpa >= 4.0) {
            messageLabel.setText("<html>🎉 와우! 장학금 대상자일지도 몰라요! 🥳</html>");
            messageLabel.setForeground(new Color(0, 120, 0));
        } else if (gpa < 2.0) {
            messageLabel.setText("<html>💖 할 수 있어요! 조금만 더 힘내봐요! 💪</html>");
            messageLabel.setForeground(Color.RED);
        } else {
            messageLabel.setText("<html>안정적인 성적이에요. 고생 많으셨어요! 👍</html>");
            messageLabel.setForeground(TEXT_COLOR);
        }

        // 졸업 최소 이수 학점(2.0) 미만 경고 창
        if (totalCredits > 0 && gpa < 2.0 && !isUnderGpaWarned) {
            JOptionPane.showMessageDialog(this, 
                "⚠️ 현재 평균 평점이 졸업 최소 기준(2.0) 미만으로 내려갔습니다!\n성적 관리에 주의가 필요합니다.", 
                "졸업 학점 경고", 
                JOptionPane.WARNING_MESSAGE);
            isUnderGpaWarned = true; 
        } else if (gpa >= 2.0) {
            isUnderGpaWarned = false;
        }
    }
    
    private void loadPreviousData() {

        int answer =
                JOptionPane.showConfirmDialog(
                        this,
                        "이전 데이터를 불러오시겠습니까?",
                        "데이터 복구",
                        JOptionPane.YES_NO_OPTION
                );

        if (answer != JOptionPane.YES_OPTION) {
            return;
        }

        List<Subject> loaded =
                FileManager.load();

        for (Subject s : loaded) {

            manager.addSubject(s);

            tableModel.addRow(new Object[]{
                    s.getSemester(),
                    s.getName(),
                    s.getType(),
                    s.getCredits(),
                    s.getGrade()
            });
        }

        updateResultLabel();
    }

    // --- 기능 1: 졸업 이수 학점 확인 ---
    private void showGraduationStatus() {
        int total = manager.getTotalCredits();
        int majorBasic = manager.getCreditsByCategory("전공기초");
        int majorReq = manager.getCreditsByCategory("전공필수");
        int majorElec = manager.getCreditsByCategory("전공선택");
        int majorTotal = manager.getCreditsByCategory("전공"); 
        
        // 선필교 영역 추출 로직 (추가됨)
        Set<String> completedAreas = manager.getCompletedGenEdAreas();
        int completedAreaCount = completedAreas.size();
        String areaNames = completedAreas.isEmpty() ? "없음" : String.join(", ", completedAreas);
        
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='width: 320px; font-family: 맑은 고딕;'>");
        sb.append("<h2 style='color: #FF69B4;'>🎓 졸업 요건 이수 현황</h2>");
        
        sb.append(String.format("<b>총 학점:</b> %d / %d %s<br>", total, GradeManager.REQ_TOTAL_CREDITS, (total >= GradeManager.REQ_TOTAL_CREDITS) ? "✅" : "❌"));
        sb.append(String.format("<b>전공 전체:</b> %d / %d %s<br><hr>", majorTotal, GradeManager.REQ_MAJOR_TOTAL, (majorTotal >= GradeManager.REQ_MAJOR_TOTAL) ? "✅" : "❌"));
        
        sb.append(String.format("- 전공 기초: %d / %d %s<br>", majorBasic, GradeManager.REQ_MAJOR_BASIC, (majorBasic >= GradeManager.REQ_MAJOR_BASIC) ? "✅" : "❌"));
        sb.append(String.format("- 전공 필수: %d / %d %s<br>", majorReq, GradeManager.REQ_MAJOR_REQ, (majorReq >= GradeManager.REQ_MAJOR_REQ) ? "✅" : "❌"));
        sb.append(String.format("- 전공 선택: %d / %d %s<br><hr>", majorElec, GradeManager.REQ_MAJOR_ELEC, (majorElec >= GradeManager.REQ_MAJOR_ELEC) ? "✅" : "❌"));
        
        // 선택 필수 교양 조건 출력 (추가됨)
        sb.append(String.format("<b>선필교 영역 이수:</b> %d / %d 영역 %s<br>", completedAreaCount, GradeManager.REQ_GEN_AREA_COUNT, (completedAreaCount >= GradeManager.REQ_GEN_AREA_COUNT) ? "✅" : "❌"));
        sb.append(String.format("<span style='font-size:11px; color:gray;'>- 이수한 영역: %s</span><br><br>", areaNames));

        sb.append("</body></html>");

        JOptionPane.showMessageDialog(this, sb.toString(), "졸업 이수 학점 현황", JOptionPane.INFORMATION_MESSAGE);
    }

    // --- 기능 2: 목표 학점 역산기 ---
    private void showTargetGpaCalculator() {
        if (manager.getSubjects().isEmpty()) {
            JOptionPane.showMessageDialog(this, "입력된 과목이 없습니다.", "알림", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String targetStr = JOptionPane.showInputDialog(this, "목표하는 졸업 평점을 입력하세요 (예: 4.0):");
        if (targetStr == null || targetStr.trim().isEmpty()) return;

        try {
            double targetGpa = Double.parseDouble(targetStr);
            int currentCredits = manager.getTotalCredits();
            double currentGpa = manager.calculateGPA();
            int remainingCredits = GradeManager.REQ_TOTAL_CREDITS - currentCredits;

            if (remainingCredits <= 0) {
                JOptionPane.showMessageDialog(this, "이미 졸업 학점을 채웠습니다!", "알림", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            double requiredPoints = (targetGpa * GradeManager.REQ_TOTAL_CREDITS) - (currentGpa * currentCredits);
            double requiredGpa = requiredPoints / remainingCredits;

            String msg;
            if (requiredGpa > 4.5) {
                msg = String.format("남은 %d학점 동안 올 A+를 받아도 목표 학점(%.2f)에 도달할 수 없습니다. 😭\n(필요 평점: %.2f)", remainingCredits, targetGpa, requiredGpa);
            } else if (requiredGpa <= 0) {
                msg = String.format("남은 %d학점을 전부 F 받아도 목표 학점을 넘습니다! 🎉", remainingCredits);
            } else {
                msg = String.format("목표 학점 %.2f를 달성하려면,\n남은 %d학점 동안 평균 [ %.2f ] 이상을 받아야 합니다! 💪", targetGpa, remainingCredits, requiredGpa);
            }

            JOptionPane.showMessageDialog(this, msg, "목표 학점 역산 결과", JOptionPane.INFORMATION_MESSAGE);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "숫자만 입력해주세요.", "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- 기능 3: 재수강 가성비 분석 ---
    private void showRetakeAnalysis() {
        List<Subject> subjects = manager.getSubjects();
        if (subjects.isEmpty()) {
            JOptionPane.showMessageDialog(this, "분석할 과목이 없습니다.", "알림", JOptionPane.WARNING_MESSAGE);
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='width: 350px; font-family: 맑은 고딕;'>");
        sb.append("<h2 style='color: #FF69B4;'>🔄 재수강 효율 분석 (A+ 달성 시)</h2>");
        sb.append("<table border='1' style='border-collapse: collapse; width: 100%; text-align: center;'>");
        sb.append("<tr style='background-color: #ffe6f0;'><th>과목</th><th>현재</th><th>총 평점 상승량</th></tr>");

        double currentTotalPoints = 0;
        int totalCredits = manager.getTotalCredits();
        for (Subject s : subjects) {
            currentTotalPoints += s.getGradePoint() * s.getCredits();
        }

        for (Subject s : subjects) {
            if (s.getGrade().equals("A+")) continue; 
            
            double pointDiff = (4.5 - s.getGradePoint()) * s.getCredits();
            double newTotalPoints = currentTotalPoints + pointDiff;
            double newGpa = newTotalPoints / totalCredits;
            double gpaIncrease = newGpa - manager.calculateGPA();

            sb.append(String.format("<tr><td>%s</td><td>%s</td><td style='color:red;'><b>+%.3f</b></td></tr>", 
                    s.getName(), s.getGrade(), gpaIncrease));
        }

        sb.append("</table></body></html>");
        JOptionPane.showMessageDialog(this, sb.toString(), "재수강 가성비 분석", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {}

        SwingUtilities.invokeLater(() -> {
            new Main().setVisible(true);
        });
    }
}