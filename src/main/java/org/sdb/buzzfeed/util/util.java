package org.sdb.buzzfeed.util;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class util {
    public static Map<String, String> objectToMap(Object obj) {
        Map<String, String> map = new HashMap<>();
        try {
            for (Field field : obj.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(obj);
                if (value != null) {
                    map.put(field.getName(), value.toString());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("对象转Map失败", e);
        }
        return map;
    }

}
