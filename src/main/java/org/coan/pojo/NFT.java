package org.coan.pojo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@TableName("tb_nft")
public class NFT {

    @TableId("nft")
    private String nft;

    private long owner;

    private double amount;

}
