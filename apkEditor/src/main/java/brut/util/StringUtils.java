package brut.util;

public class StringUtils {

    public static String replace(String src, String pattern, String replace) {
        return src.replaceAll(pattern, replace);
    }

}
