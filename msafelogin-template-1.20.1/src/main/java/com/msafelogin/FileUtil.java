package com.msafelogin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Environment(EnvType.SERVER)
public final class FileUtil {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /* 读 64-bit 数字，文件不存在则生成并返回 */
    public static long loadOrCreateKey(Path file) throws IOException {
        if (Files.exists(file)) {
            return Long.parseLong(Files.readString(file).trim());
        }
        Files.createDirectories(file.getParent());
        long key = new java.security.SecureRandom().nextLong();
        Files.writeString(file, String.valueOf(key));
        System.out.println("===========================================");
        System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
        System.out.println("[MSafeLogin] 初始密钥已生成，请记录下面的文件地址：");
        System.out.println("[MSafeLogin] " + file.toAbsolutePath());
        System.out.println("[MSafeLogin] 请妥善保管初始密钥，泄露会导致安全登录");
        System.out.println("[MSafeLogin] 验证失效，服务器可能被攻击。");
        System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
        System.out.println("===========================================");
        return key;
    }

    /* 玩家密钥库读写 */
    public static JsonObject loadPlayerDB(Path file) throws IOException {
        return Files.exists(file) ? JsonParser.parseString(Files.readString(file)).getAsJsonObject() : new JsonObject();
    }
    public static void savePlayerDB(Path file, JsonObject db) throws IOException {
        Files.writeString(file, GSON.toJson(db));
    }
}