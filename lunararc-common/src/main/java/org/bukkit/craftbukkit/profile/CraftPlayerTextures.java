package org.bukkit.craftbukkit.profile;

import org.bukkit.profile.PlayerTextures;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;

public final class CraftPlayerTextures implements PlayerTextures {
    private URL skin;
    private URL cape;
    private SkinModel skinModel = SkinModel.CLASSIC;
    private long timestamp;
    private boolean signed;
    private com.destroystokyo.paper.profile.SharedPlayerProfile profile;
    private boolean dirty;

    public CraftPlayerTextures() {}

    public CraftPlayerTextures(com.destroystokyo.paper.profile.SharedPlayerProfile profile) {
        this.profile = profile;
    }

    private boolean loaded;

    private void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        if (profile != null) load();
    }

    private void load() {
        com.mojang.authlib.properties.Property property = profile.getProperty("textures");
        if (property == null) return;
        try {
            String json = new String(java.util.Base64.getDecoder().decode(property.value()), java.nio.charset.StandardCharsets.UTF_8);
            com.google.gson.JsonObject root = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
            this.timestamp = root.has("timestamp") ? root.get("timestamp").getAsLong() : 0L;
            this.signed = property.hasSignature();
            com.google.gson.JsonObject textures = root.has("textures") ? root.getAsJsonObject("textures") : null;
            if (textures == null) return;
            if (textures.has("SKIN")) {
                com.google.gson.JsonObject skinData = textures.getAsJsonObject("SKIN");
                this.skin = parseUrl(skinData.has("url") ? skinData.get("url").getAsString() : null);
                boolean slim = skinData.has("metadata") && skinData.getAsJsonObject("metadata").has("model")
                        && "slim".equals(skinData.getAsJsonObject("metadata").get("model").getAsString());
                this.skinModel = slim ? SkinModel.SLIM : SkinModel.CLASSIC;
            }
            if (textures.has("CAPE")) {
                this.cape = parseUrl(textures.getAsJsonObject("CAPE").get("url").getAsString());
            }
        } catch (RuntimeException ignored) {
        }
    }

    private static URL parseUrl(String value) {
        if (value == null) return null;
        try {
            return java.net.URI.create(value).toURL();
        } catch (java.net.MalformedURLException | IllegalArgumentException error) {
            return null;
        }
    }

    public void copyFrom(PlayerTextures other) {
        ensureLoaded();
        this.skin = other.getSkin();
        this.skinModel = other.getSkinModel();
        this.cape = other.getCape();
        this.timestamp = 0L;
        this.signed = false;
        this.dirty = true;
    }

    public void rebuildPropertyIfDirty() {
        if (!dirty || profile == null) return;
        dirty = false;
        if (isEmpty()) {
            profile.removeProperty("textures");
            return;
        }
        com.google.gson.JsonObject textures = new com.google.gson.JsonObject();
        if (skin != null) {
            com.google.gson.JsonObject skinData = new com.google.gson.JsonObject();
            skinData.addProperty("url", skin.toString());
            if (skinModel == SkinModel.SLIM) {
                com.google.gson.JsonObject metadata = new com.google.gson.JsonObject();
                metadata.addProperty("model", "slim");
                skinData.add("metadata", metadata);
            }
            textures.add("SKIN", skinData);
        }
        if (cape != null) {
            com.google.gson.JsonObject capeData = new com.google.gson.JsonObject();
            capeData.addProperty("url", cape.toString());
            textures.add("CAPE", capeData);
        }
        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        root.addProperty("timestamp", System.currentTimeMillis());
        root.addProperty("profileId", profile.getUniqueId() == null ? "" : profile.getUniqueId().toString().replace("-", ""));
        root.addProperty("profileName", profile.getName() == null ? "" : profile.getName());
        root.add("textures", textures);
        String encoded = java.util.Base64.getEncoder().encodeToString(root.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        profile.setProperty("textures", new com.mojang.authlib.properties.Property("textures", encoded));
    }

    @Override
    public boolean isEmpty() {
        ensureLoaded();
        return skin == null && cape == null;
    }

    @Override
    public void clear() {
        ensureLoaded();
        skin = null;
        cape = null;
        skinModel = SkinModel.CLASSIC;
        timestamp = 0L;
        signed = false;
        dirty = true;
    }

    @Override
    public @Nullable URL getSkin() {
        ensureLoaded();
        return skin;
    }

    @Override
    public void setSkin(@Nullable URL skinUrl) {
        setSkin(skinUrl, SkinModel.CLASSIC);
    }

    @Override
    public void setSkin(@Nullable URL skinUrl, @Nullable SkinModel skinModel) {
        ensureLoaded();
        this.skin = skinUrl;
        this.skinModel = skinUrl == null || skinModel == null ? SkinModel.CLASSIC : skinModel;
        this.timestamp = 0L;
        this.signed = false;
        this.dirty = true;
    }

    @Override
    public @NotNull SkinModel getSkinModel() {
        ensureLoaded();
        return skinModel;
    }

    @Override
    public @Nullable URL getCape() {
        ensureLoaded();
        return cape;
    }

    @Override
    public void setCape(@Nullable URL capeUrl) {
        ensureLoaded();
        this.cape = capeUrl;
        this.timestamp = 0L;
        this.signed = false;
        this.dirty = true;
    }

    @Override
    public long getTimestamp() {
        ensureLoaded();
        return timestamp;
    }

    @Override
    public boolean isSigned() {
        ensureLoaded();
        return signed;
    }
}
