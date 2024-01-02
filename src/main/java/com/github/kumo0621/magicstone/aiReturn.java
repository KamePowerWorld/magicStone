package com.github.kumo0621.magicstone;
import com.theokanning.openai.completion.chat.*;
import com.theokanning.openai.service.OpenAiService;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.List;

public class aiReturn {
    // 外部クラスのフィールド
    public static String result = "";

    public static String ai(String gamePlayer) {
        String apiKey = key.api;
        OpenAiService service = new OpenAiService(apiKey);
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage userMessage = new ChatMessage(ChatMessageRole.USER.value(), "次の文章の、採点を行って具体的な魔法のステータスを決めてください。"+gamePlayer+"対象を、自分or敵or範囲の中から1つ。魔法の種類を、攻撃、バフ、、回復、その他、の中から1つ。文章に応じて威力、効果範囲、すべての数値を1~50の中で決める。返答メッセージに余分なものはつけず、決めたものだけ書き出してください。");
        messages.add(userMessage);
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest
                .builder()
                .model("gpt-3.5-turbo-0613")
                .messages(messages)
                .maxTokens(256)
                .build();

        service.streamChatCompletion(chatCompletionRequest).subscribeWith(new Subscriber<ChatCompletionChunk>() {
            private Subscription subscription;
            private StringBuilder sb = new StringBuilder();

            @Override
            public void onSubscribe(Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(ChatCompletionChunk chatCompletionChunk) {
                String content = chatCompletionChunk.getChoices().get(0).getMessage().getContent();
                if(content != null) {
                    sb.append(content);
                }
                subscription.request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("error");
            }

            @Override
            public void onComplete() {
                result = sb.toString();

            }
        });


        // 結果を返す
        return result;
    }

    // 非同期処理が完了するのを待つためのメソッド
}

