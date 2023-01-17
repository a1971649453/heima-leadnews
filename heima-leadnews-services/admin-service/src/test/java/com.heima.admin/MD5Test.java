package com.heima.admin;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;

/**
 * @author 金宗文
 * @version 1.0
 */
public class MD5Test {

    @Test
    public void md5(){
        byte[] hellos = DigestUtils.md5("hello");
    }
}
