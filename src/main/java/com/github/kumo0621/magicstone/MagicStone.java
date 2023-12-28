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

public final class MagicStone extends JavaPlugin implements Listener {

    private HashMap<UUID, Long> cooldowns;
    private final Map<UUID, Integer> playerExpUsage = new HashMap<>();
    private final Map<UUID, String> playerMessages = new HashMap<>();
    private final Map<UUID, Location> playerLocations = new HashMap<>();

    @Override
    public void onEnable() {
        cooldowns = new HashMap<>();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPlayerUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        // クールダウンをチェック
        if (cooldowns.containsKey(player.getUniqueId()) && cooldowns.get(player.getUniqueId()) > System.currentTimeMillis()) {
            return; // クールダウン中は何もしない
        }
        Team team = player.getScoreboard().getEntryTeam(player.getName());
        if (team != null && team.getName().equals("冒険者")) {
            // プレイヤーが赤色の染料を持っているか確認
            if (player.getInventory().getItemInMainHand().getType() == Material.RED_DYE) {

                // エフェクトとサウンドを再生
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0F, 1.0F);
                player.getWorld().spawnParticle(Particle.SPELL_WITCH, player.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);

                // 5メートル以内の非プレイヤーエンティティを燃やす
                for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
                    if (entity instanceof LivingEntity && entity.getType() != EntityType.PLAYER) {
                        LivingEntity livingEntity = (LivingEntity) entity;
                        livingEntity.setFireTicks(100); // 5秒間燃やす
                    }
                }
                // 手に持っているアイテムの数量を1減らす
                ItemStack itemInHand = player.getInventory().getItemInMainHand();
                if (itemInHand.getAmount() > 1) {
                    itemInHand.setAmount(itemInHand.getAmount() - 1);
                    player.getInventory().setItemInMainHand(itemInHand);
                } else {
                    // アイテムが1個の場合は削除する
                    player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                }
            }
            // プレイヤーが白色の染料を持っているか確認
            if (player.getInventory().getItemInMainHand().getType() == Material.WHITE_DYE) {

                // エフェクトとサウンドを再生
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0F, 1.0F);
                player.getWorld().spawnParticle(Particle.HEART, player.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);

