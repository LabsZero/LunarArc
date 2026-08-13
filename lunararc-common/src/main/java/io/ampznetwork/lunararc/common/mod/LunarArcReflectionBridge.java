package io.ampznetwork.lunararc.common.mod;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Runtime reflection compatibility for Spigot-mapped NMS plugins on a Mojang runtime. */
public final class LunarArcReflectionBridge {
    private static final LunarArcRemapper REMAPPER = new LunarArcRemapper(true);

    private LunarArcReflectionBridge() {
    }

    public static Field getField(Class<?> owner, String name) throws NoSuchFieldException {
        String mapped = REMAPPER.mapRuntimeFieldName(owner, name);
        try {
            return owner.getField(mapped);
        } catch (NoSuchFieldException first) {
            Field field = findDeclaredField(owner, mapped);
            if (field != null) return field;
            if (!mapped.equals(name)) {
                try {
                    return owner.getField(name);
                } catch (NoSuchFieldException ignored) {
                    field = findDeclaredField(owner, name);
                    if (field != null) return field;
                }
            }
            throw first;
        }
    }

    public static Field getDeclaredField(Class<?> owner, String name) throws NoSuchFieldException {
        String mapped = REMAPPER.mapRuntimeFieldName(owner, name);
        try {
            return owner.getDeclaredField(mapped);
        } catch (NoSuchFieldException first) {
            if (!mapped.equals(name)) {
                try {
                    return owner.getDeclaredField(name);
                } catch (NoSuchFieldException ignored) {
                }
            }
            throw first;
        }
    }

    public static Method getMethod(Class<?> owner, String name, Class<?>[] parameterTypes) throws NoSuchMethodException {
        String mapped = REMAPPER.mapRuntimeMethodName(owner, name);
        try {
            return owner.getMethod(mapped, parameterTypes);
        } catch (NoSuchMethodException first) {
            Method method = findDeclaredMethod(owner, mapped, parameterTypes);
            if (method != null) return method;
            if (!mapped.equals(name)) {
                try {
                    return owner.getMethod(name, parameterTypes);
                } catch (NoSuchMethodException ignored) {
                    method = findDeclaredMethod(owner, name, parameterTypes);
                    if (method != null) return method;
                }
            }
            throw first;
        }
    }

    public static Method getDeclaredMethod(Class<?> owner, String name, Class<?>[] parameterTypes) throws NoSuchMethodException {
        String mapped = REMAPPER.mapRuntimeMethodName(owner, name);
        try {
            return owner.getDeclaredMethod(mapped, parameterTypes);
        } catch (NoSuchMethodException first) {
            if (!mapped.equals(name)) {
                try {
                    return owner.getDeclaredMethod(name, parameterTypes);
                } catch (NoSuchMethodException ignored) {
                }
            }
            throw first;
        }
    }

    private static Field findDeclaredField(Class<?> owner, String name) {
        for (Class<?> current = owner; current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(name);
                try { field.setAccessible(true); } catch (RuntimeException ignored) {}
                return field;
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }

    private static Method findDeclaredMethod(Class<?> owner, String name, Class<?>[] parameterTypes) {
        for (Class<?> current = owner; current != null; current = current.getSuperclass()) {
            try {
                Method method = current.getDeclaredMethod(name, parameterTypes);
                try { method.setAccessible(true); } catch (RuntimeException ignored) {}
                return method;
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }
}
