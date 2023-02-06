package com.heima.behavior.controller.v1;

import com.heima.behavior.service.ApBehaviorEntryService;
import com.heima.model.behavior.pojos.ApBehaviorEntry;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author 金宗文
 * @version 1.0
 */
@RestController
@Slf4j
public class ApBehaviorEntryController {
    @Resource
    ApBehaviorEntryService apBehaviorEntryService;

    public ApBehaviorEntry findByUserIdOrEquipmentId(Integer userId, Integer equipmentId){
        return apBehaviorEntryService.findByUserIdOrEquipmentId(userId,equipmentId);
    }


}
