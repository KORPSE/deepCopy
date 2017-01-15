package io.ics.deepcopy;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
import java.util.Map;

import sun.misc.Unsafe;

public class CopyUtils {

    public static <T> T copy(T src) {
        return doCopy(src, new IdentityHashMap<>());
    }

    private static <T> T doCopy(T src, Map<Object, Object> visited) {
        if (src == null) {
            return null;
        }
        if (src.getClass().isAssignableFrom(String.class)
                || src.getClass().isEnum()
                || src.getClass() == Class.class) {
            return src;
        }
        if (visited.containsKey(src)) {
            //noinspection unchecked
            return (T) visited.get(src);
        }
        if (src.getClass().isArray()) {
            return copyArray(src, visited);
        }
        return copyObject(src, visited);
    }

    private static Unsafe getUnsafe() {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return (Unsafe) f.get(null);
        } catch (ReflectiveOperationException ex) {
            throw new CopyException(ex);
        }
    }

    private static void makeUnfinal(Field field)
            throws NoSuchFieldException, IllegalAccessException {
        Field modifiers = field.getClass().getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
    }

    private static <T> T copyArray(T src, Map<Object, Object> visited) {
        int length = Array.getLength(src);
        Object result =
                Array.newInstance(
                        src.getClass().getComponentType(),
                        Array.getLength(src));
        visited.put(src, result);
        for (int i = 0; i < length; i++) {
            Array.set(result, i, doCopy(Array.get(src, i), visited));
        }
        //noinspection unchecked
        return (T) result;
    }

    private static boolean hasModifier(Field field, int modifier) {
        return (field.getModifiers() & modifier) != 0;
    }

    private static <T> T copyObject(T src, Map<Object, Object> visited) {
        try {
            Object result = getUnsafe().allocateInstance(src.getClass());
            visited.put(src, result);
            Class<?> clazz = src.getClass();
            do {
                Field[] fields = clazz.getDeclaredFields();
                for (Field field : fields) {
                    // if the field is static we won't do anything with it
                    if (hasModifier(field, Modifier.STATIC)) {
                        continue;
                    }
                    field.setAccessible(true);
                    // if the field is final we should do some extra actions to make it writable
                    if (hasModifier(field, Modifier.FINAL)) {
                        makeUnfinal(field);
                    }
                    // if it's primitive, we just set it's value to our new instance
                    // otherwise we recursively copy it's value
                    if (field.getType().isPrimitive()) {
                        field.set(result, field.get(src));
                    } else {
                        field.set(result, doCopy(field.get(src), visited));
                    }
                }
                clazz = clazz.getSuperclass();
            } while (clazz != null);
            //noinspection unchecked
            return (T) result;
        } catch (Exception ex) {
            throw new CopyException(ex);
        }
    }
}
