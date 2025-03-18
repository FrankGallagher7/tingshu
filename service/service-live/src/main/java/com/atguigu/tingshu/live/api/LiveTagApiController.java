package com.atguigu.tingshu.live.api;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.live.service.LiveTagService;
import com.atguigu.tingshu.model.live.LiveTag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/live/liveTag")
@SuppressWarnings({"unchecked", "rawtypes"})
public class LiveTagApiController {

	/**
	 * 获取全部直播标签-开直播间选择直播标签
	 * /api/live/liveTag/findAllLiveTag
	 */
	@Autowired
	private LiveTagService liveTagService;
	@Operation(summary = "获取全部直播标签")
	@GetMapping("findAllLiveTag")
	public Result<List<LiveTag>> findAllLiveTag() {
		List<LiveTag> list = liveTagService.list();
		return Result.ok(list);
	}

}

