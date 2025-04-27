package com.pbdvmobile.app.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;

public class ListJsonConverter {
    private static final Gson gson = new Gson();

    /**
     * Converts a list to a JSON string.
     *
     * @param list The list to convert.
     * @param <T>  The type of elements in the list.
     * @return The JSON string representation of the list.
     */
    public static <T> String listToJson(List<T> list) {
        return gson.toJson(list);
    }

    /**
     * Converts a JSON string to a list.
     *
     * @param json     The JSON string to convert.
     * @param listType The Type of list (e.g., new TypeToken<List<YourClass>>() {}.getType()).
     * @param <T>      The type of elements in the list.
     * @return The list represented by the JSON string.
     */
    public static <T> List<T> jsonToList(String json, Type listType) {
        return gson.fromJson(json, listType);
    }

    /**
     * Helper method to get the type token for a list of a specific class.
     *
     * @param <T> The type of the class contained in the list.
     * @param clazz The class of elements in the list.
     * @return The Type for the list.
     */
    public static <T> Type getListType(Class<T> clazz) {
        return TypeToken.getParameterized(List.class, clazz).getType();
    }
}
