package org.bukkit.craftbukkit.structure;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.block.CraftBlockState;
import org.bukkit.structure.Palette;
import org.jetbrains.annotations.NotNull;

public final class CraftPalette implements Palette {
    private final StructureTemplate.Palette handle;

    public CraftPalette(StructureTemplate.Palette handle) {
        this.handle = java.util.Objects.requireNonNull(handle, "handle");
    }

    @Override
    public @NotNull List<BlockState> getBlocks() {
        List<BlockState> result = new ArrayList<>();
        for (StructureTemplate.StructureBlockInfo info : handle.blocks()) {
            result.add(CraftBlockState.unplacedAt(info.pos(), info.state()));
        }
        return List.copyOf(result);
    }

    @Override
    public int getBlockCount() {
        return handle.blocks().size();
    }
}
