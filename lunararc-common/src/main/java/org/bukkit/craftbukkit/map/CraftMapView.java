package org.bukkit.craftbukkit.map;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

public final class CraftMapView implements MapView {
    private final int id;
    private WeakReference<World> world;
    private int centerX;
    private int centerZ;
    private Scale scale = Scale.NORMAL;
    private boolean trackingPosition = true;
    private boolean unlimitedTracking;
    private boolean locked;
    private final List<MapRenderer> renderers = new ArrayList<>();
    private final Map<MapRenderer, Map<CraftPlayer, CraftMapCanvas>> canvases = new HashMap<>();
    private final Map<CraftPlayer, RenderData> renderCache = new WeakHashMap<>();
    private RenderData sharedRender;

    public CraftMapView(int id, World world) {
        this.id = id;
        this.world = new WeakReference<>(Objects.requireNonNull(world, "world"));
    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public boolean isVirtual() {
        return !this.renderers.isEmpty();
    }

    @Override
    public Scale getScale() {
        return this.scale;
    }

    @Override
    public void setScale(Scale scale) {
        this.scale = Objects.requireNonNull(scale, "scale");
    }

    @Override
    public int getCenterX() {
        return this.centerX;
    }

    @Override
    public int getCenterZ() {
        return this.centerZ;
    }

    @Override
    public void setCenterX(int x) {
        this.centerX = x;
    }

    @Override
    public void setCenterZ(int z) {
        this.centerZ = z;
    }

    @Override
    public World getWorld() {
        return this.world.get();
    }

    @Override
    public void setWorld(World world) {
        this.world = new WeakReference<>(Objects.requireNonNull(world, "world"));
    }

    @Override
    public List<MapRenderer> getRenderers() {
        return new ArrayList<>(this.renderers);
    }

    @Override
    public void addRenderer(MapRenderer renderer) {
        Objects.requireNonNull(renderer, "renderer");
        if (this.renderers.contains(renderer)) return;
        this.renderers.add(renderer);
        this.canvases.put(renderer, new WeakHashMap<>());
        renderer.initialize(this);
        this.sharedRender = null;
        this.renderCache.clear();
    }

    @Override
    public boolean removeRenderer(MapRenderer renderer) {
        if (renderer == null || !this.renderers.remove(renderer)) return false;
        this.canvases.remove(renderer);
        this.sharedRender = null;
        this.renderCache.clear();
        return true;
    }

    public RenderData render(CraftPlayer player) {
        boolean contextual = this.renderers.stream().anyMatch(MapRenderer::isContextual);
        RenderData output;
        if (contextual) {
            output = this.renderCache.computeIfAbsent(player, ignored -> new RenderData());
            this.sharedRender = null;
        } else {
            if (this.sharedRender == null) this.sharedRender = new RenderData();
            output = this.sharedRender;
        }
        Arrays.fill(output.buffer, (byte) 0);
        output.cursors.clear();

        for (MapRenderer renderer : this.renderers) {
            Map<CraftPlayer, CraftMapCanvas> rendererCanvases = this.canvases.get(renderer);
            CraftPlayer key = renderer.isContextual() ? player : null;
            CraftMapCanvas canvas = rendererCanvases.computeIfAbsent(key, ignored -> new CraftMapCanvas(this));
            canvas.setBase(output.buffer);
            try {
                renderer.render(this, canvas, player);
            } catch (Throwable throwable) {
                Bukkit.getLogger().log(Level.SEVERE, "Could not render map using renderer " + renderer.getClass().getName(), throwable);
            }
            byte[] layer = canvas.getBuffer();
            for (int i = 0; i < layer.length; i++) {
                byte color = layer[i];
                if (color >= 0 || color <= -9) output.buffer[i] = color;
            }
            for (int i = 0; i < canvas.getCursors().size(); i++) {
                output.cursors.add(canvas.getCursors().getCursor(i));
            }
        }
        return output;
    }

    @Override
    public boolean isTrackingPosition() {
        return this.trackingPosition;
    }

    @Override
    public void setTrackingPosition(boolean trackingPosition) {
        this.trackingPosition = trackingPosition;
    }

    @Override
    public boolean isUnlimitedTracking() {
        return this.unlimitedTracking;
    }

    @Override
    public void setUnlimitedTracking(boolean unlimited) {
        this.unlimitedTracking = unlimited;
    }

    @Override
    public boolean isLocked() {
        return this.locked;
    }

    @Override
    public void setLocked(boolean locked) {
        this.locked = locked;
    }
}
