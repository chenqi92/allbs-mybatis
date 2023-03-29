package cn.allbs.mybatis.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 类 TableConsoleUtil
 * </p>
 *
 * @author ChenQi
 * @since 2023/3/24 13:45
 */
public class TableConsoleUtil {

    public static String printResult(List<String> rows) {
        StringBuilder sb = new StringBuilder();
        String[] tempA = rows.get(0).split(",");
        int maxLen = tempA.length;
        for (int i = 1; i < rows.size(); i++) {
            tempA = rows.get(i).split(",");
            if (maxLen < tempA.length) maxLen = tempA.length;
        }
        String[][] row = new String[rows.size()][maxLen];
        for (int i = 0; i < row.length; i++)
            for (int j = 0; j < row[0].length; j++)
                row[i][j] = "";
        for (int i = 0; i < rows.size(); i++) {
            tempA = rows.get(i).split(",");
            System.arraycopy(tempA, 0, row[i], 0, tempA.length);
        }
        int[] maxJ = new int[maxLen];
        for (int j = 0; j < maxLen; j++) {
            for (int i = 0; i < rows.size(); i++) {
                int vLen = (getWordCount(row[i][j]) - 1 >> 3) * 8 + 8;
                if (vLen > maxJ[j]) {
                    maxJ[j] = vLen;
                }
            }
        }
        StringBuilder opera = new StringBuilder("+");
        for (int value : maxJ) {
            for (int k = 0; k < value; k++) {
                opera.append('-');

            }
            opera.append('+');
        }
        boolean first = true;
        for (String[] strings : row) {
            if (first) {
                sb.append(opera);
                sb.append("\n");
            }
            sb.append("|");
            for (int j = 0; j < row[0].length; j++) {
                int len = maxJ[j] - getWordCount(strings[j]);
                String format;
                if (len == 0) {
                    format = "%s";
                } else {
                    format = "%" + len + "s";
                }
                sb.append(strings[j]);
                sb.append(String.format(format, ""));
                sb.append("|");
            }
            sb.append("\n");
            if (first) {
                sb.append(opera);
                sb.append("\n");
                first = false;
            }
        }
        sb.append(opera);
        sb.append("\n");
        return sb.toString();
    }

    public static int getWordCount(String s) {
        int length = 0;
        int chineseNum = 0;
        for (int i = 0; i < s.length(); i++) {
            int ascii = Character.codePointAt(s, i);
            if (ascii >= 0 && ascii <= 255) length++;
            else chineseNum++;

        }
        return BigDecimal.valueOf(length + chineseNum * 1.5).setScale(0, RoundingMode.HALF_UP).intValue();
    }
}
