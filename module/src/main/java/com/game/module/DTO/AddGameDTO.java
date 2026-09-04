package com.game.module.DTO;

import lombok.Data;

@Data
public class AddGameDTO {

    private Long itemId;
    private String coverImage;
    private String wallImages;
    private String gameName;
    private String price;
    private String details;
    private Integer isDeleted;

}
