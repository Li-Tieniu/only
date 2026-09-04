package com.game.app.service;

import com.game.module.VO.DetailsVO;
import com.game.module.VO.ListVO;
import com.game.module.VO.ProductVO;
import com.game.module.entity.GameEntity;
import com.game.module.mapper.GameMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SearchService {

    @Autowired
    private GameMapper gameMapper;

    //查询商品列表
    public ListVO<ProductVO> produnt(){

        ListVO<ProductVO> getproduct = new ListVO<>();
        for( GameEntity list : gameMapper.getinfo()){
            ProductVO vo = new ProductVO();
            vo.setCoverImage(list.getCoverImage());
            vo.setGameName(list.getGameName());
            vo.setPrice(list.getPrice());
            getproduct.getList().add(vo);
        }

        return getproduct;

    }

    //查询详细信息
    public ListVO<DetailsVO> details(){

        ListVO<DetailsVO> getdetails = new ListVO<>();
        for( GameEntity list : gameMapper.getinfo()){
            DetailsVO vo = new DetailsVO();
            vo.setWallImages(list.getWallImages());
            vo.setGameName(list.getGameName());
            vo.setPrice(list.getPrice());
            vo.setDetails(list.getDetails());
            getdetails.getList().add(vo);
        }

        return getdetails;

    }
}
