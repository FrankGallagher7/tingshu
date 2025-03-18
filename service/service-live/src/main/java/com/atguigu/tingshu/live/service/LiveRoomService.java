package com.atguigu.tingshu.live.service;

import com.atguigu.tingshu.model.live.LiveRoom;
import com.atguigu.tingshu.vo.live.LiveRoomVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface LiveRoomService extends IService<LiveRoom> {
    /**
     * 获取用户当前正在直播的信息
     * @param userId
     * @return
     */
    LiveRoom getCurrentLive(Long userId);

    /**
     * 创建直播-提交直播表单
     * @param liveRoomVo
     * @param userId
     * @return
     */
    LiveRoom saveLiveRoom(LiveRoomVo liveRoomVo, Long userId);


    /**
     * 获取当前直播列表-用户获取所有直播列表
     * @return
     */
    List<LiveRoom> findLiveList();
}
