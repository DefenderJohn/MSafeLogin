package com.example.msafelogin;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

@Environment(EnvType.CLIENT)
public class MSafeLoginClient implements ClientModInitializer {
    private static final Path IMPORT_KEY = Path.of("server.key");
    private static long clientKey = 0L;   // 0 表示还没加载

    public static final Identifier CHANNEL = Identifier.of("msafelogin", "handshake");
    @Override
    public void onInitializeClient() {
        // ① 首次加载密钥（文件存在就用，不存在就等服务端下发）
        try {
            if (Files.exists(IMPORT_KEY)) {
                clientKey = Long.parseLong(Files.readString(IMPORT_KEY).trim());
                System.out.println("[Clt] 已导入服务器密钥: " + clientKey);
            } else {
                System.out.println("[Clt] 服务器密钥不存在");
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("[Clt] 无可用密钥，退出");
        }

        // ② 注册登录回调（用导入的密钥）
        ClientLoginNetworking.registerGlobalReceiver(CHANNEL,
                (client, handler, buf, listenerAdder) -> {
                    long salt = buf.readLong();
                    long sig = CryptoUtil.sign(salt, clientKey);   // 用导入钥匙
                    System.out.println("[Clt] 计算签名: " + sig);
                    PacketByteBuf out = new PacketByteBuf(Unpooled.buffer());
                    out.writeLong(sig);
                    return CompletableFuture.completedFuture(out);
                });
    }
}