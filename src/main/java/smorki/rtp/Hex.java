/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.ChatColor
 */
package smorki.rtp;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.ChatColor;

public class Hex {
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public static String translateAllColorCodes(String message) {
        if (message == null) {
            return null;
        }
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hexColor = matcher.group(1);
            char var10002 = hexColor.charAt(0);
            matcher.appendReplacement(buffer, "\u00a7x\u00a7" + var10002 + "\u00a7" + hexColor.charAt(1) + "\u00a7" + hexColor.charAt(2) + "\u00a7" + hexColor.charAt(3) + "\u00a7" + hexColor.charAt(4) + "\u00a7" + hexColor.charAt(5));
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes((char)'&', (String)buffer.toString());
    }

    public static String stripColorCodes(String message) {
        return message == null ? null : message.replaceAll("(&|\u00a7)[0-9a-fk-or]", "").replaceAll("&#[A-Fa-f0-9]{6}", "");
    }
}

