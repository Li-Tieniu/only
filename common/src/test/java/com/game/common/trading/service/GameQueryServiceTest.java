package com.game.common.trading.service;

import com.game.common.exception.BusinessException;
import com.game.common.trading.cache.GameQueryCache;
import com.game.common.trading.entity.GameEntity;
import com.game.common.trading.mapper.GameMapper;
import com.game.common.trading.vo.DetailsVO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class GameQueryServiceTest {

    private final GameMapper gameMapper = mock(GameMapper.class);
    private final GameQueryCache gameQueryCache = mock(GameQueryCache.class);
    private final GameQueryService service = new GameQueryService(gameMapper, gameQueryCache);

    @Test
    void getDetailByIdShouldReturnExistingProduct() {
        GameEntity entity = new GameEntity();
        entity.setItemId(1001L);
        entity.setIsDeleted(0);
        entity.setGameName("王者荣耀");
        entity.setPrice("199.00");
        entity.setDetails("商品详情");
        entity.setWallImages(" first.jpg $ second.jpg ");
        when(gameMapper.selectById(1001L)).thenReturn(entity);

        DetailsVO result = service.getDetailById(1001L);

        assertThat(result.getItemId()).isEqualTo(1001L);
        assertThat(result.getGameName()).isEqualTo("王者荣耀");
        assertThat(result.getPrice()).isEqualTo("199.00");
        assertThat(result.getDetails()).isEqualTo("商品详情");
        assertThat(result.getWallImages()).containsExactly("first.jpg", "second.jpg");
        verify(gameMapper).selectById(1001L);
        verifyNoMoreInteractions(gameMapper);
        verifyNoInteractions(gameQueryCache);
    }

    @Test
    void getDetailByIdShouldRejectMissingProduct() {
        when(gameMapper.selectById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.getDetailById(999L));

        assertThat(exception.getCode()).isEqualTo(404);
        assertThat(exception.getMessage()).isEqualTo("商品不存在或已下架");
    }

    @Test
    void getDetailByIdShouldRejectDelistedProduct() {
        GameEntity entity = new GameEntity();
        entity.setItemId(1001L);
        entity.setIsDeleted(1);
        when(gameMapper.selectById(1001L)).thenReturn(entity);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.getDetailById(1001L));

        assertThat(exception.getCode()).isEqualTo(404);
        assertThat(exception.getMessage()).isEqualTo("商品不存在或已下架");
    }
}
