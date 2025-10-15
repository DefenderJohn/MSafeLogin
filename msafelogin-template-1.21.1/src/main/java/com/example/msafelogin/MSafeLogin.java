package com.example.msafelogin;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.network.ServerLoginNetworkHandler;

@Environment(EnvType.SERVER)
public class MSafeLogin implements DedicatedServerModInitializer {
    public static final String MOD_ID = "msafelogin";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    /* 网络通道常量放类里 */
    public static final Identifier CHANNEL = Identifier.of("msafelogin", "handshake");
    // 使用 ConcurrentHashMap 来安全地存储盐值，键为 ServerLoginNetworkHandler，值为盐
    private static final Map<ServerLoginNetworkHandler, Long> SALT_STORAGE = new ConcurrentHashMap<>();

    private static final Path SERVER_KEY_FILE = Path.of("config/msafelogin/server.key");
    private static long SERVER_INITIAL_KEY;

    @Override
    public void onInitializeServer() {
        LOGGER.info("[MSafeLogin] MSafeLogin 启动中...");
        try {
            SERVER_INITIAL_KEY = FileUtil.loadOrCreateKey(SERVER_KEY_FILE);
            LOGGER.info("[MSafeLogin] 初始密钥已加载: {}", SERVER_INITIAL_KEY);
        } catch (IOException e) { throw new RuntimeException(e); }

        // 1. 登录阶段触发
        ServerLoginConnectionEvents.QUERY_START.register((handler, server, packetSender, synchronizer) -> {

            long salt = new SecureRandom().nextLong();   // 随机盐
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeLong(salt);                                   // 下发 64-bit
            packetSender.sendPacket(CHANNEL, buf);
            System.out.println("[Srv] 下发盐: " + salt);
            SALT_STORAGE.put(handler, salt);
        });

        // 2. 监听客户端回包（官方事件）
        ServerLoginNetworking.registerGlobalReceiver(CHANNEL,
                (server, handler, understood, buf, synchronizer, packetSender) -> {
                    if (!understood) {                       // 客户端没装 mod
                        handler.disconnect(Text.literal("服务器被 MSafeLogin 保护，请联系服主获取客户端mod和密钥"));
                        return;
                    }
                    long clientSignature = buf.readLong();
                    Long salt = SALT_STORAGE.remove(handler);   // 取出之前存的盐

                    long expectedSignature = CryptoUtil.sign(salt, SERVER_INITIAL_KEY);
                    if (clientSignature != expectedSignature) {
                        System.out.println("[Srv] 校验失败: 期望=" + expectedSignature + ", 收到=" + clientSignature);
                        handler.disconnect(Text.literal("密钥校验失败"));
                        return;
                    }
                    System.out.println("[Srv] 校验通过 ✔");
                });
    }
}

