package com.heima.wemedia;

import com.heima.wemedia.service.WmNewsAutoScanService;
import org.junit.jupiter.api.Test;

import javax.annotation.Resource;

/**
 * @author 金宗文
 * @version 1.0
 */
public class AutoScanText {

    @Resource
    WmNewsAutoScanService wmNewsAutoScanService;

    @Test
    public void autoScan(){
        wmNewsAutoScanService.autoScanWmNews(6269);
    }
    
}