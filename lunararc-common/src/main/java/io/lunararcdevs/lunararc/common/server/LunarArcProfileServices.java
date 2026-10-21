package io.lunararcdevs.lunararc.common.server;

import com.destroystokyo.paper.event.profile.FillProfileEvent;
import com.destroystokyo.paper.event.profile.LookupProfileEvent;
import com.destroystokyo.paper.event.profile.PreFillProfileEvent;
import com.destroystokyo.paper.event.profile.PreLookupProfileEvent;
import com.destroystokyo.paper.profile.CraftPlayerProfile;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.minecraft.InsecurePublicKeyException;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import com.mojang.authlib.minecraft.SessionService;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.services.ProfileResult;
import com.mojang.authlib.services.response.NameAndId;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Paper's profile events (PreLookupProfileEvent, LookupProfileEvent, PreFillProfileEvent, FillProfileEvent) work by wrapping the
 * authlib services the server talks to Mojang through. The wrappers delegate everything else untouched.
 */
public final class LunarArcProfileServices {
    private LunarArcProfileServices() {
    }

    private static boolean listened(HandlerList handlers) {
        return handlers.getRegisteredListeners().length != 0 && Bukkit.getServer() != null;
    }

    private static void call(Event event) {
        Bukkit.getPluginManager().callEvent(event);
    }

    public static GameProfileRepository wrap(GameProfileRepository delegate) {
        return new Repository(delegate);
    }

    public static SessionService wrap(SessionService delegate) {
        return new Sessions(delegate);
    }

    private record Repository(GameProfileRepository delegate) implements GameProfileRepository {
        @Override
        public void findProfilesByNames(String[] names, ProfileLookupCallback callback) {
            if (!listened(PreLookupProfileEvent.getHandlerList()) && !listened(LookupProfileEvent.getHandlerList())) {
                delegate.findProfilesByNames(names, callback);
                return;
            }
            List<String> unresolved = new ArrayList<>();
            for (String name : names) {
                PreLookupProfileEvent event = new PreLookupProfileEvent(name);
                call(event);
                if (event.getUUID() != null) callback.onProfileLookupSucceeded(name, event.getUUID());
                else unresolved.add(name);
            }
            if (unresolved.isEmpty()) return;
            delegate.findProfilesByNames(unresolved.toArray(new String[0]), new ProfileLookupCallback() {
                @Override
                public void onProfileLookupSucceeded(String name, UUID id) {
                    call(new LookupProfileEvent(new CraftPlayerProfile(id, name)));
                    callback.onProfileLookupSucceeded(name, id);
                }

                @Override
                public void onProfileLookupFailed(String name, Exception exception) {
                    callback.onProfileLookupFailed(name, exception);
                }
            });
        }

        @Override
        public Optional<NameAndId> findProfileByName(String name) {
            if (listened(PreLookupProfileEvent.getHandlerList())) {
                PreLookupProfileEvent event = new PreLookupProfileEvent(name);
                call(event);
                // a plugin that already knows the UUID spares the network call
                if (event.getUUID() != null) return Optional.of(new NameAndId(event.getUUID(), name));
            }
            return delegate.findProfileByName(name);
        }
    }

    private record Sessions(SessionService delegate) implements SessionService {
        @Override
        public void joinServer(UUID profileId, String authenticationToken, String serverId) throws AuthenticationException {
            delegate.joinServer(profileId, authenticationToken, serverId);
        }

        @Override
        public ProfileResult hasJoinedServer(String profileName, String serverId, InetAddress address) throws AuthenticationUnavailableException {
            return delegate.hasJoinedServer(profileName, serverId, address);
        }

        @Override
        public Property getPackedTextures(GameProfile profile) {
            return delegate.getPackedTextures(profile);
        }

        @Override
        public MinecraftProfileTextures unpackTextures(Property packedTextures) {
            return delegate.unpackTextures(packedTextures);
        }

        @Override
        public MinecraftProfileTextures getTextures(GameProfile profile) {
            return delegate.getTextures(profile);
        }

        @Override
        public String getSecurePropertyValue(Property property) throws InsecurePublicKeyException {
            return delegate.getSecurePropertyValue(property);
        }

        @Override
        public ProfileResult fetchProfile(UUID profileId, boolean requireSecure) {
            if (!listened(PreFillProfileEvent.getHandlerList()) && !listened(FillProfileEvent.getHandlerList())) {
                return delegate.fetchProfile(profileId, requireSecure);
            }
            CraftPlayerProfile before = new CraftPlayerProfile(profileId, null);
            call(new PreFillProfileEvent(before));
            ProfileResult result = delegate.fetchProfile(profileId, requireSecure);
            if (result == null) return null;
            // properties a plugin supplied before the lookup (textures, ...) fill in what Mojang did not return
            GameProfile game = new GameProfile(result.profile().id(), result.profile().name());
            game.properties().putAll(result.profile().properties());
            before.getProperties().forEach(property -> {
                if (!game.properties().containsKey(property.getName())) {
                    game.properties().put(property.getName(), new Property(property.getName(), property.getValue(), property.getSignature()));
                }
            });
            FillProfileEvent event = new FillProfileEvent(CraftPlayerProfile.asBukkitCopy(game));
            call(event);
            GameProfile filled = new GameProfile(game.id(), game.name());
            event.getPlayerProfile().getProperties().forEach(property -> filled.properties().put(property.getName(),
                    new Property(property.getName(), property.getValue(), property.getSignature())));
            return new ProfileResult(filled, result.actions());
        }
    }
}
