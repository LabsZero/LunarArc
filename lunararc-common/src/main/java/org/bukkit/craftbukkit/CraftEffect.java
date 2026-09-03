package org.bukkit.craftbukkit;

import java.util.Objects;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.block.Block;
import org.bukkit.Axis;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;


public final class CraftEffect {
    private CraftEffect() {
    }

    public static <T> int getDataValue(Effect effect, T data) {
        return getDataValue(effect, data, null);
    }

    public static <T> int getDataValue(Effect effect, T data, RegistryAccess registryAccess) {
        Objects.requireNonNull(effect, "effect");
        return switch (effect) {
            case PARTICLES_SCULK_CHARGE,
                    TRIAL_SPAWNER_DETECT_PLAYER,
                    BEE_GROWTH,
                    TURTLE_EGG_PLACEMENT,
                    SMASH_ATTACK,
                    TRIAL_SPAWNER_DETECT_PLAYER_OMINOUS,
                    VILLAGER_PLANT_GROW,
                    BONE_MEAL_USE -> ((Number) data).intValue();
            case POTION_BREAK, INSTANT_POTION_BREAK -> ((Color) data).asRGB();
            case RECORD_PLAY -> {
                Material material = (Material) data;
                if (material != Material.AIR && !material.isRecord()) {
                    throw new IllegalArgumentException("Invalid record type for Material " + material);
                }
                net.minecraft.world.item.Item item = org.bukkit.craftbukkit.util.CraftMagicNumbers.getItem(material);
                yield item == null ? 0 : net.minecraft.world.item.Item.getId(item);
            }
            case SHOOT_WHITE_SMOKE -> {
                BlockFace face = (BlockFace) data;
                if (!face.isCartesian()) throw new IllegalArgumentException(face + " isn't cartesian");
                yield to3dData(face);
            }
            case SMOKE -> switch ((BlockFace) data) {
                case UP -> 1;
                case NORTH -> 2;
                case SOUTH -> 3;
                case WEST -> 4;
                case EAST -> 5;
                case DOWN, NORTH_EAST, NORTH_WEST, SOUTH_EAST, SOUTH_WEST, SELF -> 0;
                default -> throw new IllegalArgumentException("Bad smoke direction");
            };
            case STEP_SOUND -> {
                if (data instanceof Material material) {
                    if (!material.isBlock()) throw new IllegalArgumentException("Material " + material + " is not a block");
                    net.minecraft.world.level.block.Block block = org.bukkit.craftbukkit.util.CraftMagicNumbers.getBlock(material);
                    if (block == null) throw new IllegalArgumentException("Unknown block material " + material);
                    yield Block.getId(block.defaultBlockState());
                }
                if (data instanceof org.bukkit.craftbukkit.block.data.CraftBlockData blockData) {
                    yield Block.getId(blockData.getState());
                }
                throw new IllegalArgumentException("STEP_SOUND requires Material or CraftBlockData");
            }
            case PARTICLES_AND_SOUND_BRUSH_BLOCK_COMPLETE -> {
                if (!(data instanceof org.bukkit.craftbukkit.block.data.CraftBlockData blockData)) {
                    throw new IllegalArgumentException("PARTICLES_AND_SOUND_BRUSH_BLOCK_COMPLETE requires CraftBlockData");
                }
                yield Block.getId(blockData.getState());
            }
            case COMPOSTER_FILL_ATTEMPT,
                    TRIAL_SPAWNER_SPAWN,
                    TRIAL_SPAWNER_SPAWN_MOB_AT,
                    VAULT_ACTIVATE,
                    VAULT_DEACTIVATE,
                    TRIAL_SPAWNER_BECOME_OMINOUS,
                    TRIAL_SPAWNER_SPAWN_ITEM -> ((Boolean) data) ? 1 : 0;
            case ELECTRIC_SPARK -> {
                if (data == null) yield -1;
                yield switch ((Axis) data) {
                    case X -> 0;
                    case Y -> 1;
                    case Z -> 2;
                };
            }
            default -> 0;
        };
    }

    private static int to3dData(BlockFace face) {
        return switch (face) {
            case DOWN -> 0;
            case UP -> 1;
            case NORTH -> 2;
            case SOUTH -> 3;
            case WEST -> 4;
            case EAST -> 5;
            default -> throw new IllegalArgumentException(face + " isn't cartesian");
        };
    }
}
