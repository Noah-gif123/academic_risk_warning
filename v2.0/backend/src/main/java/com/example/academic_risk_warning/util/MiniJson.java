package com.example.academic_risk_warning.util;

import java.util.ArrayList;
import java.util.List;

/**
 * 极简 JSON 工具：只处理本项目需要的几种固定结构（答案 / 选项），
 * 避免引入对象映射带来的迁移成本。写：quote/array；读：arrayStrings/boolValue。
 */
public final class MiniJson {

    private MiniJson() {
    }

    public static String quote(String s) {
        if (s == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.append('"').toString();
    }

    /** 把若干字符串写成 JSON 数组 */
    public static String array(List<String> values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(quote(values.get(i)));
        }
        return sb.append(']').toString();
    }

    /** 取 {"key":[...]} 里的字符串元素 */
    public static List<String> arrayStrings(String json, String key) {
        List<String> out = new ArrayList<>();
        int k = indexOfKey(json, key);
        if (k < 0) {
            return out;
        }
        int lb = json.indexOf('[', k);
        if (lb < 0) {
            return out;
        }
        int depth = 0;
        for (int i = lb; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth == 0) {
                    break;
                }
            } else if (c == '"') {
                StringBuilder sb = new StringBuilder();
                int j = i + 1;
                while (j < json.length()) {
                    char ch = json.charAt(j);
                    if (ch == '\\') {
                        char nx = j + 1 < json.length() ? json.charAt(j + 1) : ' ';
                        if (nx == 'n') sb.append('\n');
                        else if (nx == 'r') sb.append('\r');
                        else if (nx == 't') sb.append('\t');
                        else if (nx == 'u' && j + 5 < json.length()) {
                            sb.append((char) Integer.parseInt(json.substring(j + 2, j + 6), 16));
                            j += 4;
                        } else sb.append(nx);
                        j += 2;
                        continue;
                    }
                    if (ch == '"') {
                        break;
                    }
                    sb.append(ch);
                    j++;
                }
                out.add(sb.toString());
                i = j;
            }
        }
        return out;
    }

    /** 取 {"key":true} 的布尔值 */
    public static Boolean boolValue(String json, String key) {
        int k = indexOfKey(json, key);
        if (k < 0) {
            return null;
        }
        int colon = json.indexOf(':', k);
        if (colon < 0) {
            return null;
        }
        String rest = json.substring(colon + 1).trim();
        if (rest.startsWith("true")) {
            return Boolean.TRUE;
        }
        if (rest.startsWith("false")) {
            return Boolean.FALSE;
        }
        return null;
    }

    private static int indexOfKey(String json, String key) {
        if (json == null) {
            return -1;
        }
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
