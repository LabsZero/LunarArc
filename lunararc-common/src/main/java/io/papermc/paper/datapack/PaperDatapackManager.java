package io.papermc.paper.datapack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public final class PaperDatapackManager implements DatapackManager {
    private final MinecraftServer server;
    private final PackRepository repository;

    public PaperDatapackManager(@NotNull MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
        this.repository = server.getPackRepository();
    }

    @Override
    public void refreshPacks() {
        repository.reload();
    }

    @Override
    public @Nullable Datapack getPack(@NotNull String name) {
        Objects.requireNonNull(name, "name");
        Pack pack = repository.getPack(name);
        return pack == null ? null : wrap(pack);
    }

    @Override
    public @NotNull Collection<Datapack> getPacks() {
        Collection<Pack> selected = repository.getSelectedPacks();
        List<Datapack> result = new ArrayList<>();
        for (Pack pack : repository.getAvailablePacks()) {
            result.add(new PaperDatapack(server, repository, pack, selected.contains(pack)));
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public @NotNull Collection<Datapack> getEnabledPacks() {
        List<Datapack> result = new ArrayList<>();
        for (Pack pack : repository.getSelectedPacks()) {
            result.add(new PaperDatapack(server, repository, pack, true));
        }
        return Collections.unmodifiableList(result);
    }

    private PaperDatapack wrap(Pack pack) {
        return new PaperDatapack(server, repository, pack, repository.getSelectedPacks().contains(pack));
    }
}
