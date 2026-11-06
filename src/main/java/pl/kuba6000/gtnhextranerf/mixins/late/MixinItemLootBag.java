package pl.kuba6000.gtnhextranerf.mixins.late;

import java.util.Collections;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import eu.usrv.enhancedlootbags.core.LootGroupsHandler;
import eu.usrv.enhancedlootbags.core.items.ItemLootBag;
import eu.usrv.enhancedlootbags.core.serializer.LootGroups.LootGroup;
import eu.usrv.enhancedlootbags.core.serializer.LootGroups.LootGroup.Drop;
import pl.kuba6000.gtnhextranerf.loot.runtime.ServerLootRuntime;

@Mixin(value = ItemLootBag.class, remap = false)
public abstract class MixinItemLootBag {

    @Inject(method = "getRandomLootItems", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void gtnhextranerf$firstController(EntityPlayer player, LootGroup group,
        CallbackInfoReturnable<List<ItemStack>> callback) {
        ItemStack guaranteed = ServerLootRuntime.guaranteedController(player, group);
        if (guaranteed != null) callback.setReturnValue(Collections.singletonList(guaranteed));
    }

    @WrapMethod(method = "onItemRightClick", remap = true, require = 1)
    private ItemStack gtnhextranerf$opening(ItemStack stack, World world, EntityPlayer player,
        Operation<ItemStack> original) {
        return ServerLootRuntime.open(stack, world, player, () -> original.call(stack, world, player));
    }

    @WrapOperation(
        method = "getRandomLootItems",
        at = @At(
            value = "INVOKE",
            target = "Leu/usrv/enhancedlootbags/core/LootGroupsHandler;isDropAllowedForPlayer(Lnet/minecraft/entity/player/EntityPlayer;Leu/usrv/enhancedlootbags/core/serializer/LootGroups$LootGroup;Leu/usrv/enhancedlootbags/core/serializer/LootGroups$LootGroup$Drop;Z)Z"),
        require = 1)
    private boolean gtnhextranerf$allow(LootGroupsHandler handler, EntityPlayer player, LootGroup group, Drop drop,
        boolean update, Operation<Boolean> original) {
        return ServerLootRuntime.allowed(drop) && original.call(handler, player, group, drop, update);
    }

    @WrapOperation(
        method = "getRandomLootItems",
        at = @At(
            value = "INVOKE",
            target = "Leu/usrv/enhancedlootbags/core/serializer/LootGroups$LootGroup$Drop;getItemStack(I)Lnet/minecraft/item/ItemStack;"),
        require = 1)
    private ItemStack gtnhextranerf$claim(Drop drop, int amount, Operation<ItemStack> original) {
        return ServerLootRuntime.claim(drop, original.call(drop, amount));
    }

    @WrapOperation(
        method = "onItemRightClick",
        remap = true,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/World;spawnEntityInWorld(Lnet/minecraft/entity/Entity;)Z"),
        require = 1)
    private boolean gtnhextranerf$observeDelivery(World world, Entity entity, Operation<Boolean> original) {
        boolean accepted = original.call(world, entity);
        if (entity instanceof EntityItem) ServerLootRuntime.spawned((EntityItem) entity, accepted);
        return accepted;
    }
}
