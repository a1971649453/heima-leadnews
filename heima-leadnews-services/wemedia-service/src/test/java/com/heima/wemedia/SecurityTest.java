package com.heima.wemedia;

import com.google.j2objc.annotations.ReflectionSupport;
import com.heima.aliyun.scan.GreenScan;
import com.heima.aliyun.scan.ScanResult;
import com.heima.wemedia.WemediaApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @author 金宗文
 * @version 1.0
 */
@SpringBootTest(classes = WemediaApplication.class)
public class SecurityTest {

    @Resource
    GreenScan greenScan;

    @Test
    public void scanTest() throws Exception {
        ScanResult scanResult = greenScan.greenTextScan("不要贩卖冰毒");
        System.out.println(scanResult);
    }
}
