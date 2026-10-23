package io.lunararcdevs.lunararc.common.server;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Opt-in (-Dlunararc.debugIngredients=true): logs every loaded recipe that holds an ingredient containing the air item. */
public final class LunarArcIngredientDebug {
    private static final Logger LOGGER = LoggerFactory.getLogger("LunarArc");

    private LunarArcIngredientDebug() {
    }

    private static int scans;

    public static synchronized void scan(MinecraftServer server) {
        if (!Boolean.getBoolean("lunararc.debugIngredients") || scans++ >= 4) return;
        int found = 0;
        for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
            String where = find(holder.value(), 0, new IdentityHashMap<>());
            if (where != null) {
                found++;
                LOGGER.warn("[ingredients] {} ({}) has an ingredient with air at {}", holder.id().identifier(), holder.value().getClass().getName(), where);
            }
        }
        LOGGER.info("[ingredients] scan finished, {} recipe(s) contain an air ingredient", found);
    }

    private static String find(Object value, int depth, Map<Object, Boolean> seen) {
        if (value == null || depth > 4 || seen.put(value, Boolean.TRUE) != null) return null;
        if (value instanceof Ingredient ingredient) {
            return ingredient.items().anyMatch(item -> item.value() == Items.AIR) ? "Ingredient" : null;
        }
        if (value instanceof Collection<?> collection) {
            int index = 0;
            for (Object element : collection) {
                String inner = find(element, depth + 1, seen);
                if (inner != null) return "[" + index + "]." + inner;
                index++;
            }
            return null;
        }
        Class<?> type = value.getClass();
        if (type.isPrimitive() || type.isEnum() || type.getName().startsWith("java.")) return null;
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) continue;
                try {
                    field.setAccessible(true);
                    String inner = find(field.get(value), depth + 1, seen);
                    if (inner != null) return field.getName() + "." + inner;
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    // inaccessible module member: skip it
                }
            }
        }
        return null;
    }
}
