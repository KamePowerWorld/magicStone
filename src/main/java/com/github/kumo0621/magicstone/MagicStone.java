package com.github.kumo0621.magicstone;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChatEvent;
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

import static org.bukkit.potion.PotionEffectType.*;

public final class MagicStone extends JavaPlugin implements Listener {
    private final Map<UUID, Integer> playerExpUsage = new HashMap<>();
    Random random = new Random();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPlayerChat(PlayerChatEvent event) {
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
        int lengthOfWord4 = word4.length() * 20;
        int lengthOfWord5 = word5.length();
        int cost = lengthOfWord3 * lengthOfWord4 / 40;
        // 対象の決定
        int currentExp = player.getLevel();
        // 必要な経験値が足りているかチェック
        if (currentExp >= cost) {
            // 経験値を減らす
            player.setLevel(currentExp - cost);
            if (hasConsecutiveCharacters(word1)) {
                executeMagicActions(player, lengthOfWord3, lengthOfWord4, lengthOfWord5);
            }
            if (hasConsecutiveCharacters(word2)) {
                executeMagicActions(player, lengthOfWord3, lengthOfWord4, lengthOfWord5);
            }
            if (hasConsecutiveCharacters(word3)) {
                executeMagicActions(player, lengthOfWord3, lengthOfWord4, lengthOfWord5);
            }
            if (hasConsecutiveCharacters(word4)) {
                executeMagicActions(player, lengthOfWord3, lengthOfWord4, lengthOfWord5);
            }
            if (hasConsecutiveCharacters(word5)) {
                executeMagicActions(player, lengthOfWord3, lengthOfWord4, lengthOfWord5);
            }

            switch (lengthOfWord1) {
                case 1://自分自身

                    // word2の内容に基づく追加のチェック
                    if (word2.contains("発光")) {
                        onPotionGive(player, GLOWING, lengthOfWord4, lengthOfWord3);
                        playMagicSound(player, lengthOfWord5);
                        particle(player, lengthOfWord1);
                    } else if (word2.contains("回復")) {
                        onPotionGive(player, HEAL, lengthOfWord4, lengthOfWord3);
                        playMagicSound(player, lengthOfWord5);
                        particle(player, lengthOfWord1);
                    } else if (word2.contains("削除")) {
                        clearAllPotionEffects(player);
                        playMagicSound(player, lengthOfWord5);
                        particle(player, lengthOfWord1);
                    } else if (word2.contains("満腹")) {
                        onPotionGive(player, SATURATION, lengthOfWord4, lengthOfWord3);
                        playMagicSound(player, lengthOfWord5);
                        particle(player, lengthOfWord1);
                    } else if (word2.contains("燃")) {
                        ignitePlayer(player, lengthOfWord4 / 20);
                        playMagicSound(player, lengthOfWord5);
                    } else {
                        onPotionGive(player, SLOW, lengthOfWord4, lengthOfWord3);
                        playMagicSound(player, lengthOfWord5);
                        deleteParticle(player, random.nextInt(2));
                    }
                    break;
                case 2:
                case 3://対象１体
                    if (word2.contains("発光")) {
                        onPointView(player, GLOWING, lengthOfWord4, lengthOfWord3);
                        playMagicSound(player, lengthOfWord5);
                        particle(player, lengthOfWord1);
                    } else if (word2.contains("回復")) {
                        onPointView(player, HEAL, lengthOfWord4, lengthOfWord3);
                        playMagicSound(player, lengthOfWord5);
                        particle(player, lengthOfWord1);
                    } else if (word2.contains("削除")) {
                        clearEffectsFromTargetPlayer(player);
                        playMagicSound(player, lengthOfWord5);
                        particle(player, lengthOfWord1);
                    } else if (word2.contains("追尾")) {
                        spawnHomingArrow(player, lengthOfWord3, lengthOfWord4);
                        playMagicSound(player, lengthOfWord5);
                    } else if (word2.contains("満腹")) {
                        onPointView(player, SATURATION, lengthOfWord4, lengthOfWord3);
                        playMagicSound(player, lengthOfWord5);
                        particle(player, lengthOfWord1);
                    } else if (word2.contains("怒れ")) {
                        onBeamSpawn(player, lengthOfWord3, lengthOfWord4);
                        playMagicSound(player, lengthOfWord5);
                    } else if (word2.contains("散れ")) {
                        onMegaFlare(player, lengthOfWord4 / 20, lengthOfWord3);
                        playMagicSound(player, lengthOfWord5);
                    } else if (word2.contains("燃")) {
                        igniteEntityInSight(player, lengthOfWord4);
                        playMagicSound(player, lengthOfWord5);
                    } else if (word2.contains("炎の竜巻")) {
                        summonFireTornado(player,lengthOfWord4,lengthOfWord3);
                        playMagicSound(player, lengthOfWord5);
                    } else if (word2.contains("雪氷")) {
                        castIceMagic(player,lengthOfWord3,lengthOfWord4);
                        playMagicSound(player, lengthOfWord5);
                    }else {
                        onPotionGive(player, SLOW, lengthOfWord4, lengthOfWord3);
                        playMagicSound(player, lengthOfWord5);
                        deleteParticle(player, random.nextInt(2));
                    }
                    break;
                default://複数対象
                    // それ以外の場合:
                    if (word2.contains("爆裂")) {
                        spawnTNT(player, lengthOfWord3, lengthOfWord4);
                        playMagicSound(player, lengthOfWord5);
                    } else if (word2.contains("雷神")) {
                        castLightningSpell(player, lengthOfWord4 / 20 * 2, lengthOfWord3);
                        playMagicSound(player, lengthOfWord5);
                    } else if (word2.contains("虚心")) {
                        summonZombies(player, lengthOfWord4, lengthOfWord3);
                        playMagicSound(player, lengthOfWord5);
                    } else if (word2.contains("君臨")) {
                        skeletonSpawn(player, lengthOfWord3);
                        playMagicSound(player, lengthOfWord5);
                    } else {
                        onPotionGive(player, SLOW, lengthOfWord4, lengthOfWord3);
                        playMagicSound(player, lengthOfWord5);
                        deleteParticle(player, random.nextInt(2));
                    }
                    break;
            }
        } else {
            // 経験値が不足している場合のメッセージ
            player.sendMessage(cost + "レベル足りません。別のスペルで試してください。");
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


    private void executeMagicActions(Player player, int lengthOfWord3, int lengthOfWord4, int lengthOfWord5) {
        onPotionGive(player, PotionEffectType.SLOW, lengthOfWord4, lengthOfWord3);
        playMagicSound(player, lengthOfWord5);
        deleteParticle(player, random.nextInt(2));
    }
    private void summonFireTornado(Player player,int timer,int attack) {
        new BukkitRunnable() {
            Location loc = player.getEyeLocation();
            Vector direction = loc.getDirection().normalize().multiply(0.5); // 竜巻の速度と方向
            int ticks = 0; // 竜巻の持続時間のカウンター

            @Override
            public void run() {
                if (ticks > 100) { // 竜巻の持続時間（例: 100ティック）
                    this.cancel();
                    return;
                }

                loc.add(direction);
                loc.getWorld().spawnParticle(Particle.FLAME, loc, 20, 0.3, 0.3, 0.3, 0.05);
                loc.getWorld().spawnParticle(Particle.SMOKE_NORMAL, loc, 20, 0.3, 0.3, 0.3, 0.05);

                // 竜巻の当たり判定
                for (Entity entity : loc.getWorld().getNearbyEntities(loc, 1, 1, 1)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        ((LivingEntity) entity).damage(attack); // 3ダメージ
                        entity.setFireTicks(timer); // 3秒間炎上
                    }
                }

                ticks++;
            }
        }.runTaskTimer(this, 0L, 1L); // タスクを毎ティック実行
    }

    private void ignitePlayer(Player player, int duration) {
        player.setFireTicks(duration);
    }

    private boolean hasConsecutiveCharacters(String word) {
        for (int i = 0; i < word.length() - 1; i++) {
            if (word.charAt(i) == word.charAt(i + 1)) {
                return true;
            }
        }
        return false;
    }

    private void summonZombies(Player player, int radius, int numberOfZombies) {
        Location playerLocation = player.getLocation();
        World world = player.getWorld();
        Random random = new Random();

        for (int i = 0; i < numberOfZombies; i++) {
            int dx = random.nextInt(radius * 2) - radius;
            int dz = random.nextInt(radius * 2) - radius;
            Location spawnLocation = playerLocation.clone().add(dx, 0, dz);

            Zombie zombie = (Zombie) world.spawnEntity(spawnLocation, EntityType.ZOMBIE);
            zombie.setMaxHealth(40);
            zombie.setHealth(40);
        }
    }

    private void castLightningSpell(Player player, int radius, int numberOfStrikes) {
        Location playerLocation = player.getLocation();
        World world = player.getWorld();
        Random random = new Random();

        for (int i = 0; i < numberOfStrikes; i++) {
            int dx = random.nextInt(radius * 2) - radius;
            int dz = random.nextInt(radius * 2) - radius;
            Location strikeLocation = playerLocation.clone().add(dx, 0, dz);

            world.strikeLightning(strikeLocation);
        }
    }

    public static void playMagicSound(Player player, int soundIndex) {
        if (soundIndex >= 1 && soundIndex <= magicSounds.length) {
            player.playSound(player.getLocation(), magicSounds[soundIndex - 1], 1.0f, 1.0f);
        } else {
            // 無効なインデックスの場合の処理
            player.sendMessage("Invalid sound index.");
        }
    }

    private void onPotionGive(Player player, PotionEffectType effect, int timer, int level) {
        PotionEffect potionEffect = new PotionEffect(effect, timer, level);
        player.addPotionEffect(potionEffect);
    }

    private void clearAllPotionEffects(Player player) {
        // プレイヤーが持っているすべてのポーション効果を削除
        for (PotionEffectType effectType : PotionEffectType.values()) {
            if (player.hasPotionEffect(effectType)) {
                player.removePotionEffect(effectType);
            }
        }
    }

    private void clearEffectsFromTargetPlayer(Player player) {
        Player targetPlayer = getTargetPlayer(player);
        if (targetPlayer != null) {
            clearAllPotionEffects(targetPlayer);
        }
    }

    private void spawnTNT(Player player, int lengthOfWord3, int lengthOfWord4) {
        Location location = player.getLocation();
        Random random = new Random();

        for (int i = 0; i < lengthOfWord3; i++) {
            Location tntLocation = location.clone().add(random.nextInt(41) - 20, random.nextInt(2), random.nextInt(41) - 20);
            TNTPrimed tnt = location.getWorld().spawn(tntLocation, TNTPrimed.class);
            tnt.setFuseTicks(lengthOfWord4 / 20 + 10); // TNTの導火線を設定（オプション）
        }
    }

    private void spawnHomingArrow(Player player, int lengthOfWord3, int lengthOfWord4) {
        for (int i = 0; i < lengthOfWord3; i++) {
            getServer().getScheduler().runTaskLater(this, () -> {
                Arrow arrow = player.launchProjectile(Arrow.class);
                arrow.setVelocity(player.getLocation().getDirection().multiply(2.0)); // 速度の設定
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0F, 1.0F); // 発射音

                // ホーミングタスク
                getServer().getScheduler().runTaskTimer(this, new Runnable() {
                    @Override
                    public void run() {
                        if (!arrow.isValid() || arrow.isOnGround()) {
                            arrow.remove();
                            return;
                        }

                        // パーティクルを表示
                        arrow.getWorld().spawnParticle(Particle.SPELL, arrow.getLocation(), 10, 0.3, 0.3, 0.3, 0.05);

                        // 最も近いエンティティを探す（他のプレイヤーを除外）
                        LivingEntity target = null;
                        double closestDistance = Double.MAX_VALUE;
                        for (Entity entity : arrow.getNearbyEntities(10, 10, 10)) {
                            if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                                double distance = entity.getLocation().distanceSquared(arrow.getLocation());
                                if (distance < closestDistance) {
                                    closestDistance = distance;
                                    target = (LivingEntity) entity;
                                }
                            }
                        }

                        // ターゲットに向けて矢を誘導
                        if (target != null) {
                            Vector direction = target.getLocation().add(0, 1, 0).subtract(arrow.getLocation()).toVector().normalize();
                            arrow.setVelocity(direction.multiply(1.5)); // 速度の更新
                        }
                    }
                }, 0L, 1L); // 0L: 遅延なし, 1L: 1ティックごとに更新
            }, lengthOfWord4 / 20L * i); // i秒後に矢を発射
        }

    }
    private void castIceMagic(Player player,int time ,int damage) {
        new BukkitRunnable() {
            Location loc = player.getEyeLocation();
            Vector direction = loc.getDirection().normalize().multiply(0.5); // プレイヤーの向きに合わせた方向と速度
            int ticks = 0;

            @Override
            public void run() {
                if (ticks > 60) { // 魔法の持続時間
                    this.cancel();
                    return;
                }

                loc.add(direction);
                loc.getWorld().spawnParticle(Particle.SNOW_SHOVEL, loc, 30, 0.5, 0.5, 0.5, 0.1);
                // 当たり判定
                for (Entity entity : loc.getWorld().getNearbyEntities(loc, 1, 1, 1)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        LivingEntity target = (LivingEntity) entity;
                        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 100)); // 3秒間の移動速度低下
                        target.damage((double) damage /2); // 継続ダメージ
                    }
                }

                ticks++;
            }
        }.runTaskTimer(this, 0L, 1L);
    }

    private void skeletonSpawn(Player player, int count) {
        if (player.getWorld().getEnvironment() != World.Environment.THE_END) {
            player.sendMessage("エンドにいないと使えません。");
            return;
        }
        UUID playerId = player.getUniqueId();
        playerExpUsage.putIfAbsent(playerId, 0);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || player.getWorld().getEnvironment() != World.Environment.THE_END) {
                    playerExpUsage.remove(playerId);
                    this.cancel();
                    return;
                }

                int expUsed = playerExpUsage.get(playerId) + 1;
                if (expUsed <= 30) {
                    if (player.getLevel() > 0) {
                        player.setLevel(player.getLevel() - 1);
                        playerExpUsage.put(playerId, expUsed);
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 0.5F);
                    } else {
                        player.sendMessage("経験値が不足しています。");
                        playerExpUsage.remove(playerId);
                        this.cancel();
                        return;
                    }
                }

                if (expUsed == 30) {
                    // パーティクルとサウンドを再生
                    player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 100, 1, 1, 1, 0.5);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);

                    // 視線の先にスケルトンを20体スポーン
                    Location spawnLocation = player.getTargetBlock(null, 50).getLocation().add(0, 1, 0);
                    for (int i = 0; i < count; i++) {
                        spawnLocation.getWorld().spawnEntity(spawnLocation, EntityType.SKELETON);
                    }

                    playerExpUsage.remove(playerId);
                    this.cancel();
                }
            }
        }.runTaskTimer(this, 0L, 20L); // 1秒ごとに実行
    }

    private void onBeamSpawn(Player player, int lengthOfWord3, int lengthOfWord4) {
        new BukkitRunnable() {
            Location loc = player.getEyeLocation();
            Vector direction = loc.getDirection().normalize().multiply(2); // ビームの方向と速度
            int range = lengthOfWord4 / 20; // ビームの射程

            @Override
            public void run() {
                for (int i = 0; i <= range; i++) {
                    loc.add(direction);
                    player.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, loc, 1, 0, 0, 0, 0);

                    // 当たり判定
                    for (Entity entity : loc.getWorld().getNearbyEntities(loc, 2, 2, 2)) {
                        if (entity instanceof LivingEntity && entity != player) {
                            ((LivingEntity) entity).damage(lengthOfWord3 - 6);
                            this.cancel();
                            return;
                        }
                    }

                    if (loc.getBlock().getType() != Material.AIR) {
                        this.cancel();
                        return;
                    }
                }
                this.cancel();
            }
        }.runTaskTimer(this, 0L, 1L); // タスクを毎ティック実行
    }

    private void onMegaFlare(Player player, int range, int damage) {
        Location targetLocation = player.getTargetBlock(null, range).getLocation().add(0, 2, 0); // 3ブロック上の位置

        // ファイヤーチャージのエフェクトとサウンドを再生
        player.getWorld().playSound(targetLocation, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0F, 1.0F);
        player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, targetLocation, 100, 0.5, 0.5, 0.5, 0.1);

        new BukkitRunnable() {
            @Override
            public void run() {
                // 着地点近くのエンティティにダメージを与える
                for (Entity entity : targetLocation.getWorld().getNearbyEntities(targetLocation, 5, 5, 5)) {
                    if (entity instanceof LivingEntity) {
                        ((LivingEntity) entity).damage(damage);
                    }
                }
                // 爆発のパーティクルを表示
                targetLocation.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, targetLocation, 10, 0.5, 0.5, 0.5, 0.1);
            }
        }.runTaskLater(this, 20L); // 1秒後に爆発
    }

    private void particle(Player player, int count) {
        switch (count) {
            case 1:
                player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);
                break;
            case 2:
                player.getWorld().spawnParticle(Particle.SPELL_WITCH, player.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);
                break;
            default:
                player.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, player.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);
                break;
        }
    }

    private void deleteParticle(Player player, int count) {
        switch (count) {
            case 0:
                player.getWorld().spawnParticle(Particle.SMOKE_NORMAL, player.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);
                break;
            case 1:
                player.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, player.getLocation(), 10, 1.0, 1.0, 1.0, 0.1);
                break;
            default:
                player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
                break;
        }
    }

    private void onPointView(Player player, PotionEffectType effect, int timer, int level) {
        LivingEntity target = getTargetEntity(player);
        if (target != null) {
            PotionEffect potionEffect = new PotionEffect(effect, timer * 20, level);
            target.addPotionEffect(potionEffect);
        }
    }

    private LivingEntity getTargetEntity(Player player) {
        List<Entity> nearbyEntities = player.getNearbyEntities(50, 50, 50); // 距離は必要に応じて調整
        Vector playerDirection = player.getLocation().getDirection().normalize();

        for (Entity entity : nearbyEntities) {
            if (entity instanceof LivingEntity) {
                Vector toEntity = entity.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
                double dot = toEntity.dot(playerDirection);

                if (dot > 0.98) { // この値は必要に応じて調整（1に近いほど正確）
                    return (LivingEntity) entity;
                }
            }
        }
        return null;
    }

    private Player getTargetPlayer(Player player) {
        List<Entity> nearbyEntities = player.getNearbyEntities(50, 50, 50); // 距離は必要に応じて調整
        Vector playerDirection = player.getLocation().getDirection().normalize();

        for (Entity entity : nearbyEntities) {
            if (entity instanceof Player && entity != player) {
                Vector toEntity = entity.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
                double dot = toEntity.dot(playerDirection);

                if (dot > 0.98) { // この値は必要に応じて調整（1に近いほど正確）
                    return (Player) entity;
                }
            }
        }
        return null;
    }

    private void igniteEntityInSight(Player player, int duration) {
        LivingEntity target = getTargetEntity(player);
        target.setFireTicks(duration);

    }
}
