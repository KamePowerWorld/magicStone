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

    public static String ai(String gamePlayer,Player player) {
        String apiKey = MagicStone.getInstance().getConfig().getString("openaiKey");
        OpenAiService service = new OpenAiService(apiKey);
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage userMessage = new ChatMessage(ChatMessageRole.USER.value(),
                "次の文章の、採点を行って具体的な魔法の種類とステータスと、盛大な魔法名を決めてください。"+gamePlayer+"種類を、「鈍足、発光、毒、守護、燃える、明、暗黒、癒し、潔白、満腹、雨、神、骨装備、追尾、光線、隕石、ドラゴン、斬撃、吸血、矢、消化、吹雪、相手を発光、味方ヒール、味方満腹、相手を燃やす、相手の潔白、炎の竜巻、雷、ゾンビ召喚、スケルトン召喚、お花、楽しい魔法」の中から近いものを絶対1つ決めてください。文章に応じて威力、効果時間、すべての数値を1~50の中で決める。文章に応じた、魔法名を決める。指定された値以外絶対に返さないでください。返答メッセージに余分なものはつけず、決めたものだけ書き出してください。JSON形式で、「{\"種類\":～,\"魔法名\":～,\"威力\":～,\"効果時間\":～}」また、意味のわからないものは、意味不明と返してください。また、指定のなかった値に関しては一律15の値を入れてください。");
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
                MagicStone plugin = MagicStone.getInstance();
                result = sb.toString();
                System.out.println("完了");
                System.out.println(result);
                plugin.giveAiMessage(player,gamePlayer,result);

            }
        });


        // 結果を返す
        return result;
    }

    // 非同期処理が完了するのを待つためのメソッド
}

