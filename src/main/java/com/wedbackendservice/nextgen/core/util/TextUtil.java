package com.wedbackendservice.nextgen.core.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class TextUtil {

  private static final Pattern WHITESPACE = Pattern.compile("[\\s/]+");
  private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
  private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
  private static final Pattern MULTIDASH = Pattern.compile("-{2,}");

  private TextUtil() {}

  /** Xóa dấu tiếng Việt, đưa về ký tự ASCII không dấu. */
  public static String stripAccents(String input) {
    if (input == null) {
      return "";
    }
    String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
    return DIACRITICS.matcher(normalized).replaceAll("").replace("đ", "d").replace("Đ", "d");
  }

  /** Chuyển đổi chuỗi tiếng Việt có dấu thành URL slug thân thiện SEO. */
  public static String toSlug(String input) {
    if (input == null || input.isBlank()) {
      return "";
    }
    String nowhitespace = WHITESPACE.matcher(input.trim()).replaceAll("-");
    String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
    String slug = DIACRITICS.matcher(normalized).replaceAll("");
    slug = slug.replace("đ", "d").replace("Đ", "d");
    slug = NONLATIN.matcher(slug).replaceAll("");
    slug = MULTIDASH.matcher(slug).replaceAll("-");
    return slug.toLowerCase(Locale.ENGLISH).replaceAll("^-|-$", "");
  }

  /** Che số điện thoại bảo vệ thông tin cá nhân (PII), ví dụ: 098*****21 */
  public static String maskPhone(String phone) {
    if (phone == null || phone.length() < 7) {
      return "***";
    }
    int len = phone.length();
    return phone.substring(0, 3) + "*****" + phone.substring(len - 2);
  }
}
