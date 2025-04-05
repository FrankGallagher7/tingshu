package com.atguigu.tingshu.user.service;

import com.atguigu.tingshu.model.user.UserListenProcess;
import com.atguigu.tingshu.vo.user.UserListenProcessListVo;
import com.atguigu.tingshu.vo.user.UserListenProcessVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.math.BigDecimal;
import java.util.Map;

public interface UserListenProcessService {

    /**
     * 获取当前用户收听声音播放进
     * @param userId
     * @param trackId
     * @return
     */
    BigDecimal getTrackBreakSecond(Long userId, Long trackId);

    /**
     * 更新当前用户收听声音播放进度
     * @param userId
     * @param userListenProcessVo
     */
    void updateListenProcess(Long userId, UserListenProcessVo userListenProcessVo);

    /**
     * 获取当前用户上次播放专辑声音记录--听专辑按钮
     * @param userId
     * @return
     */
    Map<String, Long> getLatelyTrack(Long userId);

    /**
     * 分页查询当前用户历史播放记录
     * @param userListenProcessListPage
     * @param userId
     * @return
     */
    Page<UserListenProcessListVo> findUserPage(Page<UserListenProcessListVo> userListenProcessListPage, Long userId);

    /**
     * 删除用户历史界面声音播放进度记录
     * @param id
     */
    void deleteUserListenProcess(String id);
}
