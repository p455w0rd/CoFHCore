package cofh.core.item.tool;

import cofh.core.enchantment.CoFHEnchantment;
import cofh.lib.util.helpers.ItemHelper;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArrow;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;

import java.util.List;

//import net.minecraft.client.renderer.texture.IIconRegister;
//import net.minecraft.util.IIcon;

public class ItemBowAdv extends ItemBow {

    //    protected IIcon normalIcons[] = new IIcon[4];
    protected ToolMaterial toolMaterial;

    public String repairIngot = "";
    public float arrowSpeedMultiplier = 2.0F;
    public float arrowDamageMultiplier = 1.25F;
    protected boolean showInCreative = true;

    public ItemBowAdv(Item.ToolMaterial toolMaterial) {

        super();
        this.toolMaterial = toolMaterial;
        setMaxDamage(toolMaterial.getMaxUses());
    }

    public int cofh_canEnchantApply(ItemStack stack, Enchantment ench) {

        if (ench == Enchantments.LOOTING) {
            return 1;
        }
        if (ench.type == EnumEnchantmentType.BOW) {
            return 1;
        }
        return -1;
    }

    public ItemBowAdv setRepairIngot(String repairIngot) {

        this.repairIngot = repairIngot;
        return this;
    }

    public ItemBowAdv setArrowSpeed(float multiplier) {

        this.arrowSpeedMultiplier = multiplier;
        return this;
    }

    public ItemBowAdv setArrowDamage(float multiplier) {

        arrowDamageMultiplier = multiplier;
        return this;
    }

    public ItemBowAdv setShowInCreative(boolean showInCreative) {

        this.showInCreative = showInCreative;
        return this;
    }

    @Override
    public void getSubItems(Item item, CreativeTabs tab, List list) {

        if (showInCreative) {
            list.add(new ItemStack(item, 1, 0));
        }
    }

    @Override
    public int getItemEnchantability() {

        return toolMaterial.getEnchantability();
    }

    @Override
    public boolean getIsRepairable(ItemStack itemToRepair, ItemStack stack) {

        return ItemHelper.isOreNameEqual(stack, repairIngot);
    }

    // TODO: This will need a custom render or something
    @Override
    public boolean isFull3D() {

        return true;
    }

    @Override
    public boolean isItemTool(ItemStack stack) {

        return true;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(ItemStack itemStack, World world, EntityPlayer player, EnumHand hand) {
        boolean flag = this.findAmmo(player) != null;

        ActionResult<ItemStack> ret = net.minecraftforge.event.ForgeEventFactory.onArrowNock(itemStack, world, player, hand, flag);
        if (ret != null) {
            return ret;
        }

        if (!player.capabilities.isCreativeMode && !flag) {
            return !flag ? new ActionResult<ItemStack>(EnumActionResult.FAIL, itemStack) : new ActionResult<ItemStack>(EnumActionResult.PASS, itemStack);
        }
        else {
            player.setActiveHand(hand);
            return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, itemStack);
        }
    }

