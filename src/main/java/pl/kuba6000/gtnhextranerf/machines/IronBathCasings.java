package pl.kuba6000.gtnhextranerf.machines;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.common.blocks.BlockCasingsAbstract;

/** Refractory brick casings with an optional Wrought Iron reinforcement. */
public final class IronBathCasings extends Block {

    @SideOnly(Side.CLIENT)
    private IIcon refractorySide;
    @SideOnly(Side.CLIENT)
    private IIcon refractoryTop;
    @SideOnly(Side.CLIENT)
    private IIcon reinforcedSide;

    public IronBathCasings() {
        super(Material.iron);
        setBlockName("gtnhextranerf.iron_bath_casing");
        setHardness(5);
        setResistance(15);
        setStepSound(soundTypeMetal);
        setCreativeTab(CreativeTabs.tabBlock);
    }

    public void register() {
        GameRegistry.registerBlock(this, Subtypes.class, "iron_bath_casing");
    }

    @Override
    public boolean canCreatureSpawn(EnumCreatureType type, IBlockAccess world, int x, int y, int z) {
        return false;
    }

    @Override
    public int damageDropped(int metadata) {
        return metadata;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister icons) {
        refractorySide = icons.registerIcon("gtnhextranerf:iron_bath_refractory");
        refractoryTop = icons.registerIcon("gtnhextranerf:iron_bath_refractory_top");
        reinforcedSide = icons.registerIcon("gtnhextranerf:iron_bath_reinforced");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        if (side < 2) return refractoryTop;
        return metadata == 1 ? reinforcedSide : refractorySide;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getSubBlocks(Item item, CreativeTabs tab, List<ItemStack> stacks) {
        stacks.add(new ItemStack(item, 1, 0));
        stacks.add(new ItemStack(item, 1, 1));
    }

    public static final class Subtypes extends ItemBlock {

        public Subtypes(Block block) {
            super(block);
            setHasSubtypes(true);
            setMaxDamage(0);
        }

        @Override
        public int getMetadata(int damage) {
            return damage;
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
            super.addInformation(stack, player, tooltip, advanced);
            tooltip.add(BlockCasingsAbstract.NO_MOB_SPAWNING.get());
        }

        @Override
        public String getUnlocalizedName(ItemStack stack) {
            return "tile.gtnhextranerf.iron_bath_casing." + stack.getItemDamage();
        }
    }
}
