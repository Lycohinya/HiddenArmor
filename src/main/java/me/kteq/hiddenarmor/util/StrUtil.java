package me.kteq.hiddenarmor.util;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

public abstract class StrUtil {
    public static String color(String s){
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static List<String> color(List<String> stringList){
        List<String> coloredStringList = new ArrayList<>();
        for(String s : stringList){
            coloredStringList.add(color(s));
        }
        return coloredStringList;
    }

    /// Space-delimited title case: "IRON HELMET" -> "Iron Helmet".
    /// Replaces commons-lang2 `WordUtils.capitalizeFully`, which paper-api does not provide.
    public static String capitalizeFully(String s){
        StringBuilder builder = new StringBuilder(s.length());
        boolean wordStart = true;
        for(int i = 0; i < s.length(); i++){
            char c = s.charAt(i);
            if(c == ' '){
                wordStart = true;
                builder.append(c);
            } else {
                builder.append(wordStart ? Character.toUpperCase(c) : Character.toLowerCase(c));
                wordStart = false;
            }
        }
        return builder.toString();
    }

}