                // 自分自身と5メートル以内のプレイヤーに自動回復効果を付与
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 4)); // レベル5の自動回復
                for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
                    if (entity instanceof Player) {
                        Player nearbyPlayer = (Player) entity;
                        nearbyPlayer.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 4)); // レベル5の自動回復
                    }
                }
                // 手に持っているアイテムの数量を1減らす
                ItemStack itemInHand = player.getInventory().getItemInMainHand();
                if (itemInHand.getAmount() > 1) {
                    itemInHand.setAmount(itemInHand.getAmount() - 1);
                    player.getInventory().setItemInMainHand(itemInHand);
                } else {
                    // アイテムが1個の場合は削除する
                    player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                }
            }
            if (player.getInventory().getItemInMainHand().getType() == Material.YELLOW_DYE) {

                // 視線の方向を取得
                Vector direction = player.getEyeLocation().getDirection();
                Entity closestEntity = null;
                double closestDistance = Double.MAX_VALUE;
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
                player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);
                // プレイヤーの周囲のエンティティを取得
                for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), 50, 50, 50)) {
                    if (entity instanceof LivingEntity && !entity.equals(player)) {
                        Vector toEntity = entity.getLocation().toVector().subtract(player.getLocation().toVector());
                        double angle = direction.angle(toEntity);
                        double distance = toEntity.length();

                        // 視線の方向に最も近いエンティティを見つける
                        if (angle < Math.toRadians(30) && distance < closestDistance) { // 30度以内
                            closestDistance = distance;
                            closestEntity = entity;
                        }
                    }
                }

                // 視線の方向にいる最も近いエンティティにグローイング効果を付与
                if (closestEntity != null) {
                    ((LivingEntity) closestEntity).addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 200, 0)); // 200ティック（10秒）のグローイング効果
                }
                // 手に持っているアイテムの数量を1減らす
                ItemStack itemInHand = player.getInventory().getItemInMainHand();
                if (itemInHand.getAmount() > 1) {
                    itemInHand.setAmount(itemInHand.getAmount() - 1);
                    player.getInventory().setItemInMainHand(itemInHand);
                } else {
                    // アイテムが1個の場合は削除する
                    player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                }
            }              // プレイヤーが灰色の染料を持っているか確認
            if (player.getInventory().getItemInMainHand().getType() == Material.GRAY_DYE) {
                // 矢を5回発射する
                for (int i = 0; i < 5; i++) {
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
                    }, 5L * i); // i秒後に矢を発射
                }

                // 手に持っているアイテムの数量を1減らす
                ItemStack itemInHand = player.getInventory().getItemInMainHand();
                if (itemInHand.getAmount() > 1) {
                    itemInHand.setAmount(itemInHand.getAmount() - 1);
                    player.getInventory().setItemInMainHand(itemInHand);
                } else {
                    // アイテムが1個の場合は削除する
                    player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                }
            }
            if (player.getInventory().getItemInMainHand().getType() == Material.LIGHT_GRAY_DYE) {
                Location targetLocation = player.getTargetBlock(null, 10).getLocation().add(0, 3, 0); // 3ブロック上の位置

                // ファイヤーチャージのエフェクトとサウンドを再生
                player.getWorld().playSound(targetLocation, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0F, 1.0F);
                player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, targetLocation, 100, 0.5, 0.5, 0.5, 0.1);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        // 着地点近くのエンティティにダメージを与える
                        for (Entity entity : targetLocation.getWorld().getNearbyEntities(targetLocation, 2, 2, 2)) {
                            if (entity instanceof LivingEntity && entity != player) {
                                ((LivingEntity) entity).damage(15);
                            }
                        }
                        // 爆発のパーティクルを表示
                        targetLocation.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, targetLocation, 10, 0.5, 0.5, 0.5, 0.1);
                    }
                }.runTaskLater(this, 20L); // 1秒後に爆発
                // 手に持っているアイテムの数量を1減らす
                ItemStack itemInHand = player.getInventory().getItemInMainHand();
                if (itemInHand.getAmount() > 1) {
                    itemInHand.setAmount(itemInHand.getAmount() - 1);
                    player.getInventory().setItemInMainHand(itemInHand);
                } else {
                    // アイテムが1個の場合は削除する
                    player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                }
            }
            // プレイヤーが黒色の染料を持っているか確認
            if (player.getInventory().getItemInMainHand().getType() == Material.BLACK_DYE) {
                // 満腹度を最大まで回復
                player.setFoodLevel(20);

                // 体力を少し回復
                player.setHealth(Math.min(player.getHealth() + 1.0, player.getMaxHealth()));

                // パーティクルとサウンドを再生
                player.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, player.getLocation(), 10, 0.5, 0.5, 0.5, 0.1);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1.0F, 1.0F);
                // 手に持っているアイテムの数量を1減らす
                ItemStack itemInHand = player.getInventory().getItemInMainHand();
                if (itemInHand.getAmount() > 1) {
                    itemInHand.setAmount(itemInHand.getAmount() - 1);
                    player.getInventory().setItemInMainHand(itemInHand);
                } else {
                    // アイテムが1個の場合は削除する
                    player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                }
            }
            // プレイヤーが緑色の染料を持っているか確認
            if (player.getInventory().getItemInMainHand().getType() == Material.GREEN_DYE) {
                // 目立つパーティクルを再生
                player.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, player.getLocation(), 30, 2.5, 0.5, 2.5, 0.01);
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GRASS_PLACE, 1.0F, 1.0F);

                // 5メートル以内のエンティティに移動速度低下効果を付与
                for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
                    if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                        ((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 4)); // 5秒間の移動速度低下
                    }
                }
                // 手に持っているアイテムの数量を1減らす
                ItemStack itemInHand = player.getInventory().getItemInMainHand();
                if (itemInHand.getAmount() > 1) {
                    itemInHand.setAmount(itemInHand.getAmount() - 1);
                    player.getInventory().setItemInMainHand(itemInHand);
                } else {
                    // アイテムが1個の場合は削除する
                    player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                }
            }
            if (player.getInventory().getItemInMainHand().getType() == Material.PURPLE_DYE) {
                // プレイヤーがエンドにいるか確認
                UUID playerId = player.getUniqueId();
                if (player.getWorld().getEnvironment() != World.Environment.THE_END) {
                    player.sendMessage("エンドにいないと使えません。");
                    return;
                }

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
                            for (int i = 0; i < 20; i++) {
                                spawnLocation.getWorld().spawnEntity(spawnLocation, EntityType.SKELETON);
                            }

                            playerExpUsage.remove(playerId);
                            this.cancel();
                        }
                    }
                }.runTaskTimer(this, 0L, 20L); // 1秒ごとに実行
                ItemStack itemInHand = player.getInventory().getItemInMainHand();
                if (itemInHand.getAmount() > 1) {
                    itemInHand.setAmount(itemInHand.getAmount() - 1);
                    player.getInventory().setItemInMainHand(itemInHand);
                } else {
                    // アイテムが1個の場合は削除する
                    player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                }
            }
            if (player.getInventory().getItemInMainHand().getType() == Material.BLUE_DYE) {
                if (player.getLevel() < 30) {
                    player.sendMessage("経験値が不足しています。");
                    return;
                }

                // 経験値を30減らす

                UUID playerId = player.getUniqueId();
                if (player.getWorld().getEnvironment() != World.Environment.THE_END) {
                    player.sendMessage("エンドにいないと使えません。");
                    return;
                }
                player.setLevel(player.getLevel() - 30);
                String[] messages = {"空蝉に忍び寄る叛逆の摩天楼。我が前に訪れた静寂なる神雷。時は来た！今、眠りから目覚め、我が狂気を以て現界せよ！穿て！エクスプロージョン！", "光に覆われし漆黒よ。夜を纏いし爆炎よ。紅魔の名のもとに原初の崩壊を顕現す。終焉の王国の地に、力の根源を隠匿せし者。我が前に統べよ！エクスプロージョン！", "最高最強にして最大の魔法、爆裂魔法の使い手、我が名はめぐみん。我に許されし一撃は同胞の愛にも似た盲目を奏で、塑性を脆性へと葬り去る。強き鼓動を享受する！哀れな獣よ、紅き黒炎と同調し、血潮となりて償いたまえ！穿て！エクスプロージョン！"};
                String randomMessage = messages[new Random().nextInt(messages.length)];

                player.sendMessage("次のメッセージをチャットで入力してください: " + randomMessage);
                playerMessages.put(player.getUniqueId(), randomMessage);
                playerLocations.put(player.getUniqueId(), player.getLocation());
                ItemStack itemInHand = player.getInventory().getItemInMainHand();
                if (itemInHand.getAmount() > 1) {
                    itemInHand.setAmount(itemInHand.getAmount() - 1);
                    player.getInventory().setItemInMainHand(itemInHand);
                } else {
                    // アイテムが1個の場合は削除する
                    player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                }
            }
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (3 * 1000)); // 5秒後
        }
    }


    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Arrow) {
            Arrow arrow = (Arrow) event.getDamager();
            if (arrow.getShooter() instanceof Player) {
                // 矢がエンティティに当たったときに10のダメージを与える
                event.setDamage(5.0);
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (playerMessages.containsKey(player.getUniqueId())) {
            Location initialLocation = playerLocations.get(player.getUniqueId());
            if (initialLocation.distance(player.getLocation()) > 2.0) {
                playerMessages.remove(player.getUniqueId());
                playerLocations.remove(player.getUniqueId());
                player.sendMessage("動きすぎてイベントがキャンセルされました。");
            }
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        if (playerMessages.containsKey(player.getUniqueId()) && playerMessages.get(player.getUniqueId()).equals(message)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    spawnTNT(player);
                }
            }.runTask(this); // 非同期から同期タスクに切り替える
            player.sendMessage("TNTがスポーンしました！");
            playerMessages.remove(player.getUniqueId());
            playerLocations.remove(player.getUniqueId());
        } else {
            playerMessages.remove(player.getUniqueId());
            playerLocations.remove(player.getUniqueId());
        }
    }

    private void spawnTNT(Player player) {
        Location location = player.getLocation();
        Random random = new Random();

        for (int i = 0; i < 90; i++) {
            Location tntLocation = location.clone().add(random.nextInt(41) - 20, 1, random.nextInt(41) - 20);
            TNTPrimed tnt = location.getWorld().spawn(tntLocation, TNTPrimed.class);
            tnt.setFuseTicks(80); // TNTの導火線を設定（オプション）
        }
    }
}

