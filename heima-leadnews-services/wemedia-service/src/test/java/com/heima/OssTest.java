package com.heima;

import com.heima.file.service.FileStorageService;
import com.heima.wemedia.WemediaApplication;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;

/**
 * @author 金宗文
 * @version 1.0
 */
@SpringBootTest(classes = WemediaApplication.class)
@RunWith(SpringRunner.class)
public class OssTest {
    @Resource
    FileStorageService fileStorageService;

    @Value("${file.oss.web-site}")
    String webSite;

    @Value("${file.oss.prefix}")
    String prefix;



    @Test
    public void testFileUpload() throws Exception {

//        FileInputStream inputStream = new FileInputStream(new File("D:\\UserData\\Pictures\\Camera Roll\\下载.png"));
//
//        //prefix 文件存入oss中bucket的哪个文件夹
//        //filename 文件名称
//        // prefix + 2023/01/19 + filename
//        String wemedia = fileStorageService.store(prefix, "aaa1.jpg", inputStream);
//        System.out.println(webSite+wemedia);

//         删除文件
        fileStorageService.delete("material/2023/1/20230119/aaa1.jpg");
    }
}
