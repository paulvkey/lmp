package com.xjtu.springboot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.security.SecureRandom;
import java.util.Base64;

@SpringBootTest
class SpringbootApplicationTests {

	@Test
	void contextLoads() {
	}

    public static void main(String[] args) {
        // 生成 32 字节（256 位）的随机密钥
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);

        // 转为 Base64 格式（方便存储到配置文件）
        String secretKey = Base64.getEncoder().encodeToString(key);
        System.out.println("生成的 secret-key: " + secretKey);
    }

}
