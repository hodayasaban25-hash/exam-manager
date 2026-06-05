package com.example.exam_manager.service;

import com.example.exam_manager.model.Answer;
import com.example.exam_manager.model.Question;
import com.example.exam_manager.model.Submission;
import com.example.exam_manager.model.Test;
import com.example.exam_manager.repository.QuestionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class AiService {

    private final QuestionRepository questionRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper   = new ObjectMapper();

    @Value("${gemini.api.key}")
    private String apiKey;

    private static final String GEMINI_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    private static final List<String> MOCK_FEEDBACKS = List.of(
        "תשובה טובה! הבנת את הרעיון המרכזי בצורה ברורה.",
        "תשובה חלקית. הכיוון נכון אך חסרים פרטים חשובים.",
        "תשובה מצוינת! הסברת את הנושא בצורה מדויקת ומקיפה.",
        "יש להשתפר. התשובה לא מדויקת מספיק לעומת מה שנדרש.",
        "תשובה סבירה. ניכר שהבנת את הנושא אך הניסוח יכול להיות ברור יותר."
    );

    public AiService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    /**
     * שולח את תוכן קובץ השאלון ל-Gemini, מקבל רשימת שאלות ב-JSON,
     * ושומר אותן בבסיס הנתונים עם ניקוד לכל שאלה.
     */
    public void parseMasterExam(Test test) {
        String fileContent = test.getQuestionPaperFile() != null
            ? test.getQuestionPaperFile()
            : test.getMasterSolutionFile();

        if (fileContent == null || fileContent.isBlank()) return;

        // חתוך ל-8000 תווים כדי לא לחרוג ממגבלת הטוקנים
        if (fileContent.length() > 8000) {
            fileContent = fileContent.substring(0, 8000);
        }

        String prompt =
            "אתה מנתח שאלוני מבחן. קרא את תוכן השאלון הבא וחלץ ממנו את כל השאלות.\n" +
            "החזר תשובה ב-JSON בלבד, ללא הסברים נוספים, בפורמט הבא:\n" +
            "[\n" +
            "  {\"content\": \"טקסט השאלה\", \"maxScore\": ניקוד_מספרי},\n" +
            "  ...\n" +
            "]\n" +
            "אם הניקוד של שאלה לא מצוין במפורש, חלק את 100 נקודות שווה בין השאלות.\n" +
            "חשוב: החזר אך ורק את מערך ה-JSON, ללא markdown (\u0060\u0060\u0060) ובלי שום טקסט לפניו או אחריו.\n\n" +
            "תוכן השאלון:\n" + fileContent;

        try {
            String geminiResponse = callGemini(prompt);
            List<Map<String, Object>> parsed = parseQuestionsJson(geminiResponse);

            if (parsed == null || parsed.isEmpty()) {
                System.err.println("[AiService] Gemini לא החזיר שאלות תקינות. תגובה: " + geminiResponse);
                saveFallbackQuestions(test);
                return;
            }

            for (Map<String, Object> q : parsed) {
                String content = (String) q.get("content");
                int maxScore   = toInt(q.get("maxScore"), 10);
                if (content == null || content.isBlank()) continue;

                Question question = new Question();
                question.setContent(content.trim());
                question.setMaxScore(Math.max(1, maxScore));
                question.setTest(test);
                questionRepository.save(question);
            }

            System.out.println("[AiService] חולצו " + parsed.size() + " שאלות מהמבחן: " + test.getTitle());

        } catch (Exception e) {
            System.err.println("[AiService] שגיאה בחילוץ שאלות: " + e.getMessage());
            e.printStackTrace();
            saveFallbackQuestions(test);
        }
    }

    /**
     * מדמה פענוח כתב יד מתמונה, בדיקת תשובה ומתן ציון ומשוב.
     * בגרסה האמיתית — כאן יישלח ה-Base64 ל-Vision API.
     */
    public void evaluateAnswer(Question question, Answer answer) {
        int maxScore = question.getMaxScore();
        int score    = (int) Math.round(maxScore * (0.6 + new Random().nextDouble() * 0.4));
        String feedback = MOCK_FEEDBACKS.get(new Random().nextInt(MOCK_FEEDBACKS.size()));
        answer.setScore(score);
        answer.setFeedback(feedback);
    }

    /**
     * מדמה בדיקת מבחן שלם.
     * בגרסה האמיתית — כאן יישלחו הקבצים ל-Vision API.
     */
    public void evaluateFullExam(Submission submission,
                                 String questionPaperFile,
                                 String masterSolutionFile) {
        int totalGrade = 70 + new Random().nextInt(31);
        submission.setTotalGrade(totalGrade);
        submission.setGeneralFeedback(buildFullExamFeedback(totalGrade));
    }

    // ── helpers ────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private String callGemini(String prompt) {
        String url = GEMINI_URL + apiKey;

        Map<String, Object> body = Map.of(
            "contents", List.of(
                Map.of("role", "user", "parts", List.of(
                    Map.of("text", prompt)
                ))
            )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
        Map<?, ?> respBody = response.getBody();

        List<?> candidates = (List<?>) respBody.get("candidates");
        Map<?, ?> candidate = (Map<?, ?>) candidates.get(0);
        Map<?, ?> content   = (Map<?, ?>) candidate.get("content");
        List<?> parts       = (List<?>) content.get("parts");
        Map<?, ?> part      = (Map<?, ?>) parts.get(0);
        return (String) part.get("text");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseQuestionsJson(String text) throws Exception {
        if (text == null) return null;

        // נקה markdown אם Gemini עטף את ה-JSON בגרשיים
        String cleaned = text.trim()
            .replaceAll("(?s)^```json\\s*", "")
            .replaceAll("(?s)^```\\s*", "")
            .replaceAll("```$", "")
            .trim();

        // מצא את גבולות מערך ה-JSON
        int start = cleaned.indexOf('[');
        int end   = cleaned.lastIndexOf(']');
        if (start < 0 || end < 0 || end <= start) return null;

        String jsonArray = cleaned.substring(start, end + 1);
        return objectMapper.readValue(jsonArray,
            objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
    }

    private void saveFallbackQuestions(Test test) {
        Question q1 = new Question();
        q1.setContent("שאלה 1: לא הצלחנו לחלץ שאלות אוטומטית — ערוך ידנית");
        q1.setMaxScore(50);
        q1.setTest(test);
        questionRepository.save(q1);

        Question q2 = new Question();
        q2.setContent("שאלה 2: לא הצלחנו לחלץ שאלות אוטומטית — ערוך ידנית");
        q2.setMaxScore(50);
        q2.setTest(test);
        questionRepository.save(q2);
    }

    private int toInt(Object val, int fallback) {
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(String.valueOf(val)); } catch (Exception e) { return fallback; }
    }

    private String buildFullExamFeedback(int grade) {
        if (grade >= 90) return "עבודה מצוינת! המבחן מוכיח שליטה מלאה בחומר הנלמד. המשך כך.";
        if (grade >= 80) return "תוצאה טובה מאוד. יש כמה נקודות לשיפור, אך הבנת הבסיס איתנה.";
        if (grade >= 70) return "עמדת בדרישות הבסיסיות. כדאי לחזור על הנושאים שגרמו לירידה בציון ולתרגל עוד.";
        return "יש מקום לשיפור משמעותי. מומלץ לעבור שוב על החומר ולפנות למורה לקבלת עזרה.";
    }
}
