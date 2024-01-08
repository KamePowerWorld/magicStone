package com.github.kumo0621.magicstone;

import com.theokanning.openai.completion.chat.ChatCompletionChunk;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AIに魔法の種類とステータスと、盛大な魔法名を決めてもらうクラス
 */
public class MagicAI {
    private final Logger logger;

    private final OpenAiService service;

    public MagicAI(Logger logger, String openaiKey) {
        this.logger = logger;
        this.service = new OpenAiService(openaiKey);
    }

    public CompletableFuture<String> ai(String gamePlayer) {
        // AIに指示する文章を定義
        String initMessageText = """
                次のユーザーの文章の、採点を行って具体的な魔法の種類とステータスと、盛大な魔法名を決めてください。
                種類は、「剣、魚、元カノ砲、鈍足、発光、毒、守護、燃える、明、暗黒、癒し、潔白、満腹、雨、神、骨装備、追尾、光線、隕石、ドラゴン、斬撃、吸血、矢、消化、吹雪、相手を発光、味方ヒール、味方満腹、相手を燃やす、相手の潔白、炎の竜巻、雷、ゾンビ召喚、スケルトン召喚、お花、楽しい魔法」の中から近いものを絶対1つ決めてください。
                文章に応じて威力、効果時間、すべての数値を1~50の中で決める。
                文章に応じた、魔法名を決める。
                指定された値以外絶対に返さないでください。
                返答メッセージに余分なものはつけず、決めたものだけ書き出してください。
                JSON形式で、「{"種類":～,"魔法名":～,"威力":～,"効果時間":～}」
                また、意味のわからないものは、意味不明と返してください。
                また、指定のなかった値に関しては一律15の値を入れてください。""";
        List<ChatMessage> messages = Arrays.asList(
                new ChatMessage(ChatMessageRole.SYSTEM.value(), initMessageText),
                new ChatMessage(ChatMessageRole.USER.value(), gamePlayer)
        );
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest
                .builder()
                .model("gpt-3.5-turbo-0613")
                .messages(messages)
                .maxTokens(256)
                .build();

        // 非同期処理でAIリクエストを送信、結果を返す
        return completeAsync(chatCompletionRequest);
    }

    // 非同期処理が完了するのを待つためのメソッド
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        // MagicAIクラスのインスタンスを生成
        MagicAI magicAI = new MagicAI(Logger.getLogger("MagicAI"), System.getenv("OPENAI_KEY"));

        // Scannerで入力を受け取り、AIに送信、結果を表示
        Scanner scanner = new Scanner(System.in);
        System.out.println("AIに送信する文章を入力してください");

        while (true) {
            // キーボードから入力を受け取る
            String input = scanner.nextLine();
            // exitと入力されたら終了
            if (input.equals("exit")) {
                break;
            }
            System.out.println("AIに送信中...");

            // 同期処理に変換して取得
            String aiResult = magicAI.ai(input).get();

            System.out.println("AIの返答:" + aiResult);
        }
    }

    /**
     * 非同期処理でAIリクエストを送信、結果を返す
     * @param request AIリクエスト
     * @return AIの返答
     */
    private CompletableFuture<String> completeAsync(ChatCompletionRequest request) {
        CompletableFuture<String> future = new CompletableFuture<>();
        service.streamChatCompletion(request).subscribeWith(new Subscriber<ChatCompletionChunk>() {
            private Subscription subscription;
            private final StringBuilder sb = new StringBuilder();

            @Override
            public void onSubscribe(Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(ChatCompletionChunk chatCompletionChunk) {
                String content = chatCompletionChunk.getChoices().get(0).getMessage().getContent();
                if (content != null) {
                    sb.append(content);
                }
                subscription.request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                logger.log(Level.WARNING, "error", throwable);
                future.completeExceptionally(throwable);
            }

            @Override
            public void onComplete() {
                String result = sb.toString();
                future.complete(result);
                logger.info("AI返答:" + result);
            }
        });
        return future;
    }
}

