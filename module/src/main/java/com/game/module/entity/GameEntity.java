package com.game.module.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameEntity {

    private Long itemId;
    private Integer isDeleted;
    private String coverImage;
    private String wallImages;
    private String gameName;
    private String price;
    private String details;
    private Integer createTime;
    private Integer updateTime;

}
