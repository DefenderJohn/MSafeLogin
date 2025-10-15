package com.example.msafelogin;

public final class CryptoUtil {
    /* ① 固定 64-bit 密钥（写死）后面再改成动态派发 */
    static final long FIXED_KEY = 0x5F3759DF5F3759DFL;

    /* ② 签名 = 循环左移(key % 64) 再异或 key */
    public static long sign(long salt, long key) {
        long mix = salt ^ key;
        int rot = (int)(key & 63);
        return (mix << rot) | (mix >>> (64 - rot));
    }
}