package com.game.console.controller;

import com.game.module.VO.DetailsVO;
import com.game.module.VO.ListVO;
import com.game.module.VO.PageResult;
import com.game.module.VO.ProductVO;
import com.game.console.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/gametrading")
@RestController
public class QueryController {

    @Autowired
    private SearchService searchService;

    //查询数据(列表)
    @GetMapping("/getinfoproduct")
    public ListVO<ProductVO> getinfoproduct() {

        return searchService.getinfoproduct();

    }

    //查询数据(详情)
    @GetMapping("/getinfodetails")
    public PageResult<DetailsVO> getinfodetails(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {

        if (size > 100){
            size = 100;
        }
        if(size < 1){
            size = 1;
        }
        if(page < 1){
            page = 1;
        }
        return searchService.getDetailsPage(page, size);

    }

}





/*
    //查询所有未删除数据
    @GetMapping("/getinfo")
    public List<GameEntity> getinfo(){

        return searchService.getinfo();

    }

    //查询所有游戏数据
    @GetMapping("/getall")
    public List<GameEntity> getall(){

        return searchService.getall();

    }

*/