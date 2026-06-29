package etiya.omniAutomation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import etiya.omniAutomation.business.dto.ProcessFlowDto;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class UtilityService {

    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz";
    private static final Random RANDOM = new Random();
    @Getter
    public final DatabaseHelper databaseHelper;

    public String generateRandomTcKimlikNo() {
        Random random = new Random();
        int[] digits = new int[11];

        // İlk haneyi 1-8 arasında rastgele oluştur (9 ile başlamayacak)
        digits[0] = random.nextInt(8) + 1; // 1-8 arasında
        // Diğer 8 haneyi rastgele oluştur
        for (int i = 1; i < 9; i++) {
            digits[i] = random.nextInt(10); // 0-9 arasında
        }

        // 10. haneyi hesapla
        int tenthDigit = (
                7 * (digits[0] + digits[2] + digits[4] + digits[6] + digits[8])
                        - (digits[1] + digits[3] + digits[5] + digits[7])
        ) % 10;
        if (tenthDigit < 0) tenthDigit += 10;
        digits[9] = tenthDigit;

        // 11. haneyi hesapla
        int eleventhDigit = (
                digits[0] + digits[1] + digits[2] + digits[3] + digits[4]
                        + digits[5] + digits[6] + digits[7] + digits[8] + digits[9]
        ) % 10;
        if (eleventhDigit < 0) eleventhDigit += 10;
        digits[10] = eleventhDigit;

        // Sonucu stringe çevir
        StringBuilder sb = new StringBuilder();
        for (int digit : digits) {
            sb.append(digit);
        }

        return sb.toString();
    }
    public String generateRandomYabanciKimlikNo() {
        Random random = new Random();
        int[] digits = new int[11];

        // İlk iki haneyi 99 olarak sabit belirle
        digits[0] = 9;
        digits[1] = 9;

        // Sonraki 7 haneyi rastgele oluştur
        for (int i = 2; i < 9; i++) {
            digits[i] = random.nextInt(10); // 0-9 arasında
        }

        // 10. haneyi hesapla
        int tenthDigit = (
                7 * (digits[0] + digits[2] + digits[4] + digits[6] + digits[8])
                        - (digits[1] + digits[3] + digits[5] + digits[7])
        ) % 10;
        if (tenthDigit < 0) tenthDigit += 10;
        digits[9] = tenthDigit;

        // 11. haneyi hesapla
        int eleventhDigit = (
                digits[0] + digits[1] + digits[2] + digits[3] + digits[4]
                        + digits[5] + digits[6] + digits[7] + digits[8] + digits[9]
        ) % 10;
        if (eleventhDigit < 0) eleventhDigit += 10;
        digits[10] = eleventhDigit;

        // Sonucu stringe çevir
        StringBuilder sb = new StringBuilder();
        for (int digit : digits) {
            sb.append(digit);
        }

        return sb.toString();
    }

    public static String generatePhoneNumber() {
        String digits = "123456789";  // Kullanılacak rakamlar
        StringBuilder phoneNumber = new StringBuilder("513");  // Başında 5 olacak

        Random random = new Random();

        // 9 basamaklı rastgele sayı üret
        for (int i = 0; i < 7; i++) {
            int index = random.nextInt(digits.length());  // 0-8 arası rastgele indeks
            phoneNumber.append(digits.charAt(index));     // Rakamı ekle
        }

        return phoneNumber.toString();  // Telefon numarasını döndür
    }

    public String generateTransactionId() {
        return UUID.randomUUID().toString();
    }

    public String generateEmail() {
        return randomWord(9, 13) + "@tempmail.com";
    }

    public String generateName() {
        return randomWord(5, 8);
    }

    private String randomWord(int minLen, int maxLen) {
        int len = RANDOM.nextInt(maxLen - minLen + 1) + minLen;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            char c = ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length()));
            sb.append(i == 0 ? Character.toUpperCase(c) : c);
        }
        return sb.toString();
    }
    public static long random10Digit() {
        return ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L);
    }

    public static String currentEpochMillisAsString() {
        return String.valueOf(System.currentTimeMillis());
    }

    public static String thirtyDaysLaterEpochMillisAsString() {
        long now = System.currentTimeMillis();
        long thirtyDaysLater = now + (30L * 24 * 60 * 60 * 1000); // 30 gün = 2.592.000.000 ms
        return String.valueOf(thirtyDaysLater);
    }

    public String removeRequest(String request, String deletePath) {
        try {
            DocumentContext jsonContext = JsonPath.parse(request);
            jsonContext.delete(deletePath);
            return jsonContext.jsonString();
        } catch (Exception e) {
            return request;
        }
    }

    public String addRequest(String request, String value) {
        try {
            DocumentContext jsonContext = JsonPath.parse(request);

            String[] parts = value.split("&", 2);

            String arrayPath = parts[0];
            String dataToAdd = parts[1];

            Object jsonData = JsonPath.parse(dataToAdd).json();

            jsonContext.add(arrayPath, jsonData);

            return jsonContext.jsonString();

        } catch (Exception e) {
            return request;
        }
    }

    public String updateRequest(String request, String value) {
        try {
            DocumentContext jsonContext = JsonPath.parse(request);

            String[] parts = value.split("&", 2);

            String jsonPath = parts[0];
            String newValue = parts[1];
            if (jsonPath.isEmpty()) return newValue;

            Object parsedValue;
            parsedValue = JsonPath.parse(newValue).json();

            jsonContext.set(jsonPath, parsedValue);

            return jsonContext.jsonString();

        } catch (Exception e) {
            return request;
        }
    }

    public String updateRequestWithParameter(String request, String value, ProcessFlowDto processFlow) {
        try {
            DocumentContext jsonContext = JsonPath.parse(request);

            String[] parts = value.split("&", 2);

            String jsonPath = parts[0];
            String parameterName = parts[1];

            String parameterValue = processFlow.getParameterContext().get(parameterName);
            Object parsedValue;
            try {
                parsedValue = JsonPath.parse(parameterValue).json();
            } catch (Exception e) {
                parsedValue = parameterValue;
            }
            jsonContext.set(jsonPath, parsedValue);

            return jsonContext.jsonString();
        } catch (Exception e) {
            return request;
        }
    }

    public String replaceParametersInJson(String jsonString, ProcessFlowDto processFlow) {
        try {
            String result = jsonString;

            for (Map.Entry<String, String> entry : processFlow.getParameterContext().entrySet()) {
                String parameterName = entry.getKey();
                String parameterValue = entry.getValue();

                String placeholder = "${" + parameterName + "}";
                if (result.contains(placeholder)) {
                    if (parameterValue == null) {
                        result = result.replace(placeholder, "null");
                    } else if (isJsonValue(parameterValue)) {
                        result = result.replace(placeholder, parameterValue);
                    } else {
                        result = result.replace(placeholder, "\"" + parameterValue + "\"");
                    }
                }
            }


            return result;
        } catch (Exception e) {
            return jsonString;
        }
    }

    private boolean isJsonValue(String value) {
        if (value == null) return false;

        String trimmed = value.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                (trimmed.startsWith("[") && trimmed.endsWith("]")) ||
                trimmed.equals("true") || trimmed.equals("false") ||
                trimmed.equals("null") ||
                isNumeric(trimmed);
    }

    private boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public String addJsonWithParameters(String request, String value, ProcessFlowDto processFlow) {
        try {
            String[] parts = value.split("&", 2);

            if (parts.length < 2) {
                return request;
            }

            String targetPath = parts[0].trim();
            String jsonToAdd = parts[1];

            if (targetPath.isEmpty()) {
                return replaceParametersInJson(jsonToAdd, processFlow);
            }
            DocumentContext jsonContext = JsonPath.parse(request);

            String processedJson = replaceParametersInJson(jsonToAdd, processFlow);
            Object jsonData = JsonPath.parse(processedJson).json();

            handleArrayOrNestedPath(jsonContext, targetPath, jsonData);

            return jsonContext.jsonString();

        } catch (Exception e) {
            return request;
        }
    }

    private void handleArrayOrNestedPath(DocumentContext jsonContext, String targetPath, Object jsonData) {
        try {
            if (targetPath.contains(".")) {
                handleNestedPath(jsonContext, targetPath, jsonData);
            } else {
                handleArrayElement(jsonContext, targetPath, jsonData);
            }
        } catch (Exception e) {
            jsonContext.set(targetPath, jsonData);
        }
    }

    private void handleNestedPath(DocumentContext jsonContext, String targetPath, Object jsonData) {
        try {
            int lastDotIndex = targetPath.lastIndexOf(".");
            String parentPath = targetPath.substring(0, lastDotIndex);
            String fieldName = targetPath.substring(lastDotIndex + 1);

            Object parentObject = jsonContext.read(parentPath);

            if (parentObject instanceof java.util.Map) {
                java.util.Map<String, Object> parentMap = (java.util.Map<String, Object>) parentObject;

                java.util.Map<String, Object> updatedParent = new java.util.HashMap<>(parentMap);

                updatedParent.put(fieldName, jsonData);

                jsonContext.set(parentPath, updatedParent);
            } else {
                jsonContext.set(targetPath, jsonData);
            }
        } catch (Exception e) {
            jsonContext.set(targetPath, jsonData);
        }
    }

    private void handleArrayElement(DocumentContext jsonContext, String targetPath, Object jsonData) {
        try {
            Object existingObject = jsonContext.read(targetPath);

            if (existingObject instanceof java.util.Map && jsonData instanceof java.util.Map) {
                java.util.Map<String, Object> existingMap = (java.util.Map<String, Object>) existingObject;
                java.util.Map<String, Object> newDataMap = (java.util.Map<String, Object>) jsonData;

                java.util.Map<String, Object> mergedMap = new java.util.HashMap<>(existingMap);

                mergedMap.putAll(newDataMap);

                jsonContext.set(targetPath, mergedMap);
            } else {
                jsonContext.set(targetPath, jsonData);
            }
        } catch (Exception e) {
            jsonContext.set(targetPath, jsonData);
        }
    }

    public String removeDuplicateNamespaces(String xml) {
        if (xml == null || xml.isEmpty()) {
            return xml;
        }
        
        String result = xml;
        
        // Keep replacing until no more duplicates found
        // This handles any duplicate namespace declarations within the same tag
        boolean changed = true;
        int maxIterations = 100; // Safety limit
        int iteration = 0;
        
        while (changed && iteration < maxIterations) {
            String before = result;
            // Remove any duplicate namespace (xmlns or xmlns:prefix) within same tag
            // Pattern: (xmlns:?[^=]*="[^"]+") ... same declaration again
            result = result.replaceAll(
                "(xmlns(?::[a-zA-Z0-9]+)?=\"[^\"]+\")([^>]*?)\\1",
                "$1$2"
            );
            changed = !result.equals(before);
            iteration++;
        }
        
        return result;
    }
}
