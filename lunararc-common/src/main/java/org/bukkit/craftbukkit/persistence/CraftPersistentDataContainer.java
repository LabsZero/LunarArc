package org.bukkit.craftbukkit.persistence;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class CraftPersistentDataContainer implements PersistentDataContainer {
    private static final int SERIAL_VERSION = 1;
    private final Map<NamespacedKey, StoredValue> customData = new HashMap<>();
    private final PersistentDataAdapterContext adapterContext = new PersistentDataAdapterContext() {
        @Override public @NotNull PersistentDataContainer newPersistentDataContainer() {
            return new CraftPersistentDataContainer();
        }
    };

    public CraftPersistentDataContainer() {}
    public CraftPersistentDataContainer(CraftPersistentDataContainer source) {
        source.customData.forEach((key, value) -> customData.put(key, value.copy()));
    }

    @Override
    public <T, Z> void set(@NotNull NamespacedKey key, @NotNull PersistentDataType<T, Z> type, @NotNull Z value) {
        if (key == null || type == null || value == null) throw new IllegalArgumentException("key, type and value cannot be null");
        T primitive = type.toPrimitive(value, adapterContext);
        customData.put(key, new StoredValue(type.getPrimitiveType(), copyPrimitive(primitive)));
    }

    @Override public <T, Z> boolean has(@NotNull NamespacedKey key, @NotNull PersistentDataType<T, Z> type) {
        StoredValue stored = customData.get(key);
        return stored != null && primitiveTypeMatches(type.getPrimitiveType(), stored.primitiveType());
    }
    @Override public boolean has(@NotNull NamespacedKey key) { return customData.containsKey(key); }

    @Override
    @SuppressWarnings("unchecked")
    public <T, Z> @Nullable Z get(@NotNull NamespacedKey key, @NotNull PersistentDataType<T, Z> type) {
        StoredValue stored = customData.get(key);
        if (stored == null || !primitiveTypeMatches(type.getPrimitiveType(), stored.primitiveType())) return null;
        return type.fromPrimitive((T) copyPrimitive(stored.value()), adapterContext);
    }

    @Override public <T, Z> @NotNull Z getOrDefault(@NotNull NamespacedKey key, @NotNull PersistentDataType<T, Z> type, @NotNull Z defaultValue) {
        Z value = get(key, type);
        return value != null ? value : defaultValue;
    }

    @Override public @NotNull Set<NamespacedKey> getKeys() { return Collections.unmodifiableSet(customData.keySet()); }
    @Override public void remove(@NotNull NamespacedKey key) { customData.remove(key); }
    @Override public boolean isEmpty() { return customData.isEmpty(); }

    @Override
    public void copyTo(@NotNull PersistentDataContainer other, boolean replace) {
        if (other instanceof CraftPersistentDataContainer target) {
            customData.forEach((key, value) -> {
                if (replace || !target.customData.containsKey(key)) target.customData.put(key, value.copy());
            });
            return;
        }
        customData.forEach((key, value) -> {
            if (!replace && other.has(key)) return;
            copyKnownValueTo(other, key, value);
        });
    }

    @Override public @NotNull PersistentDataAdapterContext getAdapterContext() { return adapterContext; }

    @Override
    public byte[] serializeToBytes() {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream out = new DataOutputStream(bytes)) {
                out.writeInt(SERIAL_VERSION);
                List<Map.Entry<NamespacedKey, StoredValue>> entries = new ArrayList<>(customData.entrySet());
                entries.sort(Comparator.comparing(e -> e.getKey().toString()));
                out.writeInt(entries.size());
                for (Map.Entry<NamespacedKey, StoredValue> entry : entries) {
                    out.writeUTF(entry.getKey().toString());
                    writePrimitive(out, entry.getValue());
                }
            }
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to serialize persistent data", e);
        }
    }

    @Override
    public void readFromBytes(byte[] bytes, boolean clear) {
        if (bytes == null) throw new IllegalArgumentException("bytes cannot be null");
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            int version = in.readInt();
            if (version != SERIAL_VERSION) throw new IllegalArgumentException("Unsupported persistent data version " + version);
            if (clear) customData.clear();
            int size = readLength(in);
            for (int i = 0; i < size; i++) {
                NamespacedKey key = NamespacedKey.fromString(in.readUTF());
                StoredValue value = readPrimitive(in);
                if (key != null && value != null) customData.put(key, value);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read persistent data", e);
        }
    }


    public CompoundTag toTagCompound() {
        return toTag();
    }


    public Map<String, Tag> getRaw() {
        return new AbstractMap<>() {
            @Override
            public Tag get(Object key) {
                if (!(key instanceof String name)) return null;
                return CraftPersistentDataContainer.this.toTag().get(name);
            }

            @Override
            public Tag put(String key, Tag value) {
                Tag previous = get(key);
                NamespacedKey namespaced = NamespacedKey.fromString(key);
                if (namespaced == null) throw new IllegalArgumentException("Invalid persistent data key " + key);
                StoredValue stored = fromNbt(value);
                if (stored == null) throw new IllegalArgumentException("Unsupported persistent NBT type for " + key);
                customData.put(namespaced, stored);
                return previous;
            }

            @Override
            public Tag remove(Object key) {
                if (!(key instanceof String name)) return null;
                Tag previous = get(name);
                NamespacedKey namespaced = NamespacedKey.fromString(name);
                if (namespaced != null) customData.remove(namespaced);
                return previous;
            }

            @Override
            public Set<Entry<String, Tag>> entrySet() {
                LinkedHashSet<Entry<String, Tag>> entries = new LinkedHashSet<>();
                CompoundTag tag = CraftPersistentDataContainer.this.toTag();
                for (String key : tag.getAllKeys()) {
                    Tag value = tag.get(key);
                    if (value != null) entries.add(new SimpleImmutableEntry<>(key, value));
                }
                return entries;
            }
        };
    }


    public void putAll(CompoundTag tag) {
        if (tag == null) return;
        for (String keyString : tag.getAllKeys()) {
            NamespacedKey key = NamespacedKey.fromString(keyString);
            if (key == null) continue;
            StoredValue stored = fromNbt(tag.get(keyString));
            if (stored != null) customData.put(key, stored);
        }
    }

    public Tag getTag(String key) {
        return key == null ? null : toTag().get(key);
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<NamespacedKey, StoredValue> entry : customData.entrySet()) {
            String key = entry.getKey().toString();
            Object value = entry.getValue().value();
            if (value instanceof String v) tag.putString(key, v);
            else if (value instanceof Byte v) tag.putByte(key, v);
            else if (value instanceof Short v) tag.putShort(key, v);
            else if (value instanceof Integer v) tag.putInt(key, v);
            else if (value instanceof Long v) tag.putLong(key, v);
            else if (value instanceof Float v) tag.putFloat(key, v);
            else if (value instanceof Double v) tag.putDouble(key, v);
            else if (value instanceof byte[] v) tag.putByteArray(key, v.clone());
            else if (value instanceof int[] v) tag.putIntArray(key, v.clone());
            else if (value instanceof long[] v) tag.putLongArray(key, v.clone());
            else if (value instanceof CraftPersistentDataContainer v) tag.put(key, v.toTag());
        }
        return tag;
    }

    public void fromTag(CompoundTag tag) {
        customData.clear();
        if (tag == null) return;
        for (String keyString : tag.getAllKeys()) {
            NamespacedKey key = NamespacedKey.fromString(keyString);
            if (key == null) continue;
            StoredValue stored = fromNbt(tag.get(keyString));
            if (stored != null) customData.put(key, stored);
        }
    }

    private static StoredValue fromNbt(Tag value) {
        if (value instanceof net.minecraft.nbt.StringTag v) return new StoredValue(String.class, v.getAsString());
        if (value instanceof net.minecraft.nbt.ByteTag v) return new StoredValue(Byte.class, v.getAsByte());
        if (value instanceof net.minecraft.nbt.ShortTag v) return new StoredValue(Short.class, v.getAsShort());
        if (value instanceof net.minecraft.nbt.IntTag v) return new StoredValue(Integer.class, v.getAsInt());
        if (value instanceof net.minecraft.nbt.LongTag v) return new StoredValue(Long.class, v.getAsLong());
        if (value instanceof net.minecraft.nbt.FloatTag v) return new StoredValue(Float.class, v.getAsFloat());
        if (value instanceof net.minecraft.nbt.DoubleTag v) return new StoredValue(Double.class, v.getAsDouble());
        if (value instanceof net.minecraft.nbt.ByteArrayTag v) return new StoredValue(byte[].class, v.getAsByteArray());
        if (value instanceof net.minecraft.nbt.IntArrayTag v) return new StoredValue(int[].class, v.getAsIntArray());
        if (value instanceof net.minecraft.nbt.LongArrayTag v) return new StoredValue(long[].class, v.getAsLongArray());
        if (value instanceof CompoundTag v) {
            CraftPersistentDataContainer nested = new CraftPersistentDataContainer();
            nested.fromTag(v);
            return new StoredValue(PersistentDataContainer.class, nested);
        }
        return null;
    }

    private static boolean primitiveTypeMatches(Class<?> requested, Class<?> stored) {
        return requested.equals(stored) || requested.isAssignableFrom(stored) || stored.isAssignableFrom(requested);
    }

    private static Object copyPrimitive(Object value) {
        if (value instanceof byte[] v) return v.clone();
        if (value instanceof int[] v) return v.clone();
        if (value instanceof long[] v) return v.clone();
        if (value instanceof CraftPersistentDataContainer v) return new CraftPersistentDataContainer(v);
        return value;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void copyKnownValueTo(PersistentDataContainer target, NamespacedKey key, StoredValue value) {
        Class<?> type = value.primitiveType();
        PersistentDataType dataType = null;
        if (type == Byte.class) dataType = PersistentDataType.BYTE;
        else if (type == Short.class) dataType = PersistentDataType.SHORT;
        else if (type == Integer.class) dataType = PersistentDataType.INTEGER;
        else if (type == Long.class) dataType = PersistentDataType.LONG;
        else if (type == Float.class) dataType = PersistentDataType.FLOAT;
        else if (type == Double.class) dataType = PersistentDataType.DOUBLE;
        else if (type == String.class) dataType = PersistentDataType.STRING;
        else if (type == byte[].class) dataType = PersistentDataType.BYTE_ARRAY;
        else if (type == int[].class) dataType = PersistentDataType.INTEGER_ARRAY;
        else if (type == long[].class) dataType = PersistentDataType.LONG_ARRAY;
        else if (PersistentDataContainer.class.isAssignableFrom(type)) dataType = PersistentDataType.TAG_CONTAINER;
        if (dataType != null) target.set(key, dataType, copyPrimitive(value.value()));
    }

    private static void writePrimitive(DataOutputStream out, StoredValue stored) throws IOException {
        Object value = stored.value();
        if (value instanceof Byte v) { out.writeByte(1); out.writeByte(v); }
        else if (value instanceof Short v) { out.writeByte(2); out.writeShort(v); }
        else if (value instanceof Integer v) { out.writeByte(3); out.writeInt(v); }
        else if (value instanceof Long v) { out.writeByte(4); out.writeLong(v); }
        else if (value instanceof Float v) { out.writeByte(5); out.writeFloat(v); }
        else if (value instanceof Double v) { out.writeByte(6); out.writeDouble(v); }
        else if (value instanceof String v) {
            out.writeByte(7); byte[] data=v.getBytes(StandardCharsets.UTF_8); out.writeInt(data.length); out.write(data);
        } else if (value instanceof byte[] v) { out.writeByte(8); out.writeInt(v.length); out.write(v); }
        else if (value instanceof int[] v) { out.writeByte(9); out.writeInt(v.length); for (int x:v) out.writeInt(x); }
        else if (value instanceof long[] v) { out.writeByte(10); out.writeInt(v.length); for (long x:v) out.writeLong(x); }
        else if (value instanceof CraftPersistentDataContainer v) {
            out.writeByte(11); byte[] nested=v.serializeToBytes(); out.writeInt(nested.length); out.write(nested);
        } else throw new IOException("Unsupported persistent primitive " + value.getClass().getName());
    }

    private static StoredValue readPrimitive(DataInputStream in) throws IOException {
        return switch (in.readUnsignedByte()) {
            case 1 -> new StoredValue(Byte.class, in.readByte());
            case 2 -> new StoredValue(Short.class, in.readShort());
            case 3 -> new StoredValue(Integer.class, in.readInt());
            case 4 -> new StoredValue(Long.class, in.readLong());
            case 5 -> new StoredValue(Float.class, in.readFloat());
            case 6 -> new StoredValue(Double.class, in.readDouble());
            case 7 -> new StoredValue(String.class, new String(readByteArray(in), StandardCharsets.UTF_8));
            case 8 -> new StoredValue(byte[].class, readByteArray(in));
            case 9 -> { int n=readLength(in); int[] a=new int[n]; for(int i=0;i<n;i++) a[i]=in.readInt(); yield new StoredValue(int[].class,a); }
            case 10 -> { int n=readLength(in); long[] a=new long[n]; for(int i=0;i<n;i++) a[i]=in.readLong(); yield new StoredValue(long[].class,a); }
            case 11 -> { CraftPersistentDataContainer c=new CraftPersistentDataContainer(); c.readFromBytes(readByteArray(in),true); yield new StoredValue(PersistentDataContainer.class,c); }
            default -> throw new IOException("Unsupported persistent primitive type");
        };
    }

    private static byte[] readByteArray(DataInputStream in) throws IOException {
        int length=readLength(in); byte[] data=new byte[length]; in.readFully(data); return data;
    }

    private static int readLength(DataInputStream in) throws IOException {
        int length=in.readInt();
        if (length < 0 || length > 16*1024*1024) throw new IOException("Invalid persistent data length " + length);
        return length;
    }

    private record StoredValue(Class<?> primitiveType, Object value) {
        StoredValue copy() { return new StoredValue(primitiveType, copyPrimitive(value)); }
    }
}
