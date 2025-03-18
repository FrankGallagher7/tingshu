package com.atguigu.tingshu.vo.live;

import lombok.Data;

@Data
public class TencentLiveAddressVo {
    //推流
    private String pushWebRtcUrl;
    private String pullFlvUrl;
    private String pullM3u8Url;
    private String pullRtmpUrl;
    //拉流
    private String pullWebRtcUrl;
}
