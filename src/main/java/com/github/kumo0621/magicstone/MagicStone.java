package com.github.kumo0621.magicstone;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.*;

import static org.bukkit.potion.PotionEffectType.GLOWING;

public final class MagicStone extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        String[] words = message.split("、", 5);

        String word1 = words.length > 0 ? words[0] : "";
        String word2 = words.length > 1 ? words[1] : "";
        String word3 = words.length > 2 ? words[2] : "";
        String word4 = words.length > 3 ? words[3] : "";
        String word5 = words.length > 4 ? words[4] : "";

        getLogger().info("Word 1: " + word1);//対象
        getLogger().info("Word 2: " + word2);//発動効果
        getLogger().info("Word 3: " + word3);//威力
        getLogger().info("Word 4: " + word4);//効果時間
        getLogger().info("Word 5: " + word5);//効果音
        int lengthOfWord1 = word1.length();
        int lengthOfWord3 = word3.length();
        int lengthOfWord4 = word4.length()*20;
        int lengthOfWord5 = word5.length();
        // 対象の決定
        switch (lengthOfWord1) {
            case 1:
            case 2:
                // word2の内容に基づく追加のチェック
                if (word2.contains("発光")) {
                    PotionEffect potionEffect = new PotionEffect(PotionEffectType.GLOWING, lengthOfWord4, lengthOfWord3);

                    // メインスレッドでポーション効果を適用
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.addPotionEffect(potionEffect);
                        }
                    }.runTask(this);
                    playMagicSound(player, lengthOfWord5);
                } else {

                }
                break;
            case 3:

                break;
            default:
                // それ以外の場合: 範囲魔法
                break;
        }
    }

    private static final Sound[] magicSounds = {
            Sound.BLOCK_ENCHANTMENT_TABLE_USE,      // 1
            Sound.ENTITY_ELDER_GUARDIAN_CURSE,      // 2
            Sound.ENTITY_ENDER_DRAGON_GROWL,        // 3
            Sound.ENTITY_EVOKER_CAST_SPELL,         // 4
            Sound.ENTITY_EVOKER_PREPARE_ATTACK,     // 5
            Sound.ENTITY_ILLUSIONER_CAST_SPELL,     // 6
            Sound.ENTITY_ILLUSIONER_MIRROR_MOVE,    // 7
            Sound.ENTITY_WITCH_CELEBRATE,           // 8
            Sound.BLOCK_PORTAL_TRAVEL,              // 9
            Sound.BLOCK_PORTAL_AMBIENT,             // 10
            Sound.BLOCK_END_PORTAL_FRAME_FILL,      // 11
            Sound.BLOCK_BREWING_STAND_BREW,         // 12
            Sound.ENTITY_WITCH_THROW,               // 13
            Sound.ENTITY_WITCH_DRINK,               // 14
            Sound.ENTITY_WITHER_SHOOT,              // 15
            Sound.ENTITY_ZOMBIE_VILLAGER_CURE,      // 16
            Sound.ENTITY_GHAST_SHOOT,               // 17
            Sound.ENTITY_GHAST_WARN,                // 18
            Sound.ENTITY_BLAZE_SHOOT,               // 19
            Sound.ENTITY_ENDER_EYE_LAUNCH           // 20
    };

    public static void playMagicSound(Player player, int soundIndex) {
        if (soundIndex >= 1 && soundIndex <= magicSounds.length) {
            player.playSound(player.getLocation(), magicSounds[soundIndex - 1], 1.0f, 1.0f);
        } else {
            // 無効なインデックスの場合の処理
            player.sendMessage("Invalid sound index.");
        }
    }

}

