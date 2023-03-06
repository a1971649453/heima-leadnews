package com.heima.search.service;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.search.dtos.UserSearchDTO;
public interface ArticleSearchService {
    /**
     ES文章分页搜索
     @return
     */
    ResponseResult search(UserSearchDTO userSearchDto);
}