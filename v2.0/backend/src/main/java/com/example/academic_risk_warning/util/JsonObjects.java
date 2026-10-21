package com.example.academic_risk_warning.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 只解析「扁平对象数组」：如 [{"studentId":1,"ratio":0.3}]。
 * 项目/组队/贡献度的 JSON 都是这个形态，没必要引入完整解析器。
 */
public final class JsonObjects {

    private JsonObjects() {
    }

    public static List<Map<String, String>> flatObjects(String json, String arrayKey) {
        if (json == null) {
            return new ArrayList<>();
        }
        int k = indexOfKey(json, arrayKey);
        if (k < 0) {
            return new ArrayList<>();
        }
        int lb = json.indexOf('[', k);
        if (lb < 0) {
            return new ArrayList<>();
        }
        return parseFrom(json, lb);
    }

    /** 解析裸数组：[{"studentId":1,"ratio":0.3}] */
    public static List<Map<String, String>> arrayObjects(String json) {
        if (json == null) {
            return new ArrayList<>();
        }
        int lb = json.indexOf('[');
        if (lb < 0) {
            return new ArrayList<>();
        }
        return parseFrom(json, lb);
    }

    private static List<Map<String, String>> parseFrom(String json, int lb) {
        List<Map<String, String>> out = new ArrayList<>();
        int i = lb + 1;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '{') {
                Map<String, String> obj = new LinkedHashMap<>();
                int j = i + 1;
                while (j < json.length() && json.charAt(j) != '}') {
                    if (json.charAt(j) == '"') {
                        StringBuilder key = new StringBuilder();
                        j = readString(json, j, key);
                        int colon = json.indexOf(':', j);
                        if (colon < 0) {
                            break;
                        }
                        j = colon + 1;
                        while (j < json.length() && Character.isWhitespace(json.charAt(j))) {
                            j++;
                        }
                        String value;
                        if (j < json.length() && json.charAt(j) == '"') {
                            StringBuilder val = new StringBuilder();
                            j = readString(json, j, val);
                            value = val.toString();
                        } else {
                            int start = j;
                            while (j < json.length() && json.charAt(j) != ',' && json.charAt(j) != '}') {
                                j++;
                            }
                            value = json.substring(start, j).trim();
                        }
                        obj.put(key.toString(), value);
                    } else {
                        j++;
                    }
                }
                out.add(obj);
                i = j + 1;
            } else if (c == ']') {
                break;
            } else {
                i++;
            }
        }
        return out;
    }

    /** 读取一个带转义的字符串，返回结束后的位置 */
    private static int readString(String json, int quoteIndex, StringBuilder sb) {
        int j = quoteIndex + 1;
        while (j < json.length()) {
            char ch = json.charAt(j);
            if (ch == '\\') {
                char nx = j + 1 < json.length() ? json.charAt(j + 1) : ' ';
                if (nx == 'n') {
                    sb.append('\n');
                } else if (nx == 'r') {
                    sb.append('\r');
                } else if (nx == 't') {
                    sb.append('\t');
                } else if (nx == 'u' && j + 5 < json.length()) {
                    sb.append((char) Integer.parseInt(json.substring(j + 2, j + 6), 16));
                    j += 4;
                } else {
                    sb.append(nx);
                }
                j += 2;
                continue;
            }
            if (ch == '"') {
                return j + 1;
            }
            sb.append(ch);
            j++;
        }
        return j;
    }

    private static int indexOfKey(String json, String key) {
        String needle = '"' + key + '"';
        int i = json.indexOf(needle);
        while (i >= 0) {
            int j = i + needle.length();
            while (j < json.length() && Character.isWhitespace(json.charAt(j))) {
                j++;
            }
            if (j < json.length() && json.charAt(j) == ':') {
                return j;
            }
            i = json.indexOf(needle, i + 1);
        }
        return -1;
    }
}
