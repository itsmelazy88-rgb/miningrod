package com.miningrod.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MiningRodItem extends Item {

    private static final int TUNNEL_WIDTH  = 3;
    private static final int TUNNEL_HEIGHT = 3;
    private static final int TUNNEL_DEPTH  = 5;
    private static final int BLAST_WIDTH   = 5;
    private static final int BLAST_HEIGHT  = 5;
    private static final int BLAST_DEPTH   = 3;
    private static final int COOLDOWN_TICKS = 20;

    public MiningRodItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            boolean sneaking = player.isShiftKeyDown();
            Direction facing = player.getDirection();
            BlockPos origin  = player.blockPosition();

            int width  = sneaking ? BLAST_WIDTH  : TUNNEL_WIDTH;
            int height = sneaking ? BLAST_HEIGHT : TUNNEL_HEIGHT;
            int depth  = sneaking ? BLAST_DEPTH  : TUNNEL_DEPTH;

            String msg = sneaking
                ? "\u00a7c\uD83D\uDCA5 Blast! \u00a77(" + width + "x" + height + "x" + depth + ")"
                : "\u00a7b\u26A1 Tunnel! \u00a77(" + width + "x" + height + "x" + depth + ")";

            List<ItemStack> collected = mineArea(
                (ServerLevel) level, player, origin, facing, width, height, depth);

            level.playSound(null, origin,
                SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS,
                sneaking ? 1.0f : 0.5f,
                sneaking ? 0.9f : 1.2f);

            int total = collected.stream().mapToInt(ItemStack::getCount).sum();
            player.displayClientMessage(
                Component.literal(msg + " \u00a7aCollected \u00a7e" + total + " \u00a7aores!"), true);

            if (!player.isCreative()) {
                stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(EquipmentSlot.MAINHAND));
            }

            player.getCooldowns().addCooldown(stack.getItem(), COOLDOWN_TICKS);
        }

        return InteractionResultHolder.success(stack);
    }

    private List<ItemStack> mineArea(ServerLevel level, Player player,
                                     BlockPos origin, Direction facing,
                                     int width, int height, int depth) {
        Direction sideways = facing.getClockWise();
        int halfW = width  / 2;
        int halfH = height / 2;
        List<ItemStack> collected = new ArrayList<>();

        for (int d = 1; d <= depth; d++) {
            for (int w = -halfW; w <= halfW; w++) {
                for (int h = -halfH; h <= halfH; h++) {
                    BlockPos pos = origin
                        .relative(facing,       d)
                        .relative(sideways,     w)
                        .relative(Direction.UP, h);

                    BlockState state = level.getBlockState(pos);

                    if (state.isAir()
                        || state.is(Blocks.BEDROCK)
                        || !state.getFluidState().isEmpty()
                        || level.getBlockEntity(pos) != null) {
                        continue;
                    }

                    List<ItemStack> drops = Block.getDrops(
                        state, level, pos, null, player, player.getMainHandItem());

                    for (ItemStack drop : drops) {
                        if (isValuableOre(drop)) {
                            collected.add(drop.copy());
                        }
                    }

                    level.destroyBlock(pos, false);

                    level.sendParticles(
                        new BlockParticleOption(ParticleTypes.BLOCK, state),
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        6, 0.3, 0.3, 0.3, 0.05);
                }
            }
        }

        for (ItemStack drop : collected) {
            if (!player.getInventory().add(drop)) {
                player.drop(drop, false);
            }
        }

        return collected;
    }

    private boolean isValuableOre(ItemStack stack) {
        return stack.is(Items.DIAMOND)
            || stack.is(Items.RAW_IRON)
            || stack.is(Items.RAW_GOLD)
            || stack.is(Items.RAW_COPPER)
            || stack.is(Items.EMERALD)
            || stack.is(Items.COAL)
            || stack.is(Items.LAPIS_LAZULI)
            || stack.is(Items.REDSTONE)
            || stack.is(Items.QUARTZ)
            || stack.is(Items.AMETHYST_SHARD)
            || stack.is(Items.ANCIENT_DEBRIS);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("\u00a77Right-click \u00a7b\u2192 \u00a7fDrill 3x3x5 tunnel"));
        tooltip.add(Component.literal("\u00a77Sneak+Click \u00a7c\u2192 \u00a7fBlast 5x5x3 area"));
        tooltip.add(Component.literal("\u00a7aAuto-collects ores into inventory!"));
        tooltip.add(Component.literal("\u00a78Skips bedrock, water and chests"));
        tooltip.add(Component.literal("\u00a76128 uses \u00a77| \u00a7e1s cooldown"));
    }
}
