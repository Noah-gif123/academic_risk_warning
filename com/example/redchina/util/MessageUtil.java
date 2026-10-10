package com.example.redchina.util;

import jakarta.annotation.Resource;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 多语言工具类（获取当前用户语言对应的文本）
 */
@Component
public class MessageUtil {

    @Resource
    private MessageSource messageSource;

    /**
     * 获取多语言文本
     * @param code 资源文件中的key
     * @return 对应语言的文本
     */
    public String getMessage(String code) {
        return getMessage(code, null);
    }

    /**
     * 获取带参数的多语言文本
     * @param code 资源文件中的key
     * @param args 参数数组
     * @return 对应语言的文本
     */
    public String getMessage(String code, Object[] args) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(code, args, code, locale);
    }

    /**
     * 根据语言类型获取多语言文本
     * @param code 资源文件中的key
     * @param language 语言类型（zh_CN/en_US/ja_JP/ko_KR）
     * @return 对应语言的文本
     */
    public String getMessageByLanguage(String code, String language) {
        Locale locale = switch (language) {
            case "en_US" -> Locale.US;
            case "ja_JP" -> Locale.JAPAN;
            case "ko_KR" -> Locale.KOREA;
            default -> Locale.SIMPLIFIED_CHINESE;
        };
        return messageSource.getMessage(code, null, code, locale);
    }
}
