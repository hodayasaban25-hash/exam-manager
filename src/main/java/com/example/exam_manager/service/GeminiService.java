package com.example.exam_manager.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private static final String GEMINI_BASE_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    private static final String SYSTEM_CONTEXT =
        "אתה עוזר דיגיטלי פדגוגי במערכת ניהול המבחנים. תפקידך לסייע למורים להשתמש באתר. " +
        "השתמש בידע הבא כדי לענות על שאלותיהם בצורה אדיבה, ברורה ומדויקת בעברית:\n" +
        "1. יצירת מבחן: בלשונית 'ניהול מבחנים' -> 'יצירת מבחן חדש'. ממלאים פרטים ובוחרים כיתה ומקצוע.\n" +
        "2. חומרי מקור: הזנה ידנית או העלאת קובץ שאלון (Word/PDF) לחילוץ אוטומטי של שאלות על ידי ה-AI.\n" +
        "3. מפתח תשובות: מעלים את קובץ הפתרונות המקורי בעריכת המבחן כדי שה-AI ידע לפיו לבדוק.\n" +
        "4. ניהול כיתות: בלשונית 'ניהול כיתות ותלמידים' ניתן להוסיף תלמיד או לייבא באקסל.\n" +
        "5. דוחות וסטטיסטיקות: בלשונית 'דוחות וסטטיסטיקות' רואים גרפים, ממוצעים, חציונים ותלמידים לחיזוק.\n" +
        "6. ניקוד חלקי: המערכת בודקת את שלבי הפתרון של התלמיד מול המפתח, ומעניקה ניקוד חלקי על דרך נכונה " +
        "או שגיאות נגררות/חישוביות (לא נותנת 0 אוטומטית).";

    private final RestTemplate restTemplate = new RestTemplate();

    public String chat(String userMessage) {
        String url = GEMINI_BASE_URL + apiKey;

        Map<String, Object> body = Map.of(
            "systemInstruction", Map.of(
                "parts", List.of(Map.of("text", SYSTEM_CONTEXT))
            ),
            "contents", List.of(
                Map.of("role", "user", "parts", List.of(
                    Map.of("text", userMessage)
                ))
            )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            return extractText(response.getBody());
        } catch (org.springframework.web.client.HttpClientErrorException |
                 org.springframework.web.client.HttpServerErrorException httpEx) {
            System.err.println("[Gemini] HTTP שגיאה " + httpEx.getStatusCode() + ": " + httpEx.getMessage());
            System.err.println("[Gemini] Response Body: " + httpEx.getResponseBodyAsString());
            httpEx.printStackTrace();
            return "מצטער, אירעה שגיאה בתקשורת עם ה-AI. נסה שוב.";
        } catch (Exception e) {
            System.err.println("[Gemini] שגיאה כללית: " + e.getMessage());
            e.printStackTrace();
            return "מצטער, אירעה שגיאה בתקשורת עם ה-AI. נסה שוב.";
        }
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<?, ?> body) {
        try {
            List<?> candidates = (List<?>) body.get("candidates");
            Map<?, ?> candidate = (Map<?, ?>) candidates.get(0);
            Map<?, ?> content   = (Map<?, ?>) candidate.get("content");
            List<?> parts       = (List<?>) content.get("parts");
            Map<?, ?> part      = (Map<?, ?>) parts.get(0);
            return (String) part.get("text");
        } catch (Exception e) {
            return "לא הצלחתי לפענח את תשובת ה-AI.";
        }
    }
}
