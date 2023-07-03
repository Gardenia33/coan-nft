package org.coan.pojo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@TableName("tb_game_property")
public class GameProperty {
    @TableId("name")
    private String name;

    private long owner;

    private double amount;
}
