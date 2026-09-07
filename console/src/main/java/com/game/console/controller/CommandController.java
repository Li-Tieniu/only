package com.game.console.controller;

import com.game.module.DTO.AddDTO;
import com.game.console.service.CommandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/gametrading")
@RestController
public class CommandController {

    @Autowired
    private CommandService commandService;

    //新增数据
    @PostMapping("/addgame")
    public Long addGame(@RequestBody AddDTO dto){

        return commandService.addGame(dto);

    }

    //修改数据
    @PostMapping("/updategame")
    public int updateGame(@RequestBody AddDTO dto){

        return commandService.updateGame(dto);

    }

    //逻辑删除数据
    @PostMapping("/softdeletegame")
    public int softDeleteGame(@RequestBody AddDTO dto){

        return commandService.softDeleteGame(dto);

    }

    //物理删除数据
    @PostMapping("/harddeletegame")
    public int hardDeleteGame(@RequestBody AddDTO dto){

        return commandService.hardDeleteGame(dto);

    }
}