    //TODO Multishot enchant can use Arrow Loose Efent for better mod compatibility.
    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityLivingBase livingBase, int timeLeft) {
        if (livingBase instanceof EntityPlayer) {
            EntityPlayer entityplayer = (EntityPlayer) livingBase;
            boolean flag = entityplayer.capabilities.isCreativeMode || EnchantmentHelper.getEnchantmentLevel(Enchantments.INFINITY, stack) > 0;
            ItemStack itemstack = this.findAmmo(entityplayer);

            int i = this.getMaxItemUseDuration(stack) - timeLeft;
            i = net.minecraftforge.event.ForgeEventFactory.onArrowLoose(stack, world, (EntityPlayer) livingBase, i, itemstack != null || flag);
            if (i < 0) return;

            if (itemstack != null || flag) {
                if (itemstack == null) {
                    itemstack = new ItemStack(Items.ARROW);
                }

                float f = getArrowVelocity(i);

                if ((double) f >= 0.1D) {
                    boolean flag1 = entityplayer.capabilities.isCreativeMode || (itemstack.getItem() instanceof ItemArrow ? ((ItemArrow) itemstack.getItem()).isInfinite(itemstack, stack, entityplayer) : false);

                    if (!world.isRemote) {
                        int enchantMultishot = EnchantmentHelper.getEnchantmentLevel(CoFHEnchantment.multishot, stack);
                        int punchLvl = EnchantmentHelper.getEnchantmentLevel(Enchantments.PUNCH, stack);
                        int powerLvl = EnchantmentHelper.getEnchantmentLevel(Enchantments.POWER, stack);
                        boolean flame = EnchantmentHelper.getEnchantmentLevel(Enchantments.FLAME, stack) > 0;
                        stack.damageItem(1, entityplayer);

                        for (int shot = 0; shot <= enchantMultishot; shot++) {
                            ItemArrow itemarrow = (ItemArrow) (itemstack.getItem() instanceof ItemArrow ? itemstack.getItem() : Items.ARROW);
                            EntityArrow entityarrow = itemarrow.createArrow(world, itemstack, entityplayer);
                            entityarrow.setAim(entityplayer, entityplayer.rotationPitch, entityplayer.rotationYaw, 0.0F, f * 3.0F, 1.0F);

                            if (f == 1.0F) {
                                entityarrow.setIsCritical(true);
                            }

                            if (powerLvl > 0) {
                                entityarrow.setDamage(entityarrow.getDamage() + (double) powerLvl * 0.5D + 0.5D);
                            }

                            if (punchLvl > 0) {
                                entityarrow.setKnockbackStrength(punchLvl);
                            }

                            if (flame) {
                                entityarrow.setFire(100);
                            }

                            if (flag1) {
                                entityarrow.pickupStatus = EntityArrow.PickupStatus.CREATIVE_ONLY;
                            }

                            world.spawnEntityInWorld(entityarrow);
                        }
                    }

                    world.playSound(null, entityplayer.posX, entityplayer.posY, entityplayer.posZ, SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.NEUTRAL, 1.0F, 1.0F / (itemRand.nextFloat() * 0.4F + 1.2F) + f * 0.5F);

                    if (!flag1) {
                        --itemstack.stackSize;

                        if (itemstack.stackSize == 0) {
                            entityplayer.inventory.deleteStack(itemstack);
                        }
                    }

                    entityplayer.addStat(StatList.getObjectUseStats(this));
                }
            }
        }

//        int draw = this.getMaxItemUseDuration(stack) - timeLeft;
//
//        ArrowLooseEvent event = new ArrowLooseEvent(livingBase, stack, draw);
//        MinecraftForge.EVENT_BUS.post(event);
//        if (event.isCanceled()) {
//            return;
//        }
//        draw = event.charge;
//
//        boolean flag = livingBase.capabilities.isCreativeMode || EnchantmentHelper.getEnchantmentLevel(Enchantment.infinity.effectId, stack) > 0;
//
//        if (flag || livingBase.inventory.hasItem(Items.arrow)) {
//            float drawStrength = draw / 20.0F;
//            drawStrength = (drawStrength * drawStrength + drawStrength * 2.0F) / 3.0F;
//
//            if (drawStrength > 1.0F) {
//                drawStrength = 1.0F;
//            } else if (drawStrength < 0.1F) {
//                return;
//            }
//            int enchantPower = EnchantmentHelper.getEnchantmentLevel(Enchantment.power.effectId, stack);
//            int enchantKnockback = EnchantmentHelper.getEnchantmentLevel(Enchantment.punch.effectId, stack);
//            int enchantFire = EnchantmentHelper.getEnchantmentLevel(Enchantment.flame.effectId, stack);
//            int enchantMultishot = EnchantmentHelper.getEnchantmentLevel(CoFHEnchantment.multishot.effectId, stack);
//
//            EntityArrow arrow = new EntityArrow(world, livingBase, drawStrength * arrowSpeedMultiplier);
//            double damage = arrow.getDamage() * arrowDamageMultiplier;
//            arrow.setDamage(damage);
//
//            if (drawStrength == 1.0F) {
//                arrow.setIsCritical(true);
//            }
//            if (enchantPower > 0) {
//                arrow.setDamage(damage + enchantPower * 0.5D + 0.5D);
//            }
//            if (enchantKnockback > 0) {
//                arrow.setKnockbackStrength(enchantKnockback);
//            }
//            if (enchantFire > 0) {
//                arrow.setFire(100);
//            }
//            if (flag) {
//                arrow.canBePickedUp = 2;
//            } else {
//                livingBase.inventory.consumeInventoryItem(Items.arrow);
//            }
//            world.playSoundAtEntity(livingBase, "random.bow", 1.0F, 1.0F / (itemRand.nextFloat() * 0.4F + 1.2F) + drawStrength * 0.5F);
//
//            if (ServerHelper.isServerWorld(world)) {
//                world.spawnEntityInWorld(arrow);
//            }
//            for (int i = 0; i < enchantMultishot; i++) {
//                arrow = new EntityArrow(world, livingBase, drawStrength * arrowSpeedMultiplier);
//                arrow.setThrowableHeading(arrow.motionX, arrow.motionY, arrow.motionZ, 1.5f * drawStrength * arrowSpeedMultiplier, 3.0F);
//
//                arrow.setDamage(damage);
//
//                if (drawStrength == 1.0F) {
//                    arrow.setIsCritical(true);
//                }
//                if (enchantPower > 0) {
//                    arrow.setDamage(damage + enchantPower * 0.5D + 0.5D);
//                }
//                if (enchantKnockback > 0) {
//                    arrow.setKnockbackStrength(enchantKnockback);
//                }
//                if (enchantFire > 0) {
//                    arrow.setFire(100);
//                }
//                arrow.canBePickedUp = 2;
//
//                if (ServerHelper.isServerWorld(world)) {
//                    world.spawnEntityInWorld(arrow);
//                }
//            }
//            if (!livingBase.capabilities.isCreativeMode) {
//                stack.damageItem(1, livingBase);
//            }
//        }
    }

    //    @Override
//    public IIcon getIconIndex(ItemStack stack) {
//
//        return getIcon(stack, 0);
//    }
//
//    @Override
//    public IIcon getIcon(ItemStack stack, int pass) {
//
//        return this.normalIcons[0];
//    }
//
//    @Override
//    public IIcon getIcon(ItemStack stack, int renderPass, EntityPlayer player, ItemStack usingItem, int useRemaining) {
//
//        if (useRemaining > 0) {
//            int draw = stack.getMaxItemUseDuration() - useRemaining;
//
//            if (draw > 17) {
//                return this.normalIcons[3];
//            } else if (draw > 13) {
//                return this.normalIcons[2];
//            } else if (draw > 0) {
//                return this.normalIcons[1];
//            }
//        }
//        return this.normalIcons[0];
//    }
//
//    @Override
//    public void registerIcons(IIconRegister ir) {
//
//        this.normalIcons[0] = ir.registerIcon(this.getIconString());
//
//        for (int i = 1; i < 4; i++) {
//            this.normalIcons[i] = ir.registerIcon(this.getIconString() + "_" + (i - 1));
//        }
//    }

}
