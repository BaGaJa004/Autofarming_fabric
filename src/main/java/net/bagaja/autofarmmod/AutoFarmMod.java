package net.bagaja.autofarmmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AutoFarmMod implements ModInitializer {
    public static final String MOD_ID = "autofarmmod";
    private static final Set<BlockPos> autoFarmBlocks = new HashSet<>();

    @Override
    public void onInitialize() {
        registerEvents();
    }

    private void registerEvents() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;

            BlockPos pos = hitResult.getBlockPos();
            BlockState state = world.getBlockState(pos);

            if (state.getBlock() instanceof CropBlock) {
                CropBlock crop = (CropBlock) state.getBlock();
                if (crop.isMature(state)) {
                    harvestAndReplant((ServerWorld) world, pos, crop);

                    if (player.getStackInHand(hand).getItem() == Items.DIAMOND) {
                        autoFarmBlocks.add(pos);
                        player.getStackInHand(hand).decrement(1);
                    }

                    return ActionResult.SUCCESS;
                }
            }

            return ActionResult.PASS;
        });

        ServerTickEvents.END_WORLD_TICK.register(world -> {
            for (BlockPos pos : autoFarmBlocks) {
                BlockState state = world.getBlockState(pos);
                if (state.getBlock() instanceof CropBlock) {
                    CropBlock crop = (CropBlock) state.getBlock();
                    if (crop.isMature(state)) {
                        harvestAndReplant(world, pos, crop);
                    }
                }
            }
        });
    }

    private void harvestAndReplant(ServerWorld world, BlockPos pos, CropBlock crop) {
        BlockState state = world.getBlockState(pos);
        List<ItemStack> drops = Block.getDroppedStacks(state, world, pos, null);

        for (ItemStack drop : drops) {
            Block.dropStack(world, pos, drop);
        }

        world.setBlockState(pos, crop.getDefaultState(), 3);
    }

}
