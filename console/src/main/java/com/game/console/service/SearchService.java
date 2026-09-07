package com.game.console.service;

import com.game.module.DTO.QueryDTO;
import com.game.module.VO.DetailsVO;
import com.game.module.VO.ListVO;
import com.game.module.VO.PageResult;
import com.game.module.VO.ProductVO;
import com.game.module.entity.GameEntity;
import com.game.module.mapper.GameMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class SearchService {

    @Autowired
    private GameMapper gameMapper;


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

    //分页查询数据(详情)
    public PageResult<DetailsVO> getDetailsPage(Integer page, Integer size) {

        int offset = (page - 1) * size;

        List<GameEntity> entities = gameMapper.page(offset, size);

        List<DetailsVO> voList = new ArrayList<>();
            for (GameEntity entity : entities){
                DetailsVO vo = new DetailsVO();
                vo.setWallImages(entity.getWallImages());
                vo.setGameName(entity.getGameName());
                vo.setPrice(entity.getPrice());
                vo.setDetails(entity.getDetails());
                vo.setCreateTime(formatTime(entity.getCreateTime()));
                vo.setUpdateTime(formatTime(entity.getUpdateTime()));

                voList.add(vo);
            }

            int total = gameMapper.count();

            int pages = (total + size - 1)/size;

            PageResult<DetailsVO> result = new PageResult<>();
            result.setList(voList);
            result.setTotal((long) total);
            result.setPage(page);
            result.setSize(size);
            result.setPages(pages);

            return result;
    }

    //时间戳转换
    private String formatTime(Integer timestamp) {
        if (timestamp == null) {
            return null;
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new Date(timestamp * 1000L));
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