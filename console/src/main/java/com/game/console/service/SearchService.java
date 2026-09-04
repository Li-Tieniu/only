package com.game.console.service;

import com.game.module.VO.DetailsVO;
import com.game.module.VO.ListVO;
import com.game.module.VO.ProductVO;
import com.game.module.entity.GameEntity;
import com.game.module.mapper.GameMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchService {

    @Autowired
    private GameMapper gameMapper;

    //查询数据(详情)
    public ListVO<DetailsVO> getinfodetails() {

        ListVO<DetailsVO> consoledetails = new ListVO<>();
        for( GameEntity list : gameMapper.getinfo()){
            DetailsVO vo = new DetailsVO();
            vo.setWallImages(list.getWallImages());
            vo.setGameName(list.getGameName());
            vo.setPrice(list.getPrice());
            vo.setDetails(list.getDetails());
            consoledetails.getList().add(vo);
        }

        return consoledetails;

    }

    //查询数据(列表)
    public ListVO<ProductVO> getinfoproduct() {

        ListVO<ProductVO> consoleproduct = new ListVO<>();
        for( GameEntity list : gameMapper.getinfo()){
            ProductVO vo = new ProductVO();
            vo.setCoverImage(list.getCoverImage());
            vo.setGameName(list.getGameName());
            vo.setPrice(list.getPrice());
            consoleproduct.getList().add(vo);
        }

        return consoleproduct;

    }

}




/*
//查询所有未删除数据
public List<GameEntity> getinfo() {

    return gameMapper.getinfo();

}

//查询所有数据
public List<GameEntity> getall() {

    return gameMapper.getall();

}
*/