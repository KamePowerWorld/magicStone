package com.github.kumo0621.magicstone;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.*;

import static org.bukkit.potion.PotionEffectType.*;

public final class MagicStone extends JavaPlugin implements Listener {
    private final Map<UUID, Integer> playerExpUsage = new HashMap<>();
    Random random = new Random();
    private Map<UUID, EnderCrystal> playerCrystals = new HashMap<>();
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            UUID playerId = player.getUniqueId();

            if (playerCrystals.containsKey(playerId)) {
                EnderCrystal crystal = playerCrystals.get(playerId);
                if (crystal != null && !crystal.isDead()) {
                    event.setCancelled(true); // ダメージをキャンセル
                    crystal.remove(); // クリスタルを削除
                    playerCrystals.remove(playerId);
                }
            }
        }
    }
    @EventHandler
    public void onPlayerChat(PlayerChatEvent event) {

        Player player = event.getPlayer();
        String message = event.getMessage();
        if (message.startsWith("@")) {
            event.setCancelled(true);

            String[] words = message.split("、", 5);
            String target = words.length > 0 ? words[0] : "";
            String magicType = words.length > 1 ? words[1] : "";
            String damage = words.length > 2 ? words[2] : "";
            String time = words.length > 3 ? words[3] : "";
            String particleType = words.length > 4 ? words[4] : "";

            int lengthOfWord1 = target.length();
            int lengthOfWord2 = magicType.length();
            int lengthOfWord3 = damage.length();
            int lengthOfWord4 = time.length() * 20;
            int lengthOfWord5 = particleType.length();
            int cost = lengthOfWord3 * lengthOfWord4 / 40;
            // 対象の決定
            int currentExp = player.getLevel();
            // 必要な経験値が足りているかチェック
            if (currentExp >= cost) {
                if (ConsecutiveCharacters(target) || ConsecutiveCharacters(magicType)
                        || ConsecutiveCharacters(damage) || ConsecutiveCharacters(time)
                        || ConsecutiveCharacters(particleType)) {

                    player.sendMessage("メッセージに不正な形式が含まれています。");
                    return; // 連続する文字が含まれているため、処理を中止
                }
                if (hasConsecutiveCharacters(target)) {
                    executeMagicActions(player, lengthOfWord3, lengthOfWord4, lengthOfWord5);
                } else if (hasConsecutiveCharacters(magicType)) {
                    executeMagicActions(player, lengthOfWord3, lengthOfWord4, lengthOfWord5);
                } else if (hasConsecutiveCharacters(damage)) {
                    executeMagicActions(player, lengthOfWord3, lengthOfWord4, lengthOfWord5);
                } else if (hasConsecutiveCharacters(time)) {
                    executeMagicActions(player, lengthOfWord3, lengthOfWord4, lengthOfWord5);
                } else if (hasConsecutiveCharacters(particleType)) {
                    executeMagicActions(player, lengthOfWord3, lengthOfWord4, lengthOfWord5);
                } else {
                    switch (lengthOfWord1) {
                        case 1://自分自身
                        case 2:
                            switch (lengthOfWord2) {
                                case 1://自分を光らせる魔法
                                case 2:
                                    onPotionGive(player, GLOWING, lengthOfWord4, lengthOfWord3);
                                    playMagicSound(player, lengthOfWord5);
                                    particle(player, lengthOfWord1);
                                    break;
                                case 3://自分を回復させる
                                case 4:
                                    onPotionGive(player, HEAL, lengthOfWord4, lengthOfWord3);
                                    playMagicSound(player, lengthOfWord5);
                                    particle(player, lengthOfWord1);
                                    break;
                                case 5://自分のエフェクトを解除する
                                case 6:
                                    clearAllPotionEffects(player);
                                    playMagicSound(player, lengthOfWord5);
                                    particle(player, lengthOfWord1);
                                    break;
                                case 7://満腹度回復
                                case 8:
                                    onPotionGive(player, SATURATION, lengthOfWord4, lengthOfWord3);
                                    playMagicSound(player, lengthOfWord5);
                                    particle(player, lengthOfWord1);
                                    break;
                                case 9://自分にどく
                                case 10:
                                    onPotionGive(player, POISON, lengthOfWord4, lengthOfWord3);
                                    playMagicSound(player, lengthOfWord5);
                                    particle(player, lengthOfWord1);
                                    break;
                                case 11://雨を降らす
                                case 12:
                                    castRainSpell(player);
                                    playMagicSound(player, lengthOfWord5);
                                    particle(player, lengthOfWord1);
                                    break;
                                case 13://かめすたを呼び出す
                                case 14:
                                    String response = ChatColor.GOLD + "<システム> プログラミングの先生かめすたを呼び出した。" +
                                            "\nしかし、かめすたは忙しかった。直接話しかけてみよう。";
                                    event.getPlayer().sendMessage(response);
                                    playMagicSound(player, lengthOfWord5);
                                    particle(player, lengthOfWord1);
                                    break;
                                case 15://2秒無敵
                                case 16:
                                    onPointView(player, PotionEffectType.DAMAGE_RESISTANCE, 2, 10); // 例: 耐性効果を適用
                                    playMagicSound(player, lengthOfWord5);
                                    particle(player, lengthOfWord1);
                                    break;
                                case 17://肩代わりクリスタル
                                case 18:
                                    summonEnderCrystal(player);
                                    playMagicSound(player, lengthOfWord5);
                                    particle(player, lengthOfWord1);
                                    break;
                                case 19://自分を燃やす
                                case 20:
                                    ignitePlayer(player, lengthOfWord4 / 20);
                                    playMagicSound(player, lengthOfWord5);
                                    particle(player, lengthOfWord1);
                                    break;
                                case 21://骨をつける
                                case 22:
                                    equipBoneHelmet(player);
                                    playMagicSound(player, lengthOfWord5);
                                    particle(player, lengthOfWord1);
                                    break;
                                case 23://暗視をつける
                                case 24:
                                    onPointView(player, NIGHT_VISION, lengthOfWord4, lengthOfWord3); // 例: 耐性効果を適用
                                    playMagicSound(player, lengthOfWord5);
                                    particle(player, lengthOfWord1);
                                    break;
                                case 25://2秒無敵
                                case 26:
                                    onPointView(player, PotionEffectType.BLINDNESS, 3, lengthOfWord3); // 例: 耐性効果を適用
                                    playMagicSound(player, lengthOfWord5);
                                    particle(player, lengthOfWord1);
                                    break;
                                default://高速移動
                                    warpPlayer(player, lengthOfWord3);
                                    playMagicSound(player, lengthOfWord5);
                                    particle(player, lengthOfWord1);
                                    break;
                            }break;
                        case 3://対象１体
                            switch (lengthOfWord2) {
                                case 1://相手をひとり光らせる
                                case 2:
                                    onPointView(player, GLOWING, lengthOfWord4, lengthOfWord3);
                                    playMagicSound(player, lengthOfWord5);
                                    particle(player, lengthOfWord1);
                                    break;
                                case 3://相手を一人光らせる
                                case 4:
                                    onPointView(player, HEAL, lengthOfWord4, lengthOfWord3);
                                    playMagicSound(player, lengthOfWord5);
                                    particle(player, lengthOfWord1);
                                    break;
                                case 5://相手一人のエフェクトを解除する
                                case 6:
                                    clearEffectsFromTargetPlayer(player);
                                    playMagicSound(player, lengthOfWord5);
                                    particle(player, lengthOfWord1);
                                    break;
                                case 7://追尾する矢を召喚する
                                case 8:
                                    spawnHomingArrow(player, lengthOfWord3, lengthOfWord4);
                                    playMagicSound(player, lengthOfWord5);
                                    break;
                                case 9://相手一人の満腹度を回復する
                                case 10:
                                    onPointView(player, SATURATION, lengthOfWord4, lengthOfWord3);
                                    playMagicSound(player, lengthOfWord5);
                                    particle(player, lengthOfWord1);
                                    break;
                                case 11://正面にビームを発射する
                                case 12:
                                    onBeamSpawn(player, lengthOfWord3, lengthOfWord4);
                                    playMagicSound(player, lengthOfWord5);
                                    break;
                                case 13://隕石を落とす
                                case 14:
                                    onMegaFlare(player, lengthOfWord4 / 20, lengthOfWord3);
                                    playMagicSound(player, lengthOfWord5);
                                    break;
                                case 15://相手を燃やす魔法
                                case 16:
                                    igniteEntityInSight(player, lengthOfWord4);
                                    playMagicSound(player, lengthOfWord5);
                                    break;
                                case 17://炎の竜巻
                                case 18:
                                    summonFireTornado(player, lengthOfWord4 / 20, lengthOfWord3);
                                    playMagicSound(player, lengthOfWord5);
                                    break;
                                case 19://エンダードラゴン発信
                                case 20:
                                    summonEnderDragon(player, lengthOfWord4);
                                    playMagicSound(player, lengthOfWord5);
                                    particle(player, lengthOfWord1);
                                    break;
                                case 21://斬撃
                                case 22:
                                    castSanGekiMagic(player, lengthOfWord4, lengthOfWord3);
                                    playMagicSound(player, lengthOfWord5);
                                    particle(player, lengthOfWord1);
                                    break;
                                case 23://ホーミングTNT
                                case 24:
                                    spawnHomingTNT(player, lengthOfWord3, lengthOfWord4);
                                    playMagicSound(player, lengthOfWord5);
                                    particle(player, lengthOfWord1);
                                    break;
                                case 25://きゅうあゆう
                                case 26:
                                    castVampiricSpell(player, lengthOfWord3, lengthOfWord4);
                                    playMagicSound(player, lengthOfWord5);
                                    particle(player, lengthOfWord1);
                                    break;
                                case 27://高速で矢を発射する
                                case 28:
                                    shootPowerfulArrow(player, lengthOfWord4);
                                    playMagicSound(player, lengthOfWord5);
                                    particle(player, lengthOfWord1);
                                    break;
                                case 29://高速で矢を発射する
                                case 30:
                                    castWaterSplashSpell(player);
                                    playMagicSound(player, lengthOfWord5);
                                    particle(player, lengthOfWord1);
                                    break;
                                default://雪の吹雪の魔法。雪氷
                                    castIceMagic(player, lengthOfWord4 / 20, lengthOfWord3);
                                    playMagicSound(player, lengthOfWord5);
                                    break;

                            }break;
                        default://複数対象
                            // それ以外の場合:
                            switch (lengthOfWord2) {
                                case 1://広範囲に爆発を行う
                                case 2:
                                    spawnTNT(player, lengthOfWord3, lengthOfWord4);
                                    playMagicSound(player, lengthOfWord5);
                                    particle(player, lengthOfWord1);
                                    break;
                                case 3://広範囲に落雷を落とす
                                case 4:
                                    castLightningSpell(player, lengthOfWord4 / 20 * 2, lengthOfWord3);
                                    playMagicSound(player, lengthOfWord5);
                                    particle(player, lengthOfWord1);
                                    break;
                                case 5://広範囲にゾンビをわかせる
                                case 6:
                                    summonZombies(player, lengthOfWord4, lengthOfWord3);
                                    playMagicSound(player, lengthOfWord5);
                                    particle(player, lengthOfWord1);
                                    break;
                                case 7://お花をわかせる
                                case 8:
                                    castFlowerSpell(player, lengthOfWord3, lengthOfWord4);
                                    playMagicSound(player, lengthOfWord5);
                                    particle(player, lengthOfWord1);
                                    break;
                                case 9://爆発トラップ
                                case 10:
                                    createExplosionAtSight(player, lengthOfWord4, lengthOfWord3);
                                    playMagicSound(player, lengthOfWord5);
                                    particle(player, lengthOfWord1);
                                    break;
                                case 11://周りに水スプラッシュを出す
                                case 12:
                                    castRandomSplashSpell(player,lengthOfWord3,lengthOfWord4);
                                    playMagicSound(player, lengthOfWord5);
                                    particle(player, lengthOfWord1);
                                    break;
                                default://広範囲にスケルトンをわかせる
                                    skeletonSpawn(player, lengthOfWord3);
                                    playMagicSound(player, lengthOfWord5);
                                    particle(player, lengthOfWord1);
                                    break;

                            }break;
                    }
                    // 経験値を減らす
                    player.setLevel(currentExp - cost);
                    summonArmorStand(player,message);
                }
            } else {
                // 経験値が不足している場合のメッセージ
                player.sendMessage(cost + "レベル必要です。別のスペルで試してください。");
            }
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

    private boolean ConsecutiveCharacters(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }

        char lastChar = str.charAt(0);
        for (int i = 1; i < str.length(); i++) {
            char currentChar = str.charAt(i);
            if (currentChar == lastChar + 1 || currentChar == lastChar - 1) {
                return true; // 連続する文字が見つかった
            }
            lastChar = currentChar;
        }
        return false; // 連続する文字が見つからなかった
    }
    private void summonArmorStand(Player player, String text) {
        String processedText = text.substring(1).replace("、", "");

        Location location = player.getLocation().add(0, 0.1, 0); // プレイヤーの頭上
        ArmorStand armorStand = location.getWorld().spawn(location, ArmorStand.class);

        armorStand.setVisible(false); // 防具立てを見えなくする
        armorStand.setGravity(false); // 重力の影響を受けないようにする
        armorStand.setCustomName(processedText); // 処理された名前を設定
        armorStand.setCustomNameVisible(true); // 名前を表示

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks > 60) { // 3秒後（60ティック）
                    armorStand.remove(); // 防具立てを消去
                    this.cancel();
                    return;
                }

                // 防具立ての位置を徐々に上昇させる
                armorStand.teleport(armorStand.getLocation().add(0, 0.05, 0)); // 少しずつ上に移動
                ticks++;
            }
        }.runTaskTimer(this, 0L, 1L); // ティックごとにタスクを実行
    }
    private void equipBoneHelmet(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack helmet = new ItemStack(Material.BONE);

        // 既に頭に装備しているアイテムを取得
        ItemStack currentHelmet = inventory.getHelmet();

        // 既に頭に何かを装備している場合、そのアイテムを落とす
        if (currentHelmet != null && currentHelmet.getType() != Material.AIR) {
            player.getWorld().dropItemNaturally(player.getLocation(), currentHelmet);
        }

        // 骨を頭に装備
        inventory.setHelmet(helmet);
    }
    private void castWaterSplashSpell(Player player) {
        ItemStack potionItem = new ItemStack(Material.SPLASH_POTION);
        PotionMeta meta = (PotionMeta) potionItem.getItemMeta();
        meta.setBasePotionData(new PotionData(PotionType.WATER));
        potionItem.setItemMeta(meta);

        ThrownPotion thrownPotion = player.getWorld().spawn(player.getEyeLocation(), ThrownPotion.class);
        thrownPotion.setItem(potionItem);
        thrownPotion.setVelocity(player.getLocation().getDirection().multiply(1.5));
    }
    private void shootPowerfulArrow(Player player,int time) {
        Arrow arrow = player.launchProjectile(Arrow.class);
        Vector direction = player.getLocation().getDirection();
        arrow.setVelocity(direction.multiply(time)); // 高速で発射
        arrow.setDamage((double) time /20*1.25); // 威力を設定
    }
    private void castRandomSplashSpell(Player player, int count, int time) {
        for (int i = 0; i < count; i++) {
            getServer().getScheduler().runTaskLater(this, () -> {
                Location randomLocation = player.getLocation().clone().add(
                        random.nextInt(10) - 5, // X軸
                        random.nextInt(3),     // Y軸
                        random.nextInt(10) - 5 // Z軸
                );

                ItemStack potionItem = new ItemStack(Material.SPLASH_POTION);
                PotionMeta meta = (PotionMeta) potionItem.getItemMeta();
                meta.setBasePotionData(new PotionData(PotionType.WATER));
                potionItem.setItemMeta(meta);

                ThrownPotion thrownPotion = player.getWorld().spawn(randomLocation, ThrownPotion.class);
                thrownPotion.setItem(potionItem);
                thrownPotion.setVelocity(new Vector(0, 0.5, 0)); // 上方向に投擲
            }, random.nextInt(time)); // ランダムな遅延（最大3秒）
        }
    }
    private void warpPlayer(Player player, int power) {
        Location location = player.getLocation();
        Vector direction = location.getDirection().setY(0).normalize(); // Y軸成分を0にして平面上での方向を取得
        Location forwardLocation = location.clone().add(direction.multiply(2)); // 2マス前の位置

        // 正面にブロックがない場合にワープ
        if (forwardLocation.getBlock().getType() == Material.AIR) {
            player.teleport(forwardLocation);

            // 威力に応じて四方に動く
            switch (power) {
                case 1: // 北
                    player.teleport(player.getLocation().add(0, 0, -1));
                    break;
                case 2: // 東
                    player.teleport(player.getLocation().add(1, 0, 0));
                    break;
                case 3: // 南
                    player.teleport(player.getLocation().add(0, 0, 1));
                    break;
                default: // 西
                    player.teleport(player.getLocation().add(-1, 0, 0));
                    break;
            }
        }
    }
    private void summonEnderCrystal(Player player) {
        if (playerCrystals.containsKey(player.getUniqueId())) {
            player.sendMessage("すでにエンドクリスタルを持っています。");
            return;
        }

        Location spawnLocation = player.getLocation().add(0, 1, 0);
        EnderCrystal crystal = player.getWorld().spawn(spawnLocation, EnderCrystal.class);
        playerCrystals.put(player.getUniqueId(), crystal);
    }
    private void createExplosionAtSight(Player player,int time,int coalTime) {
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection();
        Location targetLocation = eyeLocation.add(direction.multiply(5)); // 視点から一定距離の位置

        new BukkitRunnable() {
            int count = 0;

            @Override
            public void run() {
                if (count >= time/20) { // 5秒 x 12 = 60秒
                    this.cancel();
                    return;
                }

                player.getWorld().createExplosion(targetLocation, coalTime); // 爆発を発生させる
                count++;
            }
        }.runTaskTimer(this, 0L, 100L); // 100ティック（5秒）ごとに実行
    }
    private void summonEnderDragon(Player player,int time) {
        Location spawnLocation = player.getLocation().add(player.getLocation().getDirection().multiply(10));
        EnderDragon dragon = (EnderDragon) player.getWorld().spawnEntity(spawnLocation, EntityType.ENDER_DRAGON);

        // ドラゴンをプレイヤーの視線方向に向かわせる
        Vector direction = player.getLocation().getDirection();
        dragon.setVelocity(direction);

        new BukkitRunnable() {
            @Override
            public void run() {
                dragon.remove(); // 10秒後にドラゴンを消去
            }
        }.runTaskLater(this, time); // 200ティック後に実行（10秒）
    }
    private void castFlowerSpell(Player player, int flowerCount, int numberOfFlowers) {
        Location playerLocation = player.getLocation();
        Random random = new Random();

        new BukkitRunnable() {
            int count = 0;

            @Override
            public void run() {
                if (count >= 3) {
                    this.cancel();
                    return;
                }

                int dx = random.nextInt(5);
                int dz = random.nextInt(5);
                Location spawnLocation = playerLocation.clone().add(dx, 0, dz);
                spawnLocation.setY(spawnLocation.getWorld().getHighestBlockYAt(spawnLocation));

                Material flower = random.nextBoolean() ? Material.POPPY : Material.DANDELION;
                spawnLocation.getWorld().dropItemNaturally(spawnLocation, new ItemStack(flower, flowerCount));

                count++;
            }
        }.runTaskTimer(this, 0L, 20L); // 1秒ごとに実行
    }
    private void executeMagicActions(Player player, int lengthOfWord3, int lengthOfWord4, int lengthOfWord5) {
        onPotionGive(player, PotionEffectType.SLOW, lengthOfWord4, lengthOfWord3);
        playMagicSound(player, lengthOfWord5);
        deleteParticle(player, random.nextInt(2));
    }
    private void castRainSpell(Player player) {
        World world = player.getWorld();
        world.setStorm(true); // 雨を降らせる

        new BukkitRunnable() {
            @Override
            public void run() {
                world.setStorm(false); // 10秒後に雨を止める
            }
        }.runTaskLater(this, 200L); // 200ティック後に実行（10秒）
    }

    private void summonFireTornado(Player player, int timer, int attack) {
        new BukkitRunnable() {
            Location loc = player.getEyeLocation();
            Vector direction = loc.getDirection().normalize().multiply(0.5); // 竜巻の速度と方向
            int ticks = 0; // 竜巻の持続時間のカウンター

            @Override
            public void run() {
                if (ticks > timer) { // 竜巻の持続時間（例: 100ティック）
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
                        entity.setFireTicks(timer * 20); // 3秒間炎上
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
        if (player.getWorld().getEnvironment() != World.Environment.THE_END) {
            player.sendMessage("エンドにいないと使えません。");
            return;
        }
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
        if (player.getWorld().getEnvironment() != World.Environment.THE_END) {
            player.sendMessage("エンドにいないと使えません。");
            return;
        }
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
            player.sendMessage("スペルの文法が間違っています");
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
        if (player.getWorld().getEnvironment() != World.Environment.THE_END) {
            player.sendMessage("エンドにいないと使えません。");
            return;
        }
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
                            if (entity instanceof LivingEntity && !(entity instanceof Player)&& !(entity instanceof ArmorStand)) {
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
    private void spawnHomingTNT(Player player, int numberOfTNT, int fuseTicks) {
        if (player.getWorld().getEnvironment() != World.Environment.THE_END) {
            player.sendMessage("エンドにいないと使えません。");
            return;
        }
        for (int i = 0; i < numberOfTNT; i++) {
            getServer().getScheduler().runTaskLater(this, () -> {
                Location launchLocation = player.getLocation().add(player.getLocation().getDirection().multiply(2));
                TNTPrimed tnt = player.getWorld().spawn(launchLocation, TNTPrimed.class);
                tnt.setFuseTicks(fuseTicks);
                tnt.setVelocity(player.getLocation().getDirection().multiply(2.0)); // 速度の設定

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!tnt.isValid()) {
                            this.cancel();
                            return;
                        }

                        // 最も近いエンティティを探す（他のプレイヤーを除外）
                        LivingEntity target = null;
                        double closestDistance = Double.MAX_VALUE;
                        for (Entity entity : tnt.getNearbyEntities(10, 10, 10)) {
                            if (entity instanceof LivingEntity && !(entity instanceof Player)&& !(entity instanceof ArmorStand)) {
                                double distance = entity.getLocation().distanceSquared(tnt.getLocation());
                                if (distance < closestDistance) {
                                    closestDistance = distance;
                                    target = (LivingEntity) entity;
                                }
                            }
                        }

                        // ターゲットに向けてTNTを誘導
                        if (target != null) {
                            Vector direction = target.getLocation().add(0, 1, 0).subtract(tnt.getLocation()).toVector().normalize();
                            tnt.setVelocity(direction.multiply(1.5)); // 速度の更新
                        }
                    }
                }.runTaskTimer(this, 0L, 1L);
            }, fuseTicks / 20L * i); // i秒後にTNTを発射
        }
    }
    private void castIceMagic(Player player, int time, int damage) {
        new BukkitRunnable() {
            Location loc = player.getEyeLocation();
            Vector direction = loc.getDirection().normalize().multiply(0.5); // プレイヤーの向きに合わせた方向と速度
            int ticks = 0;

            @Override
            public void run() {
                if (ticks > time) { // 魔法の持続時間
                    this.cancel();
                    return;
                }

                loc.add(direction);
                loc.getWorld().spawnParticle(Particle.SNOW_SHOVEL, loc, 30, 0.5, 0.5, 0.5, 0.1);
                // 当たり判定
                for (Entity entity : loc.getWorld().getNearbyEntities(loc, 1, 1, 1)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        LivingEntity target = (LivingEntity) entity;
                        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, time, 100)); // 3秒間の移動速度低下
                        target.damage((double) damage / 2); // 継続ダメージ
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
    private void castSanGekiMagic(Player player, int attackCount, double damageSu) {
        LivingEntity target = getTargetEntity(player);
        if (target == null) {
            return;
        }

        new BukkitRunnable() {
            int count = 0;

            @Override
            public void run() {
                if (count >= attackCount) {
                    this.cancel();
                    return;
                }

                // ターゲットにダメージを与える
                target.damage(damageSu/2);

                // パーティクルと音を再生
                Location loc = target.getLocation();
                loc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 1);
                loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0F, 1.0F);

                count++;
            }
        }.runTaskTimer(this, 0L, 5L); // 連撃間の遅延を設定（ここでは5ティック）
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
    private void castVampiricSpell(Player player, int damage ,int targetHpBind) {
        LivingEntity target = getTargetEntity(player);
        if (target != null && target != player) {
            // ターゲットから体力を吸収
            double healthToSteal = Math.min((double) targetHpBind /20, target.getHealth());
            target.setHealth(Math.max(target.getHealth() - healthToSteal, 0));
            player.setHealth(Math.min(player.getHealth() + healthToSteal, player.getMaxHealth()));
        } else {
            // ターゲットが見つからなかった場合、プレイヤーにダメージ
            player.damage(damage);
        }
    }
}
