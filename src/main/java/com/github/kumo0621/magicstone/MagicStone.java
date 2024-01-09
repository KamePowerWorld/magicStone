package com.github.kumo0621.magicstone;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static org.bukkit.potion.PotionEffectType.*;

public final class MagicStone extends JavaPlugin implements Listener {
    private final Map<UUID, Integer> playerExpUsage = new HashMap<>();
    private static MagicStone instance;
    Random random = new Random();
    private Map<UUID, EnderCrystal> playerCrystals = new HashMap<>();
    private final HashMap<UUID, Long> cooldowns = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        saveDefaultConfig();
        instance = this;

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                    ItemStack[] items = player.getInventory().getContents();
                    int carrotStickCount = 0;

                    // ニンジン付き棒の総数を数える
                    for (ItemStack item : items) {
                        if (item != null && item.getType() == Material.CARROT_ON_A_STICK) {
                            carrotStickCount += item.getAmount();
                        }
                    }

                    // 4つ以上持っている場合、プレイヤーをkill
                    if (carrotStickCount > 3) {
                        player.setHealth(0);
                        player.sendMessage("持てる魔法は3つまでです。3つ以下にシない限りkillされ続けます。");
                    }
                }
            }
        }.runTaskTimer(this, 0L, 600L); // 1分ごとに実行 (20L = 1秒)
    }

    public static MagicStone getInstance() {
        return instance;
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
    public void onPlayerChat2(PlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        if (message.startsWith("@")) {
            int currentExp = XpUtils.getPlayerExperience(player);
            // 必要な経験値が足りているかチェック
            int xpLv30 = XpUtils.levelToExp(30);
            if (currentExp >= xpLv30) {
                aiReturn.ai(message, player);
                XpUtils.setPlayerExperience(player, currentExp - xpLv30);
            } else {
                player.sendMessage("魔法を生成するのは30Lv必要です。");
            }
        }
    }

    public void giveAiMessage(Player player, String message, String status) {
        Random random = new Random();
        JsonObject aiData = null;
        try {
            JsonElement jsonElement = JsonParser.parseString(status);
            if (jsonElement.isJsonObject()) {
                aiData = jsonElement.getAsJsonObject();
            }
        } catch (JsonSyntaxException ignored) {

        }
        if (aiData != null) {
            // String target = aiData.get("対象").getAsString();    // 対象
            String magicType = aiData.get("魔法名").getAsString();  // 魔法の種類
            int power = aiData.get("威力").getAsInt();  // 威力
            int range = aiData.get("効果時間").getAsInt() * 20;    // 効果範囲
            int effectType = random.nextInt(20);
            int magicNumber = random.nextInt(3);
            String properties = aiData.get("種類").getAsString();
            giveCustomCarrotStick(player, message, magicNumber, magicType, power, range, effectType, properties);
        } else {
            player.sendMessage("無効なスペルです。");

        }
    }

    public MagicData getItemData(ItemMeta itemMeta) {
        PersistentDataContainer data = itemMeta.getPersistentDataContainer();
        @Nullable PersistentDataContainer magicData = data.get(new NamespacedKey(this, "magicData"), PersistentDataType.TAG_CONTAINER);
        if (magicData == null) {
            return null;
        }
        int magicNumber = magicData.get(new NamespacedKey(this, "magicNumber"), PersistentDataType.INTEGER);
        String magicType = magicData.get(new NamespacedKey(this, "magicType"), PersistentDataType.STRING);
        String target = magicData.get(new NamespacedKey(this, "target"), PersistentDataType.STRING);
        int power = magicData.get(new NamespacedKey(this, "power"), PersistentDataType.INTEGER);
        int range = magicData.get(new NamespacedKey(this, "range"), PersistentDataType.INTEGER);
        int effect = magicData.get(new NamespacedKey(this, "effect"), PersistentDataType.INTEGER);
        String message = magicData.get(new NamespacedKey(this, "message"), PersistentDataType.STRING);
        String properties = magicData.get(new NamespacedKey(this, "properties"), PersistentDataType.STRING);
        // MagicData オブジェクトの生成
        return new MagicData(magicNumber, magicType, target, power, range, effect, message, properties);

    }


    public void setItemData(ItemMeta itemMeta, String message, int magicNumber, String magicType, int power, int range, int effectType, String properties) {
        PersistentDataContainer data = itemMeta.getPersistentDataContainer();
        @Nullable PersistentDataContainer magicData = data.get(new NamespacedKey(this, "magicData"), PersistentDataType.TAG_CONTAINER);
        if (magicData == null) {
            magicData = data.getAdapterContext().newPersistentDataContainer();
        }
        magicData.set(new NamespacedKey(this, "magicNumber"), PersistentDataType.INTEGER, magicNumber);
        magicData.set(new NamespacedKey(this, "magicType"), PersistentDataType.STRING, magicType);
        magicData.set(new NamespacedKey(this, "power"), PersistentDataType.INTEGER, power);
        magicData.set(new NamespacedKey(this, "range"), PersistentDataType.INTEGER, range);
        magicData.set(new NamespacedKey(this, "effect"), PersistentDataType.INTEGER, effectType);
        magicData.set(new NamespacedKey(this, "message"), PersistentDataType.STRING, message);
        magicData.set(new NamespacedKey(this, "properties"), PersistentDataType.STRING, properties);
        data.set(new NamespacedKey(this, "magicData"), PersistentDataType.TAG_CONTAINER, magicData);

    }


    public void giveCustomCarrotStick(Player player, String message, int magicNumber, String magicType, int power, int range, int effectType, String properties) {
        // ニンジン付き棒のアイテムスタックを生成
        ItemStack carrotStick = new ItemStack(Material.CARROT_ON_A_STICK);
        // アイテムメタデータを取得して編集
        ItemMeta meta = carrotStick.getItemMeta();
        if (meta != null) {
            int cost = power * range / 60 / 15;
            // 名前を設定（カラーコードで装飾可能）
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', magicType + " 必要経験値 " + cost));
            setItemData(meta, message, magicNumber, magicType, power, range, effectType, properties);
            // アイテムスタックにメタデータを設定
            carrotStick.setItemMeta(meta);
        }
        // プレイヤーのインベントリにアイテムを追加
        player.getInventory().addItem(carrotStick);
    }


    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // アイテムがニンジン付き棒であるかチェック
        if (item != null && item.getType() == Material.CARROT_ON_A_STICK) {
            UUID playerId = player.getUniqueId();
            long currentTime = System.currentTimeMillis();

            // プレイヤーのクールダウンをチェック
            if (cooldowns.containsKey(playerId) && (currentTime - cooldowns.get(playerId) < 10000)) {
                player.sendMessage("10秒に一回しか魔法は使えません。");
                event.setCancelled(true);
            } else {
                // アイテムメタデータを取得し処理を行う
                MagicData magicData = getItemData(item.getItemMeta());
                if (magicData != null) {
                    Magic(player, magicData);
                }

                // クールダウンを更新
                cooldowns.put(playerId, currentTime);
            }
        }
    }


    public void Magic(Player player, MagicData magicData) {
        int cost = XpUtils.levelToExp(magicData.getPower() * magicData.getRange() / 60 / 15);
        int currentExp = XpUtils.getPlayerExperience(player);
        // 必要な経験値が足りているかチェック
        if (currentExp >= cost) {
            switch (magicData.getProperties()) {

                case "発光" -> {
                    onPotionGive(player, GLOWING, magicData.getRange(), magicData.getPower());
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }
                case "毒" -> {
                    onPotionGive(player, POISON, magicData.getRange(), magicData.getPower());
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }
                case "守護" -> {
                    onPointView(player, PotionEffectType.DAMAGE_RESISTANCE, 2, 10); // 例: 耐性効果を適用
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }
                case "燃" -> {
                    ignitePlayer(player, magicData.getRange() / 20);
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }
                case "明" -> {
                    onPointView(player, NIGHT_VISION, magicData.getRange(), magicData.getPower()); // 例: 耐性効果を適用
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }
                case "暗黒" -> {
                    onPointView(player, PotionEffectType.BLINDNESS, 3, magicData.getPower()); // 例: 耐性効果を適用
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }


                case "癒し" -> {
                    onPotionGive(player, HEAL, magicData.getRange(), magicData.getPower());
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }
                case "潔白" -> {
                    clearAllPotionEffects(player);
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }
                case "満腹" -> {
                    onPotionGive(player, SATURATION, magicData.getRange(), magicData.getPower());
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }

                case "雨" -> {
                    castRainSpell(player);
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }
                case "神" -> {
                    String response = ChatColor.GOLD + "<システム> プログラミングの先生かめすたを呼び出した。" +
                            "\nしかし、かめすたは忙しかった。直接話しかけてみよう。";
                    player.getPlayer().sendMessage(response);
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }
                case "骨" -> {
                    equipBoneHelmet(player);
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }

                case "追尾" -> {
                    spawnHomingArrow(player, magicData.getPower(), magicData.getRange());
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }
                case "光線" -> {
                    onBeamSpawn(player, magicData.getPower(), magicData.getRange());
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }
                case "隕石" -> {
                    onMegaFlare(player, magicData.getRange() / 20, magicData.getPower() / 2);
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }
                case "ドラゴン" -> {
                    summonEnderDragon(player, magicData.getRange());
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }
                case "斬撃" -> {
                    castSanGekiMagic(player, magicData.getRange(), magicData.getPower());
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }
                case "吸血" -> {
                    castVampiricSpell(player, magicData.getPower(), magicData.getRange());
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }
                case "矢" -> {
                    shootPowerfulArrow(player, magicData.getRange());
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }
                case "消化" -> {
                    castWaterSplashSpell(player);
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }
                case "吹雪" -> {
                    castIceMagic(player, magicData.getRange() / 20, magicData.getPower());
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }

                case "相手を発光" -> {
                    onPointView(player, GLOWING, magicData.getRange(), magicData.getPower());
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }
                case "鈍足" -> {
                    onPointView(player, SLOW, magicData.getRange(), magicData.getPower());
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }
                case "味方ヒール" -> {
                    onPointView(player, HEAL, magicData.getRange(), magicData.getPower());
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }
                case "味方満腹" -> {
                    onPointView(player, SATURATION, magicData.getRange(), magicData.getPower());
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }
                case "相手を燃やす" -> {
                    igniteEntityInSight(player, magicData.getRange());
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }

                case "相手の潔白" -> {
                    clearEffectsFromTargetPlayer(player);
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }

                case "炎の竜巻" -> {
                    summonFireTornado(player, magicData.getRange() / 20, magicData.getPower());
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }
                case "雷" -> {
                    castLightningSpell(player, magicData.getRange() / 2, magicData.getPower());
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }
                case "ゾンビ召喚" -> {
                    summonZombies(player, magicData.getRange(), magicData.getPower());
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }
                case "スケルトン召喚" -> {
                    skeletonSpawn(player, magicData.getPower());
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }
                case "彼女" -> {
                    createMagicEffect(player);
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }

                case "お花" -> {
                    castFlowerSpell(player, magicData.getPower(), magicData.getRange());
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }
                case "楽しい魔法" -> {
                    castRandomSplashSpell(player, magicData.getPower(), magicData.getRange());
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }
                case "魚" -> {
                    summonMagicFish(player, magicData.getPower(), magicData.getRange());
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }
                case "剣" -> {
                    launchSwordFromPlayer(player, magicData.getPower(), magicData.getRange());
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }
                case "全方位剣" -> {
                    launchSwordsInAllDirections(player, magicData.getPower(), magicData.getRange());
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }
                case "全方位放火" -> {
                    launchFireParticles(player, magicData.getPower(), magicData.getRange());
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }
                case "爆撃" -> {
                    selfDestruct(player, magicData.getPower(), magicData.getRange());
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }
                case "連続魔法" -> {
                    launchExplosionEffect(player, magicData.getPower(), magicData.getRange());
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }
                case "落下低下" -> {
                    onPotionGive(player, SLOW_FALLING, magicData.getRange(), magicData.getPower());
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }
                case "速度上昇" -> {
                    onPotionGive(player, SPEED, magicData.getRange(), magicData.getPower());
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }
                case "浮遊" -> {
                    onPotionGive(player, LEVITATION, magicData.getRange(), magicData.getPower());
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }
                case "味方落下速度低下" -> {
                    onPointView(player, SLOW_FALLING, magicData.getRange(), magicData.getPower());
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }
                case "味方浮遊" -> {
                    onPointView(player, LEVITATION, magicData.getRange(), magicData.getPower());
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }
                case "味方速度上昇" -> {
                    onPointView(player, SPEED, magicData.getRange(), magicData.getPower());
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }
                case "ジャンプ" -> {
                    onPointView(player, JUMP, magicData.getRange(), magicData.getPower());
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }
                case "相手ジャンプ" -> {
                    onPotionGive(player, JUMP, magicData.getRange(), magicData.getPower());
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }
                case "全員守護" -> {
                    allAreaEffect(player, DAMAGE_RESISTANCE, 400, 99);
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }
                case "全員回復" -> {
                    allAreaEffect(player, HEAL, magicData.getPower(), magicData.getRange());
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }
                case "全員速度上昇" -> {
                    allAreaEffect(player, SPEED, magicData.getPower(), magicData.getRange());
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }
                case "全員浮遊" -> {
                    allAreaEffect(player, LEVITATION, magicData.getPower(), magicData.getRange());
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());
                }

                default -> {
                    onPotionGive(player, SLOW, magicData.getRange(), magicData.getPower());
                    playMagicSound(player, magicData.getEffect());
                    particle(player, magicData.getMagicNumber());

                }
            }
            XpUtils.setPlayerExperience(player, currentExp - cost);
        } else {
            player.sendMessage("経験値が足りない。");
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

    public void allAreaEffect(Player player, PotionEffectType effect, int power, int range) {
        Location playerLocation = player.getLocation();
        int radius = 3; // 効果を適用する半径

        for (Player target : player.getWorld().getPlayers()) {
            if (target.getLocation().distance(playerLocation) <= radius) {
                // 味方プレイヤーに耐性効果を適用
                target.addPotionEffect(new PotionEffect(effect, range, power / 20)); // 2秒間、レベル100

                // パーティクルを表示
                target.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, target.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);
            }
        }
    }

    public void launchExplosionEffect(Player player, int power, int range) {
        Location startLocation = player.getEyeLocation();
        Vector direction = startLocation.getDirection().normalize();

        new BukkitRunnable() {
            int distance = 0;
            Location currentLocation = startLocation.clone();

            public void run() {
                if (distance > range / 20) { // 10マス先まで移動
                    this.cancel();
                    return;
                }

                currentLocation.add(direction);
                player.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, currentLocation, 1, 0, 0, 0, 0);

                // 爆発エフェクトがモンスターに当たった場合にダメージを与える
                for (Entity entity : currentLocation.getWorld().getNearbyEntities(currentLocation, 0.5, 0.5, 0.5)) {
                    if (entity instanceof Monster) {
                        ((Monster) entity).damage((double) power / 2); // ダメージ量
                    }
                }

                distance++;
            }
        }.runTaskTimer(this, 0L, 1L); // 1ティックごとに実行
    }

    public void selfDestruct(Player player, int power, int range) {
        Location location = player.getLocation();

        // 爆発のエフェクトを表示
        location.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, location, 1);

        // 近くのエンティティにダメージを与える
        location.getWorld().getNearbyEntities(location, range, range, range).forEach(entity -> {
            if (entity instanceof LivingEntity && entity != player) {
                ((LivingEntity) entity).damage((double) power); // ダメージ量を設定
            }
        });

        // 自分自身にもダメージを与える
        player.damage((double) power);
    }

    public void launchFireParticles(Player player, int power, int range) {
        Location playerLocation = player.getLocation().add(0, 1, 0); // プレイヤーの腰の位置
        Vector[] directions = {
                new Vector(1, 0, 0),   // 東
                new Vector(1, 0, 1),   // 南東
                new Vector(0, 0, 1),   // 南
                new Vector(-1, 0, 1),  // 南西
                new Vector(-1, 0, 0),  // 西
                new Vector(-1, 0, -1), // 北西
                new Vector(0, 0, -1),  // 北
                new Vector(1, 0, -1)   // 北東
        };

        for (Vector direction : directions) {
            new BukkitRunnable() {
                int ticks = 0;

                public void run() {
                    if (ticks > range) {
                        this.cancel();
                        return;
                    }

                    // 小さな火の玉を発射
                    Location currentLocation = playerLocation.clone().add(direction.clone().multiply(ticks * 0.3));
                    player.getWorld().spawnParticle(Particle.FLAME, currentLocation, 0, 0, 0, 0, 0.01);

                    // 近くのエンティティに火傷の効果を与える
                    currentLocation.getWorld().getNearbyEntities(currentLocation, 0.5, 0.5, 0.5).forEach(entity -> {
                        if (entity instanceof LivingEntity && entity != player) {
                            entity.setFireTicks(power * 20); // 火傷の効果
                        }
                    });

                    ticks++;
                }
            }.runTaskTimer(this, 0L, 1L); // 1ティックごとに実行
        }
    }

    public void launchSwordsInAllDirections(Player player, int power, int range) {
        Location playerLocation = player.getLocation();
        Vector[] directions = {
                new Vector(0, 0, -1),   // 北
                new Vector(1, 0, -1),   // 北東
                new Vector(1, 0, 0),    // 東
                new Vector(1, 0, 1),    // 南東
                new Vector(0, 0, 1),    // 南
                new Vector(-1, 0, 1),   // 南西
                new Vector(-1, 0, 0),   // 西
                new Vector(-1, 0, -1)   // 北西
        };

        for (Vector direction : directions) {
            ArmorStand swordStand = (ArmorStand) playerLocation.getWorld().spawnEntity(playerLocation, EntityType.ARMOR_STAND);
            swordStand.setVisible(false);
            swordStand.setGravity(false);
            swordStand.setInvulnerable(true);
            swordStand.setHelmet(new ItemStack(Material.STONE_SWORD)); // 石の剣を装備

            new BukkitRunnable() {
                int steps = 0;
                public void run() {
                    if (steps >= range) {
                        swordStand.remove();
                        this.cancel();
                        return;
                    }

                    // ArmorStandの位置を更新
                    Location currentLocation = swordStand.getLocation().add(direction);
                    swordStand.teleport(currentLocation);

                    // 近くのエンティティにダメージを適用
                    currentLocation.getNearbyEntities(1, 1, 1).forEach(entity -> {
                        if (entity instanceof LivingEntity && entity != player) {
                            ((LivingEntity) entity).damage((double) power / 2); // ダメージ量
                        }
                    });

                    steps++;
                }
            }.runTaskTimer(this, 0L, 1L); // 1ティックごとに実行
        }
    }

    public void launchSwordFromPlayer(Player player, int Power, int range) {
        Location startLocation = player.getLocation();
        ArmorStand swordStand = (ArmorStand) startLocation.getWorld().spawnEntity(startLocation, EntityType.ARMOR_STAND);
        swordStand.setVisible(false);
        swordStand.setGravity(false);
        swordStand.setInvulnerable(true);
        swordStand.setHelmet(new ItemStack(Material.STONE_SWORD)); // 石の剣を装備

        Vector direction = player.getLocation().getDirection().multiply(0.3); // プレイヤーの向いている方向に速度を設定

        new BukkitRunnable() {
            public void run() {
                swordStand.teleport(swordStand.getLocation().add(direction));

                // 近くのすべてのエンティティにダメージを適用
                swordStand.getLocation().getNearbyEntities(1, 1, 1).forEach(entity -> {
                    if (entity instanceof LivingEntity && entity != player) {
                        ((LivingEntity) entity).damage((double) Power / 2); // ダメージ量
                    }
                });

                // 一定距離を超えたら消滅
                if (swordStand.getLocation().distance(startLocation) > (double) range / 20) {
                    swordStand.remove();
                    this.cancel();
                }
            }
        }.runTaskTimer(this, 0L, 1L); // 1ティックごとに実行
    }

    public void summonMagicFish(Player player, int Power, int range) {
        Location playerLocation = player.getLocation();
        Random random = new Random();

        // プレイヤーの周囲にランダムな位置を生成
        Location fishLocation = playerLocation.clone().add((random.nextDouble() * 10) - 5, (random.nextDouble() * 5), (random.nextDouble() * 10) - 5);

        // 魚をスポーンさせる
        Item fish = fishLocation.getWorld().dropItem(fishLocation, new ItemStack(Material.COD));
        fish.setVelocity(new Vector(0, 0.5, 0)); // 魚に少しの上向きの速度を与える

        // パーティクルを表示
        fishLocation.getWorld().spawnParticle(Particle.WATER_SPLASH, fishLocation, 30, 0.5, 0.5, 0.5, 0.1);

        // 近くのプレイヤーにウィザー効果を適用
        new BukkitRunnable() {
            public void run() {
                fishLocation.getWorld().getNearbyEntities(fishLocation, 5, 5, 5).forEach(entity -> {
                    if (entity instanceof Player && entity != player) {
                        ((Player) entity).addPotionEffect(new PotionEffect(PotionEffectType.WITHER, range / 2, Power / 2)); // 10秒間のウィザー効果
                    }
                });

                // 魚を消滅させる
                fish.remove();
            }
        }.runTaskLater(this, 20 * 5); // 5秒後に実行
    }

    private void createMagicEffect(Player player) {
        Location center = player.getLocation();
        new BukkitRunnable() {
            double radius = 0.0;
            final double maxRadius = 10.0; // 最大半径を10マスに設定

            public void run() {
                if (radius > maxRadius) {
                    this.cancel(); // 半径が10マスを超えたら終了
                    return;
                }

                for (int i = 0; i < 360; i += 10) { // 360度の円を形成
                    double angle = Math.toRadians(i);
                    double x = center.getX() + radius * Math.cos(angle);
                    double z = center.getZ() + radius * Math.sin(angle);
                    Location loc = new Location(center.getWorld(), x, center.getY(), z);

                    center.getWorld().spawnParticle(Particle.HEART, loc, 1, 0, 0, 0, 0);
                }

                // 半径内のエンティティにダメージを与える
                for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        ((LivingEntity) entity).damage(1.0); // エンティティにダメージを与える
                    }
                }

                radius += 0.5; // 半径を広げる
            }
        }.runTaskTimer(this, 0L, 20L); // 20ティック（約1秒）ごとに実行
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

    private void shootPowerfulArrow(Player player, int time) {
        Arrow arrow = player.launchProjectile(Arrow.class);
        Vector direction = player.getLocation().getDirection();
        arrow.setVelocity(direction.multiply(time)); // 高速で発射
        arrow.setDamage((double) time / 20 * 1.25); // 威力を設定
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


    private void summonEnderDragon(Player player, int time) {
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


    private void spawnHomingArrow(Player player, int extractedPower, int extractedRange) {
        for (int i = 0; i < extractedPower; i++) {
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
                            if (entity instanceof LivingEntity && !(entity instanceof Player) && !(entity instanceof ArmorStand)) {
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
            }, extractedRange / 20L * i); // i秒後に矢を発射
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

                int xpLv30 = XpUtils.levelToExp(30);
                int expUsed = playerExpUsage.get(playerId) + 1;
                if (expUsed <= xpLv30) {
                    int playerXp = XpUtils.getPlayerExperience(player);
                    if (playerXp > 0) {
                        XpUtils.setPlayerExperience(player, playerXp - XpUtils.levelToExp(1));
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

    private void onBeamSpawn(Player player, int extractedPower, int extractedRange) {
        new BukkitRunnable() {
            Location loc = player.getEyeLocation();
            Vector direction = loc.getDirection().normalize().multiply(2); // ビームの方向と速度
            int range = extractedRange / 20; // ビームの射程

            @Override
            public void run() {
                for (int i = 0; i <= range; i++) {
                    loc.add(direction);
                    player.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, loc, 1, 0, 0, 0, 0);

                    // 当たり判定
                    for (Entity entity : loc.getWorld().getNearbyEntities(loc, 2, 2, 2)) {
                        if (entity instanceof LivingEntity && entity != player) {
                            ((LivingEntity) entity).damage(extractedPower - 6);
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
                if (count >= attackCount / 20) {
                    this.cancel();
                    return;
                }

                // ターゲットにダメージを与える
                target.damage(damageSu / 2);

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

    private void castVampiricSpell(Player player, int damage, int targetHpBind) {
        RayTraceResult rayTrace = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getLocation().getDirection(),
                5.0, // 視線を追跡する最大距離
                entity -> entity instanceof LivingEntity && entity != player
        );

        if (rayTrace != null && rayTrace.getHitEntity() instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) rayTrace.getHitEntity();
            // ターゲットから体力を吸収
            double healthToSteal = Math.min((double) targetHpBind / 40, target.getHealth());
            target.setHealth(Math.max(target.getHealth() - healthToSteal, 0));
            player.setHealth(Math.min(player.getHealth() + healthToSteal, player.getMaxHealth()));
        } else {
            // ターゲットが見つからなかった場合、プレイヤーにダメージ
            player.damage(damage);
        }
    }
}
