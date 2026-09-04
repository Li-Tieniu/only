package com.game.app.controller;


import com.game.app.service.SearchService;
import com.game.module.VO.DetailsVO;
import com.game.module.VO.ListVO;
import com.game.module.VO.ProductVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/gametrading")
@RestController
public class GameController {

    @Autowired
    private SearchService searchService;

    //查询商品列表
    @GetMapping("/product")
    public ListVO<ProductVO> product(){

        return searchService.produnt();

    }


    //查询详细信息
    @GetMapping("/details")
    public ListVO<DetailsVO> details(){

        return searchService.details();

    }
}
